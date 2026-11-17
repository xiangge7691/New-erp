package com.tonghui.erp.Common.Dto.Performance;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 发布绩效系数版本请求
 */
@Data
public class CoefficientPublishRequest {

    /** 调整说明，必填 */
    private String note;

    /** 工序系数：工序类型ID -> 系数 */
    private Map<Long, BigDecimal> processCoefficients;

    /** 人员系数：人员档案ID -> 系数 */
    private Map<Long, BigDecimal> personCoefficients;
}
