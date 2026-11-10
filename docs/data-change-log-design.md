# 字段级数据变更日志设计方案

> 日期：2026-11-10  
> 目标：记录每次业务操作中**每条数据修改前后的字段值**，并覆盖 A 表修改后级联修改 B 表的场景。  
> 关联模块：操作日志（`sys_operation_log`）、登录日志（`sys_login_log`）。

---

## 0. 结论先行

| 事项 | 推荐方案 |
|------|----------|
| 采集方式 | **MyBatis Executor 拦截器 + 请求级变更 Trace**，自动抓取 Mapper 层的增删改，业务代码基本零改动 |
| 级联修改 | 同一业务请求内，A 表、B 表的每次 Mapper 写入都进入同一个 Trace，自动全部记录 |
| 存储模型 | 新增 `sys_data_change_log` 子表，**一条变更记录一行**，字段差异存 JSON |
| 主/关联标记 | 通过 `@OperationLog` 的 `businessId` 与业务类型匹配主记录，其余标为“关联变更” |
| 操作日志关联 | `sys_operation_log` 增加 `trace_id`、`change_count`，变更子表通过 `trace_id` / `operation_log_id` 关联 |
| 前后值内容 | `before_json`、`after_json` 保存整行快照（脱敏+截断），`changed_fields` 保存字段级差异数组 |
| 字段中文名 | 从 `information_schema.COLUMNS.COLUMN_COMMENT` 读取 DDL 注释并缓存，前端直接显示中文 |
| 写入方式 | 变更快照在请求线程采集，随操作日志异步落库；日志失败不影响业务 |
| 保留策略 | 变更日志默认保留 180 天，可配置；随操作日志定时清理 |

**推荐理由**：项目有 165 处 `updateById`、52 处 `update(wrapper)`、52 处 `removeById`，级联修改分散在 Service 内部。逐个改业务代码成本高且容易漏；DB 触发器拿不到操作人、请求 Trace，也无法脱敏。MyBatis 拦截器能覆盖绝大多数 Mapper 写入，并且天然覆盖级联修改。

---

## 1. 现状分析

### 1.1 更新方式统计

| 写法 | 数量 | 拦截器是否覆盖 |
|------|------|----------------|
| `updateById(entity)` | 165 | ✅ 可直接取实体主键 |
| `update(entity, wrapper)` / `update(wrapper)` / `update(null, wrapper)` | 52 | ✅ 先按 wrapper 查受影响行 ID，再按 ID 抓前后快照 |
| `removeById(id)` / `deleteById(id)` | 52 | ✅ 主键可从参数或 Mapper 泛型解析 |
| `saveBatch` / `updateBatchById` | 8 | ⚠️ MyBatis 批处理可能绕过 `Executor.update`，一期先按“批量操作摘要”处理，二期补精确采集 |
| `@Update` 自定义 SQL | 3 | ⚠️ 主要在文件模块；文件模块已有 `file_operation_log`，一期排除，二期按需显式记录 |
| XML `<update>` | 0 | — |
| `@Transactional` 业务方法 | 152 | ✅ 拦截器运行在同一事务内，能看到未提交数据 |

### 1.2 典型级联场景

`ProductionPlanServiceImpl.voidProductionPlan`：

1. 查询计划下所有未作废工单；
2. 循环执行 `workOrderMapper.update(null, wrapper)`，把每个工单状态改为“作废”；
3. 最后 `this.update(wrapper)` 把生产计划状态改为“作废”。

拦截器方案会把 1 条生产计划 + N 条工单的每次变化都记录到同一个 Trace 中，并标明哪条是主记录、哪些是级联记录。

### 1.3 现有基础

- 操作日志已有 `@OperationLog` 注解和切面，254 个写接口已标注；
- 操作日志上下文 `OperationLogContext` 已在请求线程组装，异步写入；
- 已有脱敏工具 `LogParamUtils`、IP 归属地、日志线程池、定时清理；
- 表结构注释齐全，可用于生成字段中文名。

---

## 2. 方案对比

### 方案 A：MyBatis 拦截器 + 变更 Trace（推荐）

**做法**：在 `Executor.update` 上拦截 Mapper 的 insert/update/delete，执行前后按主键查询整行，计算字段差异，放入当前请求的 Trace；业务方法结束后与操作日志一起落库。

**优点**
- 业务代码基本零改动，自动覆盖级联修改；
- 能拿到操作人、请求地址、Trace，和操作日志天然关联；
- 能统一脱敏、截断、排除审计字段；
- 可覆盖 `updateById`、wrapper 更新、`removeById`。

**缺点**
- 每次写操作多 1～2 次按主键查询，写入性能有少量开销；
- 批处理、自定义 SQL 需要额外适配；
- 拦截器属于基础组件，需要充分测试，避免影响业务。

### 方案 B：Service 层显式记录

**做法**：在每个 Service 更新方法中先查旧值、更新后再查新值，调用 `ChangeLogRecorder` 记录。

**优点**：精确、可控，能表达“A 导致 B”的因果关系。  
**缺点**：需要改 200+ 处调用点，级联修改容易漏；后续新增业务必须记得调用，维护成本高。  
**结论**：不作为主方案，仅作为批处理/自定义 SQL 的补充。

### 方案 C：数据库触发器

**做法**：MySQL `AFTER INSERT/UPDATE/DELETE` 触发器写入审计表。

**优点**：覆盖所有 SQL，包括直接 JDBC。  
**缺点**：拿不到应用层操作人/Trace；需要为每个业务请求设置会话变量；无法统一脱敏；不同环境触发器维护复杂。  
**结论**：不采用。

---

## 3. 数据模型

### 3.1 新增：数据变更日志 `sys_data_change_log`

一条记录代表“某次操作中，某张表的某一行发生了一次变化”。

```sql
CREATE TABLE IF NOT EXISTS `sys_data_change_log` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '变更日志ID',
  `trace_id`         VARCHAR(64)  NOT NULL COMMENT '请求变更TraceID',
  `operation_log_id` BIGINT       DEFAULT NULL COMMENT '关联操作日志ID',
  `table_name`       VARCHAR(64)  NOT NULL COMMENT '表名',
  `entity_class`     VARCHAR(255) DEFAULT NULL COMMENT '实体类全名',
  `business_id`      VARCHAR(64)  DEFAULT NULL COMMENT '业务记录主键值',
  `change_type`      VARCHAR(10)  NOT NULL COMMENT '变更类型：INSERT/UPDATE/DELETE',
  `change_source`    VARCHAR(10)  NOT NULL DEFAULT 'RELATED' COMMENT '来源：PRIMARY主记录/RELATED关联变更',
  `before_json`      MEDIUMTEXT   DEFAULT NULL COMMENT '修改前整行快照（脱敏、截断）',
  `after_json`       MEDIUMTEXT   DEFAULT NULL COMMENT '修改后整行快照（脱敏、截断）',
  `changed_fields`   TEXT         DEFAULT NULL COMMENT '字段差异JSON：[{field,label,before,after}]',
  `field_count`      INT          NOT NULL DEFAULT 0 COMMENT '发生变化的字段数',
  `user_id`          BIGINT       DEFAULT NULL COMMENT '操作人ID',
  `user_account`     VARCHAR(64)  DEFAULT NULL COMMENT '操作人账号',
  `user_name`        VARCHAR(64)  DEFAULT NULL COMMENT '操作人姓名',
  `created_time`     DATETIME     NOT NULL COMMENT '变更时间',
  PRIMARY KEY (`id`),
  KEY `idx_change_trace` (`trace_id`),
  KEY `idx_change_operation` (`operation_log_id`),
  KEY `idx_change_table_biz` (`table_name`, `business_id`),
  KEY `idx_change_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据变更日志表';
```

字段说明：

| 字段 | 说明 |
|------|------|
| `trace_id` | 一次 Controller 写操作生成一个 UUID，同一请求内的主记录和级联记录共用 |
| `operation_log_id` | 异步写入操作日志后回填，用于详情页关联 |
| `table_name` | 物理表名，来自 MyBatis-Plus `TableInfo` |
| `entity_class` | 实体类全名，便于排查 |
| `business_id` | 被修改行的主键值（字符串化） |
| `change_type` | `INSERT` / `UPDATE` / `DELETE`；软删除 `is_deleted 0→1` 归类为 `DELETE` |
| `change_source` | `PRIMARY` 表示注解指定的主业务对象；`RELATED` 表示同一请求内的级联修改 |
| `before_json` / `after_json` | 整行快照，敏感字段脱敏、长文本截断；DELETE 只有 before，INSERT 只有 after |
| `changed_fields` | 只包含实际变化的字段，前端按“字段 / 修改前 / 修改后”三列展示 |

`changed_fields` 示例：

```json
[
  {"field": "status", "label": "订单状态", "before": "待采购", "after": "运输中"},
  {"field": "order_time", "label": "下单时间", "before": null, "after": "2026-11-10 09:30:00"}
]
```

### 3.2 扩展：操作日志 `sys_operation_log`

```sql
ALTER TABLE sys_operation_log
  ADD COLUMN trace_id VARCHAR(64) DEFAULT NULL COMMENT '变更TraceID',
  ADD COLUMN change_count INT NOT NULL DEFAULT 0 COMMENT '数据变更记录数',
  ADD COLUMN change_truncated TINYINT NOT NULL DEFAULT 0 COMMENT '数据变更是否截断：0否/1是';
```

- `trace_id`：与 `sys_data_change_log.trace_id` 关联；
- `change_count`：列表页可直接显示“本次修改了 3 条数据”，详情页再按需加载明细；
- `change_truncated`：单次操作变更超过上限时标记为 1，提示前端“变更记录已截断”。

### 3.3 配置项

`log.change.enabled`、`max-*` 通过 `application.yml` 配置（应用启动时生效）；保留天数放在 `system_config`，便于运维调整：

| 配置位置 | 配置键 | 默认值 | 说明 |
|----------|--------|--------|------|
| application.yml | `log.change.enabled` | true | 数据变更日志总开关 |
| application.yml | `log.change.max-changes` | 100 | 单次操作最多记录多少条变更 |
| application.yml | `log.change.max-value-length` | 2000 | 单个字段值最大字符数 |
| application.yml | `log.change.max-rows` | 1000 | wrapper 批量更新时最多抓取多少行快照 |
| system_config | `log.change.retention.days` | 180 | 变更日志保留天数 |

> 变更日志开关不读数据库，是为了避免 MyBatis 拦截器依赖 `system_config` 查询而形成循环依赖；如需运行时开关，可通过重启或后续增加独立缓存配置服务实现。

---

## 4. 采集机制

### 4.1 总体流程

```
Controller 写接口（@OperationLog）
  └─ 切面：开始 ChangeTrace(traceId)
        └─ Service 业务方法（@Transactional）
              ├─ Mapper.updateById(A)   ─┐
              ├─ Mapper.update(B, w)    ─┼─ MyBatis 拦截器：
              └─ Mapper.removeById(C)   ─┘   执行前查旧行、执行后查新行、算差异
                                             └─ 写入当前线程 ChangeTrace
        └─ 事务提交
  └─ 切面：结束 ChangeTrace，拿到全部变更
        └─ 组装 OperationLogContext（traceId、changes）
              └─ 异步：insert sys_operation_log → 得到ID
                    └─ batch insert sys_data_change_log（回填 operation_log_id）
```

### 4.2 ChangeTrace 上下文

新增 `DataChangeTraceContext`（ThreadLocal）：

```java
public final class DataChangeTraceContext {
    private static final ThreadLocal<Trace> CURRENT = new ThreadLocal<>();

    public static Trace begin();          // 切面进入时创建
    public static Trace current();        // 拦截器读取，没有则返回 null
    public static List<ChangeRecord> end(); // 切面退出时取出并清理
}
```

`Trace` 包含：

- `traceId`：UUID；
- `userId` / `userAccount` / `userName`：从请求上下文同步取；
- `List<ChangeRecord>`：按执行顺序记录；
- `startTime`；
- `truncated`：是否因为超过条数上限被截断。

**注意**：业务方法都是同步执行，项目中没有数据写入型 `@Async` 方法，ThreadLocal 可安全使用。若后续出现异步写数据，需要在异步任务中显式传递 Trace。

### 4.3 MyBatis 拦截器

新增 `DataChangeInterceptor implements Interceptor`，拦截：

```java
@Intercepts({
    @Signature(type = Executor.class, method = "update",
               args = {MappedStatement.class, Object.class})
})
```

处理逻辑：

1. **无 Trace 或开关关闭** → 直接放行，零额外开销。
2. **解析 MappedStatement**：
   - 语句 ID 形如 `com.tonghui.erp.Data.mapper.XxxMapper.updateById`；
   - 从 Mapper 接口泛型解析实体类（`GenericTypeResolver.resolveTypeArgument(mapperClass, BaseMapper.class)`）；
   - 用 `TableInfoHelper.getTableInfo(entityClass)` 获取表名、主键列、主键属性；
   - 命中排除表（日志表、文件日志表等）→ 放行。
3. **按语句类型采集**：

| Mapper 方法 | 执行前 | 执行后 | 记录 |
|-------------|--------|--------|------|
| `insert` / `save` | — | 按生成的主键查整行 | INSERT |
| `updateById` | 按实体主键查整行 | 按主键查整行 | UPDATE |
| `update(entity, wrapper)` / `update(wrapper)` | 用同一 wrapper 执行 `selectList` 取受影响行 ID，再按 ID 查整行 | 按这些 ID 查整行 | UPDATE |
| `deleteById` | 按主键查整行 | 按主键查整行（软删后仍能查到） | DELETE 或 UPDATE（恢复） |
| `delete(wrapper)` | 用 wrapper 查受影响行 ID 并查整行 | 按 ID 查（硬删为空） | DELETE |
| 自定义 SQL | 方法标注 `@DataChangeAudit(table, whereColumn, param, primaryKeyColumn, changeType)`，按 WHERE 条件先查受影响行 | 按主键查整行 | UPDATE/DELETE |

> **受影响行数为 0 时直接跳过**：`invocation.proceed()` 返回的影响行数为 0（主键不存在、乐观锁失败、状态条件不匹配等），说明数据没有实际变化，不记录变更日志。

4. **wrapper 更新先取 ID，再按 ID 复查**：
   - 不能更新后直接用原 wrapper 查，因为 wrapper 的 WHERE 字段可能被本次修改（例如 `WHERE status='待采购' SET status='运输中'`），更新后条件不再匹配；
   - 必须先 `selectList` 收集 ID，再按主键逐条复查。

5. **软删除识别**：
   - 全局逻辑删除开启后，`removeById` 实际是 `UPDATE ... SET is_deleted=1`；
   - 如果 before `is_deleted=0`、after `is_deleted=1` → `change_type=DELETE`；
   - 如果 before `is_deleted=1`、after `is_deleted=0` → `change_type=UPDATE`，差异中体现恢复。

6. **快照查询方式**：
   - 用 `JdbcTemplate` 按表名 + 主键列执行 `SELECT * FROM table WHERE key = ?`；
   - 与业务共用同一事务连接，能看到未提交数据；
   - 表名/列名来自 `TableInfoHelper`，不拼接用户输入，并做标识符校验；
   - 不用 Mapper 查询，避免拦截器递归和逻辑删除过滤。

7. **批量与自定义 SQL**：
   - `saveBatch` / `updateBatchById` 可能绕过 `Executor.update`，一期在操作日志中记录“批量操作摘要”，二期通过 `@RecordChange` 显式补充；
   - 3 个 `@Update` 自定义方法中，文件模块排除；`StockMapper.softDeleteByProdUnitId` 这类关键自定义 SQL 通过显式 `ChangeLogRecorder.record...` 补充。

### 4.4 级联修改如何记录

拦截器不区分“主表/子表”，它在一次 Trace 内记录**所有** Mapper 写入。因此：

- A 表更新后，Service 内部循环更新 B 表；
- B 表的每次 `update` 都会进入同一个 Trace；
- 最终 `sys_data_change_log` 中会有 A 的变更 + 每条 B 的变更；
- `change_source` 标记：
  - 优先匹配 `@OperationLog` 的 `businessId` 与 `businessType`（业务类型归一化后与实体类名比对）；
  - 匹配到的主记录标 `PRIMARY`，其余标 `RELATED`；
  - 匹配不到时，第一条标 `PRIMARY`，其余 `RELATED`。

> 说明：自动方案能证明“这些变更是同一次请求内发生的”，但无法严格证明“B 是因为 A 才变的”。如果后续需要精确因果链，可在关键 Service 中增加显式 `ChangeTrace.markCausedBy(...)` 标记，作为二期增强；一期先用执行顺序 + 主/关联标记满足审计需要。

---

## 5. 字段差异与脱敏

### 5.1 差异计算规则

- 只比较实际发生变化的列；
- 排除技术字段，不进入 `changed_fields`：
  - `created_by`、`created_time`
  - `updated_by`、`updated_time`
  - `version`
- `is_deleted` 不展示为普通字段，但用于识别删除/恢复；
- 空字符串、`null`、`0` 按数据库实际值比较，不归一化；
- 数值类型按字符串比较，避免 `BigDecimal` 精度展示问题；
- 日期时间统一格式化为 `yyyy-MM-dd HH:mm:ss`；
- 字节/二进制字段显示 `[二进制数据]`，不记录内容；
- 单字段值超过 `log.change.max-value-length` 截断并追加 `...(已截断)`。

### 5.2 敏感字段脱敏

复用登录/操作日志的敏感字段规则，以下字段（忽略大小写，字段名包含即脱敏）值统一替换为 `******`：

`password`、`passwd`、`token`、`authorization`、`secret`、`privatekey`、`credential`

例如用户表修改密码：

```json
{"field": "password", "label": "密码", "before": "******", "after": "******"}
```

### 5.3 字段中文名

启动后首次访问某张表时，查询并缓存：

```sql
SELECT COLUMN_NAME, COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?
```

- `label` 取 `COLUMN_COMMENT`，为空时回退到列名；
- 缓存放在 `ConcurrentHashMap<String, Map<String,String>>`，表结构变更后重启生效；
- 这样前端无需维护字段字典。

### 5.4 整行快照

- `before_json` / `after_json` 保存脱敏、截断后的整行；
- 整行 JSON 超过 64KB 时截断并标记 `truncated`（可在 JSON 中加 `_truncated: true`）；
- INSERT 只存 `after_json`，`changed_fields` 为所有非空业务字段（before=null，after=值）；
- DELETE 只存 `before_json`，`changed_fields` 为所有非空业务字段（before=值，after=null）；
- 列表页不返回整行 JSON，只在详情接口返回。

---

## 6. 写入与事务

### 6.1 与操作日志的关联

- `OperationLogContext` 增加 `traceId`、`List<DataChangeRecord> changes`；
- 切面在 `proceed()` 前 `DataChangeTraceContext.begin()`，在 `finally` 中 `end()` 并把变更放入上下文；
- 异步写法：

```java
operationLogService.recordAsync(context)
  ├─ insert sys_operation_log（含 trace_id、change_count）
  ├─ 取得 operationLogId
  └─ 逐条 insert sys_data_change_log（回填 operation_log_id）
```

- 变更采集是同步的，异步线程只负责落库，不存在上下文丢失问题。

### 6.2 成功/失败处理

- 操作返回成功（`ApiResponse.code = 200`）才持久化变更日志；
- 操作抛异常或返回业务错误时，变更通常已回滚，不写入变更日志；
- 若业务出现“部分提交后返回错误”的异常情况，以操作结果为准，避免出现“数据库没改但日志显示改了”；
- 日志写入失败只打应用日志，不影响业务。

### 6.3 性能控制

- 每次更新最多增加 2 次按主键查询 + 1 次 JSON 序列化；
- wrapper 批量更新先 `selectList` 收集 ID，超过 `log.change.max-rows` 时：
  - 不再逐行抓快照；
  - 记录一条 `change_type=UPDATE`、`changed_fields=[]` 的摘要行，`after_json` 写入 `{"_summary": "批量更新 N 行，已超过快照上限"}`；
- 单次操作变更条数超过 `log.change.max-changes` 时停止采集，Trace 标记 `truncated`，操作日志 `change_count` 仍记录实际采集数；
- 可通过 `log.change.enabled=false` 整体关闭。

### 6.4 排除表

以下表不记录变更，避免日志自我循环和噪音：

- `sys_login_log`
- `sys_operation_log`
- `sys_data_change_log`
- `file_operation_log`
- `plan_status_log`
- `acceptance_resend_log`

文件元数据表 `file_info` 的变更由文件模块自己的 `file_operation_log` 覆盖，默认排除；如后续需要，可开启。

---

## 7. 查询接口与前端

### 7.1 操作日志详情关联变更

```http
GET /api/system/operation-log/{id}/changes
```

返回该操作产生的全部变更：

```json
{
  "code": 200,
  "data": [
    {
      "id": 501,
      "traceId": "8f3c...",
      "tableName": "purchase_orders",
      "businessId": "1024",
      "changeType": "UPDATE",
      "changeSource": "PRIMARY",
      "fieldCount": 2,
      "changedFields": [
        {"field": "status", "label": "订单状态", "before": "待采购", "after": "运输中"},
        {"field": "order_time", "label": "下单时间", "before": null, "after": "2026-11-10 09:30:00"}
      ],
      "createdTime": "2026-11-10 09:30:00"
    },
    {
      "id": 502,
      "tableName": "acceptance_order",
      "businessId": "889",
      "changeType": "INSERT",
      "changeSource": "RELATED",
      "fieldCount": 8,
      "createdTime": "2026-11-10 09:30:00"
    }
  ]
}
```

`GET /api/system/operation-log/{id}` 的详情 DTO 增加 `traceId`、`changeCount`。

### 7.2 数据变更日志查询

```http
GET /api/system/data-change-log/search?pageIndex=0&pageSize=20
    &operationLogId=1001
    &tableName=purchase_orders
    &businessId=1024
    &changeType=UPDATE
    &userAccount=root
    &startTime=2026-11-01
    &endTime=2026-11-10
```

```http
GET /api/system/data-change-log/{id}
```

详情返回完整 `beforeJson`、`afterJson`、`changedFields`，用于“查看完整快照”。

### 7.3 前端

**操作日志详情抽屉**增加“数据变更”页签：

- 按表分组，显示“表名 / 记录ID / 变更类型 / 字段数 / 主记录或关联变更”；
- 每行展开后展示：字段（中文名）、修改前、修改后；
- 关联变更用浅色标签标记；
- 支持查看整行前后 JSON。

**业务详情页**（可选）：

- 在客户、采购订单、工单等详情页增加“变更历史”按钮；
- 调用 `GET /api/system/data-change-log/search?tableName=xxx&businessId=xxx`；
- 按时间倒序展示该记录的完整变更历史。

---

## 8. 清理与保留

- `sys_data_change_log` 默认保留 180 天，由 `log.change.retention.days` 配置；
- `LogCleanupTask` 每天 03:10 增加一步：按 `created_time` 分批物理删除；
- 操作日志删除时，不强制级联删除变更日志，由各自保留策略控制；
- 变更日志量约为操作日志的 1～5 倍，按每天 1000 次写操作估算，一年约 100～300 万行、3～10GB，建议保留 180 天并监控表大小。

---

## 9. 配置项

`application.yml`：

```yaml
log:
  change:
    enabled: true
    retention-days: 180
    max-changes: 100
    max-rows: 1000
    max-value-length: 2000
    exclude-tables:
      - sys_login_log
      - sys_operation_log
      - sys_data_change_log
      - file_operation_log
      - file_info
      - business_file
      - plan_status_log
      - acceptance_resend_log
```

`log.change.enabled` 从 YAML 读取；`log.change.retention.days` 从数据库 `system_config` 读取，数据库值优先于 YAML 默认值。

---

## 10. 实施步骤

| 阶段 | 内容 | 预计 |
|------|------|------|
| 1 | 建表脚本、实体/Mapper/DTO、配置项 | 0.5 天 |
| 2 | `DataChangeTraceContext` + 操作日志切面接入 | 1 天 |
| 3 | MyBatis 拦截器：insert/updateById/update(wrapper)/deleteById/delete(wrapper) | 2～3 天 |
| 4 | 字段差异、脱敏、截断、字段注释缓存、软删除识别 | 1～2 天 |
| 5 | 操作日志详情关联、变更日志查询接口 | 1 天 |
| 6 | 清理任务、配置、异常与性能保护 | 0.5 天 |
| 7 | 集成测试：单表更新、级联更新、wrapper 更新、软删除、批量上限、脱敏 | 2 天 |
| 8 | 前端操作日志详情“数据变更”页签（可选变更历史页） | 2 天 |

**后端约 6～8 个工作日，前端约 2 天。**

---

## 11. 风险与边界

1. **性能**：每次更新多 1～2 次主键查询；高频接口需观察。可通过开关关闭或按表排除。
2. **wrapper 批量更新**：先 `selectList` 取 ID，受影响行数超过上限时只记录摘要，避免大事务内再查大量数据。
3. **批处理**：`saveBatch`/`updateBatchById` 一期可能采集不到，二期补显式记录。
4. **自定义 SQL**：通过 `@DataChangeAudit` 显式声明表、WHERE 列、参数名和主键列后即可采集；`StockMapper.softDeleteByProdUnitId` / `physicalDeleteByProdUnitId` 已标注，文件模块的 `file_info` 已排除。
5. **ThreadLocal**：仅覆盖同步业务方法；当前项目没有数据写入型 `@Async`，如后续出现需显式传递 Trace。
6. **因果关系**：自动方案记录“同一请求内的全部变更 + 执行顺序”，不能严格证明 A 导致 B；需要精确因果链时用显式标记增强。
7. **字段中文名**：依赖 DDL 的 `COLUMN_COMMENT`，后期改表需重启刷新缓存。
8. **敏感数据**：密码、token 等必须脱敏；整行快照只对授权角色开放。
9. **日志量**：变更日志增长快，需配合保留天数、监控和定期归档。
10. **事务回滚**：只记录成功操作的变更，避免日志与实际提交数据不一致。

---

## 12. 待确认项

| # | 决策点 | 推荐 |
|---|--------|------|
| 1 | 采集方式 | MyBatis 拦截器 + Trace，自动覆盖级联 |
| 2 | 存储粒度 | 一条变更记录一行 + `changed_fields` JSON |
| 3 | 整行快照 | 记录 before/after 整行（脱敏、截断），详情接口返回 |
| 4 | 主/关联标记 | 按 `businessId`/业务类型匹配，其余标关联 |
| 5 | 保留天数 | 变更日志 180 天，可配置 |
| 6 | 批处理/自定义 SQL | 一期先覆盖常规 Mapper 写，二期补精确采集 |
| 7 | 前端范围 | 一期只做操作日志详情“数据变更”页签；业务详情“变更历史”二期 |
| 8 | 是否记录创建/删除 | 记录：INSERT 存 after，DELETE 存 before |

---

## 13. 实现状态（2026-11-10）

**已完成（后端）**

- 建表与迁移：`create_data_change_log_table.sql`、`add_operation_log_change_fields.sql`；
- 实体/Mapper/DTO：`SysDataChangeLog`、`SysDataChangeLogMapper`、`DataChangeLogDto`、`DataChangeLogDetailDto`、`FieldChange`；
- 采集：`DataChangeTraceContext` + `DataChangeInterceptor`，覆盖 `insert`、`updateById`、`update(wrapper)`、`deleteById`、`delete(wrapper)`、`deleteBatchIds`；自定义 SQL 通过 `@DataChangeAudit` 声明采集规则（`StockMapper` 的软删除/物理删除已标注）；
- 差异：`DataChangeDiffUtils` 支持字段差异、审计字段排除、软删除识别、敏感字段脱敏、长值截断、列注释中文名；
- 联动：`OperationLogAspect` 在请求内开启/结束 Trace，按 `businessId`/业务类型标记 `PRIMARY`/`RELATED`；失败操作不落变更日志；
- 写入：操作日志增加 `trace_id`、`change_count`、`change_truncated`，异步写入后回填 `operation_log_id`；
- 查询：`GET /api/system/operation-log/{id}/changes`、`GET /api/system/data-change-log/search`、`GET /api/system/data-change-log/{id}`；
- 清理：`LogCleanupTask` 按 `log.change.retention.days` 分批物理删除；
- 测试：`DataChangeDiffUtilsTest`、`DataChangeInterceptorIntegrationTest`、`LogModuleIntegrationTest` 覆盖字段差异、脱敏、软删除、wrapper 更新/删除、级联多记录、异步落库和主记录标记。

**已知限制（后续迭代）**

- `saveBatch` / `updateBatchById` 可能绕过 `Executor.update`，一期未精确采集；
- 自定义 `@Update` SQL 需要按 `@DataChangeAudit` 声明采集规则；文件模块已排除，其他新增自定义 SQL 需补注解；
- 自动方案记录“同一请求内的全部变更 + 执行顺序”，不严格证明 A 导致 B；
- 前端“数据变更”页签和业务详情“变更历史”入口尚未开发。
