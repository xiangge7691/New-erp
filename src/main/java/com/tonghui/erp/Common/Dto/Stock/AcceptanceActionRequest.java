package com.tonghui.erp.Common.Dto.Stock;

import lombok.Data;

/**
 * 货物验收操作请求数据传输对象
 * <p>
 * 用于验收状态流转接口（初验/检验）的请求参数
 * </p>
 */
@Data
public class AcceptanceActionRequest {

    /**
     * 是否合格（true-合格，false-不合格）
     */
    private Boolean pass;

    /**
     * 入库仓库（生产单位ID，检验合格时必填）
     */
    private Long prodUnitId;

    /**
     * 备注说明
     */
    private String remark;
}
