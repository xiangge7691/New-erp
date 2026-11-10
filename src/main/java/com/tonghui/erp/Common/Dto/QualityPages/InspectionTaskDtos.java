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
        /** 主键ID */
        private Long id;
        /** 检验任务号 */
        private String taskCode;
        /** 工单ID */
        private Long workOrderId;
        /** 工单编号 */
        private String workOrderCode;
        /** 生产计划编号 */
        private String planNumber;
        /** 制剂编码 */
        private String preparationCode;
        /** 制剂名称 */
        private String preparationName;
        /** 批号，未生成时为空字符串 */
        private String batchNo;
        /** 规格 */
        private String spec;
        /** 被检品属性：生产原料/中间产品/成品 */
        private String itemCategory;
        /** 检验对象名称 */
        private String objectName;
        /** 当前环节：REQUEST/PLAN/SAMPLING/INSPECTION/RETENTION/DESTRUCTION/DONE */
        private String currentStep;
        /** 任务状态：待开始/进行中/已完成/已作废/已归档 */
        private String taskStatus;
        /** 归档时间 */
        private LocalDateTime archiveTime;
        /** 归档人ID */
        private Long archiveBy;
        /** 检验计划编号 */
        private String planCode;
        /** 请检单号 */
        private String requestCode;
        /** 取样编号 */
        private String samplingCode;
        /** 留样编号 */
        private String retentionCode;
        /** 检验记录编号 */
        private String inspectionCode;
        /** 销毁记录编号 */
        private String destructionCode;
        /** 检验结论 */
        private String inspectionConclusion;
        /** 留样到期日期 */
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
        /** 工单ID */
        private Long workOrderId;
        /** 工单编号 */
        private String workOrderCode;
        /** 生产计划编号 */
        private String planNumber;
        /** 制剂编码 */
        private String preparationCode;
        /** 制剂名称 */
        private String preparationName;
        /** 批号，未生成时为空字符串 */
        private String batchNo;
        /** 规格 */
        private String spec;
        /** 任务数量 */
        private Integer taskCount;
        /** 已完成任务数 */
        private Integer completedCount;
        /** 当前环节：REQUEST/PLAN/SAMPLING/INSPECTION/RETENTION/DESTRUCTION/DONE */
        private String currentStep;
        /** 批次状态 */
        private String batchStatus;
        /** 检验结论 */
        private String inspectionConclusion;
        /** 批次下的检验任务列表 */
        private List<TaskSummary> tasks = new ArrayList<>();
    }

    /**
     * 检验看板统计
     * <p>
     * 三态口径：待办 = 请检/检验计划，检验中 = 取样至销毁（尚未办结），已完成 = 全部办结（DONE）。
     * 任务维度包含无工单的生产原料任务，已作废任务不计入；批次维度只统计挂工单的任务。
     * </p>
     */
    @Data
    public static class BoardStats {
        /** 批次总数（只统计挂工单的批次） */
        private int batchTotal;
        /** 待办批次数：所有未完成任务都停留在请检/检验计划 */
        private int todoBatches;
        /** 检验中批次数：未全部完成，且至少一条任务处于取样及以后环节 */
        private int inspectingBatches;
        /** 已完成批次数：批次内所有任务均已全部办结 */
        private int completedBatches;
        /** 不合格批次数（任一任务检验结论为不合格） */
        private int unqualifiedBatches;
        /** 任务总数（含无工单的生产原料任务，不含已作废任务） */
        private int taskTotal;
        /** 待办任务数：当前环节为请检/检验计划 */
        private int todoTasks;
        /** 检验中任务数：当前环节为取样/检验记录/留样/销毁 */
        private int inspectingTasks;
        /** 已完成任务数：当前环节为 DONE */
        private int completedTasks;
        /** 六步环节计数（不含已完成环节） */
        private List<StepCount> stepCounts = new ArrayList<>();
    }

    /** 环节计数 */
    @Data
    public static class StepCount {
        /** 环节编码 */
        private String stepCode;
        /** 环节名称 */
        private String stepName;
        /** 环节批次数：至少一条任务处于该环节的批次数 */
        private int batchCount;
        /** 环节任务数 */
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
        /** 主键ID */
        private Long id;
        /** 检验任务号 */
        private String taskCode;
        /** 工单ID */
        private Long workOrderId;
        /** 工单编号 */
        private String workOrderCode;
        /** 生产计划编号 */
        private String planNumber;
        /** 制剂编码 */
        private String preparationCode;
        /** 制剂名称 */
        private String preparationName;
        /** 批号，未生成时为空字符串 */
        private String batchNo;
        /** 被检品属性：生产原料/中间产品/成品 */
        private String itemCategory;
        /** 检验对象名称 */
        private String objectName;
        /** 规格 */
        private String spec;
        /** 当前环节：REQUEST/PLAN/SAMPLING/INSPECTION/RETENTION/DESTRUCTION/DONE */
        private String currentStep;
        /** 任务状态：待开始/进行中/已完成/已作废/已归档 */
        private String taskStatus;
        /** 归档时间 */
        private LocalDateTime archiveTime;
        /** 归档人ID */
        private Long archiveBy;
        /** 是否要求留样：1是/0否 */
        private Integer retentionRequired;
        /** 是否要求销毁：1是/0否，默认非必须 */
        private Integer destructionRequired;
        /** 开始时间 */
        private LocalDateTime startTime;
        /** 完成时间 */
        private LocalDateTime completeTime;
        /** 备注 */
        private String remark;
        /** 六步明细 */
        private List<StepDetail> steps = new ArrayList<>();
    }

    /** 单步详情 */
    @Data
    public static class StepDetail {
        /** 环节编码 */
        private String stepCode;
        /** 环节名称 */
        private String stepName;
        /** 环节状态 */
        private String stepStatus;
        /** 引用业务记录ID */
        private Long refId;
        /** 引用业务表名 */
        private String refTable;
        /** 业务单号 */
        private String code;
        /** 办理人 */
        private String operatorName;
        /** 开始时间 */
        private LocalDateTime startTime;
        /** 完成时间 */
        private LocalDateTime completeTime;
        /** 跳过原因 */
        private String skipReason;
        /** 备注 */
        private String remark;
        /** 该步骤单据字段，键值对形式返回，前端按步骤渲染 */
        private List<FieldItem> fields = new ArrayList<>();
        /** 附件列表 */
        private List<FileItem> files = new ArrayList<>();
    }

    /** 通用字段项 */
    @Data
    public static class FieldItem {
        /** 字段键 */
        private String key;
        /** 字段名称 */
        private String label;
        /** 字段值 */
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
        /** 文件ID */
        private Long fileId;
        /** 文件名 */
        private String name;
        /** 文件大小 */
        private Long size;
        /** 上传时间 */
        private String time;
        /** 上传人 */
        private String uploader;
    }

    /** 创建检验任务请求 */
    @Data
    public static class CreateTaskRequest {
        /** 工单ID */
        private Long workOrderId;
        /** 被检品属性：生产原料/中间产品/成品 */
        private String itemCategory;
        /** 检验对象名称 */
        private String objectName;
        /** 是否要求留样：1是/0否 */
        private Integer retentionRequired = 1;
        /** 是否要求销毁：1是/0否，默认非必须 */
        private Integer destructionRequired = 0;
        /** 备注 */
        private String remark;
    }

    /** 检验计划步骤请求 */
    @Data
    public static class PlanStepRequest {
        /** 检验计划编号 */
        private String planCode;
        /** 计划周期 */
        private String planPeriod;
        /** 检验类型（历史字段：后端按任务被检品属性自动推导，传入值不再生效） */
        private String inspectionType;
        /** 检验对象名称 */
        private String objectName;
        /** 批号，未生成时为空字符串 */
        private String batchNo;
        /** 规格 */
        private String spec;
        /** 检验项目概要 */
        private String inspectionSummary;
        /** 计划日期 */
        private LocalDate planTime;
        /** 备注 */
        private String remark;
        /** 附件文件ID列表 */
        private List<Long> fileIds = new ArrayList<>();
    }

    /** 请检步骤请求 */
    @Data
    public static class RequestStepRequest {
        /** 请检单号 */
        private String inspectionCode;
        /** 检验计划编号（可为空，检验计划在请检之后办理） */
        private String planCode;
        /** 检验项目名称 */
        private String inspectionItemName;
        /** 被检品属性：生产原料/中间产品/成品 */
        private String itemCategory;
        /** 批号 */
        private String batchNumber;
        /** 规格 */
        private String spec;
        /** 工序/检验环节 */
        private String processName;
        /** 配置数量 */
        private String configQuantity;
        /** 请检部门 */
        private String requestDepartment;
        /** 请检人 */
        private String requester;
        /** 请检时间 */
        private LocalDateTime requestTime;
        /** 检验项目 */
        private String inspectionItems;
        /** 备注 */
        private String remark;
        /** 附件文件ID列表 */
        private List<Long> fileIds = new ArrayList<>();
    }

    /** 取样步骤请求 */
    @Data
    public static class SamplingStepRequest {
        /** 取样编号 */
        private String samplingCode;
        /** 取样地点 */
        private String location;
        /** 数量；取样或留样时表示取样量/留样量 */
        private String quantity;
        /** 取样份数 */
        private Integer count;
        /** 取样方法 */
        private String method;
        /** 取样人 */
        private String sampler;
        /** 取样时间 */
        private LocalDateTime samplingTime;
        /** 备注 */
        private String remark;
        /** 附件文件ID列表 */
        private List<Long> fileIds = new ArrayList<>();
    }

    /** 留样步骤请求 */
    @Data
    public static class RetentionStepRequest {
        /** 留样编号 */
        private String retainedCode;
        /** 数量；取样或留样时表示取样量/留样量 */
        private String quantity;
        /** 留样日期 */
        private LocalDate retainedDate;
        /** 留样到期日期 */
        private LocalDate expiryDate;
        /** 存放位置 */
        private String storageLocation;
        /** 观察记录 */
        private String observationRecord;
        /** 备注 */
        private String remark;
        /** 附件文件ID列表 */
        private List<Long> fileIds = new ArrayList<>();
    }

    /** 检验步骤请求 */
    @Data
    public static class InspectionStepRequest {
        /** 检验记录编号 */
        private String inspectionCode;
        /** 检验依据 */
        private String inspectionBasis;
        /** 检验项目 */
        private String inspectionItem;
        /** 检验人 */
        private String inspector;
        /** 复核人 */
        private String reviewer;
        /** 开始时间 */
        private LocalDateTime startTime;
        /** 结束时间 */
        private LocalDateTime endTime;
        /** 检验结论 */
        private String conclusion;
        /** 备注 */
        private String remark;
        /** 附件文件ID列表 */
        private List<Long> fileIds = new ArrayList<>();
    }

    /** 销毁步骤请求 */
    @Data
    public static class DestructionStepRequest {
        /** 销毁记录编号 */
        private String destructionCode;
        /** 销毁类型 */
        private String recordType = "留样及稳定性考察产品销毁";
        /** 销毁原因 */
        private String destroyReason;
        /** 销毁方式 */
        private String destroyMethod;
        /** 销毁地点 */
        private String destroyLocation;
        /** 申请人 */
        private String applicantName;
        /** 申请日期 */
        private LocalDate applyDate;
        /** 批准人 */
        private String approverName;
        /** 批准日期 */
        private LocalDate approveDate;
        /** 销毁人 */
        private String destroyerName;
        /** 销毁日期 */
        private LocalDate destroyDate;
        /** 监督人 */
        private String supervisorName;
        /** 监督日期 */
        private LocalDate superviseDate;
        /** 备注 */
        private String remark;
        /** 附件文件ID列表 */
        private List<Long> fileIds = new ArrayList<>();
    }

    /** 跳过步骤请求 */
    @Data
    public static class SkipStepRequest {
        /** 环节编码 */
        private String stepCode;
        /** 原因 */
        private String reason;
    }

    /** 作废任务请求 */
    @Data
    public static class CancelRequest {
        /** 原因 */
        private String reason;
    }
}
