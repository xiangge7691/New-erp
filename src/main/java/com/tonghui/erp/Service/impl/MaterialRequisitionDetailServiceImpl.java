package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.MaterialRequisitionDetail;
import com.tonghui.erp.Data.mapper.MaterialRequisitionDetailMapper;
import com.tonghui.erp.Service.MaterialRequisitionDetailService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 领料明细服务实现类
 */
@Service
public class MaterialRequisitionDetailServiceImpl extends ServiceImpl<MaterialRequisitionDetailMapper, MaterialRequisitionDetail>
        implements MaterialRequisitionDetailService {

    @Override
    public List<MaterialRequisitionDetail> getByRequisitionId(Long requisitionId) {
        QueryWrapper<MaterialRequisitionDetail> wrapper = new QueryWrapper<>();
        wrapper.eq("requisition_id", requisitionId);
        wrapper.eq("is_deleted", 0);
        return this.list(wrapper);
    }

    @Override
    @Transactional
    public void batchSave(Long requisitionId, List<MaterialRequisitionDetail> details) {
        // 先物理删除原有记录
        baseMapper.physicalDeleteByRequisitionId(requisitionId);

        // 批量插入新记录
        if (details != null && !details.isEmpty()) {
            for (MaterialRequisitionDetail detail : details) {
                detail.setId(null);
                detail.setRequisitionId(requisitionId);
                detail.setIsDeleted(0);
                detail.setVersion(1);
            }
            this.saveBatch(details);
        }
    }
}
