package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 生产工序记录表
 * @TableName production_process_record
 */
@TableName(value ="production_process_record")
@Data
public class ProductionProcessRecord {
    /**
     * 记录 ID，主键。自增长，唯一标识。
     */
    @TableId(value = "record_id", type = IdType.AUTO)
    private Long recordId;

    /**
     * 生产计划 ID。外键关联生产计划表 (production_plan.id)。
     */
    @TableField(value = "plan_id")
    private Integer planId;

    /**
     * 工序类型名称。
     */
    @TableField(value = "process_name")
    private String processName;

    /**
     * 操作人姓名。
     */
    @TableField(value = "operator_name")
    private String operatorName;

    /**
     * 工序顺序。同一生产计划内的工序执行顺序，从 1 开始递增。
     */
    @TableField(value = "step_order")
    private Integer stepOrder;

    /**
     * 配置室。记录工序执行的房间。
     */
    @TableField(value = "workshop")
    private String workshop;

    /**
     * 加工数量。本工序实际加工的数量。
     */
    @TableField(value = "processing_qty")
    private BigDecimal processingQty;

    /**
     * 计量单位。如：kg、g、L 等。
     */
    @TableField(value = "unit_name")
    private String unitName;

    /**
     * 使用设备。记录工序执行过程中使用的设备信息。
     */
    @TableField(value = "equipment")
    private String equipment;

    /**
     * 工序开始时间。工序实际开始时间。
     */
    @TableField(value = "start_time")
    private LocalDateTime startTime;

    /**
     * 工序结束时间。工序实际结束时间。
     */
    @TableField(value = "end_time")
    private LocalDateTime endTime;

    /**
     * 记录状态。`1`-正常，`0`-作废。
     */
    @TableField(value = "record_status")
    private Integer recordStatus;

    /**
     * 备注信息。记录工序执行过程中的特殊说明。
     */
    @TableField(value = "remark")
    private String remark;

    /**
     * 创建人 ID。记录创建者 ID，关联用户表 (user.user_id)。
     */
    @TableField(value = "creator_id")
    private Long creatorId;

    /**
     * 创建时间。记录插入时间，自动填充。
     */
    @TableField(value = "created_time")
    private LocalDateTime createdTime;

    /**
     * 修改人 ID。记录最后修改者 ID，关联用户表 (user.user_id)。
     */
    @TableField(value = "updater_id")
    private Long updaterId;

    /**
     * 最后修改时间。记录更新时自动更新。
     */
    @TableField(value = "updated_time")
    private LocalDateTime updatedTime;
}