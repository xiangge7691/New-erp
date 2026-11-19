package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 工单工序排产计划
 *
 * @TableName work_order_process_plan
 */
@TableName(value = "work_order_process_plan")
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkOrderProcessPlan extends AuditEntity {

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
     * 制剂工序模板ID
     */
    @TableField(value = "template_id")
    private Long templateId;

    /**
     * 工序顺序
     */
    @TableField(value = "step_order")
    private Integer stepOrder;

    /**
     * 计划开始时间
     */
    @TableField(value = "plan_start_time")
    private LocalDateTime planStartTime;

    /**
     * 计划结束时间
     */
    @TableField(value = "plan_end_time")
    private LocalDateTime planEndTime;

    /**
     * 计划用时（分钟）
     */
    @TableField(value = "plan_duration_minutes")
    private Integer planDurationMinutes;

    /**
     * 计划数量
     */
    @TableField(value = "plan_qty")
    private BigDecimal planQty;

    /**
     * 计量单位ID（快照自工序模板）
     */
    @TableField(value = "unit_id")
    private Long unitId;

    /**
     * 计划房间ID
     */
    @TableField(value = "room_id")
    private Integer roomId;

    /**
     * 计划设备ID
     */
    @TableField(value = "equipment_id")
    private Integer equipmentId;

    /**
     * 计划班组ID
     */
    @TableField(value = "team_id")
    private Long teamId;

    /**
     * 状态：草稿/已保存/需重排
     */
    @TableField(value = "status")
    private String status;

    /**
     * 是否已排产：1是/0否
     * <p>由前端保存排产时传参决定，后端不按计划起止时间推算</p>
     */
    @TableField(value = "scheduled")
    private Integer scheduled;

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;

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
     * 工序类型名称
     */
    @TableField(exist = false)
    private String processTypeName;

    /**
     * 房间名称
     */
    @TableField(exist = false)
    private String roomName;

    /**
     * 设备名称
     */
    @TableField(exist = false)
    private String equipmentName;

    /**
     * 班组名称
     */
    @TableField(exist = false)
    private String teamName;

    /**
     * 工单编号
     */
    @TableField(exist = false)
    private String workOrderCode;

    // endregion
}
