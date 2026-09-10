package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 领料明细表
 * @TableName material_requisition_detail
 */
@TableName(value = "material_requisition_detail")
@Data
public class MaterialRequisitionDetail {

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
     * 领料申请ID
     */
    @TableField(value = "requisition_id")
    private Long requisitionId;

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
     * 分类（原料/辅料/包材）
     */
    @TableField(value = "material_category")
    private String materialCategory;

    /**
     * 单位
     */
    @TableField(value = "unit_name")
    private String unitName;

    // endregion

    // region 业务字段
    // ===================================
    // 业务字段
    // ===================================

    /**
     * 处方用量
     */
    @TableField(value = "prescription_qty")
    private BigDecimal prescriptionQty;

    /**
     * 申请数量
     */
    @TableField(value = "apply_qty")
    private BigDecimal applyQty;

    /**
     * 库存数量
     */
    @TableField(value = "stock_qty")
    private BigDecimal stockQty;

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
