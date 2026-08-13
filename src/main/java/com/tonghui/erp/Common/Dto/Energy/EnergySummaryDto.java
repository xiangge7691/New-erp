package com.tonghui.erp.Common.Dto.Energy;

import lombok.Data;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 能耗费用汇总DTO
 * <p>
 * 随能耗列表查询一起返回：总金额 + 按能耗类型分类金额
 * </p>
 */
@Data
public class EnergySummaryDto {

    /**
     * 筛选条件下费用总金额（元）
     */
    private BigDecimal totalAmount;

    /**
     * 按能耗类型分类金额（元），key 为类型名：自来水/电/燃气
     */
    private Map<String, BigDecimal> byType = new LinkedHashMap<>();

    /**
     * 按类型增加金额（保持插入顺序：自来水、电、燃气）
     *
     * @param type  能耗类型
     * @param amount 金额
     */
    public void addByType(String type, BigDecimal amount) {
        byType.put(type, amount);
    }
}
