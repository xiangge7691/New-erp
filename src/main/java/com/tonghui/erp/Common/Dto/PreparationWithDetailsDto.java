package com.tonghui.erp.Common.Dto;

import com.tonghui.erp.Data.Entity.Preparation;
import com.tonghui.erp.Data.Entity.PreparationDocument;
import com.tonghui.erp.Data.Entity.PreparationFormula;
import com.tonghui.erp.Data.Entity.PreparationProcessTemplate;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 制剂包含详细信息的扩展数据传输对象
 * <p>
 * 在制剂基础上扩展了配方、文档和工艺模板列表，用于展示完整的制剂详情
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PreparationWithDetailsDto extends Preparation {

    /**
     * 制剂配方列表
     */
    private List<PreparationFormula> formulas;

    /**
     * 制剂文档列表
     */
    private List<PreparationDocument> documents;

    /**
     * 制剂工艺模板列表
     */
    private List<PreparationProcessTemplate> processTemplates;
}
