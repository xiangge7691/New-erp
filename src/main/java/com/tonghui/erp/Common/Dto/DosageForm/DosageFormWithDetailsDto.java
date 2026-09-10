package com.tonghui.erp.Common.Dto.DosageForm;

import com.tonghui.erp.Data.Entity.DosageForm;
import com.tonghui.erp.Data.Entity.Preparation;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 剂型包含关联制剂的扩展数据传输对象
 * <p>
 * 在剂型基础上扩展了关联的制剂列表，用于展示完整的剂型详情及关联制剂信息
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DosageFormWithDetailsDto extends DosageForm {

    /**
     * 该剂型关联的制剂列表
     */
    private List<Preparation> preparations;
}
