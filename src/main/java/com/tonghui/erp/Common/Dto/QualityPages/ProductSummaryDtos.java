package com.tonghui.erp.Common.Dto.QualityPages;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 产品信息汇总页 DTO 集合
 * <p>
 * 按批次返回全链路节点、单据明细、槽位统计、文件分组和缺件清单。
 * </p>
 */
public final class ProductSummaryDtos {

    private ProductSummaryDtos() {
    }

    /** 批次列表项 */
    @Data
    public static class BatchSummary {
        /** 前端统一记录键：planId + "__" + workOrderId；未下达工单为 planId + "__none" */
        private String recordKey;
        /** 工单ID；未下达工单时为空 */
        private Long workOrderId;
        /** 工单编号 */
        private String workOrderCode;
        /** 生产计划ID */
        private Long planId;
        /** 生产计划编号 */
        private String planNumber;
        /** 制剂编码 */
        private String preparationCode;
        /** 制剂名称 */
        private String preparationName;
        /** 规格 */
        private String spec;
        /** 单位 */
        private String unitName;
        /** 批号 */
        private String batchNo;
        /** 计划数量 */
        private BigDecimal planQty;
        /** 成品数量 */
        private BigDecimal finishedQty;
        /** 入库数量 */
        private BigDecimal inboundQty;
        /** 出库数量 */
        private BigDecimal outboundQty;
        /** 物料来源编码：PURCHASE/REQUISITION/DIRECT */
        private String materialSource;
        /** 物料来源显示文案：采购/院内领料/直接入库 */
        private String materialSourceLabel;
        /** 计划日期 */
        private LocalDate planDate;
        /** 工单日期 */
        private LocalDate workOrderDate;
        /** 入库日期 */
        private LocalDate inboundDate;
        /** 出库日期 */
        private LocalDate outboundDate;
        /** 检验日期（与筛选口径一致，取第一条检验记录结束日期） */
        private LocalDate inspectionDate;
        /** 生产开始日期 */
        private LocalDate productionStart;
        /** 生产结束日期 */
        private LocalDate productionEnd;
        /** 收率(%) */
        private BigDecimal yieldRate;
        /** 检验结论 */
        private String inspectionConclusion;
        /** 最新检验记录编号 */
        private String latestInspectionCode;
        /** 工序总数 */
        private Integer processTotal;
        /** 已完成工序数 */
        private Integer processCompleted;
        /** 批次/计划状态（按工单日期字段实时推导，已放行不会因已入库被覆盖） */
        private String status;
        /** 生产是否已完成（成品已入库及其后续的已放行/已归档）；未下达计划为 false */
        private boolean productionDone;
        /** 附件总数 */
        private int fileCount;
        /** 必传/条件必传应传数 */
        private int fileExpected;
        /** 必传/条件必传已传数 */
        private int fileUploaded;
        /** 缺件项数 */
        private int missingCount;
        /** 缺件环节摘要 */
        private String missingSummary;
        /** 环节状态，供品种卡片视图使用 */
        private List<StageState> stageStates = new ArrayList<>();
    }

    /** 批次环节状态 */
    @Data
    public static class StageState {
        /** 环节编码 */
        private String key;
        /** 显示名称 */
        private String label;
        /** 状态：done/active/pending/error/skipped */
        private String state;
        /** 不适用说明，如“无采购，物料直接入库” */
        private String skipReason;
    }

    /** 页面聚合概览 */
    @Data
    public static class Overview {
        /** 数据生成时间 */
        private String generatedAt;
        /** 结论条 */
        private Verdict verdict;
        /** KPI 指标 */
        private Kpi kpis;
        /** 优先处理列表 */
        private List<TodoItem> todo = new ArrayList<>();
        /** 文件完整度 */
        private Health health;
        /** 按节点的文件完整性清单（跨记录聚合，含单号对应文件） */
        private List<FileChecklistItem> fileChecklist = new ArrayList<>();
        /** 制剂卡片列表 */
        private List<PreparationSummary> preparations = new ArrayList<>();
    }

    /** 结论条 */
    @Data
    public static class Verdict {
        /** 品种数 */
        private int varietyCount;
        /** 记录数 */
        private int recordCount;
        /** 已下达批次数 */
        private int batchCount;
        /** 存在缺件的记录数 */
        private int missingRecords;
        /** 缺件项总数 */
        private int missingItems;
        /** 最优先处理记录 */
        private TodoItem topPriority;
    }

    /** 顶部 KPI */
    @Data
    public static class Kpi {
        /** 在产批次数（未成品入库，且非异常/不合格/作废） */
        private int runningBatches;
        /** 生产已完成批次数（成品已入库及其后续的已放行/已归档） */
        private int completedBatches;
        /** 已下达批次总数 */
        private int totalBatches;
        /** 缺件项总数 */
        private int missingItems;
        /** 存在缺件的记录数 */
        private int missingRecords;
        /** 必传/条件必传应传数 */
        private int fileExpected;
        /** 必传/条件必传已传数 */
        private int fileUploaded;
        /** 附件完整度(%) */
        private int completenessRate;
        /** 检验结论总数 */
        private int inspectionTotal;
        /** 合格数 */
        private int inspectionQualified;
        /** 检验合格率(%) */
        private Integer inspectionRate;
    }

    /** 待办项 */
    @Data
    public static class TodoItem {
        /** 记录键：planId__workOrderId；未下达工单为 planId__none */
        private String recordKey;
        /** 生产计划ID */
        private Long planId;
        /** 工单ID；未下达工单时为空 */
        private Long workOrderId;
        /** 制剂名称 */
        private String preparationName;
        /** 单号 */
        private String code;
        /** 批号 */
        private String batchNo;
        /** 缺件项数 */
        private int missingCount;
        /** 缺件环节摘要 */
        private String stageBrief;
    }

    /** 文件健康度 */
    @Data
    public static class Health {
        /** 环节健康度/链路节点 */
        private List<StageHealth> stages = new ArrayList<>();
        /** 缺件最集中的槽位 */
        private List<SlotHealth> topSlots = new ArrayList<>();
        /** 全部槽位口径 */
        private List<SlotHealth> allSlots = new ArrayList<>();
    }

    /** 环节健康度 */
    @Data
    public static class StageHealth {
        /** 环节 */
        private String stage;
        /** 应传数 */
        private int expected;
        /** 已传数 */
        private int uploaded;
        /** 缺失数 */
        private int missing;
    }

    /** 槽位健康度 */
    @Data
    public static class SlotHealth {
        /** 环节 */
        private String stage;
        /** 显示名称 */
        private String label;
        /** 要求：REQUIRED/CONDITIONAL/OPTIONAL */
        private String requirement;
        /** 应传数 */
        private int expected;
        /** 已传数 */
        private int uploaded;
        /** 缺失数 */
        private int missing;
    }

    /** 制剂卡片 */
    @Data
    public static class PreparationSummary {
        /** 制剂编码 */
        private String preparationCode;
        /** 制剂名称 */
        private String preparationName;
        /** 已下达批次数 */
        private int batchCount;
        /** 缺件项数 */
        private int missingCount;
    }

    /** 批次详情 */
    @Data
    public static class BatchDetail {
        /** 批次摘要 */
        private BatchSummary batch;
        /** 槽位状态 */
        private List<SlotState> slots = new ArrayList<>();
        /** 环节健康度/链路节点 */
        private List<StageItem> stages = new ArrayList<>();
        /** 文件列表/文件分组 */
        private List<FileGroup> files = new ArrayList<>();
        /** 缺件项总数 */
        private List<MissingItem> missingItems = new ArrayList<>();
        /** 按节点的文件完整性清单（含单号对应文件） */
        private List<FileChecklistItem> fileChecklist = new ArrayList<>();
    }

    /** 槽位统计 */
    @Data
    public static class SlotState {
        /** 环节 */
        private String stage;
        /** 链路节点编码 */
        private String nodeKey;
        /** 显示名称 */
        private String label;
        /** 业务类型编码 */
        private String bizType;
        /** 文件槽位编码 */
        private String fileKind;
        /** 要求：REQUIRED/CONDITIONAL/OPTIONAL */
        private String requirement;
        /** 应传数 */
        private int expected;
        /** 已传数 */
        private int uploaded;
        /** 缺失数 */
        private int missing;
    }

    /** 链路节点 */
    @Data
    public static class StageItem {
        /** 环节编码 */
        private String key;
        /** 显示名称 */
        private String label;
        /** 状态：done/active/pending/error/skipped */
        private String state;
        /** 是否不适用（如无采购、物料直接入库） */
        private Boolean skipped;
        /** 不适用说明 */
        private String skipReason;
        /** 主信息 */
        private String main;
        /** 辅助信息 */
        private String meta;
        /** 单据列表 */
        private List<DocumentItem> documents = new ArrayList<>();
        /** 单据数量（张） */
        private int documentCount;
        /** 必传+条件必传槽位应传单据数 */
        private int fileExpected;
        /** 必传+条件必传槽位已上传单据数 */
        private int fileUploaded;
        /** 必传+条件必传缺件项数 */
        private int fileMissing;
        /** 存在缺件的单据数（去重） */
        private int missingDocumentCount;
        /** 可选槽位应传单据数（不计入缺件） */
        private int optionalExpected;
        /** 可选槽位已上传单据数 */
        private int optionalUploaded;
    }

    /** 单据 */
    @Data
    public static class DocumentItem {
        /** 单据类型 */
        private String type;
        /** 被检品属性（检验单据使用：成品/中间产品/生产原料） */
        private String itemCategory;
        /** 单号 */
        private String code;
        /** 日期 */
        private LocalDate date;
        /** 时间 */
        private LocalDateTime time;
        /** 批次/计划状态 */
        private String status;
        /** 经办人 */
        private String operator;
        /** 供应商 */
        private String supplier;
        /** 客户 */
        private String customer;
        /** 金额 */
        private BigDecimal amount;
        /** 单据ID（与文件清单 documents[].docId 对齐） */
        private Long docId;
        /** 业务类型编码（BizFileType） */
        private String bizType;
        /** 单据表头字段（有序，前端按顺序展示） */
        private List<FieldItem> fields = new ArrayList<>();
        /** 本单据必传+条件必传槽位应传数 */
        private int fileExpected;
        /** 本单据必传+条件必传槽位已传数 */
        private int fileUploaded;
        /** 本单据缺件项数 */
        private int missingCount;
        /** 本单据缺件名称，如“验收检验报告” */
        private List<String> missingLabels = new ArrayList<>();
        /** 单据明细行 */
        private List<DocumentLine> lines = new ArrayList<>();
        /** 文件列表/文件分组 */
        private List<FileItem> files = new ArrayList<>();
    }

    /** 单据表头字段 */
    @Data
    public static class FieldItem {
        /** 字段名称 */
        private String label;
        /** 字段值 */
        private String value;

        public FieldItem() {
        }

        public FieldItem(String label, String value) {
            this.label = label;
            this.value = value;
        }
    }

    /** 单据明细行 */
    @Data
    public static class DocumentLine {
        /** 单号 */
        private String code;
        /** 名称/文件名 */
        private String name;
        /** 规格（成品出入库明细取制剂主数据规格） */
        private String spec;
        /** 分类 */
        private String category;
        /** 数量 */
        private BigDecimal qty;
        /** 单位 */
        private String unit;
        /** 单价 */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private BigDecimal price;
        /** 金额 */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private BigDecimal amount;
        /** 供应商 */
        private String supplier;
        /** 批号 */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String batchNo;
        /** 批次/计划状态 */
        private String status;
        /** 到货数量（验收明细） */
        private BigDecimal arrivalQty;
        /** 实际到货数量（验收明细） */
        private BigDecimal actualArrivalQty;
        /** 入库数量（验收/入库明细） */
        private BigDecimal inboundQty;
        /** 差异数量（验收明细） */
        private BigDecimal diffQty;
        /** 有效期（验收/入库明细） */
        private LocalDate expiryDate;
        /** 外包装检查（验收明细） */
        private String outerPackageCheck;
        /** 外观检查（验收明细） */
        private String appearanceCheck;
        /** 退货原因（验收明细） */
        private String returnReason;
        /** 库位（入库明细） */
        private String storageLocation;
        /** 仓库（出库/退库明细） */
        private String warehouseName;
        /** 明细备注 */
        private String remark;
    }

    /** 文件分组 */
    @Data
    public static class FileGroup {
        /** 环节 */
        private String stage;
        /** 链路节点编码 */
        private String nodeKey;
        /** 显示名称 */
        private String label;
        /** 业务类型编码 */
        private String bizType;
        /** 文件槽位编码 */
        private String fileKind;
        /** 要求：REQUIRED/CONDITIONAL/OPTIONAL */
        private String requirement;
        /** 槽位编码：bizType / fileKind */
        private String slotCode;
        /** 文件列表/文件分组 */
        private List<FileItem> files = new ArrayList<>();
    }

    /** 缺件项 */
    @Data
    public static class MissingItem {
        /** 环节 */
        private String stage;
        /** 链路节点编码 */
        private String nodeKey;
        /** 显示名称 */
        private String label;
        /** 关联单据 */
        private String doc;
        /** 槽位编码 */
        private String slotCode;
    }

    /** 文件完整性清单（按节点/槽位） */
    @Data
    public static class FileChecklistItem {
        /** 链路节点编码 */
        private String nodeKey;
        /** 环节 */
        private String stage;
        /** 显示名称 */
        private String label;
        /** 业务类型编码 */
        private String bizType;
        /** 文件槽位编码 */
        private String fileKind;
        /** 槽位编码：bizType / fileKind */
        private String slotCode;
        /** 要求：REQUIRED/CONDITIONAL/OPTIONAL */
        private String requirement;
        /** 应传数（单据数） */
        private int total;
        /** 已传数（已传文件的单据数） */
        private int uploaded;
        /** 缺失数 */
        private int missing;
        /** 缺失提示，如“缺 验收单 YS-20261019-005 缺 验收检验报告” */
        private String note;
        /** 缺失明细，如“验收单 YS-20261019-005 缺 验收检验报告” */
        private List<String> missingDetails = new ArrayList<>();
        /** 单号及对应文件，用于按节点查看每张单据的文件是否齐全 */
        private List<FileChecklistDocument> documents = new ArrayList<>();
    }

    /** 文件完整性清单中的单据及附件 */
    @Data
    public static class FileChecklistDocument {
        /** 记录键：planId__workOrderId；未下达计划为 planId__none */
        private String recordKey;
        /** 工单号 */
        private String workOrderCode;
        /** 批号 */
        private String batchNo;
        /** 单据ID */
        private Long docId;
        /** 单号 */
        private String docCode;
        /** 该槽位文件是否已上传 */
        private boolean uploaded;
        /** 该槽位文件列表 */
        private List<FileItem> files = new ArrayList<>();
    }

    /** 文件项 */
    @Data
    public static class FileItem {
        /** 文件ID */
        private Long fileId;
        /** 名称/文件名 */
        private String name;
        /** 文件大小（字节） */
        private Long size;
        /** 时间 */
        private LocalDateTime time;
        /** 上传人 */
        private String uploader;
        /** 下载地址 */
        private String fileUrl;
        /** 预览地址 */
        private String previewUrl;
        /** 内容类型 */
        private String contentType;
        /** 扩展名 */
        private String fileExtension;
        /** 业务类型编码 */
        private String bizType;
        /** 文件槽位 */
        private String fileKind;
    }

    /** 导出筛选条件 */
    @Data
    public static class ExportFilters {
        /** 品种编码 */
        private String varietyCode;
        /** 日期类型 */
        private String dateType;
        /** 开始日期 */
        private String startDate;
        /** 结束日期 */
        private String endDate;
        /** 关键字 */
        private String keyword;
        /** 是否只看缺件 */
        private Boolean onlyMissing;
    }

    /** 导出请求 */
    @Data
    public static class ExportRequest {
        /** 导出范围：SELECTED / FILTERED / ALL；为空时：有 ID 按 SELECTED，否则 FILTERED */
        private String scope;
        /** 选中的工单ID列表 */
        private List<Long> workOrderIds = new ArrayList<>();
        /** 选中的生产计划ID列表 */
        private List<Long> planIds = new ArrayList<>();
        /** 导出筛选条件 */
        private ExportFilters filters;
        /** 兼容旧调用：等同于 filters.varietyCode */
        private String varietyCode;
        /** 是否包含品种资料 */
        private Boolean includeProductFiles = true;
        /** 默认必传 + 条件必传 */
        private List<String> requirements = new ArrayList<>();
        /** 是否生成文件清单 */
        private Boolean includeManifest = true;
        /** ZIP / CSV */
        private String format = "ZIP";
        /** 下载文件名前缀 */
        private String fileName;
    }
}
