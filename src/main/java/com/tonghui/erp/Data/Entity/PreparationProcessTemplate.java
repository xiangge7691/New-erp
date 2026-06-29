package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 制剂工序模版表
 * @TableName preparation_process_template
 */
@TableName(value = "preparation_process_template")
@Data
public class PreparationProcessTemplate {
    /**
     * 模版唯一标识
     */
    @TableId(value = "template_id", type = IdType.AUTO)
    private Long templateId;

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
     * 设备要求描述
     */
    @TableField(value = "equipment_desc")
    private String equipmentDesc;

    /**
     * 配置室要求
     */
    @TableField(value = "room_desc")
    private String roomDesc;

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;

    /**
     * 创建人ID
     */
    @TableField(value = "created_by")
    private Long createdBy;

    /**
     * 更新人ID
     */
    @TableField(value = "updated_by")
    private Long updatedBy;

    /**
     * 创建时间
     */
    @TableField(value = "created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at")
    private LocalDateTime updatedAt;

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
}
