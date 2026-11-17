package com.tonghui.erp.Common.Dto.Performance;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 系数版本内的工序系数明细
 */
@Data
public class ProcessCoefficientItem {

    /** 工序类型ID */
    private Long processTypeId;

    /** 工序编码 */
    private String processCode;

    /** 工序名称 */
    private String processName;

    /** 工序系数 */
    private BigDecimal coefficient;
}
