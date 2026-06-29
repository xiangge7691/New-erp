package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.ApprovalRecord;
import com.tonghui.erp.Service.ApprovalRecordService;
import com.tonghui.erp.Data.mapper.ApprovalRecordMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author 87954
* @description 针对表【approval_record(审批记录)】的数据库操作Service实现
* @createDate 2025-12-18 09:50:00
*/
@Service
public class ApprovalRecordServiceImpl extends ServiceImpl<ApprovalRecordMapper, ApprovalRecord>
    implements ApprovalRecordService{
    
    @Override
    public List<ApprovalRecord> getRecordsByInstanceId(Long instanceId) {
        QueryWrapper<ApprovalRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("instance_id", instanceId);
        queryWrapper.orderByDesc("created_at");
        return list(queryWrapper);
    }
    
    @Override
    public List<ApprovalRecord> getRecordsByNodeId(Long nodeId) {
        QueryWrapper<ApprovalRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("node_id", nodeId);
        queryWrapper.orderByDesc("created_at");
        return list(queryWrapper);
    }
}




