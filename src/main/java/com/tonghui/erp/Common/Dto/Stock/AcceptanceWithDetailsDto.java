package com.tonghui.erp.Common.Dto.Stock;

import com.tonghui.erp.Data.Entity.AcceptanceDetail;
import com.tonghui.erp.Data.Entity.AcceptanceOrder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 货物验收单包含明细的扩展数据传输对象
 * <p>
 * 在验收单基础上扩展了验收明细列表，用于展示完整的验收单及其明细信息
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AcceptanceWithDetailsDto extends AcceptanceOrder {

    /**
     * 验收明细列表
     */
    private List<AcceptanceDetail> details;
}
