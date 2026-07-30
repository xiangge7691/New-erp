package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Data.Entity.MaterialRequisitionDetail;

import java.util.List;

/**
 * 领料明细服务接口
 */
public interface MaterialRequisitionDetailService extends IService<MaterialRequisitionDetail> {

    /**
     * 根据领料申请ID查询领料明细列表
     *
     * @param requisitionId 领料申请ID
     * @return 领料明细列表
     */
    List<MaterialRequisitionDetail> getByRequisitionId(Long requisitionId);

    /**
     * 批量保存领料明细（先删后插）
     *
     * @param requisitionId 领料申请ID
     * @param details 领料明细列表
     */
    void batchSave(Long requisitionId, List<MaterialRequisitionDetail> details);
}
