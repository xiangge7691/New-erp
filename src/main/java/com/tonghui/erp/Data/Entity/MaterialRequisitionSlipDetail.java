package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 物料领料单明细表实体
 * <p>
 * 记录每种物料的请领数量、实发数量、入库批号、单价等信息。
 * 打印区（品名/代码/请领数量）由制剂室录入，手写区（实发数量/入库批号/单价）由药剂科线下填写后制剂室回填。
 * </p>
 *
 * @TableName material_requisition_slip_detail
 */
@TableName(value = "material_requisition_slip_detail")
@Data
public class MaterialRequisitionSlipDetail {

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
     * 领料单ID（关联主表）
     */
    @TableField(value = "slip_id")
    private Long slipId;

    /**
     * 序号（明细行序号，从1开始）
     */
    @TableField(value = "seq")
    private Integer seq;

    /**
     * 物料ID
     */
    @TableField(value = "material_id")
    private Long materialId;

    /**
     * 物料编码
     */
    @TableField(value = "material_code")
    private String materialCode;

    /**
     * 物料名称
     */
    @TableField(value = "material_name")
    private String materialName;

    /**
     * 计量单位（kg/g/L/袋/盒/瓶）
     */
    @TableField(value = "unit_name")
    private String unitName;

    // endregion

    // region 业务字段
    // ===================================
    // 业务字段
    // ===================================

    /**
     * 请领数量（制剂室申领数量）
     */
    @TableField(value = "apply_qty")
    private BigDecimal applyQty;

    /**
     * 实发数量（药剂科实际发放数量，回填录入系统）
     */
    @TableField(value = "actual_qty")
    private BigDecimal actualQty;

    /**
     * 入库批号（实际入库批号，回填录入系统）
     */
    @TableField(value = "batch_number")
    private String batchNumber;

    /**
     * 单价（药剂科定价，保留2位小数，回填录入系统）
     */
    @TableField(value = "unit_price")
    private BigDecimal unitPrice;

    // endregion

    // region 状态字段
    // ===================================
    // 状态字段
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
}
