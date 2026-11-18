package com.tonghui.erp.Common.Dto.Performance;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 绩效系数版本详情
 */
@Data
public class CoefficientVersionDetail {

    /** 版本ID */
    private Long id;

    /** 版本号 */
    private String versionNo;

    /** 生效时间 */
    private LocalDateTime effectiveTime;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人姓名 */
    private String operatorName;

    /** 调整说明 */
    private String note;

    /** 状态 */
    private String status;

    /** 工序系数：工序类型ID -> 系数 */
    private Map<Long, BigDecimal> processCoefficients;

    /** 人员系数：人员档案ID -> 系数 */
    private Map<Long, BigDecimal> personCoefficients;

    /** 工序系数明细（含工序编码与名称） */
    private List<ProcessCoefficientItem> processItems;

    /** 人员系数明细（含姓名、工号与班组） */
    private List<PersonCoefficientItem> personItems;
}
