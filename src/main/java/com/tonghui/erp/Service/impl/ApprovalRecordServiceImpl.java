package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.ApprovalRecord;
import com.tonghui.erp.Service.ApprovalRecordService;
import com.tonghui.erp.Data.mapper.ApprovalRecordMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 审批记录服务实现类
 * <p>
 * 实现ApprovalRecordService接口，提供审批记录的CRUD操作及按实例/节点查询功能
 * </p>
 */
@Service
public class ApprovalRecordServiceImpl extends ServiceImpl<ApprovalRecordMapper, ApprovalRecord>
    implements ApprovalRecordService{
    
    // region 查询方法
    // ===================================
    // 查询方法
    // ===================================

    /**
     * 根据实例ID获取审批记录列表
     *
     * @param instanceId 审批实例ID
     * @return 审批记录列表，按createdTime倒序
     */
    @Override
    public List<ApprovalRecord> getRecordsByInstanceId(Long instanceId) {
        QueryWrapper<ApprovalRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("instance_id", instanceId);
        queryWrapper.orderByDesc("created_time");
        return list(queryWrapper);
    }
    
    /**
     * 根据节点ID获取审批记录列表
     *
     * @param nodeId 审批节点ID
     * @return 审批记录列表，按createdTime倒序
     */
    @Override
    public List<ApprovalRecord> getRecordsByNodeId(Long nodeId) {
        QueryWrapper<ApprovalRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("node_id", nodeId);
        queryWrapper.orderByDesc("created_time");
        return list(queryWrapper);
    }

    // endregion
}
