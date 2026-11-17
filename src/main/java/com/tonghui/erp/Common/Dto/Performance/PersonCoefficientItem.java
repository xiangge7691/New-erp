package com.tonghui.erp.Common.Dto.Performance;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 系数版本内的人员系数明细
 */
@Data
public class PersonCoefficientItem {

    /** 人员档案ID */
    private Long personnelId;

    /** 姓名 */
    private String personnelName;

    /** 工号 */
    private String employeeNo;

    /** 所属班组名称（多个班组用、连接） */
    private String teamName;

    /** 人员系数 */
    private BigDecimal coefficient;
}
