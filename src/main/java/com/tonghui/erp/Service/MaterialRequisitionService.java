package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Data.Entity.MaterialRequisition;

import java.util.List;

/**
 * 领料申请服务接口
 */
public interface MaterialRequisitionService extends IService<MaterialRequisition> {

    /**
     * 根据工单ID查询领料申请列表
     *
     * @param workOrderId 工单ID
     * @return 领料申请列表
     */
    List<MaterialRequisition> getByWorkOrderId(Long workOrderId);

    /**
     * 新增领料申请（含明细）
     *
     * @param requisition 领料申请实体
     * @return 是否成功
     */
    boolean addRequisition(MaterialRequisition requisition);
}
