package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.ApprovalNode;
import com.tonghui.erp.Service.ApprovalNodeService;
import com.tonghui.erp.Data.mapper.ApprovalNodeMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author 87954
* @description 针对表【approval_node(审批节点)】的数据库操作Service实现
* @createDate 2025-12-18 09:50:00
*/
@Service
public class ApprovalNodeServiceImpl extends ServiceImpl<ApprovalNodeMapper, ApprovalNode>
    implements ApprovalNodeService{
    
    @Override
    public List<ApprovalNode> getNodesByWorkflowId(Long workflowId) {
        QueryWrapper<ApprovalNode> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("workflow_id", workflowId);
        queryWrapper.orderByAsc("node_order");
        return list(queryWrapper);
    }
    
    @Override
    public ApprovalNode getNodeByWorkflowIdAndOrder(Long workflowId, Integer nodeOrder) {
        QueryWrapper<ApprovalNode> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("workflow_id", workflowId);
        queryWrapper.eq("node_order", nodeOrder);
        return getOne(queryWrapper);
    }
    
    @Override
    public PagedResult<ApprovalNode> getNodes(int pageIndex, int pageSize) {
        Page<ApprovalNode> page = new Page<>(pageIndex + 1, pageSize);
        QueryWrapper<ApprovalNode> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("created_time");
        
        Page<ApprovalNode> pageResult = page(page, queryWrapper);
        
        PagedResult<ApprovalNode> pagedResult = new PagedResult<>();
        pagedResult.setPageIndex((int) pageResult.getCurrent() - 1);
        pagedResult.setPageSize((int) pageResult.getSize());
        pagedResult.setTotalCount((int) pageResult.getTotal());
        pagedResult.setItems(pageResult.getRecords());
        
        return pagedResult;
    }
}




