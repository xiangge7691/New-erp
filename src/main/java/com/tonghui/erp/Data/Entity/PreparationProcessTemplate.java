package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 制剂工序模版表
 * @TableName preparation_process_template
 */
@TableName(value = "preparation_process_template")
@Data
@EqualsAndHashCode(callSuper = true)
public class PreparationProcessTemplate extends AuditEntity {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 模版唯一标识
     */
    @TableId(value = "template_id", type = IdType.AUTO)
    private Long templateId;

    // endregion

    // region 业务字段
    // ===================================
    // 业务字段
    // ===================================

    /**
     * 制剂ID
     */
    @TableField(value = "preparation_id")
    private Long preparationId;

    /**
     * 工序类型ID
     */
    @TableField(value = "process_type_id")
    private Long processTypeId;

    /**
     * 配置室ID（关联 room_info.room_id）
     * <p>工序模板的配置室以ID为准，必须与设备所在房间一致</p>
     */
    @TableField(value = "room_id")
    private Integer roomId;

    /**
     * 设备ID（关联 equipment.equipment_id）
     * <p>工序模板的设备以ID为准，避免设备重名时按名称匹配出错</p>
     */
    @TableField(value = "equipment_id")
    private Integer equipmentId;

    /**
     * 工序顺序
     */
    @TableField(value = "step_order")
    private Integer stepOrder;

    /**
     * 标准加工数量
     */
    @TableField(value = "standard_qty")
    private BigDecimal standardQty;

    /**
     * 计量单位ID
     */
    @TableField(value = "unit_id")
    private Long unitId;

    /**
     * 标准工时（分钟）
     */
    @TableField(value = "standard_duration")
    private Integer standardDuration;

    /**
     * 设备名称快照（由 equipment_id 反填，仅用于展示，不再作为关联依据）
     */
    @TableField(value = "equipment_desc")
    private String equipmentDesc;

    /**
     * 设备关键参数
     */
    @TableField(value = "equipment_params")
    private String equipmentParams;

    /**
     * 关键工艺参数
     */
    @TableField(value = "key_process_params")
    private String keyProcessParams;

    /**
     * 配置室名称快照（由 room_id 反填，仅用于展示，不再作为关联依据）
     */
    @TableField(value = "room_desc")
    private String roomDesc;

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

    // region 关联表显示字段
    // ===================================
    // 关联表显示字段
    // ===================================

    /**
     * 制剂名称（关联preparation表）
     */
    @TableField(exist = false)
    private String preparationName;

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
     * 单位名称（关联unit表）
     */
    @TableField(exist = false)
    private String unitName;

    /**
     * 配置室名称（关联room_info表，按 room_id 查询）
     */
    @TableField(exist = false)
    private String roomName;

    /**
     * 配置室编码（关联room_info表，按 room_id 查询）
     */
    @TableField(exist = false)
    private String roomCode;

    /**
     * 设备名称（关联equipment表，按 equipment_id 查询）
     */
    @TableField(exist = false)
    private String equipmentName;

    /**
     * 设备编码（关联equipment表，按 equipment_id 查询，用于区分重名设备）
     */
    @TableField(exist = false)
    private String fixedAssetCode;

    // endregion
}
