package com.tonghui.erp.Common.Dto.MaterialRequisitionSlip;

import com.tonghui.erp.Data.Entity.MaterialRequisitionSlip;
import com.tonghui.erp.Data.Entity.MaterialRequisitionSlipDetail;
import lombok.Data;

import java.util.List;

/**
 * 物料领料单保存请求数据传输对象
 * <p>
 * 用于领料单创建/更新接口的请求体，同时携带领料单主表与明细列表
 * </p>
 */
@Data
public class MaterialRequisitionSlipWithDetailsRequest {

    /**
     * 领料单主表信息
     */
    private MaterialRequisitionSlip slip;

    /**
     * 领料明细列表
     */
    private List<MaterialRequisitionSlipDetail> details;
}
