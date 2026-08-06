package com.tonghui.erp.Common.Dto.Stock;

import com.tonghui.erp.Data.Entity.AcceptanceDetail;
import com.tonghui.erp.Data.Entity.AcceptanceOrder;
import lombok.Data;

import java.util.List;

/**
 * 货物验收单保存请求数据传输对象
 * <p>
 * 用于验收单创建/更新接口的请求体，同时携带验收单主表与明细列表
 * </p>
 */
@Data
public class AcceptanceWithDetailsRequest {

    /**
     * 验收单主表信息
     */
    private AcceptanceOrder acceptance;

    /**
     * 验收明细列表
     */
    private List<AcceptanceDetail> details;
}
