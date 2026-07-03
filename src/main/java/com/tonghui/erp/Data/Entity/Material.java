package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 物料信息表（存储单位与分类名称，不用外键）
 * @TableName material
 */
@TableName(value ="material")
@Data
@EqualsAndHashCode(callSuper = true)
public class Material extends AuditEntity {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 物料唯一标识符
     */
    @TableId(value = "material_id", type = IdType.AUTO)
    private Long materialId;

    /**
     * 物料编码（唯一性约束）
     */
    @TableField(value = "material_code")
    private String materialCode;

    /**
     * 物料名称
     */
    @TableField(value = "material_name")
    private String materialName;

    // endregion

    // region 业务字段
    // ===================================
    // 业务字段
    // ===================================

    /**
     * 分类（如原料/辅料/包材等）
     */
    @TableField(value = "category_name")
    private String categoryName;

    /**
     * 计量单位（直接存文本，如kg/张/瓶）
     */
    @TableField(value = "unit_name")
    private String unitName;

    /**
     * 规格描述
     */
    @TableField(value = "spec")
    private String spec;

    /**
     * 备注信息
     */
    @TableField(value = "remark")
    private String remark;

    // endregion

    // region 状态与审计字段
    // ===================================
    // 状态与审计字段
    // ===================================

    /**
     * 状态：1启用/0禁用
     */
    @TableField(value = "material_status")
    private Integer materialStatus;

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
