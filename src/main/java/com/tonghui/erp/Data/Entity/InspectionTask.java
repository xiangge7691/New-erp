package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 检验任务实体
 * <p>
 * 将检验计划、请检、取样、留样、检验、销毁六个环节串成一条可流转的任务，
 * 与工单、批次、被检品属性绑定。任务本身只保存流程索引，具体单据仍落在原业务表。
 * </p>
 *
 * @TableName inspection_task
 */
@TableName(value = "inspection_task")
@Data
@EqualsAndHashCode(callSuper = true)
public class InspectionTask extends AuditEntity {

    // region 基本信息
    // ===================================
    // 基本信息
    // ===================================

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 检验任务号 */
    @TableField(value = "task_code")
    private String taskCode;

    /** 工单ID，生产原料请检单生成的 task 为空 */
    @TableField(value = "work_order_id")
    private Long workOrderId;

    /** 工单编号，生产原料请检单生成的 task 为空 */
    @TableField(value = "work_order_code")
    private String workOrderCode;

    /** 生产计划ID */
    @TableField(value = "plan_id")
    private Long planId;

    /** 生产计划编号 */
    @TableField(value = "plan_number")
    private String planNumber;

    /** 制剂ID */
    @TableField(value = "preparation_id")
    private Long preparationId;

    /** 制剂编码 */
    @TableField(value = "preparation_code")
    private String preparationCode;

    /** 制剂名称 */
    @TableField(value = "preparation_name")
    private String preparationName;

    /** 批号，未生成批号时为空字符串 */
    @TableField(value = "batch_no")
    private String batchNo;

    /** 被检品属性：生产原料/中间产品/成品 */
    @TableField(value = "item_category")
    private String itemCategory;

    /** 检验对象名称 */
    @TableField(value = "object_name")
    private String objectName;

    /** 规格 */
    @TableField(value = "spec")
    private String spec;

    // endregion

    // region 流程状态
    // ===================================
    // 流程状态
    // ===================================

    /** 当前环节：PLAN/REQUEST/SAMPLING/RETENTION/INSPECTION/DESTRUCTION/DONE */
    @TableField(value = "current_step")
    private String currentStep;

    /** 任务状态：待开始/进行中/已完成/已跳过/已作废 */
    @TableField(value = "task_status")
    private String taskStatus;

    /** 是否要求留样：1是/0否 */
    @TableField(value = "retention_required")
    private Integer retentionRequired;

    /** 是否要求销毁：1是/0否 */
    @TableField(value = "destruction_required")
    private Integer destructionRequired;

    /** 检验计划ID */
    @TableField(value = "plan_ref_id")
    private Long planRefId;

    /** 请检单ID */
    @TableField(value = "request_ref_id")
    private Long requestRefId;

    /** 取样记录ID */
    @TableField(value = "sampling_ref_id")
    private Long samplingRefId;

    /** 留样记录ID */
    @TableField(value = "retention_ref_id")
    private Long retentionRefId;

    /** 检验记录ID */
    @TableField(value = "inspection_ref_id")
    private Long inspectionRefId;

    /** 销毁记录ID */
    @TableField(value = "destruction_ref_id")
    private Long destructionRefId;

    /** 任务开始时间 */
    @TableField(value = "start_time")
    private LocalDateTime startTime;

    /** 任务完成时间 */
    @TableField(value = "complete_time")
    private LocalDateTime completeTime;

    /** 备注 */
    @TableField(value = "remark")
    private String remark;

    // endregion

    // region 审计与乐观锁
    // ===================================
    // 审计与乐观锁
    // ===================================

    /** 是否删除：0否/1是 */
    @TableField(value = "is_deleted")
    private Integer isDeleted;

    /** 乐观锁版本号 */
    @TableField(value = "version")
    private Integer version;

    // endregion
}
