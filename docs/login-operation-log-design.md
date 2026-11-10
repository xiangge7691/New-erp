# 登录日志与操作日志设计方案

> 日期：2026-11-09
> 范围：**登录日志（按账号记录登录 IP、归属地、时间、结果）** 与 **业务操作日志（谁在什么时间、从哪个 IP、对哪个模块做了什么、结果如何）**
> 技术栈：Spring Boot 3.5.5、MyBatis-Plus 3.5.5、MySQL 8、JWT

---

## 0. 结论先行

| 事项 | 方案 |
|------|------|
| 数据表 | 新增 `sys_login_log`（登录日志）、`sys_operation_log`（操作日志）两张表 |
| 登录采集 | 在 `AuthController` 同步捕获 IP / User-Agent / 时间，成功后由 `LoginLogService` 异步落库；**成功和失败都记** |
| 操作采集 | 新增 `@OperationLog` 注解 + AOP 切面，只标注写操作和导出类接口，避免把查询也记进来 |
| IP 归属地 | 离线 `ip2region` 双库（IPv4 + IPv6），内网 IP 标记为“内网”，解析失败标记为“未知”，不依赖外网接口 |
| 写入方式 | 自定义日志线程池异步写入，业务线程不等待；写日志异常只打应用日志，绝不影响业务 |
| 查询 | 提供按账号、时间、模块、结果等条件的分页查询接口，沿用 `PagedResult` + `ApiResponse` |
| 保留策略 | 定时任务按天物理删除过期日志，保留天数放在 `system_config` 里可调 |
| 与现有日志的边界 | `file_operation_log` 继续负责文件模块；新操作日志负责业务接口，避免重复记录 |

**默认决策**（可在实施前调整）：

1. 操作日志只记 **新增/修改/删除/审批/导入/导出/上传下载**，普通查询和下拉列表不记。
2. 登录日志记录 **登录成功、登录失败**；刷新令牌暂不记录；如需“退出登录”留痕，建议补一个 `/api/auth/logout` 接口。
3. 登录日志保留 **180 天**，操作日志保留 **365 天**。
4. IP 归属地使用离线双库，**IPv4 和 IPv6 地址均原样记录并解析归属地**，不调用第三方在线接口（线上环境更稳定，也不泄露登录 IP 给外部服务）。

---

## 1. 现状与差距

### 1.1 现有认证链路

- `JwtAuthenticationFilter`：解析 `Authorization: Bearer <token>`，把 `userId` 放入 `SecurityContext`。
- `ErpApplication.JwtInterceptor`：校验 `/api/**` 的 token，并把 `userId`、`username` 放入 `request` 属性。
- `AuthController.login`：调用 `LoginService.loginWithFullResponseAsync`，内部用 `CompletableFuture.supplyAsync` 执行。
- `LoginServiceImpl.login`：校验账号、状态、Argon2 密码，返回 JWT。

**关键注意点**：`CompletableFuture` 默认在 `ForkJoinPool` 中执行，`RequestContextHolder` 和 `SecurityContext` 都不会自动传递。因此登录日志的 IP / UA / 时间必须在 Controller 线程里先取好，再交给异步服务，不能在 `LoginServiceImpl` 内部取。

### 1.2 现有日志能力

- `file_operation_log`：只覆盖文件模块，字段和查询接口都围绕文件设计。
- `plan_status_log`：生产计划状态流水，属于业务变更记录。
- 没有通用的登录日志、操作日志、IP 工具、AOP 依赖和异步线程池。
- 全局软删除开启，但日志表不建 `is_deleted`、不继承 `AuditEntity`，日志只增不改。

### 1.3 需要补的基础设施

| 缺口 | 补充 |
|------|------|
| 无 AOP | `pom.xml` 增加 `spring-boot-starter-aop` |
| 无 IP 归属地 | `org.lionsoul:ip2region:3.3.7` + `ip2region_v4.xdb` + `ip2region_v6.xdb` |
| 无异步线程池 | 新增 `LogAsyncConfig`，`@EnableAsync` |
| 无真实 IP 获取 | 新增 `IpUtils`，兼容 Nginx 反向代理 |
| 无清理任务 | 新增 `LogCleanupTask`，每天凌晨物理删除过期数据 |

---

## 2. 总体设计

```
登录请求
  └─> AuthController（同步取 IP / UA / 时间）
        └─> LoginService（校验账号密码）
        └─> LoginLogService.recordAsync(...) ──> sys_login_log

业务请求
  └─> JwtInterceptor（解析 userId / username）
        └─> @OperationLog 标注的 Controller 方法
              ├─ 进入：记录开始时间、参数摘要、IP / UA
              ├─ 返回：读取 ApiResponse.code 判定成功/失败
              ├─ 异常：记录异常信息
              └─> OperationLogService.recordAsync(...) ──> sys_operation_log
```

共用组件：

- `IpUtils`：从请求头 / `remoteAddr` 取真实客户端 IP。
- `IpLocationService`：IP → “江苏省南京市” / “内网” / “未知”。
- `LogAsyncConfig`：日志专用线程池。
- `LogParamUtils`：请求参数 JSON 化、敏感字段脱敏、长度截断。
- `LogCleanupTask`：按保留天数清理。

---

## 3. 数据模型

### 3.1 登录日志 `sys_login_log`

```sql
CREATE TABLE IF NOT EXISTS `sys_login_log` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id`       BIGINT       DEFAULT NULL COMMENT '用户ID（登录失败可能为空）',
  `user_account`  VARCHAR(64)  NOT NULL COMMENT '登录账号（失败时记录尝试的账号）',
  `user_name`     VARCHAR(64)  DEFAULT NULL COMMENT '用户姓名（成功时快照）',
  `role_ids`      VARCHAR(500) DEFAULT NULL COMMENT '角色ID快照，逗号分隔',
  `role_names`    VARCHAR(500) DEFAULT NULL COMMENT '角色名称快照，顿号分隔',
  `menu_count`    INT          NOT NULL DEFAULT 0 COMMENT '可访问菜单数',
  `menu_keys`     TEXT         DEFAULT NULL COMMENT '菜单权限键快照，逗号分隔',
  `menu_names`    TEXT         DEFAULT NULL COMMENT '菜单名称快照，顿号分隔',
  `permission_count`  INT      NOT NULL DEFAULT 0 COMMENT '接口权限数',
  `permission_keys`   TEXT     DEFAULT NULL COMMENT '接口权限键快照，逗号分隔',
  `permission_names`  TEXT     DEFAULT NULL COMMENT '接口权限名称快照，顿号分隔',
  `login_type`    VARCHAR(20)  NOT NULL DEFAULT 'LOGIN' COMMENT '类型：LOGIN/LOGOUT',
  `login_result`  TINYINT      NOT NULL DEFAULT 1 COMMENT '结果：1成功/0失败',
  `fail_reason`   VARCHAR(255) DEFAULT NULL COMMENT '失败原因',
  `login_ip`      VARCHAR(64)  DEFAULT NULL COMMENT '登录IP',
  `ip_location`   VARCHAR(128) DEFAULT NULL COMMENT 'IP归属地，如 江苏省南京市/内网/未知',
  `user_agent`    VARCHAR(500) DEFAULT NULL COMMENT '客户端User-Agent',
  `login_time`    DATETIME     NOT NULL COMMENT '登录时间',
  PRIMARY KEY (`id`),
  KEY `idx_login_user_time` (`user_id`, `login_time`),
  KEY `idx_login_account_time` (`user_account`, `login_time`),
  KEY `idx_login_time` (`login_time`),
  KEY `idx_login_ip` (`login_ip`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';
```

字段说明：

| 字段 | 说明 |
|------|------|
| `user_id` | 登录失败时可能查不到用户，允许为空 |
| `user_account` | **按账号查询的核心字段**；失败时记录用户输入的那个账号，便于发现撞库/暴力破解 |
| `role_ids` / `role_names` | 登录时**生效**的角色快照（禁用角色不参与），角色后来改名或删除不影响历史日志 |
| `menu_keys` / `menu_names` | 当前角色可访问的菜单范围（`permission.perm_type = menu`） |
| `permission_keys` / `permission_names` | 当前角色拥有的接口权限（`permission.perm_type = api`） |
| `menu_count` / `permission_count` | 菜单数与接口权限数，列表页可直接展示，避免加载大字段 |
| `login_result` | 1 成功 / 0 失败；失败原因如“用户名或密码错误”“用户账户已被禁用”“用户名或密码不能为空” |
| `login_ip` | 真实客户端 IP，IPv6 原样保存 |
| `ip_location` | 离线库解析结果；内网 IP 直接写“内网” |
| `login_time` | 请求到达登录接口的时间，不是异步落库时间 |

### 3.2 操作日志 `sys_operation_log`

```sql
CREATE TABLE IF NOT EXISTS `sys_operation_log` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id`        BIGINT        DEFAULT NULL COMMENT '操作人ID',
  `user_account`   VARCHAR(64)   DEFAULT NULL COMMENT '操作人账号',
  `user_name`      VARCHAR(64)   DEFAULT NULL COMMENT '操作人姓名',
  `module`         VARCHAR(64)   NOT NULL COMMENT '业务模块，如 客户管理',
  `operation_type` VARCHAR(32)   NOT NULL COMMENT '操作类型：CREATE/UPDATE/DELETE/APPROVE/EXPORT/IMPORT/UPLOAD/DOWNLOAD/OTHER',
  `operation_desc` VARCHAR(255)  DEFAULT NULL COMMENT '操作描述，如 新增客户',
  `business_type`  VARCHAR(64)   DEFAULT NULL COMMENT '业务对象类型，如 CUSTOMER',
  `business_id`    VARCHAR(64)   DEFAULT NULL COMMENT '业务对象ID，便于按单据追溯',
  `request_method` VARCHAR(10)   DEFAULT NULL COMMENT 'HTTP方法',
  `request_url`    VARCHAR(500)  DEFAULT NULL COMMENT '请求地址（含查询串，已截断）',
  `class_method`   VARCHAR(255)  DEFAULT NULL COMMENT '执行的方法全名',
  `request_params` TEXT          DEFAULT NULL COMMENT '请求参数摘要（脱敏 + 截断）',
  `response_code`  INT           DEFAULT NULL COMMENT '业务响应码',
  `result_status`  TINYINT       NOT NULL DEFAULT 1 COMMENT '结果：1成功/0失败',
  `error_msg`      VARCHAR(1000) DEFAULT NULL COMMENT '失败时的异常信息',
  `cost_time`      INT           DEFAULT NULL COMMENT '耗时（毫秒）',
  `client_ip`      VARCHAR(64)   DEFAULT NULL COMMENT '客户端IP',
  `ip_location`    VARCHAR(128)  DEFAULT NULL COMMENT 'IP归属地',
  `user_agent`     VARCHAR(500)  DEFAULT NULL COMMENT '客户端User-Agent',
  `created_time`   DATETIME      NOT NULL COMMENT '操作时间（请求进入时间）',
  PRIMARY KEY (`id`),
  KEY `idx_op_user_time` (`user_id`, `created_time`),
  KEY `idx_op_account_time` (`user_account`, `created_time`),
  KEY `idx_op_module_time` (`module`, `created_time`),
  KEY `idx_op_type_time` (`operation_type`, `created_time`),
  KEY `idx_op_result_time` (`result_status`, `created_time`),
  KEY `idx_op_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';
```

字段说明：

| 字段 | 说明 |
|------|------|
| `user_account` / `user_name` | 记录操作当时的账号和姓名快照，用户后来改名不影响历史日志 |
| `module` | 与前端菜单口径一致，如“客户管理”“采购管理”“库存管理” |
| `operation_type` | 统一枚举，前端按类型显示不同颜色标签 |
| `business_type` + `business_id` | 可选；用于从日志反查单据，例如 `CUSTOMER` + `1024` |
| `request_params` | 只存摘要，默认截断 2000 字符；密码、token 等字段脱敏 |
| `result_status` | 由返回的 `ApiResponse.code` 判定：200 成功，其余失败 |
| `cost_time` | 接口耗时，便于发现慢操作 |

### 3.3 日志表的设计约定

- **不继承 `AuditEntity`**：日志没有“更新人/更新时间”的概念，也不应该被修改。
- **不加 `is_deleted`**：避免全局软删除拦截器介入；清理任务用原生 `DELETE`。
- **不建外键**：日志要能保留已删除用户、已删除单据的历史信息。
- **`user_account` 冗余快照**：用户表改名或删号后，日志仍然可读。

### 3.4 容量估算

- 登录日志：按 200 人、每人每天 2 次登录估算，约 400 条/天，一年约 15 万条，压力可忽略。
- 操作日志：按每天 1000 次写操作估算，一年约 36 万条；`request_params` 截断到 2000 字符后，单行一般小于 4KB，一年约 1～2GB，MySQL 单表可承受。
- 若后续操作日志增长到千万级，再按 `created_time` 做月度分区或归档表，本期不做。

---

## 4. 登录日志采集

### 4.1 采集位置

统一放在 `AuthController`，原因：

1. 只有 Controller 线程能可靠拿到 `HttpServletRequest`；
2. `LoginServiceImpl` 内部使用了 `CompletableFuture`，请求上下文会丢失；
3. 登录成功 / 失败两种分支在 Controller 都有明确结果，便于判定。

### 4.2 记录内容

| 场景 | user_id | user_account | login_result | fail_reason |
|------|---------|--------------|--------------|-------------|
| 登录成功 | 用户ID | 登录账号 | 1 | null |
| 账号不存在 / 密码错误 | null | 用户输入的账号 | 0 | 用户名或密码错误 |
| 账号被禁用 | 用户ID | 登录账号 | 0 | 用户账户已被禁用 |
| 参数为空 | null | 用户输入的账号（可能为空串） | 0 | 用户名或密码不能为空 |

> 登录失败时如果能在校验过程中拿到用户对象（如账号被禁用），可以补上 `user_id`，其余失败场景允许为空。

**登录成功时额外记录角色与权限快照**：由 `AuthController` 从登录响应中读取角色列表和权限树，`LoginPermissionSnapshotUtils` 将权限树按 `perm_type` 拍平为“菜单范围（menu）”和“接口权限（api）”两组，随登录日志一起落库。这样即使后续角色、权限被修改，也能回溯该账号在登录当时实际能操作的内容。

### 4.3 代码骨架

```java
/**
 * 登录日志服务
 */
public interface LoginLogService {

    /**
     * 异步记录登录日志
     *
     * @param context 登录日志上下文（由 Controller 同步采集 IP/UA/时间）
     */
    void recordAsync(LoginLogContext context);
}
```

```java
// AuthController 中
@PostMapping("/login")
public ApiResponse<AuthLoginResponse> login(@RequestBody LoginRequest loginRequest,
                                            HttpServletRequest request) {
    LocalDateTime loginTime = LocalDateTime.now();
    LoginLogContext logContext = loginLogService.buildContext(request, loginRequest.getUserName(), loginTime);
    try {
        // ... 原有登录逻辑 ...
        if (!loginResponse.isSuccess()) {
            logContext.markFail(null, loginResponse.getMessage());
            loginLogService.recordAsync(logContext);
            return ApiResponse.errorResponse(loginResponse.getMessage());
        }
        UserDto user = loginResponse.getData().getUser();
        logContext.markSuccess(user.getId(), user.getUserName(), user.getName());
        loginLogService.recordAsync(logContext);
        // ... 原有返回逻辑 ...
    } catch (Exception e) {
        logContext.markFail(null, e.getMessage());
        loginLogService.recordAsync(logContext);
        return ApiResponse.errorResponse("登录过程中发生错误：" + e.getMessage());
    }
}
```

> 注意：`UserDto.userName` 在现有转换器里对应的是 `user_account`（登录账号），`name` 对应真实姓名，落库时不要弄反。

### 4.4 退出登录（可选）

现有系统没有 `/api/auth/logout`，前端删除本地 token 即视为退出，服务端无法感知。如果需要在日志里体现“退出登录”，建议新增：

```http
POST /api/auth/logout
Authorization: Bearer <token>
```

接口只做一件事：用 token 里的 `userId` 和当前请求信息写一条 `login_type = 'LOGOUT'` 的登录日志，不维护服务端会话（JWT 无状态）。如果不做，本设计仍可正常运行，登录日志只包含登录成功/失败。

---

## 5. 操作日志采集

### 5.1 注解定义

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /** 业务模块，如 客户管理 */
    String module();

    /** 操作类型 */
    OperationType type();

    /** 操作描述，如 新增客户 */
    String desc() default "";

    /** 业务对象类型，如 CUSTOMER */
    String businessType() default "";

    /** 业务对象ID的 SpEL 表达式，如 #id、#request.customerId、#result.data.id */
    String businessId() default "";

    /** 是否记录请求参数，默认记录 */
    boolean recordParams() default true;
}
```

```java
public enum OperationType {
    CREATE, UPDATE, DELETE, APPROVE, EXPORT, IMPORT, UPLOAD, DOWNLOAD, ENABLE, DISABLE, OTHER
}
```

使用示例：

```java
@OperationLog(module = "客户管理", type = OperationType.CREATE, desc = "新增客户")
@PostMapping
public ApiResponse<Customer> create(@RequestBody Customer customer) { ... }

@OperationLog(module = "客户管理", type = OperationType.UPDATE, desc = "修改客户",
              businessType = "CUSTOMER", businessId = "#customer.customerId")
@PutMapping("/{customerId}")
public ApiResponse<Customer> update(@PathVariable Long customerId, @RequestBody Customer customer) { ... }
```

### 5.2 切面逻辑

```java
@Aspect
@Component
public class OperationLogAspect {

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        OperationLogContext context = buildContext(joinPoint, operationLog);
        try {
            Object result = joinPoint.proceed();
            fillResult(context, result);
            return result;
        } catch (Throwable ex) {
            context.markFail(ex);
            throw ex;
        } finally {
            context.setCostTime(...);
            operationLogService.recordAsync(context); // 内部 try/catch，绝不抛异常
        }
    }
}
```

要点：

1. **用户信息同步取**：从 `request.getAttribute("userId"/"username")` 取（`JwtInterceptor` 已写入），取不到再从 `SecurityContext` 取；异步线程里不再访问上下文。
2. **结果判定**：返回值是 `ApiResponse` 时读 `code`，`200` 为成功；抛异常为失败。
3. **异常继续抛出**：切面只记录，不吞异常，保持原有全局异常处理行为。
4. **日志写入异步**：`recordAsync` 内部捕获所有异常，只写应用日志（`log.warn`），不影响业务返回。
5. **自身调用不生效**：同类内部方法互相调用不会经过代理，注解要加在 Controller 的入口方法上。

### 5.3 参数脱敏与截断

`LogParamUtils.toJsonSafely(Object[] args)` 规则：

- 跳过 `HttpServletRequest`、`HttpServletResponse`、`MultipartFile`、`InputStream` 等不可序列化对象；
- 字段名命中 `password`、`oldPassword`、`newPassword`、`confirmPassword`、`token`、`refreshToken`、`secret`、`authorization`（忽略大小写）时，值替换为 `******`；
- 递归处理嵌套对象和集合；
- 序列化失败时退化为 `参数序列化失败: 类名`；
- 最终截断到 `log.operation.max-param-length`（默认 2000 字符）。

> 请求体里已有的敏感字段处理完后，日志表仍属于内部审计数据，查询接口应只对系统管理角色开放（见 8.4）。

### 5.4 业务对象 ID

`businessId` 支持 SpEL：

- `#id`：方法参数名；
- `#request.customerId`：参数对象字段；
- `#result.data.customerId`：返回值字段（需要方法返回后再解析）。

解析失败时留空，不影响日志主流程。Spring Boot 默认编译参数带 `-parameters`，参数名可直接使用。

### 5.5 需要标注的接口范围

**建议记录**：

- 所有业务模块的 **新增 / 修改 / 删除 / 启停用**；
- **审批**（提交、通过、驳回）；
- **导入 / 导出 / 上传 / 下载**；
- 系统配置修改、角色权限分配、用户启停用等敏感操作。

**不建议记录**：

- 列表查询、详情查询、下拉选项；
- 登录（已有独立登录日志）；
- 文件模块（已有 `file_operation_log`）；
- 高频轮询接口（如首页统计、消息未读数）。

**与 `file_operation_log` 的边界**：文件上传/下载继续由文件服务记录到 `file_operation_log`（包含 `FileController`、`FileManagerController`、`BusinessFileController`）；业务接口上的 `@OperationLog` 只描述业务动作，例如“新增客户”，不重复记录附件明细。

### 5.6 分阶段覆盖建议

全项目 Controller 很多，不建议一次性全部标注。按风险从高到低分三批：

1. **第一批（必须）**：用户、角色、权限、部门、系统配置、审批流程、审批放行；
2. **第二批（业务主流程）**：客户、供应商、物料、制剂、生产计划、工单、采购、出入库、请检/检验/放行；
3. **第三批（其余基础数据）**：房间、设备、工序、岗位、单位等。

未标注的接口不受影响，后续按模块逐步补注解即可。

---

## 6. IP 与归属地

### 6.1 获取真实客户端 IP

线上通常经过 Nginx 反向代理，`request.getRemoteAddr()` 拿到的是代理地址，必须优先读取代理头。`IpUtils.getClientIp(request)` 按以下顺序取第一个合法值：

1. `X-Forwarded-For`（取第一个非 `unknown` 的地址，去掉端口和空格）
2. `X-Real-IP`
3. `Proxy-Client-IP`
4. `WL-Proxy-Client-IP`
5. `HTTP_CLIENT_IP`
6. `HTTP_X_FORWARDED_FOR`
7. `request.getRemoteAddr()`

兼容处理：

- `::1`、`127.0.0.1` 统一写为 `127.0.0.1`；
- `::ffff:192.168.1.10` 转为 `192.168.1.10`；
- 多个 IP 用逗号分隔时只取第一个。

> 安全提示：`X-Forwarded-For` 可以被客户端伪造。只有在确认请求一定经过自家 Nginx 时才信任该头，并且 Nginx 要用 `proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;` 覆盖而不是透传客户端原值。可在配置中增加 `log.ip.trust-proxy-headers: true` 开关。

### 6.2 归属地解析

使用离线库 **ip2region 3.3.7**，该版本提供双协议兼容、并发安全的 `Ip2Region` 查询服务，IPv4 与 IPv6 分别加载自己的 xdb：

```xml
<dependency>
    <groupId>org.lionsoul</groupId>
    <artifactId>ip2region</artifactId>
    <version>3.3.7</version>
</dependency>
```

- 数据文件放到 `src/main/resources/ip2region/`：
  - `ip2region_v4.xdb`（IPv4，约 11MB）
  - `ip2region_v6.xdb`（IPv6，约 37MB）
- 启动时通过 `Config.custom().setXdbInputStream(...).asV4()/asV6()` + `Ip2Region.create(v4, v6)` 全量载入内存（`BufferCache`），单次查询微秒级，服务本身并发安全；
- 返回格式为 `国家|省份|城市|ISP|国家代码`（如 `中国|广东省|深圳市|电信|CN`），转换为“广东省深圳市”这种展示值；境外地址展示“国家+省州+城市”；
- 内网地址（`10.`、`172.16.`～`172.31.`、`192.168.`、`127.`，以及 IPv6 的 `::1`、`fe80::/10`、`fc00::/7`）直接返回“内网”；
- IPv4 映射的 IPv6（`::ffff:192.168.1.10`）先还原成 IPv4 再判断和解析；
- xdb 文件缺失或解析异常时返回“未知”，启动时打一条 `warn`，不影响业务；
- 路径可通过 `log.location.v4-xdb-path` / `log.location.v6-xdb-path` 指向服务器绝对路径，避免 jar 体积过大。

### 6.3 Nginx 配置示例

```nginx
location /api/ {
    proxy_pass http://127.0.0.1:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

---

## 7. 异步写入与可靠性

### 7.1 线程池

```java
@Configuration
@EnableAsync
public class LogAsyncConfig {

    @Bean("logExecutor")
    public ThreadPoolTaskExecutor logExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(2000);
        executor.setThreadNamePrefix("log-writer-");
        // 队列满时由调用线程执行，宁可让请求稍慢，也不丢审计日志
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }
}
```

- 日志量本身不大，1～2 个线程足够；
- `CallerRunsPolicy` 保证队列满时不丢日志，同时对请求形成天然背压；
- 关闭时等待队列中的日志写完，避免丢尾部数据。

### 7.2 失败隔离

- `recordAsync` 内部整体 `try/catch`，失败只打 `log.warn/error`；
- 异步方法不能和业务方法共用事务，日志写入失败不会回滚业务；
- 日志写入走独立数据库连接，不参与业务事务。

### 7.3 时间语义

- 登录日志的 `login_time` = 请求到达登录接口的时间；
- 操作日志的 `created_time` = 进入 Controller 方法的时间，`cost_time` 单独记录耗时；
- 异步写入延迟不影响展示，前端看到的是操作发生时间。

### 7.4 多实例部署

- 每个实例各自异步写库，互不影响；
- 清理任务使用数据库条件删除，多实例同时执行也安全（`DELETE ... WHERE created_time < ?` 幂等）。

---

## 8. 查询接口设计

统一放在 `/api/system` 下，Controller 继承 `BaseController`，返回 `ApiResponse`，分页使用 `PageRequestDto` / `PagedResult`。

### 8.1 登录日志查询

```http
GET /api/system/login-log/search?pageIndex=0&pageSize=20
    &userAccount=root
    &userName=张三
    &loginResult=0
    &loginIp=192.168.1.10
    &roleName=管理员
    &permissionKey=user:create
    &startTime=2026-11-01 00:00:00
    &endTime=2026-11-09 23:59:59
```

| 参数 | 必填 | 说明 |
|------|------|------|
| `pageIndex` / `pageSize` | 否 | 沿用 `PageRequestDto`，从 0 开始 |
| `userAccount` | 否 | 登录账号，精确匹配 |
| `userId` | 否 | 用户ID，精确匹配 |
| `userName` | 否 | 姓名，模糊匹配 |
| `loginResult` | 否 | 1 成功 / 0 失败 |
| `loginIp` | 否 | IP，精确匹配 |
| `roleName` | 否 | 角色名称，模糊匹配（匹配角色名称快照） |
| `permissionKey` | 否 | 接口权限键，模糊匹配（如 `user:create`） |
| `startTime` / `endTime` | 否 | 支持 `yyyy-MM-dd HH:mm:ss` 或 `yyyy-MM-dd`；只传日期时自动补 `00:00:00` / `23:59:59` |

返回：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "items": [
      {
        "id": 1001,
        "userId": 1,
        "userAccount": "root",
        "userName": "系统管理员",
        "roleIds": [1, 25],
        "roleNames": ["超级管理员", "系统管理员"],
        "menuCount": 91,
        "permissionCount": 100,
        "loginType": "LOGIN",
        "loginResult": 1,
        "failReason": null,
        "loginIp": "218.94.12.34",
        "ipLocation": "江苏省南京市",
        "userAgent": "Mozilla/5.0 ...",
        "loginTime": "2026-11-09 09:12:33"
      }
    ],
    "totalCount": 1,
    "pageIndex": 0,
    "pageSize": 20
  },
  "timestamp": 1762650000000
}
```

### 8.1.1 登录日志详情

```http
GET /api/system/login-log/1001
```

返回登录当时的完整角色、菜单范围和接口权限明细：

```json
{
  "code": 200,
  "data": {
    "id": 1001,
    "userAccount": "root",
    "roleNames": ["超级管理员", "系统管理员"],
    "menuCount": 2,
    "permissionCount": 2,
    "menuKeys": ["user_management", "role_management"],
    "menuNames": ["用户管理", "角色管理"],
    "permissionKeys": ["user:create", "user:update"],
    "permissionNames": ["新增用户", "修改用户"],
    "loginIp": "218.94.12.34",
    "ipLocation": "江苏省南京市",
    "loginTime": "2026-11-09 09:12:33"
  }
}
```

### 8.2 操作日志查询

```http
GET /api/system/operation-log/search?pageIndex=0&pageSize=20
    &userAccount=root
    &module=客户管理
    &operationType=CREATE
    &resultStatus=0
    &keyword=新增
    &startTime=2026-11-01
    &endTime=2026-11-09
```

| 参数 | 必填 | 说明 |
|------|------|------|
| `userAccount` / `userId` | 否 | 操作人 |
| `userName` | 否 | 操作人姓名，模糊 |
| `module` | 否 | 业务模块，精确或模糊 |
| `operationType` | 否 | 操作类型 |
| `resultStatus` | 否 | 1 成功 / 0 失败 |
| `businessType` / `businessId` | 否 | 按业务对象反查 |
| `keyword` | 否 | 模糊匹配操作描述、请求地址 |
| `startTime` / `endTime` | 否 | 操作时间范围 |

列表接口**不返回** `requestParams` / `errorMsg` 这类大字段，避免列表查询拖慢；详情单独查。

### 8.3 操作日志详情

```http
GET /api/system/operation-log/{id}
```

返回完整字段，包括 `requestParams`、`errorMsg`、`classMethod`、`userAgent`。用于前端“查看详情”抽屉。

### 8.4 辅助接口（可选）

| 接口 | 用途 |
|------|------|
| `GET /api/system/login-log/latest?userId=1` | 用户管理页展示“最后登录时间 / IP / 归属地” |
| `GET /api/system/login-log/summary?startTime=&endTime=` | 登录统计：成功数、失败数、去重账号数（首页或日志页顶部卡片） |
| `GET /api/system/operation-log/export?...` | 导出当前筛选结果为 Excel，复用项目已有 POI 依赖 |

### 8.5 分页限制与权限说明

日志表数据量大，查询接口**不支持 `-1` 全量查询**：缺省为第 0 页、每页 20 条，单页最多 200 条。需要导出时单独走导出接口。

现状：`SecurityConfig` 对所有请求放行，`JwtInterceptor` 只校验 token 是否有效，没有后端方法级权限校验。日志查询属于敏感功能，建议：

- 短期：接口放在 `/api/system/**` 下，前端菜单/按钮通过 `role_perm` 控制；
- 中期：给日志查询接口补角色校验（如仅管理员或“系统管理”角色可访问），不要只依赖前端隐藏菜单。

---

## 9. 清理与保留

### 9.1 定时任务

```java
@Component
public class LogCleanupTask {

    /** 每天 03:10 清理过期日志 */
    @Scheduled(cron = "0 10 3 * * ?")
    public void cleanup() {
        // 分批删除，避免长事务和锁表
    }
}
```

- 登录日志默认保留 `log.login.retention.days = 180`；
- 操作日志默认保留 `log.operation.retention.days = 365`；
- 保留天数从 `system_config` 读取，读不到用默认值；
- 每批删除 1000 条，循环直到删完，避免一次性删除大量数据造成主从延迟。

### 9.2 物理删除

日志表没有 `is_deleted` 字段，但为避免全局逻辑删除配置带来的歧义，清理仍使用 Mapper 原生 SQL：

```java
@Delete("DELETE FROM sys_operation_log WHERE created_time < #{before} LIMIT #{limit}")
int deleteBefore(@Param("before") LocalDateTime before, @Param("limit") int limit);
```

> 不要使用 `mapper.delete(wrapper)`，项目已多次踩过全局软删除改写的坑。

### 9.3 配置项

在 `system_config` 增加：

| config_key | 默认值 | 分组 | 说明 |
|------------|--------|------|------|
| `log.login.retention.days` | 180 | 系统日志 | 登录日志保留天数 |
| `log.operation.retention.days` | 365 | 系统日志 | 操作日志保留天数 |
| `log.operation.enabled` | true | 系统日志 | 操作日志总开关（排障时可临时关闭） |

初始化 SQL 放到 `init_system_config_data.sql` 或新增迁移脚本。

---

## 10. 前端页面建议

### 10.1 系统管理 → 登录日志

- 筛选：账号、姓名、登录结果、时间范围、IP；
- 表格列：登录时间、账号、姓名、结果（成功/失败标签）、失败原因、IP、归属地、客户端；
- 顶部卡片：今日成功登录数、今日失败登录数、当前筛选下失败次数；
- 行点击可查看 User-Agent 完整信息；
- 支持导出当前筛选结果（可选）。

### 10.2 系统管理 → 操作日志

- 筛选：账号、模块、操作类型、结果、时间范围、关键字；
- 表格列：操作时间、账号、姓名、模块、操作类型（彩色标签）、操作描述、结果、耗时、IP、归属地；
- “详情”抽屉：请求方法、请求地址、业务对象、请求参数（格式化 JSON）、错误信息、User-Agent；
- 支持导出（可选）。

### 10.3 用户管理页联动（可选）

用户列表增加“最后登录时间 / 最后登录 IP”，数据来自 `GET /api/system/login-log/latest?userId=`。

### 10.4 菜单与权限

新增两个菜单项（系统管理下），并在 `permission` / `role_perm` 中配置对应权限；具体菜单 SQL 按现有初始化方式维护。

---

## 11. 代码落点清单

| 文件 | 说明 |
|------|------|
| `pom.xml` | 增加 `spring-boot-starter-aop`、`ip2region` |
| `src/main/resources/Data/migration/create_login_log_table.sql` | 登录日志建表 |
| `src/main/resources/Data/migration/add_login_log_role_permission_fields.sql` | 登录日志角色/权限字段补充（幂等） |
| `src/main/resources/Data/migration/create_operation_log_table.sql` | 操作日志建表 |
| `src/main/resources/ip2region/ip2region_v4.xdb` | IPv4 归属地离线库 |
| `src/main/resources/ip2region/ip2region_v6.xdb` | IPv6 归属地离线库 |
| `Data/Entity/SysLoginLog.java` | 登录日志实体 |
| `Data/Entity/SysOperationLog.java` | 操作日志实体 |
| `Data/mapper/SysLoginLogMapper.java` | 登录日志 Mapper（含清理删除方法） |
| `Data/mapper/SysOperationLogMapper.java` | 操作日志 Mapper（含清理删除方法） |
| `Common/annotation/OperationLog.java` | 操作日志注解 |
| `Common/Constants/OperationType.java` | 操作类型枚举 |
| `Common/Config/LogAsyncConfig.java` | 日志线程池 + `@EnableAsync` |
| `Common/Config/LogProperties.java` | 日志配置绑定（IP、归属地库、截断长度） |
| `Common/utils/IpUtils.java` | 获取真实客户端 IP |
| `Common/utils/IpLocationService.java` | IP 归属地解析 |
| `Common/utils/LoginPermissionSnapshotUtils.java` | 登录角色/菜单/接口权限快照拍平 |
| `Common/utils/LogParamUtils.java` | 参数序列化、脱敏、截断 |
| `Common/Dto/Log/LoginLogDto.java` | 登录日志列表 DTO |
| `Common/Dto/Log/LoginLogDetailDto.java` | 登录日志详情 DTO（菜单/权限明细） |
| `Common/Dto/Log/OperationLogDto.java` | 操作日志列表 DTO |
| `Common/Dto/Log/OperationLogDetailDto.java` | 操作日志详情 DTO |
| `Service/LoginLogService.java` + `impl/LoginLogServiceImpl.java` | 登录日志记录与查询 |
| `Service/OperationLogService.java` + `impl/OperationLogServiceImpl.java` | 操作日志记录与查询 |
| `Common/aspect/OperationLogAspect.java` | 操作日志切面 |
| `Controller/LoginLogController.java` | 登录日志查询接口 |
| `Controller/OperationLogController.java` | 操作日志查询接口 |
| `Common/Scheduled/LogCleanupTask.java` | 过期日志清理 |
| `Controller/AuthController.java` | 登录成功/失败埋点 |
| 各业务 Controller | 按批次补 `@OperationLog` 注解 |
| `src/main/resources/application.yml` | 增加日志相关配置（开关、截断长度、保留天数默认值） |

---

## 12. 实施步骤与工作量

| 阶段 | 内容 | 预计 |
|------|------|------|
| 1 | 依赖、建表脚本、实体、Mapper | 0.5 天 |
| 2 | `IpUtils` + `ip2region` 归属地 + 单测 | 1 天 |
| 3 | 登录日志记录（AuthController 埋点 + 查询接口） | 0.5 天 |
| 4 | 操作日志注解 + 切面 + 脱敏工具 + 异步线程池 | 1.5 天 |
| 5 | 操作日志查询接口 + 清理任务 + 配置项 | 1 天 |
| 6 | 第一批高风险模块补注解 | 1 天 |
| 7 | 前端两个页面 + 联调 | 1.5～2 天 |
| 8 | 测试（成功/失败/脱敏/IP/代理/异步异常） | 1 天 |

**后端约 4～5 天，前端约 2 天**，可按“登录日志先上线、操作日志分模块覆盖”的方式拆成两个迭代。

---

## 13. 风险与注意事项

1. **登录接口的异步上下文**：不要在 `CompletableFuture` 内部取 `RequestContextHolder`，IP/UA 必须在 Controller 线程采集。
2. **代理头伪造**：只有部署在自家 Nginx 后面时才信任 `X-Forwarded-For`，并确保 Nginx 覆盖该头。
3. **敏感参数**：密码、token、密钥类字段必须脱敏；日志查询接口不要对普通角色开放。
4. **日志量失控**：`@OperationLog` 只加在写操作和导出上，禁止加到列表/轮询接口；上线后观察日增量，必要时调整保留天数。
5. **AOP 自调用失效**：注解加在 Controller 入口方法，不要加在 Service 内部被同类调用的方法上。
6. **日志失败不能影响业务**：异步方法必须整体捕获异常；线程池队列满时用 `CallerRunsPolicy`，不要静默丢弃。
7. **ip2region 数据文件体积与内存**：v4 约 11MB、v6 约 37MB，一起打进 jar 会增加约 48MB 包体；`BufferCache` 全量载入还会占用约 50MB 堆内存。如果运维不接受，可通过 `log.location.*-xdb-path` 指向服务器目录从外部加载，并按需调整缓存策略。
8. **时区**：全链路使用 `LocalDateTime`，项目已配置 `Asia/Shanghai`，不要混用 `Date`。
9. **不要软删除**：日志表不建 `is_deleted`，清理用原生 `DELETE ... LIMIT`，避免全局逻辑删除改写。
10. **登录账号非空**：`user_account` 为 `NOT NULL`，登录参数为空时落空串，保证失败尝试也能留痕。

---

## 14. 待确认项

| # | 决策点 | 默认建议 |
|---|--------|----------|
| 1 | 操作日志记录范围 | 只记写操作 + 审批 + 导入导出；查询不记 |
| 2 | 登录失败是否记录 | 记录，便于发现撞库和暴力破解 |
| 3 | 归属地数据源 | 离线 ip2region 双库（IPv4 + IPv6），不调用外网接口 |
| 4 | 保留天数 | 登录 180 天 / 操作 365 天，可在系统配置中修改 |
| 5 | 是否新增退出登录接口 | 建议新增 `/api/auth/logout` 用于留痕，不做服务端会话 |
| 6 | 是否需要导出 | 建议操作日志支持 Excel 导出，登录日志可后补 |
| 7 | 前端是否本期一起做 | 后端接口先完成，前端可并行开发 |

---

## 15. 实现状态（2026-11-10）

**已完成（后端）**

- 建表脚本：`create_login_log_table.sql`、`add_login_log_role_permission_fields.sql`、`create_operation_log_table.sql`、`init_log_config_data.sql`；
- 实体与 Mapper：`SysLoginLog`、`SysOperationLog` 及清理/统计 SQL；
- 登录日志：`AuthController` 登录成功/失败埋点，记录账号、IP、归属地、时间、结果、失败原因；
- 数据变更日志：`DataChangeInterceptor` 自动采集 Mapper 增删改的修改前后字段值，写入 `sys_data_change_log`，通过 `trace_id` 与操作日志关联，支持级联修改记录；详见 [data-change-log-design.md](./data-change-log-design.md)；
- 角色与权限快照：登录成功时记录角色ID/名称、可访问菜单（`menu`）范围、接口权限（`api`）键与名称，支持按角色名、权限键筛选，详情接口返回完整权限明细；
- 操作日志：`@OperationLog` 注解、`OperationLogAspect` 切面、参数脱敏、结果判定、业务 ID SpEL；
- IP：`IpUtils` + `IpLocationService`，离线 ip2region 3.3.7 双库，IPv4/IPv6 均原样记录并解析归属地；
- 异步：`LogAsyncConfig` 独立日志线程池，`CallerRunsPolicy` 不丢日志；
- 查询接口：登录日志分页/最近登录/统计/详情（含角色与权限快照），操作日志分页/详情，单页上限 200 条；
- 清理：`LogCleanupTask` 每天 03:10 按保留天数分批物理删除；
- 首批注解：用户、角色、权限、部门、用户角色/部门关联、角色权限关联、系统配置、审批流程、审批实例、审核放行；
- 第二批、第三批注解：客户、剂型、设备、物料、制剂及配方/工序模板/文件、生产计划/工单/工序执行/领料、采购计划/订单/供应商/审核、入库/出库、验收、请检/检验/取样/留样/销毁、清洁/消毒/温湿度/压差、偏差、维保、人员档案/证书、机构/证照、岗位/职务、调拨/退货、检验单、产品汇总导出等；
- 文件模块（`FileController`、`FileManagerController`、`BusinessFileController`）继续使用已有的 `file_operation_log`，不重复记录；登录/刷新接口由登录日志覆盖；
- 测试：`IpUtilsTest`、`IpLocationServiceTest`、`LogParamUtilsTest`、`LoginPermissionSnapshotUtilsTest`、`AuthControllerLoginLogTest`、`LogModuleIntegrationTest`，其中集成测试已覆盖写入、角色/权限快照、筛选、详情、切面、路由注册与敏感参数脱敏。

**尚未完成**

- 前端“登录日志 / 操作日志 / 数据变更”页面与操作日志详情的数据变更页签；
- 可选的退出登录接口与日志导出接口（各业务模块已有的导出接口已按 `EXPORT` 类型记录）；
- 生产库需手动执行日志相关迁移脚本（新建表时直接使用含新增字段的建表脚本；已建表则执行对应 `add_*` 迁移脚本）。
