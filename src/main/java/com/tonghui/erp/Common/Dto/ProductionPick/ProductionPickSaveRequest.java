package com.tonghui.erp.Common.Dto.ProductionPick;

import com.tonghui.erp.Data.Entity.ProductionPick;
import com.tonghui.erp.Data.Entity.ProductionPickDetail;
import lombok.Data;

import java.util.List;

/**
 * 生产领料单保存请求数据传输对象
 * <p>
 * 用于新增/修改生产领料单接口，同时携带主表信息与明细列表
 * </p>
 */
@Data
public class ProductionPickSaveRequest {

    /**
     * 生产领料单主表信息
     */
    private ProductionPick pick;

    /**
     * 生产领料明细列表
     */
    private List<ProductionPickDetail> details;
}
