package com.tonghui.erp.Common.Dto.Performance;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 修改绩效工时请求
 */
@Data
public class PerformanceModifyRequest {

    /** 绩效工时（小时）：系数计算后的时长，修改时直接覆盖该值 */
    private BigDecimal performanceHoursTotal;

    /** 完成数量（仅手动记录可改） */
    private BigDecimal completedQty;

    /** 备注，必填 */
    private String note;
}
