# 制剂到期续展管理实现计划

## 一、需求范围

在「制剂管理」页中新增「续展文件」选项卡，用于维护制剂到期续展所需的申报材料：

1. 固定 10 类续展文件，每类支持多文件上传；
2. 支持打包下载，压缩包名为「制剂名 + 续展文件」，包内按文件类型建立 10 个文件夹；
3. 该页新增「续展备注」文本框，随制剂保存。

10 类文件：

| 序号 | 文件类型 | 建议槽位编码 | 压缩包内文件夹名 |
|---|---|---|---|
| 1 | 《医疗机构中药制剂委托配制备案申请表》（原件） | `RENEWAL_APPLICATION_FORM` | `01_委托配制备案申请表` |
| 2 | 委托方的制剂批准证明文件复印件、《医疗机构制剂许可证》 | `RENEWAL_COMMISSIONER_APPROVAL` | `02_委托方批准证明文件及许可证` |
| 3 | 受托方的《药品生产许可证》《药品生产质量管理规范》认证证书复印件 | `RENEWAL_MANUFACTURER_LICENSE_GMP` | `03_受托方药品生产许可证及GMP证书` |
| 4 | 前次批准的《医疗机构中药制剂委托配制备案件》或公告 | `RENEWAL_PREVIOUS_FILING` | `04_前次备案件或公告` |
| 5 | 前次委托配制期间，配制及制剂质量情况的总结 | `RENEWAL_PREVIOUS_PERIOD_SUMMARY` | `05_前次配制及质量情况总结` |
| 6 | 与前次《…备案件》发生变化的证明文件 | `RENEWAL_CHANGE_PROOF` | `06_与前次备案变化的证明文件` |
| 7 | 委托配制合同（原件） | `RENEWAL_COMMISSION_CONTRACT` | `07_委托配制合同` |
| 8 | 申请材料真实性的自我保证声明（原件） | `RENEWAL_AUTHENTICITY_DECLARATION` | `08_申请材料真实性自我保证声明` |
| 9 | 《授权委托书》（原件） | `RENEWAL_AUTHORIZATION_LETTER` | `09_授权委托书` |
| 10 | 其他文件 | `RENEWAL_OTHER` | `10_其他文件` |

> 文件夹名带两位序号，保证解压后顺序与申报清单一致；如不需要序号可去掉前缀。

## 二、现状调研结论（可复用能力）

| 能力 | 现有实现 | 复用方式 |
|---|---|---|
| 业务附件关系 | `business_file` 表（`biz_type/biz_id/ref_id/file_kind/file_id/sort_order`） | 直接复用，续展文件不需要新表 |
| 业务类型注册 | `Common/Constants/BizFileType.java` 枚举，声明业务表、主键列、存储目录、槽位及每槽位上限 | 新增一个 `PREPARATION_RENEWAL` 枚举项 |
| 上传/查询/删除 API | `BusinessFileController`：`POST /api/business-files/upload`、`GET /api/business-files`、`DELETE /api/business-files/{id}` | 前端直接调用，仅传新的 `bizType` 和 `fileKind` |
| 物理文件存储 | `FileStorageServiceImpl.uploadFileToDir`，路径为 `{basePath}/{中文目录}/{年}/{月}/{实体名}/{uuid.ext}` | 复用，目录由 `BizFileType.storageDir` 决定 |
| ZIP 导出 | `ProductSummaryServiceImpl#export` 使用 `ZipOutputStream(bytes, UTF_8)`，500MB 上限 | 复用同一套写法，保证中文文件名兼容 |
| 批件期限 | `preparation.approval_expiry_date`、`commissioned_approval_expiry_date`，已有过期状态计算 | 可作为“到期续展”提醒依据（可选） |
| 删除联动 | `BusinessFileService.unbindAll(bizType, bizId, refId, fileKind)` | 制剂删除时解绑续展文件 |

结论：**不新建文件表、不新建业务表**，只在 `preparation` 表加一个 `renewal_remark` 字段，新增一个业务附件类型与一组查询/导出接口。

## 三、总体设计

### 3.1 独立业务类型而非复用制剂附件槽位

现有 `BizFileType.PREPARATION` 的 9 个槽位均为单文件（max=1），且「附件」选项卡会一次查出全部槽位。若把续展槽位直接加进去，会污染现有附件列表的展示与校验。

因此新增独立业务类型：

```java
PREPARATION_RENEWAL("PREPARATION_RENEWAL", "preparation", "preparation_id",
        "制剂续展", "制剂管理/续展文件", kinds(...))
```

- 业务表仍是 `preparation`，主键 `preparation_id`，`biz_id` 存制剂 ID、`ref_id` 固定 0；
- 与「附件」选项卡的数据完全隔离；
- 每个槽位独立配置数量上限。

### 3.2 数量上限

| 槽位 | 上限 |
|---|---|
| 1~9 类 | 10 个（满足“多文件上传”，防止误传） |
| 10 其他文件 | -1（不限） |

上限可在 `BizFileType` 中随时调整，无需改前端。

### 3.3 存储路径

```
{file.base-path}/制剂管理/续展文件/{yyyy}/{MM}/{制剂名}/{uuid.ext}
```

与现有业务附件一致，由 `uploadFileToDir` 自动生成，无需额外处理。

## 四、数据库变更

新增幂等迁移脚本 `src/main/resources/Data/migration/add_preparation_renewal_remark.sql`（沿用现有迁移脚本风格，手工执行）：

```sql
-- ===================================
-- 制剂信息增加“续展备注”字段
-- 说明：幂等，可重复执行
-- ===================================

SET NAMES utf8mb4;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'preparation'
      AND COLUMN_NAME = 'renewal_remark');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `preparation` ADD COLUMN `renewal_remark` VARCHAR(1000) DEFAULT NULL
     COMMENT ''续展备注（续展申报说明，可为空）'' AFTER `commissioned_approval_expiry_date`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
```

`business_file` 表无需变更，现有索引 `idx_biz(biz_type, biz_id, ref_id, file_kind)` 已覆盖续展查询。

## 五、后端改动清单

### 5.1 新增常量枚举

`src/main/java/com/tonghui/erp/Common/Constants/PreparationRenewalKind.java`

- 枚举 10 个值，字段：`code`（槽位编码）、`label`（前端展示名）、`zipFolder`（压缩包文件夹名）；
- 提供 `getByCode`、`ordered()` 等静态方法；
- 成为槽位、展示名、压缩包目录名的**唯一数据源**，避免前后端/导出三处各写一份中文名。

```java
public enum PreparationRenewalKind {
    APPLICATION_FORM("RENEWAL_APPLICATION_FORM", "委托配制备案申请表", "01_委托配制备案申请表"),
    // ... 其余 9 个
    OTHER("RENEWAL_OTHER", "其他文件", "10_其他文件");
}
```

### 5.2 注册业务附件类型

`src/main/java/com/tonghui/erp/Common/Constants/BizFileType.java`，在「制剂与剂型」region 中 `PREPARATION` 之后新增：

```java
/** 制剂续展文件 */
PREPARATION_RENEWAL("PREPARATION_RENEWAL", "preparation", "preparation_id", "制剂续展",
        "制剂管理/续展文件",
        kinds(PreparationRenewalKind.APPLICATION_FORM.getCode(), 10,
              PreparationRenewalKind.COMMISSIONER_APPROVAL.getCode(), 10,
              // ... 7 个 10
              PreparationRenewalKind.OTHER.getCode(), -1)),
```

`src/main/resources/init-data/file-types.yml` 的 `sub-types` 增加一行（保证通用上传接口 `/api/files/upload-business` 也解析到同一目录）：

```yaml
RENEWAL: 续展文件
```

### 5.3 实体字段

`src/main/java/com/tonghui/erp/Data/Entity/Preparation.java` 新增：

```java
/**
 * 续展备注
 */
@TableField(value = "renewal_remark")
private String renewalRemark;
```

现有 `PUT /api/preparation/{id}` 会自动带上该字段，无需改保存逻辑。

### 5.4 DTO

`src/main/java/com/tonghui/erp/Common/Dto/PreparationRenewalDtos.java`（嵌套类风格，参考 `ProductSummaryDtos`）：

```java
public class PreparationRenewalDtos {
    /** 续展文件分组 */
    @Data public static class RenewalFileGroup {
        private String kind;              // 槽位编码
        private String label;             // 中文名
        private Integer maxCount;         // -1 不限
        private List<BusinessFileDto> files;
    }
    /** 续展文件页数据 */
    @Data public static class RenewalFiles {
        private Long preparationId;
        private String preparationName;
        private String renewalRemark;
        private List<RenewalFileGroup> groups;
    }
    /** 续展备注保存请求 */
    @Data public static class RenewalRemarkRequest {
        private String renewalRemark;
    }
}
```

### 5.5 Service

新增 `Service/PreparationRenewalService.java` 与 `Service/impl/PreparationRenewalServiceImpl.java`：

| 方法 | 说明 |
|---|---|
| `RenewalFiles getRenewalFiles(Long preparationId)` | 校验制剂存在；按 `PreparationRenewalKind` 固定顺序返回 10 组（空组也返回，便于前端渲染上传区）；每组文件来自 `businessFileService.list("PREPARATION_RENEWAL", id, 0L, kind)` |
| `void updateRenewalRemark(Long preparationId, String remark)` | 校验制剂存在、备注长度 ≤ 1000；仅更新该字段，避免整表单覆盖 |
| `byte[] exportRenewalFiles(Long preparationId)` | 见第七节 |

### 5.6 Controller

新增 `Controller/PreparationRenewalController.java`，基础路径 `/api/preparation/{preparationId}/renewal-files`：

| # | 接口 | 方法 | 说明 |
|---|---|---|---|
| 1 | `/api/preparation/{id}/renewal-files` | GET | 获取 10 组续展文件 + 续展备注 |
| 2 | `/api/preparation/{id}/renewal-remark` | PUT | 保存续展备注 |
| 3 | `/api/preparation/{id}/renewal-files/export` | GET | 打包下载 ZIP |

上传与删除复用现有接口：

```text
POST   /api/business-files/upload
       file, bizType=PREPARATION_RENEWAL, bizId={制剂ID}, fileKind={槽位编码}, entityName={制剂名}
DELETE /api/business-files/{relationId}
GET    /api/files/{fileId}            单文件下载
GET    /api/files/preview/{fileId}    单文件预览
```

所有 Controller/Service 方法按规范补 Javadoc（含示例请求）；导出接口加 `@OperationLog(module = "制剂管理", type = OperationType.EXPORT, desc = "下载续展文件包")`，备注保存加 `UPDATE` 日志。

### 5.7 删除联动

`PreparationServiceImpl#deleteWithDetails` 在删除主表前增加：

```java
businessFileService.unbindAll(BizFileType.PREPARATION_RENEWAL.getCode(), preparationId, 0L, null);
```

避免制剂删除后 `business_file` 留下孤儿关系。

> 顺带发现：现有 `PREPARATION` 业务附件在删除制剂时也没有解绑，属于历史问题，本次可一并修复（加一行同类调用），但需确认不在本次范围可先记录。

## 六、接口契约

### 6.1 查询续展文件

```http
GET /api/preparation/12/renewal-files
```

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "preparationId": 12,
    "preparationName": "感冒灵颗粒",
    "renewalRemark": "2027-03 到期，已启动续展",
    "groups": [
      {
        "kind": "RENEWAL_APPLICATION_FORM",
        "label": "委托配制备案申请表",
        "maxCount": 10,
        "files": [
          {
            "relationId": 1001,
            "bizType": "PREPARATION_RENEWAL",
            "bizId": 12,
            "fileId": 501,
            "fileKind": "RENEWAL_APPLICATION_FORM",
            "originalName": "备案申请表.pdf",
            "fileSize": 204800,
            "fileExtension": "pdf",
            "fileUrl": "/api/files/501",
            "previewUrl": "/api/files/preview/501",
            "createdTime": "2026-11-14 10:20:00"
          }
        ]
      }
      // ... 其余 9 组，无文件时 files 为空数组
    ]
  }
}
```

### 6.2 保存续展备注

```http
PUT /api/preparation/12/renewal-remark
Content-Type: application/json

{ "renewalRemark": "2027-03 到期，已启动续展" }
```

### 6.3 上传（复用）

```http
POST /api/business-files/upload
Content-Type: multipart/form-data

file=@备案申请表.pdf
bizType=PREPARATION_RENEWAL
bizId=12
fileKind=RENEWAL_APPLICATION_FORM
entityName=感冒灵颗粒
```

超过槽位上限时返回现有提示：`制剂续展 的 RENEWAL_APPLICATION_FORM 槽位最多允许 10 个文件`。

### 6.4 打包下载

```http
GET /api/preparation/12/renewal-files/export
```

响应头：

```text
Content-Type: application/zip
Content-Disposition: attachment; filename*=UTF-8''%E6%84%9F%E5%86%92%E7%81%B5%E9%A2%97%E7%B2%92%E7%BB%AD%E5%B1%95%E6%96%87%E4%BB%B6.zip
```

## 七、打包下载实现要点

1. 压缩包名：`{制剂品名}续展文件.zip`；品名中的 `\ / : * ? " < > |` 替换为 `_`，空白时用 `制剂{ID}`；
2. 用 `ZipOutputStream(bytes, StandardCharsets.UTF_8)` 输出，确保 Windows 解压中文名不乱码；
3. 按枚举顺序写入 10 个目录项（`folder + "/"`），**即使某类没有文件也写入空目录**，保证包内始终有 10 个文件夹；
4. 每组文件按 `sort_order` 顺序写入，条目名 `{folder}/{originalName}`；
5. 同目录重名文件追加 ` (1)`、` (2)`，避免覆盖；
6. 元数据 `file_info` 不存在或磁盘文件缺失时跳过该文件并记录 warn 日志，不影响其余文件打包；
7. 累计文件大小超过 500MB 抛出业务异常「导出文件超过 500MB」，与产品信息汇总导出一致；
8. 导出失败返回 JSON 错误（`ResponseEntity<?>` + `ApiResponse`），不要返回 500 空响应；
9. 单文件 100MB 上限沿用 `file.max-size` 配置，无需另设。

参考实现位置：`ProductSummaryServiceImpl#export`（ZIP 写入与 `FileInfo` 读取）。

## 八、前端改造要点（前端仓库）

1. **选项卡**：制剂管理详情/编辑抽屉增加「续展文件」tab，与基本信息、配方、工序模板、文档、附件并列；打开 tab 时调用 `GET /api/preparation/{id}/renewal-files` 一次性获取 10 组数据与备注。
2. **分组上传区**：10 个分组，每组显示中文类型名、已上传数量/上限、文件列表（文件名、大小、上传时间、预览/下载/删除）、上传按钮；上传组件设 `multiple`，自定义 `http-request` 调 `/api/business-files/upload`，成功后刷新该组。
3. **续展备注**：tab 顶部或底部放 textarea，失焦或点击「保存备注」调 `PUT /api/preparation/{id}/renewal-remark`；也可随制剂表单保存（实体已含字段），二选一即可，建议独立保存避免整单校验干扰。
4. **批量下载**：按钮调导出接口，用 `responseType: 'blob'` 接收，文件名从 `Content-Disposition` 解析；无文件时后端仍返回含 10 个空目录的 ZIP。
5. **（可选）列表提示**：制剂列表增加「续展」列/筛选，依据 `approvalExpiryDate`、`commissionedApprovalExpiryDate` 显示「已过期 / 30 天内到期 / 正常」，点击直接打开续展文件 tab —— 对应需求标题中的“到期续展管理”。

## 九、测试与验收

1. 执行迁移脚本后确认 `preparation.renewal_remark` 存在；
2. 启动应用，`GET /api/business-files/types` 确认返回 `PREPARATION_RENEWAL` 及 10 个槽位；
3. 对同一制剂 10 个槽位各上传 2 个文件，确认：
   - 文件落盘到 `制剂管理/续展文件/{年}/{月}/{制剂名}/`；
   - 查询接口返回 10 组、顺序正确；
   - 第 11 个文件上传被拒绝；
4. 调用导出接口解压，确认：
   - 包名 `{制剂名}续展文件.zip`；
   - 包内 10 个文件夹齐全（含空目录）；
   - 中文文件名不乱码，重名文件已自动加序号；
5. 删除单个附件后关系行物理删除、文件不再被引用时软删除；
6. 删除制剂后确认 `business_file` 中 `biz_type=PREPARATION_RENEWAL` 的关系行已清理；
7. 保存/清空续展备注，重新查询确认持久化；
8. 数据测试统一使用 `8.134.48.54` 测试库，**禁止向 `8.148.27.237` 写入**。

## 十、实施顺序与工作量

| 阶段 | 内容 | 预估 |
|---|---|---|
| 1 | 迁移脚本 + `Preparation` 实体字段 | 0.5h |
| 2 | `PreparationRenewalKind` + `BizFileType` 注册 + yml | 0.5h |
| 3 | DTO + Service + Controller（查询、备注） | 2h |
| 4 | ZIP 导出 + 删除联动 | 2h |
| 5 | 前端 tab、分组上传、备注、下载 | 0.5~1d |
| 6 | 联调与验收 | 0.5d |

后端约半天，前端约一天。

## 十一、风险与待确认

| 事项 | 当前方案 | 说明 |
|---|---|---|
| 包名格式 | `{制剂名}续展文件.zip` | 需求写作“制剂名+续展文件”，按直接拼接理解 |
| 文件夹命名 | `01_委托配制备案申请表` | 带序号保证顺序；可去掉序号 |
| 单类数量上限 | 1~9 类各 10 个，其他文件不限 | 可按实际调整 |
| 续展周期留档 | 每个制剂仅一套续展文件 | 若需保留历年续展记录，应改为独立 `preparation_renewal` 批次表，工作量增加约 1 天 |
| 并发上传 | 依赖现有槽位计数校验（非原子） | 与现有业务附件一致，极端并发可能超出 1 个，可接受 |
| 孤儿附件 | 制剂删除时解绑续展文件 | 顺带修复现有 `PREPARATION` 附件未解绑的问题 |
