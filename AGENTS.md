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
│   ├── Service/        # 业务逻辑层
│   ├── Data/           # 数据访问层
│   │   ├── Entity/     # 实体类
│   │   └── mapper/     # MyBatis Mapper接口
│   ├── Common/         # 公共模块
│   │   ├── Config/     # 配置类
│   │   ├── Dto/        # 数据传输对象
│   │   └── utils/      # 工具类
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

## 相关文档
- [项目概述](docs/00-项目概述.md)
- [Service接口文档](SERVICE_DOC.md)
- [库存预警后端](库存预警后端.md)