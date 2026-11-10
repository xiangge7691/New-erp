package com.tonghui.erp.Common.Dto.QualityPages;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 审核放行页 DTO 集合
 * <p>
 * 以工单/批次为单位汇总采购、验收、生产指令、领料、退库、工序、检验、区域、
 * 收率、偏差、文件完整性和放行审核结果。
 * </p>
 */
public final class ReleaseBatchDtos {

    private ReleaseBatchDtos() {
    }

    /** 批次列表项 */
    @Data
    public static class BatchSummary {
        /** 工单ID */
        private Long workOrderId;
        /** 工单编号 */
        private String workOrderCode;
        /** 生产计划编号 */
        private String planNumber;
        /** 批号 */
        private String batchNo;
        /** 制剂编码 */
        private String preparationCode;
        /** 制剂名称 */
        private String preparationName;
        /** 规格 */
        private String spec;
        /** 单位 */
        private String unitName;
        /** 计划数量 */
        private BigDecimal planQty;
        /** 成品数量 */
        private BigDecimal finishedQty;
        /** 计划日期 */
        private LocalDate planDate;
        /** 实际生产开始日期 */
        private LocalDate actualStart;
        /** 实际生产结束日期 */
        private LocalDate actualEnd;
        /** 收率(%) */
        private BigDecimal yieldRate;
        /** 检验结论 */
        private String inspectionConclusion;
        /** 工序总数 */
        private int processTotal;
        /** 工序合格数 */
        private int processPass;
        /** 工序不合格数 */
        private int processFail;
        /** 待执行工序数 */
        private int processPending;
        /** 已上传文件数 */
        private int fileUploaded;
        /** 必传文件数 */
        private int fileRequired;
        /** 缺失文件数 */
        private int fileMissing;
        /** 偏差总数 */
        private int deviationTotal;
        /** 未关闭偏差数 */
        private int deviationOpen;
        /** 放行状态：待审核/待放行/已放行/已拒绝 */
        private String releaseStatus;
        /** 放行批准人（放行审核记录的审核人，未生成放行记录时为空） */
        private String releaseApprover;
        /** 放行日期（放行审核记录的审核日期，未生成放行记录时为空） */
        private LocalDate releaseDate;
        /** 是否可放行 */
        private boolean canRelease;
        /** 列表直接展示的阻塞项，例如：检验未完成、文件缺 3、偏差 1 未关闭 */
        private List<String> blockers = new ArrayList<>();
    }

    /** 审核放行看板统计 */
    @Data
    public static class BoardStats {
        /** 批次总数 */
        private int total;
        /** 待审核数 */
        private int pendingReview;
        /** 可放行数 */
        private int releasable;
        /** 存在阻塞项数 */
        private int blocked;
        /** 已放行数 */
        private int released;
        /** 已拒绝数 */
        private int rejected;
    }

    /** 批次详情 */
    @Data
    public static class BatchDetail {
        /** 批次基本信息 */
        private BatchInfo batch;
        /** 采购订单列表 */
        private List<PurchaseItem> purchases = new ArrayList<>();
        /** 验收单列表 */
        private List<AcceptanceItem> acceptances = new ArrayList<>();
        /** 生产指令列表 */
        private List<DocumentItem> instructions = new ArrayList<>();
        /** 领料单列表 */
        private List<DocumentItem> requisitions = new ArrayList<>();
        /** 退库单列表 */
        private List<DocumentItem> returns = new ArrayList<>();
        /** 工序记录与符合性 */
        private List<ProcessItem> processes = new ArrayList<>();
        /** 请检单列表 */
        private List<InspectionItem> requests = new ArrayList<>();
        /** 取样记录列表 */
        private List<InspectionItem> samplings = new ArrayList<>();
        /** 检验记录列表 */
        private List<InspectionItem> inspections = new ArrayList<>();
        /** 区域/环境记录 */
        private List<RoomItem> rooms = new ArrayList<>();
        /** 收率信息 */
        private YieldInfo yieldInfo;
        /** 偏差记录 */
        private List<DeviationItem> deviations = new ArrayList<>();
        /** 文件完整性清单 */
        private List<FileChecklistItem> fileChecklist = new ArrayList<>();
        /** 合规汇总 */
        private Compliance compliance;
        /** 放行信息 */
        private ReleaseInfo release;
    }

    /** 批次基本信息 */
    @Data
    public static class BatchInfo {
        /** 工单ID */
        private Long workOrderId;
        /** 工单编号 */
        private String workOrderCode;
        /** 生产计划编号 */
        private String planNumber;
        /** 批号 */
        private String batchNo;
        /** 制剂编码 */
        private String preparationCode;
        /** 制剂名称 */
        private String preparationName;
        /** 规格 */
        private String spec;
        /** 单位 */
        private String unitName;
        /** 计划数量 */
        private BigDecimal planQty;
        /** 成品数量 */
        private BigDecimal finishedQty;
        /** 计划日期 */
        private LocalDate planDate;
        /** 实际生产开始日期 */
        private LocalDate actualStart;
        /** 实际生产结束日期 */
        private LocalDate actualEnd;
        /** 收率(%) */
        private BigDecimal yieldRate;
        /** 检验结论 */
        private String inspectionConclusion;
        /** 工单状态 */
        private String workOrderStatus;
        /** 放行状态：待审核/待放行/已放行/已拒绝 */
        private String releaseStatus;
    }

    /** 采购订单 */
    @Data
    public static class PurchaseItem {
        /** 采购订单ID */
        private Long id;
        /** 采购单号 */
        private String code;
        /** 供应商 */
        private String supplier;
        /** 物料类型 */
        private String materialType;
        /** 采购状态 */
        private String status;
        /** 下单日期 */
        private LocalDate date;
        /** 下单时间 */
        private LocalDateTime orderTime;
        /** 制单人 */
        private String creator;
        /** 发票号 */
        private String invoiceNo;
        /** 备注 */
        private String remark;
        /** 采购明细 */
        private List<PurchaseLineItem> lines = new ArrayList<>();
        /** 附件数 */
        private int fileCount;
        /** 附件列表 */
        private List<FileItem> files = new ArrayList<>();
    }

    /** 采购明细行 */
    @Data
    public static class PurchaseLineItem {
        /** 物料编码 */
        private String materialCode;
        /** 物料名称 */
        private String materialName;
        /** 规格 */
        private String spec;
        /** 采购数量 */
        private BigDecimal quantity;
        /** 单位 */
        private String unit;
        /** 单价 */
        private BigDecimal unitPrice;
        /** 金额 */
        private BigDecimal amount;
        /** 实际到货数量 */
        private BigDecimal actualArrivalQty;
        /** 发票号 */
        private String invoiceNo;
        /** 供应商 */
        private String supplier;
    }

    /** 验收单 */
    @Data
    public static class AcceptanceItem {
        /** 验收单ID */
        private Long id;
        /** 验收单号 */
        private String code;
        /** 验收状态 */
        private String status;
        /** 关联采购单号 */
        private String purchaseNumber;
        /** 来源类型 */
        private String sourceType;
        /** 到货时间 */
        private LocalDateTime arrivalTime;
        /** 验收时间 */
        private LocalDateTime acceptanceTime;
        /** 验收人 */
        private String acceptor;
        /** 复核人 */
        private String reviewer;
        /** 备注 */
        private String remark;
        /** 已上传文件类型编码 */
        private List<String> uploadedKinds = new ArrayList<>();
        /** 缺失文件类型编码 */
        private List<String> missingKinds = new ArrayList<>();
        /** 已上传文件类型中文名，供页面直接展示 */
        private List<String> uploadedKindLabels = new ArrayList<>();
        /** 缺失文件类型中文名，供页面直接展示 */
        private List<String> missingKindLabels = new ArrayList<>();
        /** 验收明细 */
        private List<AcceptanceLineItem> details = new ArrayList<>();
        /** 附件列表 */
        private List<FileItem> files = new ArrayList<>();
    }

    /** 验收明细行 */
    @Data
    public static class AcceptanceLineItem {
        /** 物料编码 */
        private String materialCode;
        /** 物料名称 */
        private String materialName;
        /** 物料分类 */
        private String spec;
        /** 数量 */
        private BigDecimal quantity;
        /** 单位 */
        private String unit;
        /** 批号 */
        private String batchNumber;
        /** 有效期 */
        private LocalDate expiryDate;
        /** 外观检查 */
        private String appearanceCheck;
        /** 外包装检查 */
        private String outerPackageCheck;
        /** 验收结论 */
        private String conclusion;
        /** 供应商 */
        private String supplier;
        /** 差异数量 */
        private BigDecimal diffQuantity;
    }

    /** 指令/领料/退库等单据 */
    @Data
    public static class DocumentItem {
        /** 单据ID */
        private Long id;
        /** 单据编号 */
        private String code;
        /** 单据类型 */
        private String type;
        /** 单据日期 */
        private LocalDate date;
        /** 单据状态 */
        private String status;
        /** 经办人 */
        private String operator;
        /** 备注 */
        private String remark;

        // region 生产指令字段
        /** 品种名称 */
        private String preparation;
        /** 制剂编码 */
        private String preparationCode;
        /** 规格 */
        private String spec;
        /** 批号 */
        private String batchNo;
        /** 批量 */
        private BigDecimal qty;
        /** 计划开始时间 */
        private LocalDateTime planStart;
        /** 计划结束时间 */
        private LocalDateTime planEnd;
        /** 工艺路线 */
        private String processRoute;
        /** 编制人 */
        private String creator;
        // endregion

        // region 领料/退库字段
        /** 用途 */
        private String purpose;
        /** 仓库 */
        private String warehouse;
        /** 关联出库单号 */
        private String stockOutCode;
        /** 退库原因 */
        private String reason;
        /** 明细行 */
        private List<DocumentLineItem> lines = new ArrayList<>();
        // endregion

        /** 附件列表 */
        private List<FileItem> files = new ArrayList<>();
    }

    /** 领料/退库明细行 */
    @Data
    public static class DocumentLineItem {
        /** 物料编码 */
        private String materialCode;
        /** 物料名称 */
        private String materialName;
        /** 规格/分类 */
        private String spec;
        /** 数量 */
        private BigDecimal quantity;
        /** 单位 */
        private String unit;
        /** 批号 */
        private String batchNumber;
        /** 单价 */
        private BigDecimal unitPrice;
        /** 金额 */
        private BigDecimal amount;
    }

    /** 工序记录与符合性 */
    @Data
    public static class ProcessItem {
        /** 工序执行ID */
        private Long executionId;
        /** 工序类型ID */
        private Long processTypeId;
        /** 工序名称 */
        private String processName;
        /** 关键工艺要求 */
        private String requirement;
        /** 检查结果 */
        private String result;
        /** 检查人 */
        private String inspector;
        /** 检查日期 */
        private LocalDate checkDate;
        /** 开始时间 */
        private LocalDateTime startTime;
        /** 结束时间 */
        private LocalDateTime endTime;
        /** 操作人 */
        private String operator;
        /** 执行状态 */
        private String status;
        /** 房间名称 */
        private String roomName;
        /** 设备名称 */
        private String equipmentName;
        /** 备注 */
        private String remark;
        /** 附件列表 */
        private List<FileItem> files = new ArrayList<>();
    }

    /** 检验相关单据 */
    @Data
    public static class InspectionItem {
        /** 单据ID */
        private Long id;
        /** 单据编号 */
        private String code;
        /** 单据类型：请检单/取样记录/检验记录 */
        private String type;
        /** 关联工序 */
        private String process;
        /** 单据状态 */
        private String status;
        /** 检验结论 */
        private String conclusion;
        /** 单据时间 */
        private LocalDateTime time;
        /** 经办人 */
        private String operator;
        /** 备注 */
        private String remark;
        /** 关联检验任务编号 */
        private String relatedTaskCode;
        /** 关联检验计划编号 */
        private String relatedPlanCode;

        // region 请检单字段
        /** 请检人 */
        private String requester;
        /** 请检部门 */
        private String requestDepartment;
        /** 请检时间 */
        private LocalDateTime requestTime;
        /** 检验项目 */
        private String inspectionItems;
        /** 配置数量 */
        private String configQuantity;
        // endregion

        // region 取样记录字段
        /** 取样人 */
        private String sampler;
        /** 取样时间 */
        private LocalDateTime samplingTime;
        /** 取样量 */
        private String samplingQuantity;
        /** 取样份数 */
        private Integer samplingCount;
        /** 取样方法 */
        private String samplingMethod;
        /** 取样地点 */
        private String samplingLocation;
        // endregion

        // region 检验记录字段
        /** 检验依据 */
        private String inspectionBasis;
        /** 检验项目 */
        private String inspectionItem;
        /** 检验人 */
        private String inspector;
        /** 复核人 */
        private String reviewer;
        /** 检验开始时间 */
        private LocalDateTime startTime;
        /** 检验结束时间 */
        private LocalDateTime endTime;
        /** 关联请检编号 */
        private String relatedInspectionRequestCode;
        /** 关联取样编号 */
        private String relatedSamplingCode;
        // endregion

        /** 附件列表 */
        private List<FileItem> files = new ArrayList<>();
    }

    /** 区域/环境记录 */
    @Data
    public static class RoomItem {
        /** 房间ID */
        private Long roomId;
        /** 房间名称 */
        private String roomName;
        /** 环境记录 */
        private String record;
        /** 检查结果 */
        private String result;
    }

    /** 收率 */
    @Data
    public static class YieldInfo {
        /** 计划数量 */
        private BigDecimal planQty;
        /** 成品数量 */
        private BigDecimal finishedQty;
        /** 收率(%) */
        private BigDecimal yieldRate;
        /** 收率阈值 */
        private BigDecimal threshold;
        /** 是否达标 */
        private boolean pass;
    }

    /** 偏差 */
    @Data
    public static class DeviationItem {
        /** 偏差记录ID */
        private Long id;
        /** 偏差编号 */
        private String code;
        /** 偏差标题 */
        private String title;
        /** 偏差结论 */
        private String conclusion;
        /** 处理人 */
        private String handler;
        /** 处理日期 */
        private LocalDate handleDate;
        /** 偏差状态 */
        private String status;
        /** 发生时间 */
        private LocalDateTime occurTime;
        /** 发生工序 */
        private String processName;
        /** 偏差描述 */
        private String description;
        /** 处理措施 */
        private String action;
        /** 备注 */
        private String remark;
        /** 附件列表 */
        private List<FileItem> files = new ArrayList<>();
    }

    /** 文件完整性清单 */
    @Data
    public static class FileChecklistItem {
        /** 环节 */
        private String stage;
        /** 显示名称 */
        private String label;
        /** 业务类型 */
        private String bizType;
        /** 文件槽位 */
        private String fileKind;
        /** 要求：REQUIRED/CONDITIONAL/OPTIONAL */
        private String requirement;
        /** 已上传数 */
        private int uploaded;
        /** 应传数 */
        private int total;
        /** 备注 */
        private String note;
        /** 缺失明细，例如“验收单 YS-20261019-005 缺 验收检验报告” */
        private List<String> missingDetails = new ArrayList<>();
    }

    /** 合规汇总 */
    @Data
    public static class Compliance {
        /** 检验是否通过 */
        private boolean inspectionPass;
        /** 工序是否通过 */
        private boolean processPass;
        /** 文件是否完整 */
        private boolean filePass;
        /** 偏差是否满足放行要求 */
        private boolean deviationPass;
        /** 收率是否达标 */
        private boolean yieldPass;
        /** 是否允许放行 */
        private boolean canRelease;
        /** 合规摘要 */
        private String summary;
    }

    /** 放行信息 */
    @Data
    public static class ReleaseInfo {
        /** 放行记录ID */
        private Long id;
        /** 放行编号 */
        private String releaseCode;
        /** 放行结论 */
        private String conclusion;
        /** 审核人 */
        private String approver;
        /** 审核时间 */
        private LocalDateTime reviewTime;
        /** 审核意见 */
        private String opinion;
    }

    /** 文件项 */
    @Data
    public static class FileItem {
        /** 文件ID */
        private Long fileId;
        /** 文件名 */
        private String name;
        /** 文件大小（字节） */
        private Long size;
        /** 上传时间 */
        private String time;
        /** 上传人 */
        private String uploader;
    }

    /** 放行请求 */
    @Data
    public static class ApproveRequest {
        /** 审核人 */
        private String approver;
        /** 审核日期（HTML 日期框，yyyy-MM-dd） */
        private LocalDate reviewDate;
        /** 审核时间 */
        private LocalDateTime reviewTime;
        /** 审核意见 */
        private String opinion;
    }

    /** 拒绝请求 */
    @Data
    public static class RejectRequest {
        /** 审核人 */
        private String reviewer;
        /** 审核日期（HTML 日期框，yyyy-MM-dd） */
        private LocalDate reviewDate;
        /** 审核时间 */
        private LocalDateTime reviewTime;
        /** 拒绝原因 */
        private String reason;
    }

    /** 保存草稿请求 */
    @Data
    public static class DraftRequest {
        /** 草稿结论，只允许“待补充” */
        private String conclusion;
        /** 审核人 */
        private String approver;
        /** 审核日期（HTML 日期框，yyyy-MM-dd） */
        private LocalDate reviewDate;
        /** 审核时间 */
        private LocalDateTime reviewTime;
        /** 审核意见 */
        private String opinion;
    }

    /** 导出请求 */
    @Data
    public static class ExportRequest {
        /** 品种编码 */
        private String varietyCode;
        /** 批号/工单号 */
        private String batchNo;
        /** 放行状态 */
        private String status;
        /** 关键字（工单号/批号/品种） */
        private String keyword;
    }

    /** 品种下拉选项 */
    @Data
    public static class VarietyOption {
        /** 制剂编码 */
        private String preparationCode;
        /** 制剂名称 */
        private String preparationName;
    }

    /** 工序检查保存请求 */
    @Data
    public static class ProcessCheckSaveRequest {
        /** 工序检查项列表 */
        private List<ProcessCheckItem> items = new ArrayList<>();
    }

    /** 工序检查项 */
    @Data
    public static class ProcessCheckItem {
        /** 工序执行ID */
        private Long executionId;
        /** 工序类型ID */
        private Long processTypeId;
        /** 工序名称 */
        private String processName;
        /** 关键工艺要求快照 */
        private String requirementSnapshot;
        /** 检查结果 */
        private String checkResult;
        /** 检查人ID */
        private Long inspectorId;
        /** 检查人姓名 */
        private String inspectorName;
        /** 检查日期 */
        private LocalDate checkDate;
        /** 备注 */
        private String remark;
    }
}
