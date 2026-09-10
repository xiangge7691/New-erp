package com.tonghui.erp.Common.Dto;

import com.tonghui.erp.Data.Entity.PreparationProcessTemplate;
import com.tonghui.erp.Data.Entity.ProcessType;
import com.tonghui.erp.Data.Entity.ProductionProcessRecord;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 工序类型包含详细信息的扩展数据传输对象
 * <p>
 * 在工序类型基础上扩展了生产工序记录和工艺模板列表，用于展示完整的工序类型详情
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProcessTypeWithDetailsDto extends ProcessType {

    /**
     * 该工序类型的生产工序记录列表
     */
    private List<ProductionProcessRecord> records;

    /**
     * 该工序类型的工艺模板列表
     */
    private List<PreparationProcessTemplate> templates;
}
