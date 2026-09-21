package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.MaterialRequisition;
import com.tonghui.erp.Data.mapper.MaterialRequisitionMapper;
import com.tonghui.erp.Service.MaterialRequisitionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 领料申请服务实现类
 */
@Service
public class MaterialRequisitionServiceImpl extends ServiceImpl<MaterialRequisitionMapper, MaterialRequisition>
        implements MaterialRequisitionService {

    @Override
    public List<MaterialRequisition> getByWorkOrderId(Long workOrderId) {
        QueryWrapper<MaterialRequisition> wrapper = new QueryWrapper<>();
        wrapper.eq("work_order_id", workOrderId);
        wrapper.eq("is_deleted", 0);
        wrapper.orderByDesc("created_time");
        return this.list(wrapper);
    }

    @Override
    @Transactional
    public boolean addRequisition(MaterialRequisition requisition) {
        // 设置默认状态
        if (requisition.getStatus() == null) {
            requisition.setStatus("草稿");
        }

        return this.save(requisition);
    }
}
