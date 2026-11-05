# 统一附件关系表 —— 详细实现方案

> 本文是 [`file-upload-attachment-plan.md`](./file-upload-attachment-plan.md) 的落地细化版。
> 与计划稿相比有一处关键调整：关系表增加 `ref_id` 字段，用于审批实例 + 审批节点这类“父记录下还有子对象”的场景（`approval_node` 属于流程定义，多个实例共用同一节点 ID，不能只用 `node_id` 做 `biz_id`）。
>
> **实现状态（2026-11-03）**：后端基础模块（`business_file` 实体/Mapper/Service/Controller、`BizFileType` 注册表、文件存储扩展、旧删除接口保护）、前端的 `business-files.ts` / `business-file-upload.vue` 以及工单工序附件试点已落地；数据库建表和数据迁移脚本待执行。

---

## 1. 设计定稿

### 1.1 三张表的职责

| 表 | 职责 | 是否新增 |
|----|------|---------|
| `file_info` | 物理文件元数据：文件名、路径、大小、MD5、下载地址 | 否，继续保留 |
| `business_file` | 业务附件关系：哪条业务记录的哪个槽位用了哪个文件 | **新增** |
| `business_file_migration_conflict` | 迁移冲突清单（仅迁移期使用，可后续删除） | **新增** |

### 1.2 关联键

```
(biz_type, biz_id, ref_id, file_kind)
```

| 字段 | 含义 | 示例 |
|------|------|------|
| `biz_type` | 业务类型编码，**一表一码**，由后端注册表校验 | `WORK_ORDER_PROCESS_EXECUTION` |
| `biz_id` | 业务表主键值 | 工序执行行 ID = 123 |
| `ref_id` | 父记录下的子对象 ID；没有子对象时为 `0` | 审批节点 ID = 45 |
| `file_kind` | 文件槽位/用途，由注册表限制 | `PROCESS_ATTACHMENT`、`WAYBILL`、`INVOICE` |

示例：

```
-- 工序 123 的工序附件
biz_type = WORK_ORDER_PROCESS_EXECUTION
biz_id   = 123
ref_id   = 0
file_kind= PROCESS_ATTACHMENT

-- 审批实例 88 在节点 45 上上传的附件
biz_type = APPROVAL_INSTANCE
biz_id   = 88
ref_id   = 45
file_kind= DEFAULT
```

### 1.3 关键规则

1. `biz_type` 只接受 `BizFileType` 枚举中的值，前端不能传表名。
2. `biz_id` 必须是父表中存在的、未软删除的记录。
3. `ref_id > 0` 时，子对象必须存在且属于 `biz_id`（审批场景校验节点属于实例的工作流）。
4. `file_kind` 必须在 `BizFileType` 声明的槽位内；每个槽位可配置最大数量（`-1` 表示不限）。
5. 关系行**物理删除**（解绑），`file_info` 继续软删除/回收站。
6. `custom_path` 不再参与业务查询，只保留为磁盘目录展示信息。

---

## 2. 数据库

### 2.1 建表脚本

文件：`src/main/resources/Data/migration/create_business_file.sql`

```sql
-- ===================================
-- 业务附件关系表
-- ===================================
CREATE TABLE IF NOT EXISTS business_file (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '关联ID',
    biz_type      VARCHAR(64)  NOT NULL COMMENT '业务类型编码，一表一码',
    biz_id        BIGINT       NOT NULL COMMENT '业务表主键值',
    ref_id        BIGINT       NOT NULL DEFAULT 0 COMMENT '子对象ID（如审批节点ID），无子对象时为0',
    file_id       BIGINT       NOT NULL COMMENT '文件ID（file_info.file_id）',
    file_kind     VARCHAR(64)  NOT NULL DEFAULT 'DEFAULT' COMMENT '文件槽位/用途',
    sort_order    INT          NOT NULL DEFAULT 0 COMMENT '排序',
    is_deleted    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除（关系行实际物理删除，保留字段用于统一查询）',
    created_by    BIGINT       NULL COMMENT '创建人',
    created_time  DATETIME     NULL COMMENT '创建时间',
    updated_by    BIGINT       NULL COMMENT '更新人',
    updated_time  DATETIME     NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_biz_file (biz_type, biz_id, ref_id, file_id),
    KEY idx_biz (biz_type, biz_id, ref_id, file_kind),
    KEY idx_file (file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务附件关联表';
```

### 2.2 为什么关系行要物理删除

- 项目全局开启了 `logic-delete-field: isDeleted`，`removeById` 会被改写成 `UPDATE ... SET is_deleted=1`。
- 唯一键 `(biz_type, biz_id, ref_id, file_id)` 对软删除行同样生效：同一文件解绑后再绑定会触发唯一键冲突。
- 关系表只表达“当前是否绑定”，不需要保留历史；解绑直接 `DELETE`，由 `file_operation_log` 保留操作痕迹。
- 实体仍保留 `isDeleted` 字段并设置 `0`，让 MyBatis-Plus 查询自动追加 `is_deleted = 0`，避免读到历史脏行；物理删除通过 Mapper 原生 `@Delete` 绕过逻辑删除。

### 2.3 迁移冲突表

文件：`src/main/resources/Data/migration/create_business_file_migration_conflict.sql`

```sql
CREATE TABLE IF NOT EXISTS business_file_migration_conflict (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '冲突ID',
    file_id       BIGINT       NOT NULL COMMENT '原文件ID',
    business_type VARCHAR(64)  NULL COMMENT '原业务类型',
    business_id   BIGINT       NULL COMMENT '原业务ID',
    custom_path   VARCHAR(512) NULL COMMENT '原自定义路径',
    reason        VARCHAR(255) NULL COMMENT '冲突原因',
    created_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='附件迁移冲突清单';
```

---

## 3. 后端实现

### 3.1 业务类型注册表

文件：`src/main/java/com/tonghui/erp/Common/Constants/BizFileKinds.java`

```java
package com.tonghui.erp.Common.Constants;

/**
 * 文件槽位常量
 * <p>
 * 与 {@link BizFileType} 中声明的槽位保持一致，前端也维护同名常量。
 * </p>
 */
public final class BizFileKinds {

    /** 默认槽位 */
    public static final String DEFAULT = "DEFAULT";

    /** 随货清单 */
    public static final String WAYBILL = "WAYBILL";

    /** 发票 */
    public static final String INVOICE = "INVOICE";

    /** 检验报告 */
    public static final String INSPECTION_REPORT = "INSPECTION_REPORT";

    /** 生产批记录 */
    public static final String BATCH_RECORD = "BATCH_RECORD";

    /** 其他文件 */
    public static final String OTHER = "OTHER";

    /** 工序附件 */
    public static final String PROCESS_ATTACHMENT = "PROCESS_ATTACHMENT";

    /** 工艺规程 */
    public static final String PROCESS_SPEC = "PROCESS_SPEC";

    /** 工艺规程流程图 */
    public static final String PROCESS_FLOWCHART = "PROCESS_FLOWCHART";

    /** 批记录模板 */
    public static final String BATCH_RECORD_TEMPLATE = "BATCH_RECORD_TEMPLATE";

    /** 验证方案 */
    public static final String VERIFICATION_PLAN = "VERIFICATION_PLAN";

    /** 验证模板 */
    public static final String VERIFICATION_TEMPLATE = "VERIFICATION_TEMPLATE";

    /** 检验模板 */
    public static final String INSPECTION_TEMPLATE = "INSPECTION_TEMPLATE";

    /** 质量标准 */
    public static final String QUALITY_STANDARD = "QUALITY_STANDARD";

    /** 许可证扫描件 */
    public static final String LICENSE_SCAN = "LICENSE_SCAN";

    /** 照片 */
    public static final String PHOTO = "PHOTO";

    /** 人员档案附件 */
    public static final String ATTACHMENT = "ATTACHMENT";

    /** 健康档案 */
    public static final String HEALTH_FILE = "HEALTH_FILE";

    /** 证书文件 */
    public static final String CERTIFICATE_FILE = "CERTIFICATE_FILE";

    /** 消毒记录 */
    public static final String DISINFECTION = "DISINFECTION";

    /** 洁净检测 */
    public static final String CLEAN_INSPECTION = "CLEAN_INSPECTION";

    /** 清洁记录 */
    public static final String CLEANING_RECORD = "CLEANING_RECORD";

    private BizFileKinds() {
    }
}
```

文件：`src/main/java/com/tonghui/erp/Common/Constants/BizFileType.java`

```java
package com.tonghui.erp.Common.Constants;

import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 业务附件类型注册表
 * <p>
 * 一表一码：每个业务表只对应一个编码，从根上避免“同一 business_type 被多张表复用”。
 * 每个类型声明允许的文件槽位及每个槽位的最大数量（-1 表示不限）。
 * </p>
 */
@Getter
public enum BizFileType {

    // region 货物验收
    // ===================================
    // 货物验收
    // ===================================

    /** 货物验收单 */
    ACCEPTANCE_ORDER("ACCEPTANCE_ORDER", "acceptance_order", "acceptance_id", "货物验收单", "货物验收/验收单",
            kinds(BizFileKinds.WAYBILL, 10, BizFileKinds.INVOICE, 10, BizFileKinds.INSPECTION_REPORT, 10)),

    // endregion

    // region 质量管理
    // ===================================
    // 质量管理
    // ===================================

    /** 销毁记录 */
    DESTRUCTION_RECORD("DESTRUCTION_RECORD", "destruction_record", "id", "销毁记录", "质量管理/销毁记录",
            kinds(BizFileKinds.DEFAULT, 10)),

    /** 偏差记录 */
    DEVIATION_RECORD("DEVIATION_RECORD", "deviation_record", "id", "偏差记录", "质量管理/偏差记录",
            kinds(BizFileKinds.DEFAULT, 10)),

    /** 请检单 */
    INSPECTION_REQUEST("INSPECTION_REQUEST", "inspection_request", "id", "请检单", "质量管理/请检单",
            kinds(BizFileKinds.DEFAULT, 10)),

    /** 取样记录 */
    SAMPLING_RECORD("SAMPLING_RECORD", "sampling_record", "id", "取样记录", "质量管理/取样记录",
            kinds(BizFileKinds.DEFAULT, 10)),

    /** 审核放行 */
    RELEASE_REVIEW("RELEASE_REVIEW", "release_review", "id", "审核放行", "审核放行/放行记录",
            kinds(BizFileKinds.DEFAULT, 10)),

    /** 留样记录 */
    RETAINED_SAMPLE("RETAINED_SAMPLE", "retained_sample", "id", "留样记录", "留样管理/留样记录",
            kinds(BizFileKinds.DEFAULT, 10)),

    /** 验证记录 */
    VERIFICATION_RECORD("VERIFICATION_RECORD", "verification_record", "id", "验证记录", "验证方案/验证记录",
            kinds(BizFileKinds.DEFAULT, 10)),

    // endregion

    // region 制剂与剂型
    // ===================================
    // 制剂与剂型
    // ===================================

    /** 剂型 */
    DOSAGE_FORM("DOSAGE_FORM", "dosage_form", "dosage_id", "剂型", "剂型信息/附件",
            kinds(BizFileKinds.BATCH_RECORD_TEMPLATE, 1,
                  BizFileKinds.VERIFICATION_TEMPLATE, 1,
                  BizFileKinds.INSPECTION_TEMPLATE, 1)),

    /** 制剂 */
    PREPARATION("PREPARATION", "preparation", "preparation_id", "制剂", "制剂管理/附件",
            kinds(BizFileKinds.PROCESS_SPEC, 1,
                  BizFileKinds.BATCH_RECORD_TEMPLATE, 1,
                  BizFileKinds.VERIFICATION_PLAN, 1,
                  BizFileKinds.QUALITY_STANDARD, 1,
                  BizFileKinds.PROCESS_FLOWCHART, 1)),

    /** 制剂工序模板 */
    PREPARATION_PROCESS_TEMPLATE("PREPARATION_PROCESS_TEMPLATE", "preparation_process_template", "template_id",
            "制剂工序模板", "制剂管理/工序模板", kinds(BizFileKinds.DEFAULT, 1)),

    // endregion

    // region 生产管理
    // ===================================
    // 生产管理
    // ===================================

    /** 生产计划 */
    PRODUCTION_PLAN("PRODUCTION_PLAN", "production_plan", "id", "生产计划", "生产管理/计划",
            kinds(BizFileKinds.DEFAULT, 10)),

    /** 工单 */
    WORK_ORDER("WORK_ORDER", "work_order", "work_order_id", "生产任务", "生产管理/工单",
            kinds(BizFileKinds.BATCH_RECORD, 10, BizFileKinds.OTHER, 10)),

    /** 工单工序执行行 */
    WORK_ORDER_PROCESS_EXECUTION("WORK_ORDER_PROCESS_EXECUTION", "work_order_process_execution", "id",
            "工单工序", "生产管理/工序附件", kinds(BizFileKinds.PROCESS_ATTACHMENT, 1)),

    /** 生产单位 */
    PRODUCTION_UNIT("PRODUCTION_UNIT", "production_unit", "prod_unit_id", "生产单位", "生产管理/生产单位",
            kinds(BizFileKinds.DEFAULT, 10)),

    // endregion

    // region 设备与能耗
    // ===================================
    // 设备与能耗
    // ===================================

    /** 设备 */
    EQUIPMENT("EQUIPMENT", "equipment", "equipment_id", "设备", "设备管理/设备附件",
            kinds(BizFileKinds.DEFAULT, 10)),

    /** 设备维保 */
    EQUIPMENT_MAINTENANCE("EQUIPMENT_MAINTENANCE", "equipment_maintenance", "maintenance_id", "设备维保",
            "设备管理/维保", kinds(BizFileKinds.DEFAULT, 10)),

    /** 能耗记录 */
    ENERGY_RECORD("ENERGY_RECORD", "energy_record", "record_id", "能耗记录", "能耗管理/能耗记录",
            kinds(BizFileKinds.DEFAULT, 10)),

    /** 养护记录 */
    MAINTENANCE_RECORD("MAINTENANCE_RECORD", "maintenance_record", "id", "养护记录", "库存管理/养护记录",
            kinds(BizFileKinds.DEFAULT, 10)),

    /** 养护记录明细 */
    MAINTENANCE_RECORD_DETAIL("MAINTENANCE_RECORD_DETAIL", "maintenance_record_detail", "id", "养护明细",
            "库存管理/养护明细", kinds(BizFileKinds.DEFAULT, 10)),

    // endregion

    // region 机构、人员、岗位
    // ===================================
    // 机构、人员、岗位
    // ===================================

    /** 机构信息 */
    ORGANIZATION("ORGANIZATION", "organization", "id", "机构信息", "机构管理/机构信息",
            kinds(BizFileKinds.LICENSE_SCAN, 5, BizFileKinds.PHOTO, 5)),

    /** 机构证书 */
    ORGANIZATION_CERTIFICATE("ORGANIZATION_CERTIFICATE", "organization_certificate", "id", "机构证书",
            "机构管理/机构证书", kinds(BizFileKinds.DEFAULT, 5)),

    /** 人员档案 */
    PERSONNEL_FILE("PERSONNEL_FILE", "personnel_file", "personnel_file_id", "人员档案", "人员管理/人员档案",
            kinds(BizFileKinds.ATTACHMENT, 10, BizFileKinds.HEALTH_FILE, 10)),

    /** 人员证书 */
    PERSONNEL_CERTIFICATE("PERSONNEL_CERTIFICATE", "personnel_certificate", "certificate_id", "人员证书",
            "人员管理/证书", kinds(BizFileKinds.DEFAULT, 5)),

    /** 岗位 */
    POSITION("POSITION", "position", "position_id", "岗位", "岗位信息/岗位附件",
            kinds(BizFileKinds.DEFAULT, 5)),

    /** 职务 */
    DUTY("DUTY", "duty", "duty_id", "职务", "职务信息/职务附件",
            kinds(BizFileKinds.DEFAULT, 5)),

    // endregion

    // region 采购与供应商
    // ===================================
    // 采购与供应商
    // ===================================

    /** 采购订单 */
    PURCHASE_ORDER("PURCHASE_ORDER", "purchase_orders", "id", "采购订单", "采购管理/订单",
            kinds(BizFileKinds.DEFAULT, 10)),

    /** 供应商信息 */
    PURCHASE_SUPPLIER("PURCHASE_SUPPLIER", "purchase_suppliers", "id", "供应商信息", "供应商管理/供应商信息",
            kinds(BizFileKinds.DEFAULT, 10)),

    /** 供应商审计 */
    SUPPLIER_AUDIT("SUPPLIER_AUDIT", "supplier_audit", "id", "供应商审计", "供应商管理/审计",
            kinds(BizFileKinds.DEFAULT, 10)),

    // endregion

    // region 车间环境
    // ===================================
    // 车间环境
    // ===================================

    /** 房间信息 */
    ROOM_INFO("ROOM_INFO", "room_info", "room_id", "房间信息", "车间环境/房间信息",
            kinds(BizFileKinds.DISINFECTION, 10,
                  BizFileKinds.CLEAN_INSPECTION, 10,
                  BizFileKinds.CLEANING_RECORD, 10)),

    /** 清洁记录 */
    CLEANING_RECORD("CLEANING_RECORD", "cleaning_record", "id", "清洁记录", "车间环境/清洁记录",
            kinds(BizFileKinds.DEFAULT, 10)),

    /** 洁净检测记录 */
    CLEAN_INSPECTION_RECORD("CLEAN_INSPECTION_RECORD", "clean_inspection_record", "id", "洁净检测记录",
            "车间环境/洁净检测", kinds(BizFileKinds.DEFAULT, 10)),

    // endregion

    // region 培训与审批
    // ===================================
    // 培训与审批
    // ===================================

    /** 培训记录 */
    TRAINING_RECORD("TRAINING_RECORD", "training_record", "id", "培训记录", "培训管理/培训记录",
            kinds(BizFileKinds.DEFAULT, 10)),

    /**
     * 审批实例
     * <p>
     * 审批节点属于流程定义，多个实例共用同一节点 ID，因此 biz_id 存实例 ID，
     * ref_id 存节点 ID；查询整个实例的附件时不传 refId，查询单个节点时传 refId。
     * </p>
     */
    APPROVAL_INSTANCE("APPROVAL_INSTANCE", "approval_instance", "id", "审批实例", "审批管理/审批附件",
            kinds(BizFileKinds.DEFAULT, 20)),

    // endregion
    ;

    /** 业务类型编码 */
    private final String code;

    /** 业务表名（仅来自枚举常量，可安全拼接 SQL） */
    private final String tableName;

    /** 业务表主键列名（仅来自枚举常量，可安全拼接 SQL） */
    private final String pkColumn;

    /** 显示名称 */
    private final String displayName;

    /** 磁盘存储目录（中文，支持多级） */
    private final String storageDir;

    /** 槽位 -> 最大数量（-1 表示不限） */
    private final Map<String, Integer> kindMaxCount;

    BizFileType(String code, String tableName, String pkColumn, String displayName,
                String storageDir, Map<String, Integer> kindMaxCount) {
        this.code = code;
        this.tableName = tableName;
        this.pkColumn = pkColumn;
        this.displayName = displayName;
        this.storageDir = storageDir;
        this.kindMaxCount = Collections.unmodifiableMap(new LinkedHashMap<>(kindMaxCount));
    }

    /**
     * 按编码查找业务类型
     *
     * @param code 业务类型编码
     * @return 业务类型；编码不存在时返回 null，由调用方给出统一错误信息
     */
    public static BizFileType resolve(String code) {
        return INDEX.get(code);
    }

    /**
     * 校验并规范化槽位
     *
     * @param kind 槽位（可为空，默认 DEFAULT）
     * @return 规范化后的槽位
     * @throws IllegalArgumentException 槽位不合法
     */
    public String normalizeKind(String kind) {
        String normalized = (kind == null || kind.isBlank())
                ? BizFileKinds.DEFAULT
                : kind.trim().toUpperCase(Locale.ROOT);
        if (!kindMaxCount.containsKey(normalized)) {
            throw new IllegalArgumentException("业务类型 " + code + " 不支持文件槽位: " + normalized);
        }
        return normalized;
    }

    /**
     * 获取槽位最大数量
     *
     * @param kind 槽位
     * @return 最大数量，-1 表示不限
     */
    public int maxCount(String kind) {
        return kindMaxCount.getOrDefault(kind, -1);
    }

    /** 编码索引 */
    private static final Map<String, BizFileType> INDEX = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(BizFileType::getCode, type -> type));

    /**
     * 构造槽位配置
     *
     * @param pairs 槽位、数量交替出现
     * @return 槽位配置
     */
    private static Map<String, Integer> kinds(Object... pairs) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((String) pairs[i], (Integer) pairs[i + 1]);
        }
        return map;
    }
}
```

> 说明：`resolve` 返回 `null` 时由 Service 抛 `IllegalArgumentException`，比在枚举里抛更容易带出统一错误信息。

### 3.2 实体

文件：`src/main/java/com/tonghui/erp/Data/Entity/BusinessFile.java`

```java
package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业务附件关系表
 * <p>
 * 只表达“业务记录 -> 文件”的绑定关系；物理文件元数据仍在 file_info。
 * 关系行解绑时物理删除，is_deleted 固定为 0，供 MyBatis-Plus 查询过滤。
 * </p>
 *
 * @TableName business_file
 */
@TableName(value = "business_file")
@Data
@EqualsAndHashCode(callSuper = true)
public class BusinessFile extends AuditEntity {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 关联ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 业务类型编码（一表一码，由 BizFileType 注册表校验）
     */
    @TableField(value = "biz_type")
    private String bizType;

    /**
     * 业务表主键值
     */
    @TableField(value = "biz_id")
    private Long bizId;

    /**
     * 子对象ID（如审批节点ID），无子对象时为0
     */
    @TableField(value = "ref_id")
    private Long refId;

    /**
     * 文件ID（file_info.file_id）
     */
    @TableField(value = "file_id")
    private Long fileId;

    /**
     * 文件槽位/用途（如 WAYBILL、PROCESS_ATTACHMENT）
     */
    @TableField(value = "file_kind")
    private String fileKind;

    /**
     * 排序号
     */
    @TableField(value = "sort_order")
    private Integer sortOrder;

    // endregion

    // region 状态字段
    // ===================================
    // 状态字段
    // ===================================

    /**
     * 是否删除（关系行实际物理删除，该字段固定为0）
     */
    @TableField(value = "is_deleted")
    private Integer isDeleted;

    // endregion
}
```

### 3.3 Mapper

文件：`src/main/java/com/tonghui/erp/Data/mapper/BusinessFileMapper.java`

```java
package com.tonghui.erp.Data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tonghui.erp.Data.Entity.BusinessFile;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 业务附件关系数据访问接口
 * <p>
 * 关系行使用原生 DELETE 物理删除，避免全局逻辑删除改写导致唯一键冲突。
 * </p>
 */
public interface BusinessFileMapper extends BaseMapper<BusinessFile> {

    /**
     * 按关联ID物理删除
     *
     * @param id 关联ID
     * @return 删除行数
     */
    @Delete("DELETE FROM business_file WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);

    /**
     * 按业务对象物理删除
     * <p>
     * refId 为 null 时删除该业务对象下所有子对象的关联；
     * refId 为 0 时只删除 ref_id = 0 的关联；
     * fileKind 为 null 时删除所有槽位。
     * </p>
     *
     * @param bizType  业务类型编码
     * @param bizId    业务ID
     * @param refId    子对象ID（可为 null，表示不限子对象）
     * @param fileKind 槽位（可为 null，表示不限槽位）
     * @return 删除行数
     */
    @Delete("<script>DELETE FROM business_file WHERE biz_type = #{bizType} AND biz_id = #{bizId}"
            + "<if test='refId != null'> AND ref_id = #{refId}</if>"
            + "<if test='fileKind != null'> AND file_kind = #{fileKind}</if></script>")
    int physicalDelete(@Param("bizType") String bizType,
                       @Param("bizId") Long bizId,
                       @Param("refId") Long refId,
                       @Param("fileKind") String fileKind);

    /**
     * 统计文件被多少条业务关系引用
     *
     * @param fileId 文件ID
     * @return 引用数
     */
    @Select("SELECT COUNT(*) FROM business_file WHERE file_id = #{fileId} AND is_deleted = 0")
    long countByFileId(@Param("fileId") Long fileId);

    /**
     * 获取下一个排序号
     *
     * @param bizType  业务类型编码
     * @param bizId    业务ID
     * @param refId    子对象ID
     * @param fileKind 槽位
     * @return 下一个排序号
     */
    @Select("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM business_file "
            + "WHERE biz_type = #{bizType} AND biz_id = #{bizId} AND ref_id = #{refId} "
            + "AND file_kind = #{fileKind} AND is_deleted = 0")
    int nextSortOrder(@Param("bizType") String bizType,
                      @Param("bizId") Long bizId,
                      @Param("refId") Long refId,
                      @Param("fileKind") String fileKind);
}
```

### 3.4 DTO

文件：`src/main/java/com/tonghui/erp/Common/Dto/BusinessFile/BusinessFileDto.java`

```java
package com.tonghui.erp.Common.Dto.BusinessFile;

import com.tonghui.erp.Data.Entity.BusinessFile;
import com.tonghui.erp.Data.Entity.FileInfo;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 业务附件返回对象
 */
@Data
public class BusinessFileDto {

    /** 关系ID（删除附件时使用） */
    private Long relationId;

    /** 业务类型编码 */
    private String bizType;

    /** 业务ID */
    private Long bizId;

    /** 子对象ID（审批节点ID等，无子对象时为0） */
    private Long refId;

    /** 文件ID */
    private Long fileId;

    /** 文件槽位 */
    private String fileKind;

    /** 原始文件名 */
    private String originalName;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 内容类型 */
    private String contentType;

    /** 扩展名 */
    private String fileExtension;

    /** 文件描述 */
    private String description;

    /** 下载地址 */
    private String fileUrl;

    /** 预览地址 */
    private String previewUrl;

    /** 上传时间 */
    private LocalDateTime createdTime;

    /**
     * 由关系与文件元数据构建返回对象
     *
     * @param relation 关系
     * @param file     文件元数据
     * @return 返回对象
     */
    public static BusinessFileDto from(BusinessFile relation, FileInfo file) {
        BusinessFileDto dto = new BusinessFileDto();
        dto.setRelationId(relation.getId());
        dto.setBizType(relation.getBizType());
        dto.setBizId(relation.getBizId());
        dto.setRefId(relation.getRefId());
        dto.setFileId(file.getFileId());
        dto.setFileKind(relation.getFileKind());
        dto.setOriginalName(file.getOriginalName());
        dto.setFileSize(file.getFileSize());
        dto.setContentType(file.getContentType());
        dto.setFileExtension(file.getFileExtension());
        dto.setDescription(file.getDescription());
        dto.setFileUrl(file.getFileUrl() != null ? file.getFileUrl() : "/api/files/" + file.getFileId());
        dto.setPreviewUrl("/api/files/preview/" + file.getFileId());
        dto.setCreatedTime(relation.getCreatedTime());
        return dto;
    }
}
```

文件：`src/main/java/com/tonghui/erp/Common/Dto/BusinessFile/BusinessFileSyncDto.java`

```java
package com.tonghui.erp.Common.Dto.BusinessFile;

import lombok.Data;

import java.util.List;

/**
 * 业务附件同步请求（保存单据时一次性绑定附件）
 */
@Data
public class BusinessFileSyncDto {

    /** 业务类型编码 */
    private String bizType;

    /** 业务ID */
    private Long bizId;

    /** 子对象ID（审批节点ID等，可为空） */
    private Long refId;

    /** 文件槽位（可为空，默认 DEFAULT） */
    private String fileKind;

    /** 目标文件ID列表（空列表表示解绑全部） */
    private List<Long> fileIds;
}
```

文件：`src/main/java/com/tonghui/erp/Common/Dto/BusinessFile/BusinessFileBatchListDto.java`

```java
package com.tonghui.erp.Common.Dto.BusinessFile;

import lombok.Data;

import java.util.List;

/**
 * 批量查询业务附件请求
 */
@Data
public class BusinessFileBatchListDto {

    /** 业务类型编码 */
    private String bizType;

    /** 业务ID列表 */
    private List<Long> bizIds;

    /** 文件槽位（可为空） */
    private String fileKind;
}
```

文件：`src/main/java/com/tonghui/erp/Common/Dto/BusinessFile/BizFileTypeDto.java`

```java
package com.tonghui.erp.Common.Dto.BusinessFile;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

/**
 * 业务类型注册表返回对象（供前端生成常量/校验）
 */
@Data
@AllArgsConstructor
public class BizFileTypeDto {

    /** 业务类型编码 */
    private String code;

    /** 显示名称 */
    private String displayName;

    /** 槽位 -> 最大数量（-1 表示不限） */
    private Map<String, Integer> kinds;
}
```

### 3.5 Service 接口

文件：`src/main/java/com/tonghui/erp/Service/BusinessFileService.java`

```java
package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Common.Dto.BusinessFile.BizFileTypeDto;
import com.tonghui.erp.Common.Dto.BusinessFile.BusinessFileDto;
import com.tonghui.erp.Common.Dto.BusinessFile.BusinessFileSyncDto;
import com.tonghui.erp.Data.Entity.BusinessFile;
import com.tonghui.erp.Data.Entity.FileInfo;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 业务附件服务
 */
public interface BusinessFileService extends IService<BusinessFile> {

    // region 上传
    // ===================================
    // 上传
    // ===================================

    /**
     * 上传文件并绑定业务记录
     *
     * @param file        文件
     * @param bizType     业务类型编码
     * @param bizId       业务ID
     * @param refId       子对象ID（可为空）
     * @param fileKind    文件槽位（可为空，默认 DEFAULT）
     * @param entityName  实体名称（用于磁盘目录，可为空）
     * @param description 文件描述（可为空）
     * @return 附件信息
     * @throws IOException 文件读写失败
     */
    BusinessFileDto upload(MultipartFile file, String bizType, Long bizId, Long refId,
                           String fileKind, String entityName, String description) throws IOException;

    /**
     * 临时上传（只写物理文件，不绑定业务；保存单据时通过 sync 绑定）
     *
     * @param file        文件
     * @param bizType     业务类型编码（用于目录和后续校验）
     * @param entityName  实体名称（可为空）
     * @param description 文件描述（可为空）
     * @return 文件元数据
     * @throws IOException 文件读写失败
     */
    FileInfo uploadTemp(MultipartFile file, String bizType, String entityName, String description) throws IOException;

    // endregion

    // region 查询
    // ===================================
    // 查询
    // ===================================

    /**
     * 查询业务附件
     *
     * @param bizType  业务类型编码
     * @param bizId    业务ID
     * @param refId    子对象ID（为空或0时不按子对象过滤）
     * @param fileKind 文件槽位（可为空）
     * @return 附件列表
     */
    List<BusinessFileDto> list(String bizType, Long bizId, Long refId, String fileKind);

    /**
     * 批量查询多个业务记录的附件
     *
     * @param bizType  业务类型编码
     * @param bizIds   业务ID列表
     * @param fileKind 文件槽位（可为空）
     * @return 业务ID -> 附件列表
     */
    Map<Long, List<BusinessFileDto>> listBatch(String bizType, List<Long> bizIds, String fileKind);

    // endregion

    // region 同步与删除
    // ===================================
    // 同步与删除
    // ===================================

    /**
     * 同步附件（保存单据时调用，差量解绑/绑定）
     *
     * @param dto 同步请求
     * @return 同步后的附件列表
     */
    List<BusinessFileDto> sync(BusinessFileSyncDto dto);

    /**
     * 解绑并删除单个附件
     *
     * @param relationId 关系ID
     */
    void delete(Long relationId);

    /**
     * 解绑某业务对象的全部附件（业务记录删除时调用）
     *
     * @param bizType  业务类型编码
     * @param bizId    业务ID
     * @param refId    子对象ID；null 表示该业务对象下的所有子对象（审批实例删除时传 null），
     *                 0 或具体值表示只解绑该子对象
     * @param fileKind 文件槽位（可为空，为空时解绑全部槽位）
     * @return 解绑数量
     */
    int unbindAll(String bizType, Long bizId, Long refId, String fileKind);

    // endregion

    // region 注册表
    // ===================================
    // 注册表
    // ===================================

    /**
     * 获取全部业务类型定义
     *
     * @return 业务类型列表
     */
    List<BizFileTypeDto> listTypes();

    // endregion
}
```

### 3.6 Service 实现

文件：`src/main/java/com/tonghui/erp/Service/impl/BusinessFileServiceImpl.java`

```java
package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.Constants.BizFileType;
import com.tonghui.erp.Common.Dto.BusinessFile.BizFileTypeDto;
import com.tonghui.erp.Common.Dto.BusinessFile.BusinessFileDto;
import com.tonghui.erp.Common.Dto.BusinessFile.BusinessFileSyncDto;
import com.tonghui.erp.Data.Entity.BusinessFile;
import com.tonghui.erp.Data.Entity.FileInfo;
import com.tonghui.erp.Data.mapper.BusinessFileMapper;
import com.tonghui.erp.Service.BusinessFileService;
import com.tonghui.erp.Service.FileInfoService;
import com.tonghui.erp.Service.FileOperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 业务附件服务实现
 * <p>
 * 关系表为唯一查询来源；file_info.business_type / business_id 不再参与业务查询。
 * </p>
 */
@Service
public class BusinessFileServiceImpl extends ServiceImpl<BusinessFileMapper, BusinessFile>
        implements BusinessFileService {

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    /** 文件信息服务 */
    @Autowired
    private FileInfoService fileInfoService;

    /** 文件操作日志服务 */
    @Autowired
    private FileOperationLogService fileOperationLogService;

    /** 用于校验父记录是否存在（表名/列名来自枚举，无注入风险） */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // endregion

    // region 上传
    // ===================================
    // 上传
    // ===================================

    /**
     * 上传文件并绑定业务记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessFileDto upload(MultipartFile file, String bizType, Long bizId, Long refId,
                                  String fileKind, String entityName, String description) throws IOException {
        BizFileType type = resolveType(bizType);
        String kind = type.normalizeKind(fileKind);
        long ref = refId == null ? 0L : refId;
        validateParent(type, bizId, ref);

        // 槽位数量校验
        long exists = count(new LambdaQueryWrapper<BusinessFile>()
                .eq(BusinessFile::getBizType, type.getCode())
                .eq(BusinessFile::getBizId, bizId)
                .eq(BusinessFile::getRefId, ref)
                .eq(BusinessFile::getFileKind, kind));
        int max = type.maxCount(kind);
        if (max >= 0 && exists >= max) {
            throw new IllegalArgumentException(
                    type.getDisplayName() + " 的 " + kind + " 槽位最多允许 " + max + " 个文件");
        }

        FileInfo fileInfo = null;
        try {
            // 物理文件入库（不写业务关联）
            fileInfo = fileInfoService.uploadFileToDir(file, type.getStorageDir(),
                    StringUtils.hasText(entityName) ? entityName : type.getDisplayName(), description);

            BusinessFile relation = new BusinessFile();
            relation.setBizType(type.getCode());
            relation.setBizId(bizId);
            relation.setRefId(ref);
            relation.setFileId(fileInfo.getFileId());
            relation.setFileKind(kind);
            relation.setSortOrder(baseMapper.nextSortOrder(type.getCode(), bizId, ref, kind));
            relation.setIsDeleted(0);
            save(relation);

            fileOperationLogService.log(fileInfo.getFileId(), fileInfo.getOriginalName(),
                    fileInfo.getFilePath(), "UPLOAD", type.getCode(), null);
            return BusinessFileDto.from(relation, fileInfo);
        } catch (IOException | RuntimeException e) {
            // 关系写入失败时回收刚写入的文件记录，避免孤儿文件
            if (fileInfo != null && fileInfo.getFileId() != null) {
                fileInfoService.deleteFile(fileInfo.getFileId());
            }
            throw e;
        }
    }

    /**
     * 临时上传（不绑定业务）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileInfo uploadTemp(MultipartFile file, String bizType, String entityName,
                               String description) throws IOException {
        BizFileType type = resolveType(bizType);
        FileInfo fileInfo = fileInfoService.uploadFileToDir(file, type.getStorageDir(),
                StringUtils.hasText(entityName) ? entityName : type.getDisplayName(), description);
        fileOperationLogService.log(fileInfo.getFileId(), fileInfo.getOriginalName(),
                fileInfo.getFilePath(), "UPLOAD", type.getCode(), "临时上传");
        return fileInfo;
    }

    // endregion

    // region 查询
    // ===================================
    // 查询
    // ===================================

    /**
     * 查询业务附件
     */
    @Override
    public List<BusinessFileDto> list(String bizType, Long bizId, Long refId, String fileKind) {
        BizFileType type = resolveType(bizType);
        if (bizId == null || bizId <= 0) {
            return Collections.emptyList();
        }
        long ref = refId == null ? 0L : refId;
        String kind = StringUtils.hasText(fileKind) ? type.normalizeKind(fileKind) : null;

        List<BusinessFile> relations = list(new LambdaQueryWrapper<BusinessFile>()
                .eq(BusinessFile::getBizType, type.getCode())
                .eq(BusinessFile::getBizId, bizId)
                .eq(ref > 0, BusinessFile::getRefId, ref)
                .eq(kind != null, BusinessFile::getFileKind, kind)
                .orderByAsc(BusinessFile::getSortOrder)
                .orderByAsc(BusinessFile::getId));
        return toDtoList(relations);
    }

    /**
     * 批量查询多个业务记录的附件
     */
    @Override
    public Map<Long, List<BusinessFileDto>> listBatch(String bizType, List<Long> bizIds, String fileKind) {
        if (bizIds == null || bizIds.isEmpty()) {
            return Collections.emptyMap();
        }
        BizFileType type = resolveType(bizType);
        String kind = StringUtils.hasText(fileKind) ? type.normalizeKind(fileKind) : null;

        Map<Long, List<BusinessFileDto>> result = new LinkedHashMap<>();
        bizIds.stream().filter(Objects::nonNull).distinct().forEach(id -> result.put(id, new ArrayList<>()));
        if (result.isEmpty()) {
            return result;
        }

        List<BusinessFile> relations = list(new LambdaQueryWrapper<BusinessFile>()
                .eq(BusinessFile::getBizType, type.getCode())
                .in(BusinessFile::getBizId, result.keySet())
                .eq(kind != null, BusinessFile::getFileKind, kind)
                .orderByAsc(BusinessFile::getSortOrder)
                .orderByAsc(BusinessFile::getId));
        for (BusinessFileDto dto : toDtoList(relations)) {
            result.computeIfAbsent(dto.getBizId(), key -> new ArrayList<>()).add(dto);
        }
        return result;
    }

    // endregion

    // region 同步与删除
    // ===================================
    // 同步与删除
    // ===================================

    /**
     * 同步附件（保存单据时调用）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<BusinessFileDto> sync(BusinessFileSyncDto dto) {
        if (dto == null || dto.getBizId() == null) {
            throw new IllegalArgumentException("同步附件缺少业务ID");
        }
        BizFileType type = resolveType(dto.getBizType());
        String kind = type.normalizeKind(dto.getFileKind());
        long ref = dto.getRefId() == null ? 0L : dto.getRefId();
        validateParent(type, dto.getBizId(), ref);

        List<Long> fileIds = dto.getFileIds() == null ? Collections.emptyList()
                : dto.getFileIds().stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        int max = type.maxCount(kind);
        if (max >= 0 && fileIds.size() > max) {
            throw new IllegalArgumentException(
                    type.getDisplayName() + " 的 " + kind + " 槽位最多允许 " + max + " 个文件");
        }
        if (!fileIds.isEmpty()) {
            List<FileInfo> files = fileInfoService.listByIds(fileIds);
            if (files.size() != fileIds.size()) {
                throw new IllegalArgumentException("存在无效或已删除的文件ID");
            }
        }

        // 先全部解绑，再按目标顺序重建，保证顺序与前端一致
        baseMapper.physicalDelete(type.getCode(), dto.getBizId(), ref, kind);
        int sort = 0;
        for (Long fileId : fileIds) {
            BusinessFile relation = new BusinessFile();
            relation.setBizType(type.getCode());
            relation.setBizId(dto.getBizId());
            relation.setRefId(ref);
            relation.setFileId(fileId);
            relation.setFileKind(kind);
            relation.setSortOrder(sort++);
            relation.setIsDeleted(0);
            save(relation);
        }
        return list(type.getCode(), dto.getBizId(), ref, kind);
    }

    /**
     * 解绑并删除单个附件
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long relationId) {
        BusinessFile relation = getById(relationId);
        if (relation == null) {
            throw new IllegalArgumentException("附件关联不存在");
        }
        FileInfo fileInfo = fileInfoService.getById(relation.getFileId());
        baseMapper.physicalDeleteById(relationId);

        // 文件不再被任何业务引用时，软删除文件记录（磁盘文件由文件管理模块回收）
        if (baseMapper.countByFileId(relation.getFileId()) == 0) {
            fileInfoService.deleteFile(relation.getFileId());
            if (fileInfo != null) {
                fileOperationLogService.log(fileInfo.getFileId(), fileInfo.getOriginalName(),
                        fileInfo.getFilePath(), "DELETE", relation.getBizType(), null);
            }
        }
    }

    /**
     * 解绑某业务对象的全部附件
     * <p>
     * refId 为 null 时解绑该业务对象下所有子对象（审批实例删除时使用）；
     * refId 为 0 或具体值时只解绑该子对象。
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int unbindAll(String bizType, Long bizId, Long refId, String fileKind) {
        BizFileType type = resolveType(bizType);
        if (bizId == null || bizId <= 0) {
            return 0;
        }
        String kind = StringUtils.hasText(fileKind) ? type.normalizeKind(fileKind) : null;

        List<BusinessFile> relations = list(new LambdaQueryWrapper<BusinessFile>()
                .eq(BusinessFile::getBizType, type.getCode())
                .eq(BusinessFile::getBizId, bizId)
                .eq(refId != null, BusinessFile::getRefId, refId)
                .eq(kind != null, BusinessFile::getFileKind, kind));
        if (relations.isEmpty()) {
            return 0;
        }
        baseMapper.physicalDelete(type.getCode(), bizId, refId, kind);
        for (BusinessFile relation : relations) {
            if (baseMapper.countByFileId(relation.getFileId()) == 0) {
                fileInfoService.deleteFile(relation.getFileId());
            }
        }
        return relations.size();
    }

    // endregion

    // region 注册表
    // ===================================
    // 注册表
    // ===================================

    /**
     * 获取全部业务类型定义
     */
    @Override
    public List<BizFileTypeDto> listTypes() {
        return Arrays.stream(BizFileType.values())
                .map(type -> new BizFileTypeDto(type.getCode(), type.getDisplayName(), type.getKindMaxCount()))
                .collect(Collectors.toList());
    }

    // endregion

    // region 私有方法
    // ===================================
    // 私有方法
    // ===================================

    /**
     * 解析并校验业务类型
     *
     * @param code 业务类型编码
     * @return 业务类型
     */
    private BizFileType resolveType(String code) {
        BizFileType type = StringUtils.hasText(code) ? BizFileType.resolve(code) : null;
        if (type == null) {
            throw new IllegalArgumentException("不支持的业务类型: " + code);
        }
        return type;
    }

    /**
     * 校验父记录存在且未删除
     *
     * @param type  业务类型
     * @param bizId 业务ID
     * @param refId 子对象ID
     */
    private void validateParent(BizFileType type, Long bizId, long refId) {
        if (bizId == null || bizId <= 0) {
            throw new IllegalArgumentException("业务ID不能为空");
        }
        if (type == BizFileType.APPROVAL_INSTANCE && refId > 0) {
            // 审批：节点必须存在，且节点所属工作流与实例一致
            String sql = "SELECT COUNT(*) FROM approval_node n "
                    + "JOIN approval_instance i ON i.workflow_id = n.workflow_id "
                    + "WHERE n.id = ? AND i.id = ? AND n.is_deleted = 0 AND i.is_deleted = 0";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, refId, bizId);
            if (count == null || count == 0) {
                throw new IllegalArgumentException("审批节点不存在或不属于该审批实例");
            }
            return;
        }
        String sql = "SELECT COUNT(*) FROM " + type.getTableName()
                + " WHERE " + type.getPkColumn() + " = ? AND is_deleted = 0";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, bizId);
        if (count == null || count == 0) {
            throw new IllegalArgumentException("业务记录不存在或已删除：" + type.getDisplayName());
        }
    }

    /**
     * 关系列表转返回对象
     *
     * @param relations 关系列表
     * @return 返回对象列表
     */
    private List<BusinessFileDto> toDtoList(List<BusinessFile> relations) {
        if (relations.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> fileIds = relations.stream().map(BusinessFile::getFileId).distinct().collect(Collectors.toList());
        Map<Long, FileInfo> fileMap = fileInfoService.listByIds(fileIds).stream()
                .collect(Collectors.toMap(FileInfo::getFileId, file -> file));
        return relations.stream()
                .map(relation -> {
                    FileInfo file = fileMap.get(relation.getFileId());
                    return file == null ? null : BusinessFileDto.from(relation, file);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // endregion
}
```

### 3.7 Controller

文件：`src/main/java/com/tonghui/erp/Controller/BusinessFileController.java`

```java
package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.BusinessFile.BizFileTypeDto;
import com.tonghui.erp.Common.Dto.BusinessFile.BusinessFileBatchListDto;
import com.tonghui.erp.Common.Dto.BusinessFile.BusinessFileDto;
import com.tonghui.erp.Common.Dto.BusinessFile.BusinessFileSyncDto;
import com.tonghui.erp.Data.Entity.FileInfo;
import com.tonghui.erp.Service.BusinessFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 业务附件控制器
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────────┬────────┬─────────────────────────────┐
 * │ #  │ 接口                                 │ 方法   │ 说明                        │
 * ├────┼──────────────────────────────────────┼────────┼─────────────────────────────┤
 * │ 1  │ /api/business-files/upload           │ POST   │ 上传并绑定业务记录          │
 * │ 2  │ /api/business-files/upload-temp      │ POST   │ 临时上传（保存时再绑定）    │
 * │ 3  │ /api/business-files                  │ GET    │ 查询业务附件                │
 * │ 4  │ /api/business-files/batch-list       │ POST   │ 批量查询多个业务记录的附件  │
 * │ 5  │ /api/business-files/sync             │ POST   │ 保存单据时同步附件          │
 * │ 6  │ /api/business-files/{id}             │ DELETE │ 解绑并删除附件              │
 * │ 7  │ /api/business-files/types            │ GET    │ 业务类型注册表              │
 * └────┴──────────────────────────────────────┴────────┴─────────────────────────────┘
 */
@RestController
@RequestMapping("/api/business-files")
public class BusinessFileController extends BaseController {

    /** 业务附件服务 */
    @Autowired
    private BusinessFileService businessFileService;

    // region 上传
    // ===================================
    // 上传
    // ===================================

    /**
     * 上传并绑定业务附件
     *
     * 示例请求：
     * POST /api/business-files/upload
     * Content-Type: multipart/form-data
     * body:
     *   file:         [选择文件]
     *   bizType:      WORK_ORDER_PROCESS_EXECUTION
     *   bizId:        123
     *   refId:        0
     *   fileKind:     PROCESS_ATTACHMENT
     *   entityName:   工单工序
     *   description:  工序附件
     *
     * @param file        文件
     * @param bizType     业务类型编码
     * @param bizId       业务ID
     * @param refId       子对象ID（可选）
     * @param fileKind    文件槽位（可选，默认 DEFAULT）
     * @param entityName  实体名称（可选，用于目录）
     * @param description 文件描述（可选）
     * @return 附件信息
     */
    @PostMapping("/upload")
    public ApiResponse<BusinessFileDto> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam String bizType,
            @RequestParam Long bizId,
            @RequestParam(required = false) Long refId,
            @RequestParam(required = false) String fileKind,
            @RequestParam(required = false) String entityName,
            @RequestParam(required = false) String description) {
        try {
            return success(businessFileService.upload(file, bizType, bizId, refId, fileKind, entityName, description),
                    "文件上传成功");
        } catch (IOException e) {
            return exception(e, "文件上传");
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }

    /**
     * 临时上传（不绑定业务，保存单据时通过 sync 绑定）
     *
     * 示例请求：
     * POST /api/business-files/upload-temp
     * Content-Type: multipart/form-data
     * body:
     *   file:         [选择文件]
     *   bizType:      WORK_ORDER_PROCESS_EXECUTION
     *   entityName:   工单工序
     *   description:  工序附件
     *
     * @param file        文件
     * @param bizType     业务类型编码
     * @param entityName  实体名称（可选）
     * @param description 文件描述（可选）
     * @return 文件元数据（含 fileId）
     */
    @PostMapping("/upload-temp")
    public ApiResponse<FileInfo> uploadTemp(
            @RequestParam("file") MultipartFile file,
            @RequestParam String bizType,
            @RequestParam(required = false) String entityName,
            @RequestParam(required = false) String description) {
        try {
            return success(businessFileService.uploadTemp(file, bizType, entityName, description),
                    "文件上传成功");
        } catch (IOException e) {
            return exception(e, "文件上传");
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }

    // endregion

    // region 查询
    // ===================================
    // 查询
    // ===================================

    /**
     * 查询业务附件
     *
     * 示例请求：
     * GET /api/business-files?bizType=WORK_ORDER_PROCESS_EXECUTION&bizId=123
     * GET /api/business-files?bizType=APPROVAL_INSTANCE&bizId=88&refId=45
     *
     * @param bizType  业务类型编码
     * @param bizId    业务ID
     * @param refId    子对象ID（可选）
     * @param fileKind 文件槽位（可选）
     * @return 附件列表
     */
    @GetMapping
    public ApiResponse<List<BusinessFileDto>> list(
            @RequestParam String bizType,
            @RequestParam Long bizId,
            @RequestParam(required = false) Long refId,
            @RequestParam(required = false) String fileKind) {
        try {
            return success(businessFileService.list(bizType, bizId, refId, fileKind));
        } catch (Exception e) {
            return exception(e, "查询业务附件");
        }
    }

    /**
     * 批量查询多个业务记录的附件
     *
     * 示例请求：
     * POST /api/business-files/batch-list
     * {
     *   "bizType": "APPROVAL_INSTANCE",
     *   "bizIds": [88, 89, 90]
     * }
     *
     * @param dto 批量查询请求
     * @return 业务ID -> 附件列表
     */
    @PostMapping("/batch-list")
    public ApiResponse<Map<Long, List<BusinessFileDto>>> batchList(@RequestBody BusinessFileBatchListDto dto) {
        try {
            return success(businessFileService.listBatch(dto.getBizType(), dto.getBizIds(), dto.getFileKind()));
        } catch (Exception e) {
            return exception(e, "批量查询业务附件");
        }
    }

    // endregion

    // region 同步与删除
    // ===================================
    // 同步与删除
    // ===================================

    /**
     * 保存单据时同步附件
     *
     * 示例请求：
     * POST /api/business-files/sync
     * {
     *   "bizType": "WORK_ORDER_PROCESS_EXECUTION",
     *   "bizId": 123,
     *   "refId": 0,
     *   "fileKind": "PROCESS_ATTACHMENT",
     *   "fileIds": [101, 102]
     * }
     *
     * @param dto 同步请求
     * @return 同步后的附件列表
     */
    @PostMapping("/sync")
    public ApiResponse<List<BusinessFileDto>> sync(@RequestBody BusinessFileSyncDto dto) {
        try {
            return success(businessFileService.sync(dto), "附件同步成功");
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        } catch (Exception e) {
            return exception(e, "附件同步");
        }
    }

    /**
     * 解绑并删除附件
     *
     * 示例请求：
     * DELETE /api/business-files/1001
     *
     * @param id 关系ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        try {
            businessFileService.delete(id);
            return success(true, "附件删除成功");
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        } catch (Exception e) {
            return exception(e, "附件删除");
        }
    }

    // endregion

    // region 注册表
    // ===================================
    // 注册表
    // ===================================

    /**
     * 获取业务类型注册表
     *
     * 示例请求：
     * GET /api/business-files/types
     *
     * @return 业务类型列表
     */
    @GetMapping("/types")
    public ApiResponse<List<BizFileTypeDto>> types() {
        return success(businessFileService.listTypes());
    }

    // endregion
}
```

### 3.8 扩展文件存储服务

现有 `FileStorageService` 没有“按指定目录保存、不写业务关联”的方法，新增两个方法。

#### 3.8.1 接口新增

`src/main/java/com/tonghui/erp/Service/FileStorageService.java`：

```java
/**
 * 按指定相对目录上传文件（不写业务关联）
 * 路径格式：{basePath}/{relativeDir}/{年}/{月}/{实体名}/{uuid.ext}
 *
 * @param file        文件对象
 * @param relativeDir 相对目录（如“生产管理/工序附件”）
 * @param entityName  实体名称（用于目录名）
 * @param description 文件描述
 * @return 文件信息（未持久化）
 * @throws IOException IO异常
 */
FileInfo uploadFileToDir(MultipartFile file, String relativeDir, String entityName,
                         String description) throws IOException;
```

#### 3.8.2 实现新增

`src/main/java/com/tonghui/erp/Service/impl/FileStorageServiceImpl.java`：

```java
/**
 * 按指定相对目录上传文件（不写业务关联）
 */
@Override
public FileInfo uploadFileToDir(MultipartFile file, String relativeDir, String entityName,
                                String description) throws IOException {
    if (!isAllowedFileType(file)) {
        throw new IllegalArgumentException("不支持的文件类型: " + file.getContentType());
    }
    if (isFileSizeExceeded(file)) {
        throw new IllegalArgumentException("文件大小超出限制: " + file.getSize() + " bytes");
    }

    String originalFilename = file.getOriginalFilename();
    String extension = getFileExtension(originalFilename);
    String storedName = generateStoredName(extension);
    Path storagePath = determineDirPath(relativeDir, entityName, storedName);
    Files.createDirectories(storagePath.getParent());
    Files.copy(file.getInputStream(), storagePath, StandardCopyOption.REPLACE_EXISTING);

    FileInfo fileInfo = new FileInfo();
    fileInfo.setOriginalName(originalFilename);
    fileInfo.setStoredName(storedName);
    fileInfo.setFilePath(storagePath.toString());
    fileInfo.setFileSize(file.getSize());
    fileInfo.setContentType(file.getContentType());
    fileInfo.setFileExtension(extension);
    fileInfo.setCategory(relativeDir);
    fileInfo.setDescription(description);
    fileInfo.setStorageType("LOCAL");
    if (fileStorageConfig.isEnableMd5Check()) {
        fileInfo.setFileMd5(calculateMD5(file));
    }
    return fileInfo;
}

/**
 * 按指定相对目录确定存储路径
 */
private Path determineDirPath(String relativeDir, String entityName, String storedName) {
    String basePath = resolveBasePath();
    String dir = StringUtils.hasText(relativeDir) ? relativeDir : "通用文件";
    String year = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy"));
    String month = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM"));
    String safeName = sanitizeFileName(entityName);

    // 支持“生产管理/工序附件”这类多级目录
    String[] dirParts = dir.split("/");
    String[] pathParts = new String[dirParts.length + 4];
    System.arraycopy(dirParts, 0, pathParts, 0, dirParts.length);
    pathParts[dirParts.length] = year;
    pathParts[dirParts.length + 1] = month;
    pathParts[dirParts.length + 2] = safeName;
    pathParts[dirParts.length + 3] = storedName;
    return Paths.get(basePath, pathParts);
}
```

#### 3.8.3 FileInfoService 新增

`src/main/java/com/tonghui/erp/Service/FileInfoService.java`：

```java
/**
 * 按指定目录上传文件（不写业务关联）
 *
 * @param file        文件
 * @param relativeDir 相对目录
 * @param entityName  实体名称
 * @param description 文件描述
 * @return 保存后的文件信息（含fileId、fileUrl）
 * @throws IOException IO异常
 */
FileInfo uploadFileToDir(MultipartFile file, String relativeDir, String entityName,
                         String description) throws IOException;
```

`src/main/java/com/tonghui/erp/Service/impl/FileInfoServiceImpl.java`：

```java
/**
 * 按指定目录上传文件（不写业务关联）
 */
@Override
@Transactional
public FileInfo uploadFileToDir(MultipartFile file, String relativeDir, String entityName,
                                String description) throws IOException {
    FileInfo fileInfo = fileStorageService.uploadFileToDir(file, relativeDir, entityName, description);
    fileInfo.setFileMd5(fileStorageService.calculateMD5(file));
    fileInfo.setDescription(description);
    save(fileInfo);
    fileInfo.setFileUrl("/api/files/" + fileInfo.getFileId());
    updateById(fileInfo);
    return fileInfo;
}
```

### 3.9 业务删除级联

在各业务 Service 的删除方法中增加解绑调用。以工单为例：

```java
/**
 * 删除工单
 * <p>
 * 先解绑工序附件与工单附件，再删除工单及其子表。
 * </p>
 */
@Override
@Transactional(rollbackFor = Exception.class)
public void delete(Long workOrderId) {
    // 1. 解绑每个工序执行行的附件
    List<WorkOrderProcessExecution> processes = processExecutionMapper.selectList(
            new LambdaQueryWrapper<WorkOrderProcessExecution>()
                    .eq(WorkOrderProcessExecution::getWorkOrderId, workOrderId));
    for (WorkOrderProcessExecution process : processes) {
        businessFileService.unbindAll(BizFileType.WORK_ORDER_PROCESS_EXECUTION.getCode(),
                process.getId(), 0L, null);
    }
    // 2. 解绑工单本身的附件
    businessFileService.unbindAll(BizFileType.WORK_ORDER.getCode(), workOrderId, 0L, null);
    // 3. 删除业务数据
    processExecutionMapper.delete(new LambdaQueryWrapper<WorkOrderProcessExecution>()
            .eq(WorkOrderProcessExecution::getWorkOrderId, workOrderId));
    baseMapper.deleteById(workOrderId);
}
```

其他模块同理：

| 业务 Service | 级联调用 |
|-------------|---------|
| `WorkOrderServiceImpl.delete` | `unbindAll(WORK_ORDER_PROCESS_EXECUTION, processId)` + `unbindAll(WORK_ORDER, workOrderId)` |
| `PreparationServiceImpl.delete` | `unbindAll(PREPARATION, preparationId)` + 每个工序模板 `unbindAll(PREPARATION_PROCESS_TEMPLATE, templateId)` |
| `DosageFormServiceImpl.delete` | `unbindAll(DOSAGE_FORM, dosageId)` |
| `MaintenanceRecordServiceImpl.delete` | 每个明细 `unbindAll(MAINTENANCE_RECORD_DETAIL, detailId)` + `unbindAll(MAINTENANCE_RECORD, id)` |
| `OrganizationServiceImpl.delete` | 每个证书 `unbindAll(ORGANIZATION_CERTIFICATE, certId)` + `unbindAll(ORGANIZATION, orgId)` |
| `PersonnelFileServiceImpl.delete` | 每个证书 `unbindAll(PERSONNEL_CERTIFICATE, certId)` + `unbindAll(PERSONNEL_FILE, personnelFileId)` |
| `ApprovalInstanceServiceImpl.delete` | `unbindAll(APPROVAL_INSTANCE, instanceId, null, null)`（refId 传 null，解绑所有节点） |
| 其他模块 | 按 `BizFileType` 对应的表执行一次 `unbindAll` |

### 3.10 审批模块特殊说明

审批附件不是“实例一条、节点一条”，而是：

```
biz_type  = APPROVAL_INSTANCE
biz_id    = approval_instance.id
ref_id    = approval_node.id
file_kind = DEFAULT
```

查询规则：

- 整个实例的附件：`GET /api/business-files?bizType=APPROVAL_INSTANCE&bizId=88`
- 某个节点的附件：`GET /api/business-files?bizType=APPROVAL_INSTANCE&bizId=88&refId=45`
- 批量查询多个实例：`POST /api/business-files/batch-list`，`bizIds` 传实例 ID 列表即可，一次请求取回全部节点附件，避免前端按节点循环调用。

前端原有的 `fetchInstanceAttachments` 循环可以删除，改为一次 `listBatch` 或一次 `list`。

---

## 4. 前端实现

### 4.1 常量

文件：`src/constants/biz-file-type.ts`

```ts
/**
 * 业务附件类型与槽位常量
 * <p>
 * 必须与后端 BizFileType / BizFileKinds 保持一致。
 * 新增类型时先改后端注册表，再同步此文件；联调时可调用 /api/business-files/types 校验。
 * </p>
 */
export const BIZ_FILE_TYPE = {
  ACCEPTANCE_ORDER: 'ACCEPTANCE_ORDER',
  DESTRUCTION_RECORD: 'DESTRUCTION_RECORD',
  DEVIATION_RECORD: 'DEVIATION_RECORD',
  INSPECTION_REQUEST: 'INSPECTION_REQUEST',
  SAMPLING_RECORD: 'SAMPLING_RECORD',
  RELEASE_REVIEW: 'RELEASE_REVIEW',
  RETAINED_SAMPLE: 'RETAINED_SAMPLE',
  VERIFICATION_RECORD: 'VERIFICATION_RECORD',
  DOSAGE_FORM: 'DOSAGE_FORM',
  PREPARATION: 'PREPARATION',
  PREPARATION_PROCESS_TEMPLATE: 'PREPARATION_PROCESS_TEMPLATE',
  PRODUCTION_PLAN: 'PRODUCTION_PLAN',
  WORK_ORDER: 'WORK_ORDER',
  WORK_ORDER_PROCESS_EXECUTION: 'WORK_ORDER_PROCESS_EXECUTION',
  PRODUCTION_UNIT: 'PRODUCTION_UNIT',
  EQUIPMENT: 'EQUIPMENT',
  EQUIPMENT_MAINTENANCE: 'EQUIPMENT_MAINTENANCE',
  ENERGY_RECORD: 'ENERGY_RECORD',
  MAINTENANCE_RECORD: 'MAINTENANCE_RECORD',
  MAINTENANCE_RECORD_DETAIL: 'MAINTENANCE_RECORD_DETAIL',
  ORGANIZATION: 'ORGANIZATION',
  ORGANIZATION_CERTIFICATE: 'ORGANIZATION_CERTIFICATE',
  PERSONNEL_FILE: 'PERSONNEL_FILE',
  PERSONNEL_CERTIFICATE: 'PERSONNEL_CERTIFICATE',
  POSITION: 'POSITION',
  DUTY: 'DUTY',
  PURCHASE_ORDER: 'PURCHASE_ORDER',
  PURCHASE_SUPPLIER: 'PURCHASE_SUPPLIER',
  SUPPLIER_AUDIT: 'SUPPLIER_AUDIT',
  ROOM_INFO: 'ROOM_INFO',
  CLEANING_RECORD: 'CLEANING_RECORD',
  CLEAN_INSPECTION_RECORD: 'CLEAN_INSPECTION_RECORD',
  TRAINING_RECORD: 'TRAINING_RECORD',
  APPROVAL_INSTANCE: 'APPROVAL_INSTANCE'
} as const

export const FILE_KIND = {
  DEFAULT: 'DEFAULT',
  WAYBILL: 'WAYBILL',
  INVOICE: 'INVOICE',
  INSPECTION_REPORT: 'INSPECTION_REPORT',
  BATCH_RECORD: 'BATCH_RECORD',
  OTHER: 'OTHER',
  PROCESS_ATTACHMENT: 'PROCESS_ATTACHMENT',
  PROCESS_SPEC: 'PROCESS_SPEC',
  PROCESS_FLOWCHART: 'PROCESS_FLOWCHART',
  BATCH_RECORD_TEMPLATE: 'BATCH_RECORD_TEMPLATE',
  VERIFICATION_PLAN: 'VERIFICATION_PLAN',
  VERIFICATION_TEMPLATE: 'VERIFICATION_TEMPLATE',
  INSPECTION_TEMPLATE: 'INSPECTION_TEMPLATE',
  QUALITY_STANDARD: 'QUALITY_STANDARD',
  LICENSE_SCAN: 'LICENSE_SCAN',
  PHOTO: 'PHOTO',
  ATTACHMENT: 'ATTACHMENT',
  HEALTH_FILE: 'HEALTH_FILE',
  CERTIFICATE_FILE: 'CERTIFICATE_FILE',
  DISINFECTION: 'DISINFECTION',
  CLEAN_INSPECTION: 'CLEAN_INSPECTION',
  CLEANING_RECORD: 'CLEANING_RECORD'
} as const
```

### 4.2 API

文件：`src/api/business-files.ts`

```ts
import request from '@/utils/http'

/** 业务附件 */
export interface BusinessFileItem {
  relationId: number
  bizType: string
  bizId: number
  refId: number
  fileId: number
  fileKind: string
  originalName: string
  fileSize: number
  contentType: string
  fileExtension: string
  description?: string
  fileUrl: string
  previewUrl: string
  createdTime?: string
}

/** 上传参数 */
export interface UploadBusinessFileParams {
  file: File
  bizType: string
  bizId: number
  refId?: number
  fileKind?: string
  entityName?: string
  description?: string
}

/** 上传并绑定 */
export function uploadBusinessFile(params: UploadBusinessFileParams) {
  const fd = new FormData()
  fd.append('file', params.file)
  fd.append('bizType', params.bizType)
  fd.append('bizId', String(params.bizId))
  if (params.refId != null) fd.append('refId', String(params.refId))
  if (params.fileKind) fd.append('fileKind', params.fileKind)
  if (params.entityName) fd.append('entityName', params.entityName)
  if (params.description) fd.append('description', params.description)
  return request.post<BusinessFileItem>({ url: '/api/business-files/upload', data: fd })
}

/** 临时上传（保存单据时再 sync 绑定） */
export function uploadTempBusinessFile(params: Omit<UploadBusinessFileParams, 'bizId' | 'refId'>) {
  const fd = new FormData()
  fd.append('file', params.file)
  fd.append('bizType', params.bizType)
  if (params.entityName) fd.append('entityName', params.entityName)
  if (params.description) fd.append('description', params.description)
  return request.post<{ fileId: number; originalName: string; fileUrl: string }>({
    url: '/api/business-files/upload-temp',
    data: fd
  })
}

/** 查询业务附件 */
export function listBusinessFiles(params: {
  bizType: string
  bizId: number
  refId?: number
  fileKind?: string
}) {
  return request.get<BusinessFileItem[]>({
    url: '/api/business-files',
    params
  })
}

/** 批量查询 */
export function batchListBusinessFiles(data: {
  bizType: string
  bizIds: number[]
  fileKind?: string
}) {
  return request.post<Record<number, BusinessFileItem[]>>({
    url: '/api/business-files/batch-list',
    data
  })
}

/** 保存单据时同步附件 */
export function syncBusinessFiles(data: {
  bizType: string
  bizId: number
  refId?: number
  fileKind?: string
  fileIds: number[]
}) {
  return request.post<BusinessFileItem[]>({ url: '/api/business-files/sync', data })
}

/** 解绑并删除附件 */
export function deleteBusinessFile(relationId: number) {
  return request.del<boolean>({ url: `/api/business-files/${relationId}` })
}

/** 获取业务类型注册表 */
export function getBizFileTypes() {
  return request.get<Array<{ code: string; displayName: string; kinds: Record<string, number> }>>({
    url: '/api/business-files/types'
  })
}
```

### 4.3 `FileUpload` 组件改造

保留现有组件的 accept 校验、图片预览、blob URL 逻辑，只替换 props 和 API 调用。核心改动：

```ts
// 旧 props -> 新 props
interface Props {
  modelValue?: FileItem[]
  /** 业务类型编码，如 WORK_ORDER_PROCESS_EXECUTION */
  bizType: string
  /** 业务ID */
  bizId?: number
  /** 子对象ID（审批节点等，可空） */
  refId?: number
  /** 文件槽位，默认 DEFAULT */
  fileKind?: string
  /** 实体名称（用于磁盘目录） */
  entityName?: string
  multiple?: boolean
  limit?: number
  accept?: string
  disabled?: boolean
  listType?: 'text' | 'picture' | 'picture-card'
  /** 选择后立即上传 */
  autoSubmit?: boolean
}
```

加载：

```ts
const loadFiles = async () => {
  if (!props.bizType || !props.bizId) return
  const res = await listBusinessFiles({
    bizType: props.bizType,
    bizId: Number(props.bizId),
    refId: props.refId,
    fileKind: props.fileKind
  })
  innerFileList.value = (res || []).map((item) => ({
    uid: item.relationId,
    relationId: item.relationId,
    fileId: item.fileId,
    name: item.originalName,
    url: item.previewUrl,
    status: 'success',
    fileKind: item.fileKind
  }))
  syncToParent()
  await Promise.allSettled(innerFileList.value.map(fetchImageBlobUrl))
  syncToParent()
}
```

上传：

```ts
const submitUpload = async (overrideBizId?: number) => {
  const bizId = overrideBizId ?? props.bizId
  if (!bizId) return []
  const pending = innerFileList.value.filter((f) => !f.fileId)
  for (const item of pending) {
    const raw = rawFileMap.value[item.uid]
    if (!raw) continue
    const res = await uploadBusinessFile({
      file: raw,
      bizType: props.bizType,
      bizId: Number(bizId),
      refId: props.refId,
      fileKind: props.fileKind,
      entityName: props.entityName
    })
    item.fileId = res.fileId
    item.relationId = res.relationId
    item.name = res.originalName
    item.url = res.previewUrl
    item.status = 'success'
  }
  syncToParent()
  await loadFiles()
}
```

删除：

```ts
const doRemove = async (uid: number) => {
  const item = innerFileList.value.find((f) => f.uid === uid)
  if (item?.relationId) {
    await deleteBusinessFile(item.relationId)
  }
  innerFileList.value = innerFileList.value.filter((f) => f.uid !== uid)
  syncToParent()
}
```

临时上传（配合流程 B）：

```ts
/** 临时上传：返回 fileId 列表，父组件保存业务记录后调用 syncBusinessFiles 绑定 */
const uploadTemp = async (): Promise<number[]> => {
  const fileIds: number[] = []
  const pending = innerFileList.value.filter((f) => !f.fileId)
  for (const item of pending) {
    const raw = rawFileMap.value[item.uid]
    if (!raw) continue
    const res = await uploadTempBusinessFile({
      file: raw,
      bizType: props.bizType,
      entityName: props.entityName
    })
    item.fileId = res.fileId
    item.name = res.originalName
    item.url = res.fileUrl
    item.status = 'success'
    fileIds.push(res.fileId)
    delete rawFileMap.value[item.uid]
  }
  syncToParent()
  return fileIds
}
```

暴露：

```ts
defineExpose({ submitUpload, uploadTemp, loadFiles, removeFile, clearFiles, files: innerFileList })
```

> `FilePreviewDialog` 同步改为 `bizType` + `bizId` + `refId` + `fileKind`，内部调用 `listBusinessFiles`。

### 4.4 使用示例

#### 4.4.1 工单工序附件（本次重点）

```vue
<FileUpload
  v-model="row._fileList"
  :biz-type="BIZ_FILE_TYPE.WORK_ORDER_PROCESS_EXECUTION"
  :biz-id="row.id"
  :file-kind="FILE_KIND.PROCESS_ATTACHMENT"
  :limit="1"
  entity-name="工单工序"
  accept=".pdf,.png,.jpg,.jpeg,.doc,.docx"
  auto-submit
/>
```

- `biz_id` 是工序执行行 `work_order_process_execution.id`；
- 不再使用 `work_order_id + custom_path`；
- 每道工序最多 1 个附件由后端注册表 `PROCESS_ATTACHMENT, 1` 控制，前端 `limit=1` 只是 UI 提示。

#### 4.4.2 货物验收三槽位

```vue
<FileUpload
  :biz-type="BIZ_FILE_TYPE.ACCEPTANCE_ORDER"
  :biz-id="detail.acceptanceId"
  :file-kind="FILE_KIND.WAYBILL"
  :limit="10"
  entity-name="货物验收"
  accept=".pdf,.png,.jpg,.jpeg,.doc,.docx,.xls,.xlsx"
/>
<FileUpload
  :biz-type="BIZ_FILE_TYPE.ACCEPTANCE_ORDER"
  :biz-id="detail.acceptanceId"
  :file-kind="FILE_KIND.INVOICE"
  :limit="10"
/>
<FileUpload
  :biz-type="BIZ_FILE_TYPE.ACCEPTANCE_ORDER"
  :biz-id="detail.acceptanceId"
  :file-kind="FILE_KIND.INSPECTION_REPORT"
  :limit="10"
/>
```

#### 4.4.3 审批附件

```vue
<!-- 审批抽屉：按节点展示/上传 -->
<FileUpload
  :biz-type="BIZ_FILE_TYPE.APPROVAL_INSTANCE"
  :biz-id="instance.id"
  :ref-id="node.id"
  :file-kind="FILE_KIND.DEFAULT"
  :limit="10"
  entity-name="审批附件"
/>

<!-- 整个实例的附件：ref-id 不传，一次查回所有节点附件（每条带 refId） -->
<FileUpload
  :biz-type="BIZ_FILE_TYPE.APPROVAL_INSTANCE"
  :biz-id="instance.id"
  :file-kind="FILE_KIND.DEFAULT"
  disabled
/>
```

### 4.5 新增记录的两种绑定流程

#### 流程 A：先保存，再上传（推荐，改动最小）

```ts
const saved = await createWorkOrder(payload)
await fileRef.value?.submitUpload(saved.workOrderId)
```

适用于工单、设备、采购订单等“新增弹窗先保存主记录”的页面。

#### 流程 B：先临时上传，保存时 sync（适合明细行/内联编辑）

```ts
// 1. 选择文件时上传到临时区，拿到 fileIds
const fileIds = await fileRef.value?.uploadTemp()

// 2. 保存明细行
await saveProcessExecution({ ...payload, fileIds })

// 3. 后端保存成功后绑定
await syncBusinessFiles({
  bizType: BIZ_FILE_TYPE.WORK_ORDER_PROCESS_EXECUTION,
  bizId: savedProcessId,
  fileKind: FILE_KIND.PROCESS_ATTACHMENT,
  fileIds
})
```

这样不会出现 `biz_id = 0` 的中间态。临时文件如果最终没有绑定，会留在 `file_info` 中，可由文件管理页清理，或在后续版本增加定时清理。

---

## 5. 数据迁移

### 5.1 旧类型 → 新 biz_type 映射

| 旧 `business_type` | 新 `biz_type` | `file_kind` 规则 |
|--------------------|---------------|------------------|
| `GOODS_ACCEPTANCE_WAYBILL` | `ACCEPTANCE_ORDER` | `WAYBILL` |
| `GOODS_ACCEPTANCE_INVOICE` | `ACCEPTANCE_ORDER` | `INVOICE` |
| `GOODS_ACCEPTANCE_INSPECTION_REPORT` | `ACCEPTANCE_ORDER` | `INSPECTION_REPORT` |
| `QUALITY_DESTRUCTION_RECORD` | `DESTRUCTION_RECORD` | `DEFAULT` |
| `QUALITY_DEVIATION_RECORD` | `DEVIATION_RECORD` | `DEFAULT` |
| `QUALITY_INSPECTION_RECORD` | `INSPECTION_REQUEST` | `DEFAULT` |
| `QUALITY_SAMPLING_RECORD` | `SAMPLING_RECORD` | `DEFAULT` |
| `AUDIT_RELEASE_FILE` / `AUDIT_RELEASE_RECORD` | `RELEASE_REVIEW` | `DEFAULT` |
| `SAMPLE_RETENTION_FILE` / `SAMPLE_RETENTION_RECORD` | `RETAINED_SAMPLE` | `DEFAULT` |
| `VERIFICATION_FILE` | `VERIFICATION_RECORD` | `DEFAULT` |
| `PREPARATION_DOCUMENT` | `DOSAGE_FORM` 或 `PREPARATION` | 按 `custom_path` 第一段 + ID 归属判断 |
| `PREPARATION_PROCESS` | `PREPARATION_PROCESS_TEMPLATE` | `custom_path` 第一段是模板 ID |
| `PRODUCTION_PLAN` | `PRODUCTION_PLAN` | `DEFAULT` |
| `PRODUCTION_TASK` | `WORK_ORDER` | `custom_path` 前缀 `生产批记录上传` → `BATCH_RECORD`，`其他文件上传` → `OTHER` |
| `PRODUCTION_PROCESS` | `WORK_ORDER_PROCESS_EXECUTION` | `custom_path` 第一段是工序行 ID |
| `GENERAL_ATTACHMENT` | `PRODUCTION_UNIT` | `DEFAULT` |
| `EQUIPMENT_FILE` | `EQUIPMENT` | `DEFAULT` |
| `EQUIPMENT_MAINTENANCE` | `EQUIPMENT_MAINTENANCE` | `DEFAULT` |
| `ENERGY_FILE` / `ENERGY_RECORD` | `ENERGY_RECORD` | `DEFAULT` |
| `MAINTENANCE_RECORD` / `STOCK_MAINTENANCE_RECORD` | `MAINTENANCE_RECORD` | `DEFAULT` |
| `MAINTENANCE_DETAIL` / `STOCK_MAINTENANCE_DETAIL` | `MAINTENANCE_RECORD_DETAIL` | `DEFAULT` |
| `ORGANIZATION_CERTIFICATE` | `ORGANIZATION` 或 `ORGANIZATION_CERTIFICATE` | `许可证扫描件` 前缀 → `LICENSE_SCAN`；否则按 ID 归属判断 |
| `ORGANIZATION_LICENSE` | `ORGANIZATION` | `LICENSE_SCAN` |
| `ORGANIZATION_PHOTO` | `ORGANIZATION` | `PHOTO` |
| `PERSONNEL_ATTACHMENT` | `PERSONNEL_FILE` | `ATTACHMENT` |
| `PERSONNEL_HEALTH_FILE` | `PERSONNEL_FILE` | `HEALTH_FILE` |
| `PERSONNEL_CERTIFICATE` | `PERSONNEL_CERTIFICATE` | 从 `custom_path` 解析证书 ID；解析失败 → `PERSONNEL_FILE/CERTIFICATE_FILE` |
| `POSITION_FILE` | `POSITION` | `DEFAULT` |
| `DUTY_FILE` | `DUTY` | `DEFAULT` |
| `PURCHASE_FILE` | `PURCHASE_ORDER` | `DEFAULT` |
| `SUPPLIER_FILE` | `PURCHASE_SUPPLIER` | `DEFAULT` |
| `SUPPLIER_ATTACHMENT` | `SUPPLIER_AUDIT` | `DEFAULT` |
| `ROOM_DISINFECTION` | `ROOM_INFO` / `CLEANING_RECORD` 等 | `custom_path` 第一段为 `【记录ID】` 时按记录迁移，否则挂房间 |
| `ROOM_CLEAN_INSPECTION` | `ROOM_INFO` / `CLEAN_INSPECTION_RECORD` | 同上 |
| `ROOM_CLEANING_RECORD` | `ROOM_INFO` / `CLEANING_RECORD` | 同上 |
| `TRAINING_ATTACHMENT` | `TRAINING_RECORD` | `DEFAULT` |
| 纯数字（审批节点 ID） | `APPROVAL_INSTANCE` | `biz_id=原 business_id`，`ref_id=原 business_type` |
| `GENERAL` / 空 / 其他 | 不自动迁移 | 进入冲突清单 |

### 5.2 通用迁移 SQL 模板

```sql
-- 以采购订单为例
INSERT INTO business_file
    (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
     is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'PURCHASE_ORDER', fi.business_id, 0, fi.file_id, 'DEFAULT', 0,
       0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type = 'PURCHASE_FILE'
  AND fi.is_deleted = 0
  AND fi.business_id > 0
  AND EXISTS (SELECT 1 FROM purchase_orders p WHERE p.id = fi.business_id AND p.is_deleted = 0)
  AND NOT EXISTS (
      SELECT 1 FROM business_file bf
      WHERE bf.biz_type = 'PURCHASE_ORDER'
        AND bf.biz_id = fi.business_id
        AND bf.ref_id = 0
        AND bf.file_id = fi.file_id
  );
```

### 5.3 特殊类型迁移

#### 5.3.1 生产批记录 / 其他文件

```sql
INSERT INTO business_file
    (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
     is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'WORK_ORDER', fi.business_id, 0, fi.file_id,
       CASE
           WHEN fi.custom_path LIKE '生产批记录上传/%' THEN 'BATCH_RECORD'
           WHEN fi.custom_path LIKE '其他文件上传/%' THEN 'OTHER'
           ELSE 'DEFAULT'
       END,
       0, 0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type = 'PRODUCTION_TASK'
  AND fi.is_deleted = 0
  AND fi.business_id > 0
  AND EXISTS (SELECT 1 FROM work_order w WHERE w.work_order_id = fi.business_id AND w.is_deleted = 0)
  AND NOT EXISTS (
      SELECT 1 FROM business_file bf
      WHERE bf.biz_type = 'WORK_ORDER'
        AND bf.biz_id = fi.business_id
        AND bf.ref_id = 0
        AND bf.file_id = fi.file_id
  );
```

#### 5.3.2 工序附件（工单）

```sql
INSERT INTO business_file
    (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
     is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'WORK_ORDER_PROCESS_EXECUTION',
       CAST(SUBSTRING_INDEX(fi.custom_path, '/', 1) AS UNSIGNED),
       0, fi.file_id, 'PROCESS_ATTACHMENT',
       0, 0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type = 'PRODUCTION_PROCESS'
  AND fi.is_deleted = 0
  AND fi.custom_path REGEXP '^[0-9]+/'
  AND EXISTS (
      SELECT 1 FROM work_order_process_execution w
      WHERE w.id = CAST(SUBSTRING_INDEX(fi.custom_path, '/', 1) AS UNSIGNED)
        AND w.work_order_id = fi.business_id
        AND w.is_deleted = 0
  )
  AND NOT EXISTS (
      SELECT 1 FROM business_file bf
      WHERE bf.biz_type = 'WORK_ORDER_PROCESS_EXECUTION'
        AND bf.biz_id = CAST(SUBSTRING_INDEX(fi.custom_path, '/', 1) AS UNSIGNED)
        AND bf.ref_id = 0
        AND bf.file_id = fi.file_id
  );
```

#### 5.3.3 制剂工序模板

```sql
INSERT INTO business_file
    (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
     is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'PREPARATION_PROCESS_TEMPLATE',
       CAST(SUBSTRING_INDEX(fi.custom_path, '/', 1) AS UNSIGNED),
       0, fi.file_id, 'DEFAULT',
       0, 0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type = 'PREPARATION_PROCESS'
  AND fi.is_deleted = 0
  AND fi.custom_path REGEXP '^[0-9]+/'
  AND EXISTS (
      SELECT 1 FROM preparation_process_template t
      WHERE t.template_id = CAST(SUBSTRING_INDEX(fi.custom_path, '/', 1) AS UNSIGNED)
        AND t.preparation_id = fi.business_id
        AND t.is_deleted = 0
  )
  AND NOT EXISTS (
      SELECT 1 FROM business_file bf
      WHERE bf.biz_type = 'PREPARATION_PROCESS_TEMPLATE'
        AND bf.biz_id = CAST(SUBSTRING_INDEX(fi.custom_path, '/', 1) AS UNSIGNED)
        AND bf.ref_id = 0
        AND bf.file_id = fi.file_id
  );
```

#### 5.3.4 剂型 / 制剂（重点冲突）

先迁移无歧义的行：

```sql
-- 只存在于 dosage_form 的 ID -> DOSAGE_FORM
INSERT INTO business_file
    (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
     is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'DOSAGE_FORM', fi.business_id, 0, fi.file_id,
       CASE
           WHEN fi.custom_path LIKE '批记录模板/%' THEN 'BATCH_RECORD_TEMPLATE'
           WHEN fi.custom_path LIKE '验证方案/%' THEN 'VERIFICATION_TEMPLATE'
           WHEN fi.custom_path LIKE '检验模板/%' THEN 'INSPECTION_TEMPLATE'
           ELSE 'DEFAULT'
       END,
       0, 0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type = 'PREPARATION_DOCUMENT'
  AND fi.is_deleted = 0
  AND EXISTS (SELECT 1 FROM dosage_form d WHERE d.dosage_id = fi.business_id AND d.is_deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM preparation p WHERE p.preparation_id = fi.business_id AND p.is_deleted = 0);

-- 只存在于 preparation 的 ID -> PREPARATION
INSERT INTO business_file
    (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
     is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'PREPARATION', fi.business_id, 0, fi.file_id,
       CASE
           WHEN fi.custom_path LIKE '工艺规程/%' THEN 'PROCESS_SPEC'
           WHEN fi.custom_path LIKE '批记录模板/%' THEN 'BATCH_RECORD_TEMPLATE'
           WHEN fi.custom_path LIKE '验证方案/%' THEN 'VERIFICATION_PLAN'
           WHEN fi.custom_path LIKE '质量标准/%' THEN 'QUALITY_STANDARD'
           WHEN fi.custom_path LIKE '工艺规程流程图/%' THEN 'PROCESS_FLOWCHART'
           ELSE 'DEFAULT'
       END,
       0, 0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type = 'PREPARATION_DOCUMENT'
  AND fi.is_deleted = 0
  AND EXISTS (SELECT 1 FROM preparation p WHERE p.preparation_id = fi.business_id AND p.is_deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM dosage_form d WHERE d.dosage_id = fi.business_id AND d.is_deleted = 0);

-- 两边都存在或都不存在 -> 冲突清单
INSERT INTO business_file_migration_conflict (file_id, business_type, business_id, custom_path, reason)
SELECT fi.file_id, fi.business_type, fi.business_id, fi.custom_path,
       'PREPARATION_DOCUMENT 的 business_id 同时命中剂型和制剂，或两边都不存在'
FROM file_info fi
WHERE fi.business_type = 'PREPARATION_DOCUMENT'
  AND fi.is_deleted = 0
  AND (
      (EXISTS (SELECT 1 FROM dosage_form d WHERE d.dosage_id = fi.business_id AND d.is_deleted = 0)
       AND EXISTS (SELECT 1 FROM preparation p WHERE p.preparation_id = fi.business_id AND p.is_deleted = 0))
      OR
      (NOT EXISTS (SELECT 1 FROM dosage_form d WHERE d.dosage_id = fi.business_id AND d.is_deleted = 0)
       AND NOT EXISTS (SELECT 1 FROM preparation p WHERE p.preparation_id = fi.business_id AND p.is_deleted = 0))
  );
```

#### 5.3.5 机构 / 机构证书

```sql
-- 许可证扫描件 -> ORGANIZATION / LICENSE_SCAN
INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'ORGANIZATION', fi.business_id, 0, fi.file_id, 'LICENSE_SCAN', 0,
       0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type IN ('ORGANIZATION_CERTIFICATE', 'ORGANIZATION_LICENSE')
  AND fi.is_deleted = 0
  AND fi.custom_path LIKE '许可证扫描件/%'
  AND EXISTS (SELECT 1 FROM organization o WHERE o.id = fi.business_id AND o.is_deleted = 0);

-- 机构证书 -> ORGANIZATION_CERTIFICATE / DEFAULT
INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'ORGANIZATION_CERTIFICATE', fi.business_id, 0, fi.file_id, 'DEFAULT', 0,
       0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type = 'ORGANIZATION_CERTIFICATE'
  AND fi.is_deleted = 0
  AND fi.custom_path NOT LIKE '许可证扫描件/%'
  AND EXISTS (SELECT 1 FROM organization_certificate c WHERE c.id = fi.business_id AND c.is_deleted = 0);

-- 机构照片
INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'ORGANIZATION', fi.business_id, 0, fi.file_id, 'PHOTO', 0,
       0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type = 'ORGANIZATION_PHOTO'
  AND fi.is_deleted = 0
  AND EXISTS (SELECT 1 FROM organization o WHERE o.id = fi.business_id AND o.is_deleted = 0);
```

#### 5.3.6 人员证书

```sql
-- custom_path 形如 “证书类型_证书ID/personnelFileId”
INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'PERSONNEL_CERTIFICATE',
       CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(fi.custom_path, '/', 1), '_', -1) AS UNSIGNED),
       0, fi.file_id, 'DEFAULT', 0,
       0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type = 'PERSONNEL_CERTIFICATE'
  AND fi.is_deleted = 0
  AND fi.custom_path REGEXP '^[^/]+_[0-9]+/'
  AND EXISTS (
      SELECT 1 FROM personnel_certificate c
      WHERE c.certificate_id = CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(fi.custom_path, '/', 1), '_', -1) AS UNSIGNED)
        AND c.personnel_file_id = fi.business_id
        AND c.is_deleted = 0
  );

-- 解析失败的证书文件，保留在人员档案下
INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'PERSONNEL_FILE', fi.business_id, 0, fi.file_id, 'CERTIFICATE_FILE', 0,
       0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type = 'PERSONNEL_CERTIFICATE'
  AND fi.is_deleted = 0
  AND NOT (fi.custom_path REGEXP '^[^/]+_[0-9]+/');
```

#### 5.3.7 房间日志

```sql
-- custom_path 形如 “【123】/456”：123 是清洁/检测记录 ID
INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'CLEANING_RECORD',
       CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(fi.custom_path, '】', 1), '【', -1) AS UNSIGNED),
       0, fi.file_id, 'DEFAULT', 0,
       0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type = 'ROOM_CLEANING_RECORD'
  AND fi.is_deleted = 0
  AND fi.custom_path REGEXP '^【[0-9]+】/'
  AND EXISTS (
      SELECT 1 FROM cleaning_record c
      WHERE c.id = CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(fi.custom_path, '】', 1), '【', -1) AS UNSIGNED)
        AND c.is_deleted = 0
  );

-- 洁净检测同理，把 CLEANING_RECORD 换成 CLEAN_INSPECTION_RECORD，
-- 校验表换成 clean_inspection_record。

-- 房间页面上的规程附件（custom_path 为“消毒规程/…”“洁净检测规程/…”“洁净规程/…”）
INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'ROOM_INFO', fi.business_id, 0, fi.file_id,
       CASE fi.business_type
           WHEN 'ROOM_DISINFECTION' THEN 'DISINFECTION'
           WHEN 'ROOM_CLEAN_INSPECTION' THEN 'CLEAN_INSPECTION'
           WHEN 'ROOM_CLEANING_RECORD' THEN 'CLEANING_RECORD'
           ELSE 'DEFAULT'
       END,
       0, 0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type IN ('ROOM_DISINFECTION', 'ROOM_CLEAN_INSPECTION', 'ROOM_CLEANING_RECORD')
  AND fi.is_deleted = 0
  AND fi.custom_path NOT REGEXP '^【[0-9]+】/'
  AND EXISTS (SELECT 1 FROM room_info r WHERE r.room_id = fi.business_id AND r.is_deleted = 0);
```

#### 5.3.8 审批附件

```sql
INSERT INTO business_file (biz_type, biz_id, ref_id, file_id, file_kind, sort_order,
                           is_deleted, created_by, created_time, updated_by, updated_time)
SELECT 'APPROVAL_INSTANCE', fi.business_id, CAST(fi.business_type AS UNSIGNED), fi.file_id, 'DEFAULT', 0,
       0, fi.created_by, fi.created_time, fi.updated_by, fi.updated_time
FROM file_info fi
WHERE fi.business_type REGEXP '^[0-9]+$'
  AND fi.is_deleted = 0
  AND fi.business_id > 0
  AND EXISTS (SELECT 1 FROM approval_instance i WHERE i.id = fi.business_id AND i.is_deleted = 0)
  AND EXISTS (
      SELECT 1 FROM approval_node n
      JOIN approval_instance i2 ON i2.workflow_id = n.workflow_id
      WHERE n.id = CAST(fi.business_type AS UNSIGNED)
        AND i2.id = fi.business_id
        AND n.is_deleted = 0
  );
```

### 5.4 迁移执行顺序

1. 建 `business_file`、`business_file_migration_conflict` 表。
2. 执行无歧义类型的迁移（采购、设备、能耗、培训、验证、生产计划、生产任务、工序等）。
3. 执行剂型/制剂、机构/证书、人员证书、房间日志、审批的特殊迁移。
4. 查询冲突表，人工确认后补迁移或保留旧关联。
5. 校验：每个旧类型迁移行数 vs 新关系行数。
6. 在 `8.134.48.54` 测试库重复执行两遍，确认幂等。
7. 生产环境执行前备份 `file_info`。

---

## 6. 兼容与回滚

### 6.1 兼容期

- 旧接口 `GET /api/files/business`、`POST /api/files/upload-business` 保留一个版本；
- 新模块切换到 `/api/business-files/*`；
- 未迁移模块继续使用旧接口，互不影响；
- 迁移期间新上传只写 `business_file`，旧接口查不到新文件，所以**同一模块必须先迁移数据、再切换前后端**。

### 6.2 回滚

- 旧数据未改动，仍保留在 `file_info`；
- 回滚时把前端切回旧接口即可看到旧数据；
- 切换后新上传的文件只存在 `business_file`，回滚会看不到这些新文件，需要反向导出或保留新接口读取。

### 6.3 清理

所有模块迁移并稳定运行一个版本后：

1. 删除 `business_file_migration_conflict` 表；
2. 移除 `FileUpload` 的旧 props（`businessType` / `customPath`）；
3. 移除后端散落的 `EQUIPMENT_FILE`、`ENERGY_RECORD` 等字符串，统一引用 `BizFileType`；
4. 视情况清理 `file_info.business_type` / `business_id` / `custom_path` 的写入逻辑；
5. 删除旧页面 `preparation_info/index.vue`。

---

## 7. 测试

### 7.1 单元/集成测试清单

| 用例 | 预期 |
|------|------|
| 上传不存在的 `bizType` | 返回“不支持的业务类型” |
| 上传不存在的 `bizId` | 返回“业务记录不存在或已删除” |
| 上传不支持的 `fileKind` | 返回“不支持文件槽位” |
| 超过槽位最大数量 | 返回数量上限提示 |
| 同一文件重复绑定同一业务 | 唯一键冲突，返回友好提示 |
| 查询时文件已软删除 | 列表中不返回该文件 |
| 删除关系后无其他引用 | `file_info` 被软删除 |
| 同一文件被两个业务引用，删除一个关系 | `file_info` 保留 |
| 业务记录删除 | 关系表无残留 |
| 审批 `refId` 不属于实例 | 返回“审批节点不存在或不属于该审批实例” |
| 审批实例查询不带 `refId` | 返回所有节点附件 |
| `sync` 空 `fileIds` | 解绑全部 |
| `sync` 含无效 `fileId` | 返回“存在无效或已删除的文件ID” |
| 迁移脚本重复执行 | 不产生重复关系行 |

### 7.2 测试代码示例

```java
@SpringBootTest
@Transactional
class BusinessFileServiceTest {

    @Autowired
    private BusinessFileService businessFileService;

    @Test
    void resolveUnknownBizTypeShouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> businessFileService.list("NOT_EXISTS", 1L, 0L, null));
    }

    @Test
    void listShouldRejectInvalidBizId() {
        List<BusinessFileDto> result = businessFileService.list(
                BizFileType.WORK_ORDER_PROCESS_EXECUTION.getCode(), 0L, 0L, null);
        assertTrue(result.isEmpty());
    }
}
```

> 涉及真实文件上传的用例建议在 `8.134.48.54` 测试库执行，使用 `@Transactional` 回滚；不要向 `8.148.27.237` 写入测试数据。

---

## 8. 落地顺序与工作量

| 阶段 | 内容 | 预计 |
|------|------|------|
| 1 | 建表脚本 + `BizFileType` / `BizFileKinds` + 实体/Mapper | 0.5 天 |
| 2 | `FileStorageService` / `FileInfoService` 扩展 + `BusinessFileService` / Controller | 1.5 天 |
| 3 | 前端常量、API、`FileUpload` / `FilePreviewDialog` 改造 | 1.5 天 |
| 4 | 工单工序附件试点（后端 + 前端 + 测试） | 0.5 天 |
| 5 | 其余单槽位模块迁移 | 2–3 天 |
| 6 | 多槽位模块 + 审批模块迁移 | 1–2 天 |
| 7 | 数据迁移、冲突处理、回归测试 | 1–2 天 |
| **合计** | | **约 8–10 人天** |

**建议先做试点**：选“工单工序附件”这一条链路（`work_order_process_execution` + `PROCESS_ATTACHMENT`）打通上传、列表、删除、业务删除级联、迁移，再复制到其他模块。这样能最快验证 `ref_id`、槽位上限、级联解绑这些设计是否够用。

---

## 9. 迁移执行记录

**环境**：测试库 `8.134.48.54` / `erp_db`，执行日期 2026-11-03。

**执行脚本**：

1. `create_business_file.sql`
2. `create_business_file_migration_conflict.sql`
3. `migrate_business_file_data.sql`（执行两遍，第二遍验证幂等）

**结果**：

| 项目 | 数量 |
|------|------|
| `business_file` 关系总数 | 84 |
| 迁移冲突 | 12 |
| 父记录缺失的关系 | 0 |
| 文件缺失的关系 | 0 |
| 重复关系 | 0 |

**关系分布**：

| biz_type | 数量 |
|----------|------|
| ACCEPTANCE_ORDER | 30 |
| WORK_ORDER | 6 |
| PERSONNEL_CERTIFICATE | 6 |
| PRODUCTION_PLAN | 5 |
| PREPARATION | 5 |
| PREPARATION_PROCESS_TEMPLATE | 4 |
| PERSONNEL_FILE | 4 |
| WORK_ORDER_PROCESS_EXECUTION | 3 |
| ORGANIZATION | 3 |
| MAINTENANCE_RECORD_DETAIL | 3 |
| INSPECTION_REQUEST | 3 |
| DEVIATION_RECORD | 3 |
| DUTY | 2 |
| RELEASE_REVIEW | 2 |
| DESTRUCTION_RECORD | 2 |
| MAINTENANCE_RECORD | 1 |
| POSITION | 1 |
| ENERGY_RECORD | 1 |

**冲突明细**（父记录不存在，旧文件保留在 `file_info`，不自动迁移）：

| 原类型 | 数量 | 原因 |
|--------|------|------|
| `PREPARATION_PROCESS` | 7 | 模板 ID 146–151、181 在 `preparation_process_template` 中不存在 |
| `MAINTENANCE_DETAIL` | 4 | 明细 ID 17、18、19、30 在 `maintenance_record_detail` 中不存在 |
| `PRODUCTION_PROCESS` | 1 | 工序执行行 ID 338 不存在 |

**迁移中补充的兼容规则**：

- `PREPARATION_PROCESS` 的 `custom_path` 存在两种历史格式：`{模板ID}/{制剂ID}` 和仅 `{模板ID}`，两种都已兼容；
- `PRODUCTION_PROCESS` 的 `custom_path` 存在 `{工序行ID}/{工单ID}` 和仅 `{工序行ID}` 两种格式，两种都已兼容；
- 未匹配到规则或父记录不存在的旧文件，统一写入 `business_file_migration_conflict`，便于人工清理或补迁移。
