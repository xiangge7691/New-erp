package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 制剂处方信息表（含原料及单位信息）
 * @TableName preparation_formula
 */
@TableName(value ="preparation_formula")
@Data
public class PreparationFormula {
    /**
     * 处方明细唯一标识
     */
    @TableId(value = "formula_id", type = IdType.AUTO)
    private Long formulaId;

    /**
     * 制剂ID，引用preparation表
     */
    @TableField(value = "preparation_id")
    private Long preparationId;

    /**
     * 制剂编码
     */
    @TableField(value = "preparation_code")
    private String preparationCode;

    /**
     * 制剂品名
     */
    @TableField(value = "preparation_name")
    private String preparationName;

    /**
     * 原料ID，引用material表
     */
    @TableField(value = "material_id")
    private Long materialId;

    /**
     * 原料编号
     */
    @TableField(value = "material_code")
    private String materialCode;

    /**
     * 原料名称
     */
    @TableField(value = "material_name")
    private String materialName;

    /**
     * 原料分类（原料/辅料/包材）
     */
    @TableField(value = "material_category")
    private String materialCategory;

    /**
     * 处方量
     */
    @TableField(value = "dosage")
    private BigDecimal dosage;

    /**
     * 单位ID，引用unit表
     */
    @TableField(value = "unit_id")
    private Long unitId;

    /**
     * 单位名称（kg/张/个）
     */
    @TableField(value = "unit_name")
    private String unitName;

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