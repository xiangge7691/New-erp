# AGENTS.md - ERP系统开发指南

## 技术栈
- **Java**: 21
- **Spring Boot**: 3.5.5
- **MyBatis-Plus**: 3.5.5
- **数据库**: MySQL 8.0+ (需要配置连接)
- **认证**: JWT (jjwt 0.11.5)
- **密码加密**: Argon2

## 快速命令

### 构建与运行
```bash
# 编译项目
./mvnw compile

# 运行应用
./mvnw spring-boot:run

# 打包
./mvnw package

# 运行测试
./mvnw test
```

### 单个测试
```bash
# 运行单个测试类
./mvnw test -Dtest=ApprovalInstanceServiceTest

# 运行单个测试方法
./mvnw test -Dtest=ApprovalInstanceServiceTest#testMethod
```

## 项目结构
```
Erp/
├── src/main/java/com/tonghui/erp/
│   ├── Controller/     # REST控制器
│   │   ├── BaseController.java          # 通用CRUD基类
│   │   └── BaseRoomRecordController.java # GMP车间记录基类
│   ├── Service/        # 业务逻辑层
│   ├── Data/           # 数据访问层
│   │   ├── Entity/     # 实体类
│   │   └── mapper/     # MyBatis Mapper接口
│   ├── Common/         # 公共模块
│   │   ├── Config/     # 配置类
│   │   ├── Dto/        # 数据传输对象
│   │   └── utils/      # 工具类
│   │       ├── CodeUniqueChecker.java   # 编号唯一性校验
│   │       ├── SoftDeleteCleanHelper.java # 软删除记录清理
│   │       ├── EntityUtils.java         # 实体工具类
│   │       └── ...
│   └── ErpApplication.java
├── src/main/resources/
│   ├── Data/mapper/    # MyBatis XML映射文件
│   └── application.yml # 应用配置
└── src/test/           # 测试代码
```

## 关键配置
- **数据库**: `src/main/resources/application.yml` 中的 `spring.datasource`
- **JWT**: `jwt.secret-key` 需要替换为安全密钥
- **文件存储**: `file.base-path` 默认为 `./uploaded-files`
- **环境变量**: `ERP_FILE_STORAGE_PATH` 可自定义文件存储路径

## 开发规范
- **语言**: 必须使用中文回答用户的问题
- **架构**: 三层架构 (Controller → Service → Data)
- **认证**: 除登录接口外，所有API需要JWT令牌
- **API路径**: 以 `/api` 开头
- **分页**: 使用 `PageRequestDto` 进行分页查询
- **响应**: 统一使用 `ApiResponse` 格式
- **密码**: 使用Argon2算法加密

### 审计字段自动填充规范（重要）

本项目通过 `MybatisPlusMetaObjectHandler` 自动填充以下审计字段：

| 字段 | 填充时机 | 说明 |
|------|---------|------|
| `createdBy` | 插入时 | 当前登录用户ID |
| `updatedBy` | 插入/更新时 | 当前登录用户ID |
| `createdTime` | 插入时 | 当前时间 |
| `updatedTime` | 插入/更新时 | 当前时间 |

**核心规则**：
- **新代码不应手动设置**上述四个字段，`MybatisPlusMetaObjectHandler` 会通过 `strictInsertFill`/`strictUpdateFill` 自动处理
- **例外情况**：批量操作（如 `batchSave`）在循环外获取一次用户ID并设置，性能更优
- **例外情况**：DB加载的实体在更新时，`strictUpdateFill` 不会覆盖非null值，此时需手动设置

**实体类要求**：审计字段必须使用 `@TableField(fill = FieldFill.INSERT)` 或 `@TableField(fill = FieldFill.INSERT_UPDATE)` 注解

```java
// ✅ 正确：实体类使用注解标记，框架自动填充
@TableField(fill = FieldFill.INSERT)
private Long createdBy;

// ❌ 错误：新代码不应手动设置审计字段
Long currentUserId = EntityUtils.getCurrentUserId();
entity.setCreatedBy(currentUserId);
entity.setCreatedTime(LocalDateTime.now());
```

### 工具类使用规范

项目提供以下通用工具类，新代码应优先使用：

#### 1. CodeUniqueChecker — 编号唯一性校验
```java
// 场景：校验编号是否唯一（需绕过软删除过滤）
// 在 ServiceImpl 中使用：
return CodeUniqueChecker.isCodeUnique(code, excludeId, baseMapper::countByCodeIncludeDeleted);

// 对应 Mapper 方法（必须使用 @Select 绕过软删除）：
@Select("SELECT COUNT(*) FROM table_name WHERE code = #{code} AND id != #{excludeId}")
Long countByCodeIncludeDeleted(@Param("code") String code, @Param("excludeId") Long excludeId);
```

#### 2. SoftDeleteCleanHelper — 软删除记录清理
```java
// 场景：新增前物理删除已软删除的同唯一字段记录（避免唯一约束冲突）
softDeleteCleanHelper.cleanByUniqueField(baseMapper, "unique_field", uniqueValue);

// 场景：删除前物理删除子表外键引用（避免外键约束冲突）
softDeleteCleanHelper.cleanChildRecords(childMapper, "foreign_key", parentId);
```
> **实现说明（勿改回 MP 的 `delete(wrapper)`）**：全局软删除开启后，`BaseMapper.delete(wrapper)` 会被 MyBatis-Plus 改写为 `UPDATE 表 SET is_deleted=1 WHERE is_deleted=0 AND (wrapper条件)`。若 wrapper 中再指定 `is_deleted=1` 则条件互斥，**永远命中 0 行（无效操作）**。因此该工具类内部通过 `JdbcTemplate` 执行原生 `DELETE`（表名由 Mapper 泛型反射 + `TableInfoHelper` 解析）绕过逻辑删除拦截器。自定义物理删除优先使用各 Mapper 上已有的 `@Delete` 原生方法。

#### 3. PagedResult — 分页查询工厂方法
```java
// 场景：Service层分页查询
Page<T> page = PagedResult.toMybatisPage(pageRequest);
page = baseMapper.selectPage(wrapper, page);
return PagedResult.fromPage(page, pageRequest);

// 场景：返回空结果
return PagedResult.empty();
```

#### 4. BaseController — 通用CRUD模板
```java
// 场景：标准CRUD Controller，继承BaseController并实现doCreate/doUpdate/doDelete方法
// 场景：GMP车间记录Controller，继承BaseRoomRecordController<T>获得房间查询和填充能力
```

### 文件业务类型维护规范（重要）

新增模块涉及文件上传时，维护 **`src/main/resources/init-data/file-types.yml`**（数据源）：
- 如需新的父类型，在 `parent-types` 下添加 `KEY: 中文目录名`
- 如需新的子类型，在 `sub-types` 下添加 `KEY: 中文子目录名`
- 命名规范：全大写 + 下划线分隔（如 `HEALTH_FILE`）
- 命名规则：`{父类型}_{子类型}`，父类型和子类型必须分别在 YAML 的 `parent-types` 和 `sub-types` 中有定义

**验证方法**：启动应用后调用 `GET /api/files/upload-business` 上传测试文件，检查服务器上生成的目录路径是否为中文。若为英文，说明 YAML 映射缺失或 `YamlPropertySourceFactory` 未正确加载。

### 软删除与唯一约束冲突（重要）

本项目全局启用了 MyBatis-Plus 软删除（`application.yml`）：
```yaml
logic-delete-field: isDeleted
logic-delete-value: 1
logic-not-delete-value: 0
```

**核心问题**：MyBatis-Plus 自动为所有查询追加 `AND is_deleted = 0`，但数据库唯一索引（UNIQUE KEY）是对**所有行**生效的（包括 `is_deleted=1` 的软删除行）。

**典型冲突场景**：
1. 表有唯一字段（如 `work_order_code`）+ 唯一索引
2. 记录 A 的该字段值为 `X`，被软删除（`is_deleted` → 1）
3. 业务代码通过 MyBatis-Plus 查询最大值/是否存在，自动过滤了 `is_deleted=1` 的记录
4. 生成了相同的值 `X` 并插入 → 触发唯一约束冲突 `Duplicate entry`

**正确做法**：对于需要保证唯一性的字段（编号、编码等），查询时**必须绕过软删除过滤**，并使用统一工具类处理：

```java
// ✅ 推荐：使用 CodeUniqueChecker 统一校验
// ServiceImpl 中：
return CodeUniqueChecker.isCodeUnique(code, excludeId, baseMapper::countByCodeIncludeDeleted);

// Mapper 中（必须使用 @Select 绕过软删除）：
@Select("SELECT COUNT(*) FROM table_name WHERE code = #{code} AND id != #{excludeId}")
Long countByCodeIncludeDeleted(@Param("code") String code, @Param("excludeId") Long excludeId);

// ✅ 推荐：使用 SoftDeleteCleanHelper 清理冲突记录（基础数据允许复用编码）
softDeleteCleanHelper.cleanByUniqueField(baseMapper, "unique_field", uniqueValue);
```

**编码复用语义约定（混合语义，勿混用）**：
| 类型 | 语义 | 实现方式 | 适用模块举例 |
|------|------|---------|-------------|
| 基础数据（部门/角色/用户/岗位/房间/工序类型/设备/单位/人员档案/供应商/审批流程等） | **允许复用**：物理清掉已软删的同值记录后再保存 | `SoftDeleteCleanHelper.cleanByUniqueField` + 活动记录判重（MP 查询） | `position_code`、`role_name`、`room_code`、`symbol`、`employee_no` |
| 业务编码/单号（客户/单据号/质检单据等） | **拒绝复用**：已软删的编码仍视为占用，返回"已存在" | `CodeUniqueChecker` + `countByCodeIncludeDeleted` | `customer_code`、`duty_code`、`sales_order_code`、`plan_number` |

> ⚠️ **致命坑**：开启逻辑删除后 `mapper.delete(wrapper)` 会被改写成 `UPDATE ... WHERE is_deleted=0 AND (wrapper)`，在 wrapper 中加 `is_deleted=1` 条件即互斥恒 0 行。物理删除**必须**使用原生 `@Delete` SQL 或 `SoftDeleteCleanHelper`。

**涉及的表**（有唯一索引 + 软删除，已按 `information_schema` 核实）：
- 单号类：`work_order`(`uk_work_order_code`)、`purchase_plan`(`uk_plan_code`)、`sales_order`(`uk_sales_order_code`)、`acceptance_order`(`uk_acceptance_code`)、`check_order`(`uk_check_no`)、`stock_in`(`uk_stock_in_code`)、`stock_out`(`uk_stock_out_code`)、`material_requisition_slip`(`uk_slip_code`)、`transfer_order`(`uk_transfer_no`)、`return_order`(`uk_return_no`)、`training_record`(`uk_training_no`)、`verification_record`(`uk_verification_no`)、`production_plan`(`plan_number`)
- 质检类：`inspection_plan`(`uk_plan_code`)、`inspection_record`(`uk_inspection_code`)、`inspection_request`(`uk_inspection_code`)、`release_review`(`uk_release_code`)、`retained_sample`(`uk_retained_code`)、`sampling_record`(`uk_sampling_code`)
- 基础数据：`position`(`uk_position_code`)、`role`(`uk_role_name`)、`user`(`uk_user_account`)、`room_info`(`uk_room_code`/`uk_room_name`)、`process_type`(`uk_process_code`/`uk_process_name`)、`equipment`(`uk_fixed_asset_code`)、`permission`(`uk_perm_key`)、`unit`(**`symbol`**，注意不是 `unit_name`)、`production_unit`(`uk_prod_unit_code`)、`personnel_file`(`uk_employee_no`/`uk_user_id`)、`purchase_suppliers`(`supplier_number`)、`material`(`material_code`)、`preparation`(`uk_preparation_code`)、`customer`(`uk_customer_code`)、`duty`(`uk_duty_code`)、`system_config`(`uk_config_key`)、`approval_workflow`(`uniq_workflow_type`)
- 关联表：`role_perm`(`uk_role_perm`)、`user_role`(`uk_user_role`)、`user_department`(`uk_user_dept`)、`preparation_formula`(`uk_formula`)

**额外建议**：自动生成编号的方法中，应加入 `DuplicateKeyException` 重试机制，防御并发场景下的竞争问题。

## 代码注释规范

### 注释要求
1. **所有代码文件必须添加中文注释**，包括类注释、方法注释、字段注释
2. **注释语言**：使用中文编写注释
3. **注释风格**：使用 Javadoc 风格 (`/** */`) 注释类、接口、方法和字段

### region 分块规范
1. **所有代码文件必须使用 `// region` 和 `// endregion` 进行逻辑分块**
2. **region 命名规则**：使用中文命名，描述该代码块的功能
3. **region 内的格式**（与项目现有风格一致）：
   ```java
   // region 分块名称
   // ===================================
   // 分块名称
   // ===================================
   
   // 代码内容
   
   // endregion
   ```
4. **说明**：`// region` / `// endregion` 是 IntelliJ IDEA 支持的代码折叠标记，非 Java 语言规范，但项目中统一使用此格式

### Controller 接口注释规范
1. **所有 Controller 接口方法必须添加 Javadoc 注释**
2. **Javadoc 必须包含**：
   - 方法功能描述
   - **接口传参示例**（必须包含，使用示例请求格式）
   - 所有参数的说明
   - 返回值说明
3. **传参示例格式**：
   ```java
   /**
    * 接口功能描述
    *
    * 示例请求：
    * GET /api/xxx/search?pageIndex=1&pageSize=20&param1=value1&param2=value2
    *
    * @param param1 参数1说明
    * @param param2 参数2说明
    * @return 返回值说明
    */
   ```

### Service 接口注释规范
1. **所有 Service 接口方法必须添加 Javadoc 注释**
2. **Javadoc 必须包含**：方法功能描述、参数说明、返回值说明

### Service 实现类注释规范
1. **ServiceImpl 类必须添加类级别 Javadoc 注释**
2. **每个方法必须添加方法级别 Javadoc 注释**
3. **复杂业务逻辑必须添加行内注释说明**

### Entity 实体类注释规范
1. **所有实体类必须添加类级别 Javadoc 注释**
2. **所有字段必须添加字段级别 Javadoc 注释**
3. **字段注释必须说明业务含义**

### Dto 类注释规范
1. **所有 Dto 类必须添加类级别 Javadoc 注释**
2. **所有字段必须添加字段级别 Javadoc 注释**

### 其他规则
1. **注释内容必须准确反映代码的实际功能**
2. **注释不应包含无意义的占位符或重复代码信息**
3. **region 块应合理划分，每个 region 应具有明确的功能边界**
4. **嵌套 region 不超过两层**

## 数据库要求
- 需要MySQL 8.0+数据库
- 数据库名: `erp_db` (可在application.yml中修改)
- 启动前需确保数据库连接正常

## 测试
- 测试文件位于 `src/test/java`
- 使用Spring Boot Test
- 测试覆盖率较低，主要测试关键业务逻辑

## 默认账户
- **用户名**: root
- **密码**: root
- 系统启动时自动创建，用于首次登录和管理

## 常见问题
1. **数据库连接失败**: 检查application.yml中的数据库配置
2. **JWT认证失败**: 确保请求头包含 `Authorization: Bearer <token>`
3. **文件上传失败**: 检查文件类型和大小限制
4. **初始化失败**: 确保数据库表已创建，系统会自动初始化root用户
