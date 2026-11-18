package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 人员绩效记录
 *
 * @TableName performance_record
 */
@TableName(value = "performance_record")
@Data
@EqualsAndHashCode(callSuper = true)
public class PerformanceRecord extends AuditEntity {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // endregion

    // region 业务字段
    // ===================================
    // 业务字段
    // ===================================

    /**
     * 人员档案ID
     */
    @TableField(value = "personnel_id")
    private Long personnelId;

    /**
     * 工单ID
     */
    @TableField(value = "work_order_id")
    private Long workOrderId;

    /**
     * 工序类型ID
     */
    @TableField(value = "process_type_id")
    private Long processTypeId;

    /**
     * 排产计划行ID
     */
    @TableField(value = "plan_id")
    private Long planId;

    /**
     * 排产批次ID
     */
    @TableField(value = "plan_batch_id")
    private Long planBatchId;

    /**
     * 实际执行记录ID，手动记录为空
     */
    @TableField(value = "execution_id")
    private Long executionId;

    /**
     * 批次序号
     */
    @TableField(value = "batch_no")
    private Integer batchNo;

    /**
     * 绩效归属日期
     */
    @TableField(value = "performance_date")
    private LocalDate performanceDate;

    /**
     * 工序实际时长（小时，只读）
     */
    @TableField(value = "process_actual_hours")
    private BigDecimal processActualHours;

    /**
     * 个人工作时间（小时）
     */
    @TableField(value = "work_hours")
    private BigDecimal workHours;

    /**
     * 绩效时长（小时）
     */
    @TableField(value = "performance_hours")
    private BigDecimal performanceHours;

    /**
     * 修改前绩效时长（小时）：首次修改时写入，保留原始口径
     */
    @TableField(value = "original_performance_hours")
    private BigDecimal originalPerformanceHours;

    /**
     * 工序系数快照
     */
    @TableField(value = "process_coefficient")
    private BigDecimal processCoefficient;

    /**
     * 人员系数快照
     */
    @TableField(value = "person_coefficient")
    private BigDecimal personCoefficient;

    /**
     * 系数版本ID
     */
    @TableField(value = "coefficient_version_id")
    private Long coefficientVersionId;

    /**
     * 计算方式：按系数计算/直接计入
     */
    @TableField(value = "calc_mode")
    private String calcMode;

    /**
     * 绩效工时
     */
    @TableField(value = "performance_hours_total")
    private BigDecimal performanceHoursTotal;

    /**
     * 完成数量
     */
    @TableField(value = "completed_qty")
    private BigDecimal completedQty;

    /**
     * 来源：系统生成/手动添加
     */
    @TableField(value = "source")
    private String source;

    /**
     * 修改状态：0未修改/1已修改
     */
    @TableField(value = "modified_status")
    private Integer modifiedStatus;

    /**
     * 记录状态：1正常/0作废
     */
    @TableField(value = "record_status")
    private Integer recordStatus;

    /**
     * 备注
     */
    @TableField(value = "note")
    private String note;

    // endregion

    // region 状态与审计字段
    // ===================================
    // 状态与审计字段
    // ===================================

    /**
     * 是否已删除：0否/1是
     */
    @TableField(value = "is_deleted")
    private Integer isDeleted;

    /**
     * 乐观锁版本号
     */
    @TableField(value = "version")
    private Integer version;

    // endregion

    // region 关联显示字段（不映射数据库）
    // ===================================
    // 关联显示字段
    // ===================================

    /**
     * 人员姓名
     */
    @TableField(exist = false)
    private String personnelName;

    /**
     * 工单编号
     */
    @TableField(exist = false)
    private String workOrderCode;

    /**
     * 制剂名称
     */
    @TableField(exist = false)
    private String preparationName;

    /**
     * 批号
     */
    @TableField(exist = false)
    private String batchNumber;

    /**
     * 工序名称
     */
    @TableField(exist = false)
    private String processName;

    /**
     * 批次名称
     */
    @TableField(exist = false)
    private String planBatchName;

    /**
     * 人员所属班组名称（多个班组用、连接）
     */
    @TableField(exist = false)
    private String teamName;

    /**
     * 计划开始时间（批次优先，无批次时取工序计划）
     */
    @TableField(exist = false)
    private LocalDateTime planStartTime;

    /**
     * 计划结束时间（批次优先，无批次时取工序计划）
     */
    @TableField(exist = false)
    private LocalDateTime planEndTime;

    /**
     * 计划用时（分钟，批次优先，无批次时取工序计划）
     */
    @TableField(exist = false)
    private Integer planDurationMinutes;

    /**
     * 计划数量（批次优先，无批次时取工序计划）
     */
    @TableField(exist = false)
    private BigDecimal planQty;

    /**
     * 实际开始时间（关联实际执行记录）
     */
    @TableField(exist = false)
    private LocalDateTime actualStartTime;

    /**
     * 实际结束时间（关联实际执行记录）
     */
    @TableField(exist = false)
    private LocalDateTime actualEndTime;

    /**
     * 系数版本号
     */
    @TableField(exist = false)
    private String coefficientVersionNo;

    /**
     * 最后修改人姓名
     */
    @TableField(exist = false)
    private String updatedByName;

    // endregion
}
