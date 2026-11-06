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
        private Long workOrderId;
        private String workOrderCode;
        private String batchNo;
        private String preparationCode;
        private String preparationName;
        private String spec;
        private String unitName;
        private BigDecimal planQty;
        private BigDecimal finishedQty;
        private LocalDate planDate;
        private LocalDate actualStart;
        private LocalDate actualEnd;
        private BigDecimal yieldRate;
        private String inspectionConclusion;
        private int processTotal;
        private int processPass;
        private int processFail;
        private int processPending;
        private int fileUploaded;
        private int fileRequired;
        private int fileMissing;
        private int deviationTotal;
        private int deviationOpen;
        private String releaseStatus;
        private boolean canRelease;
    }

    /** 批次详情 */
    @Data
    public static class BatchDetail {
        private BatchInfo batch;
        private List<PurchaseItem> purchases = new ArrayList<>();
        private List<AcceptanceItem> acceptances = new ArrayList<>();
        private List<DocumentItem> instructions = new ArrayList<>();
        private List<DocumentItem> requisitions = new ArrayList<>();
        private List<DocumentItem> returns = new ArrayList<>();
        private List<ProcessItem> processes = new ArrayList<>();
        private List<InspectionItem> requests = new ArrayList<>();
        private List<InspectionItem> samplings = new ArrayList<>();
        private List<InspectionItem> inspections = new ArrayList<>();
        private List<RoomItem> rooms = new ArrayList<>();
        private YieldInfo yieldInfo;
        private List<DeviationItem> deviations = new ArrayList<>();
        private List<FileChecklistItem> fileChecklist = new ArrayList<>();
        private Compliance compliance;
        private ReleaseInfo release;
    }

    /** 批次基本信息 */
    @Data
    public static class BatchInfo {
        private Long workOrderId;
        private String workOrderCode;
        private String batchNo;
        private String preparationCode;
        private String preparationName;
        private String spec;
        private String unitName;
        private BigDecimal planQty;
        private BigDecimal finishedQty;
        private LocalDate planDate;
        private LocalDate actualStart;
        private LocalDate actualEnd;
        private BigDecimal yieldRate;
        private String inspectionConclusion;
        private String workOrderStatus;
    }

    /** 采购订单 */
    @Data
    public static class PurchaseItem {
        private Long id;
        private String code;
        private String supplier;
        private String materialType;
        private String status;
        private LocalDate date;
        private int fileCount;
        private List<FileItem> files = new ArrayList<>();
    }

    /** 验收单 */
    @Data
    public static class AcceptanceItem {
        private Long id;
        private String code;
        private String status;
        private String purchaseNumber;
        private List<String> uploadedKinds = new ArrayList<>();
        private List<String> missingKinds = new ArrayList<>();
        private List<FileItem> files = new ArrayList<>();
    }

    /** 指令/领料/退库等单据 */
    @Data
    public static class DocumentItem {
        private Long id;
        private String code;
        private String type;
        private LocalDate date;
        private String status;
        private String operator;
        private List<FileItem> files = new ArrayList<>();
    }

    /** 工序记录与符合性 */
    @Data
    public static class ProcessItem {
        private Long executionId;
        private Long processTypeId;
        private String processName;
        private String requirement;
        private String result;
        private String inspector;
        private LocalDate checkDate;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String operator;
        private String status;
        private List<FileItem> files = new ArrayList<>();
    }

    /** 检验相关单据 */
    @Data
    public static class InspectionItem {
        private Long id;
        private String code;
        private String type;
        private String process;
        private String status;
        private String conclusion;
        private LocalDateTime time;
        private String operator;
        private List<FileItem> files = new ArrayList<>();
    }

    /** 区域/环境记录 */
    @Data
    public static class RoomItem {
        private Long roomId;
        private String roomName;
        private String record;
        private String result;
    }

    /** 收率 */
    @Data
    public static class YieldInfo {
        private BigDecimal planQty;
        private BigDecimal finishedQty;
        private BigDecimal yieldRate;
        private BigDecimal threshold;
        private boolean pass;
    }

    /** 偏差 */
    @Data
    public static class DeviationItem {
        private Long id;
        private String code;
        private String title;
        private String conclusion;
        private String handler;
        private LocalDate handleDate;
        private String status;
        private List<FileItem> files = new ArrayList<>();
    }

    /** 文件完整性清单 */
    @Data
    public static class FileChecklistItem {
        private String stage;
        private String label;
        private String bizType;
        private String fileKind;
        private String requirement;
        private int uploaded;
        private int total;
        private String note;
    }

    /** 合规汇总 */
    @Data
    public static class Compliance {
        private boolean inspectionPass;
        private boolean processPass;
        private boolean filePass;
        private boolean deviationPass;
        private boolean yieldPass;
        private boolean canRelease;
        private String summary;
    }

    /** 放行信息 */
    @Data
    public static class ReleaseInfo {
        private Long id;
        private String releaseCode;
        private String conclusion;
        private String approver;
        private LocalDateTime reviewTime;
        private String opinion;
    }

    /** 文件项 */
    @Data
    public static class FileItem {
        private Long fileId;
        private String name;
        private Long size;
        private String time;
        private String uploader;
    }

    /** 放行请求 */
    @Data
    public static class ApproveRequest {
        private String approver;
        private LocalDateTime reviewTime;
        private String opinion;
    }

    /** 拒绝请求 */
    @Data
    public static class RejectRequest {
        private String reviewer;
        private LocalDateTime reviewTime;
        private String reason;
    }

    /** 工序检查保存请求 */
    @Data
    public static class ProcessCheckSaveRequest {
        private List<ProcessCheckItem> items = new ArrayList<>();
    }

    /** 工序检查项 */
    @Data
    public static class ProcessCheckItem {
        private Long executionId;
        private Long processTypeId;
        private String processName;
        private String requirementSnapshot;
        private String checkResult;
        private Long inspectorId;
        private String inspectorName;
        private LocalDate checkDate;
        private String remark;
    }
}
