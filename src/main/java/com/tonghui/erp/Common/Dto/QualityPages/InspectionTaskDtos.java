package com.tonghui.erp.Common.Dto.QualityPages;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 检验管理页 DTO 集合
 * <p>
 * 包含检验任务列表、任务详情、六步步骤以及各步骤办理请求。
 * 所有步骤请求只承载该步骤单据的业务字段，任务流转由服务层统一推进。
 * </p>
 */
public final class InspectionTaskDtos {

    private InspectionTaskDtos() {
    }

    /** 检验任务列表项 */
    @Data
    public static class TaskSummary {
        private Long id;
        private String taskCode;
        private Long workOrderId;
        private String workOrderCode;
        private String planNumber;
        private String preparationCode;
        private String preparationName;
        private String batchNo;
        private String spec;
        private String itemCategory;
        private String objectName;
        private String currentStep;
        private String taskStatus;
        private String planCode;
        private String requestCode;
        private String samplingCode;
        private String retentionCode;
        private String inspectionCode;
        private String destructionCode;
        private String inspectionConclusion;
        private LocalDate retentionExpiryDate;
    }

    /**
     * 按批次汇总的检验任务
     * <p>
     * 一个工单/批次下可包含成品、中间产品、生产原料等多条检验任务，
     * 批次状态取未完成任务的当前环节，结论按所有任务汇总。
     * </p>
     */
    @Data
    public static class BatchSummary {
        private Long workOrderId;
        private String workOrderCode;
        private String planNumber;
        private String preparationCode;
        private String preparationName;
        private String batchNo;
        private String spec;
        private Integer taskCount;
        private Integer completedCount;
        private String currentStep;
        private String batchStatus;
        private String inspectionConclusion;
        private List<TaskSummary> tasks = new ArrayList<>();
    }

    /** 检验看板统计 */
    @Data
    public static class BoardStats {
        private int batchTotal;
        private int todoBatches;
        private int inspectingBatches;
        private int completedBatches;
        private int unqualifiedBatches;
        private int taskTotal;
        private int todoTasks;
        private int inspectingTasks;
        private int completedTasks;
        private List<StepCount> stepCounts = new ArrayList<>();
    }

    /** 环节计数 */
    @Data
    public static class StepCount {
        private String stepCode;
        private String stepName;
        private int batchCount;
        private int taskCount;

        public StepCount() {
        }

        public StepCount(String stepCode, String stepName) {
            this.stepCode = stepCode;
            this.stepName = stepName;
        }
    }

    /** 检验任务详情 */
    @Data
    public static class TaskDetail {
        private Long id;
        private String taskCode;
        private Long workOrderId;
        private String workOrderCode;
        private String planNumber;
        private String preparationCode;
        private String preparationName;
        private String batchNo;
        private String itemCategory;
        private String objectName;
        private String spec;
        private String currentStep;
        private String taskStatus;
        private Integer retentionRequired;
        private Integer destructionRequired;
        private LocalDateTime startTime;
        private LocalDateTime completeTime;
        private String remark;
        private List<StepDetail> steps = new ArrayList<>();
    }

    /** 单步详情 */
    @Data
    public static class StepDetail {
        private String stepCode;
        private String stepName;
        private String stepStatus;
        private Long refId;
        private String refTable;
        private String code;
        private String operatorName;
        private LocalDateTime startTime;
        private LocalDateTime completeTime;
        private String skipReason;
        private String remark;
        /** 该步骤单据字段，键值对形式返回，前端按步骤渲染 */
        private List<FieldItem> fields = new ArrayList<>();
        private List<FileItem> files = new ArrayList<>();
    }

    /** 通用字段项 */
    @Data
    public static class FieldItem {
        private String key;
        private String label;
        private String value;

        public FieldItem() {
        }

        public FieldItem(String key, String label, String value) {
            this.key = key;
            this.label = label;
            this.value = value;
        }
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

    /** 创建检验任务请求 */
    @Data
    public static class CreateTaskRequest {
        private Long workOrderId;
        private String itemCategory;
        private String objectName;
        private Integer retentionRequired = 1;
        private Integer destructionRequired = 0;
        private String remark;
    }

    /** 检验计划步骤请求 */
    @Data
    public static class PlanStepRequest {
        private String planCode;
        private String planPeriod;
        private String inspectionType;
        private String objectName;
        private String batchNo;
        private String spec;
        private String inspectionSummary;
        private LocalDate planTime;
        private String remark;
        private List<Long> fileIds = new ArrayList<>();
    }

    /** 请检步骤请求 */
    @Data
    public static class RequestStepRequest {
        private String inspectionCode;
        private String planCode;
        private String inspectionItemName;
        private String itemCategory;
        private String batchNumber;
        private String spec;
        private String processName;
        private String configQuantity;
        private String requestDepartment;
        private String requester;
        private LocalDateTime requestTime;
        private String inspectionItems;
        private String remark;
        private List<Long> fileIds = new ArrayList<>();
    }

    /** 取样步骤请求 */
    @Data
    public static class SamplingStepRequest {
        private String samplingCode;
        private String location;
        private String quantity;
        private Integer count;
        private String method;
        private String sampler;
        private LocalDateTime samplingTime;
        private String remark;
        private List<Long> fileIds = new ArrayList<>();
    }

    /** 留样步骤请求 */
    @Data
    public static class RetentionStepRequest {
        private String retainedCode;
        private String quantity;
        private LocalDate retainedDate;
        private LocalDate expiryDate;
        private String storageLocation;
        private String observationRecord;
        private String remark;
        private List<Long> fileIds = new ArrayList<>();
    }

    /** 检验步骤请求 */
    @Data
    public static class InspectionStepRequest {
        private String inspectionCode;
        private String inspectionBasis;
        private String inspectionItem;
        private String inspector;
        private String reviewer;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String conclusion;
        private String remark;
        private List<Long> fileIds = new ArrayList<>();
    }

    /** 销毁步骤请求 */
    @Data
    public static class DestructionStepRequest {
        private String destructionCode;
        private String recordType = "留样及稳定性考察产品销毁";
        private String destroyReason;
        private String destroyMethod;
        private String destroyLocation;
        private String applicantName;
        private LocalDate applyDate;
        private String approverName;
        private LocalDate approveDate;
        private String destroyerName;
        private LocalDate destroyDate;
        private String supervisorName;
        private LocalDate superviseDate;
        private String remark;
        private List<Long> fileIds = new ArrayList<>();
    }

    /** 跳过步骤请求 */
    @Data
    public static class SkipStepRequest {
        private String stepCode;
        private String reason;
    }

    /** 作废任务请求 */
    @Data
    public static class CancelRequest {
        private String reason;
    }
}
