package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工单工序执行记录表
 * @TableName work_order_process_execution
 */
@TableName(value = "work_order_process_execution")
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkOrderProcessExecution extends AuditEntity {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联工单ID
     */
    @TableField(value = "work_order_id")
    private Long workOrderId;

    /**
     * 工序类型ID
     */
    @TableField(value = "process_type_id")
    private Long processTypeId;

    /**
     * 配置室ID
     */
    @TableField(value = "room_id")
    private Integer roomId;

    /**
     * 使用设备ID
     */
    @TableField(value = "equipment_id")
    private Integer equipmentId;

    /**
     * 工序顺序
     */
    @TableField(value = "step_order")
    private Integer stepOrder;

    // endregion

    // region 业务字段
    // ===================================
    // 业务字段
    // ===================================

    /**
     * 操作人ID
     */
    @TableField(value = "operator_id")
    private Long operatorId;

    /**
     * 操作人姓名
     */
    @TableField(value = "operator_name")
    private String operatorName;

    /**
     * 开始时间
     */
    @TableField(value = "start_time")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @TableField(value = "end_time")
    private LocalDateTime endTime;

    /**
     * 加工数量
     */
    @TableField(value = "process_qty")
    private BigDecimal processQty;

    /**
     * 关键工艺参数
     */
    @TableField(value = "key_process_params")
    private String keyProcessParams;

    /**
     * 状态（待执行/执行中/已完成）
     */
    @TableField(value = "status")
    private String status;

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
     * 是否已删除
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
     * 工序类型名称（关联process_type表）
     */
    @TableField(exist = false)
    private String processTypeName;

    /**
     * 工序编码（关联process_type表）
     */
    @TableField(exist = false)
    private String processCode;

    /**
     * 配置室名称（关联room_info表）
     */
    @TableField(exist = false)
    private String roomName;

    /**
     * 设备名称（关联equipment表）
     */
    @TableField(exist = false)
    private String equipmentName;

    /**
     * 计量单位
     */
    @TableField(exist = false)
    private String unitName;

    // endregion
}
