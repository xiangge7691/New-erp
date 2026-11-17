package com.tonghui.erp.Common.Dto.Performance;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 修改绩效时长请求
 */
@Data
public class PerformanceModifyRequest {

    /** 绩效时长（小时） */
    private BigDecimal performanceHours;

    /** 完成数量（仅手动记录可改） */
    private BigDecimal completedQty;

    /** 备注，必填 */
    private String note;
}
