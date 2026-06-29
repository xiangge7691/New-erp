package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 物料信息表（存储单位与分类名称，不用外键）
 * @TableName material
 */
@TableName(value ="material")
@Data
public class Material {
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
     * 状态：1启用/0禁用
     */
    @TableField(value = "material_status")
    private Integer materialStatus;

    /**
     * 备注信息
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
    @TableField(value = "created_time")
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    @TableField(value = "updated_time")
    private LocalDateTime updatedTime;
}