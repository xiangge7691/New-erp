package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.Dto.Approval.ApprovalWorkflowWithNodesDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.ApprovalNode;
import com.tonghui.erp.Data.Entity.ApprovalWorkflow;
import com.tonghui.erp.Data.mapper.ApprovalNodeMapper;
import com.tonghui.erp.Service.ApprovalWorkflowService;
import com.tonghui.erp.Data.mapper.ApprovalWorkflowMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
* @author 87954
* @description 针对表【approval_workflow(审批流程定义)】的数据库操作Service实现
* @createDate 2025-12-18 09:50:00
*/
@Service
public class ApprovalWorkflowServiceImpl extends ServiceImpl<ApprovalWorkflowMapper, ApprovalWorkflow>
    implements ApprovalWorkflowService{

    @Autowired
    private ApprovalNodeMapper approvalNodeMapper;

    @Override
    public ApprovalWorkflow getByWorkflowType(String workflowType) {
        QueryWrapper<ApprovalWorkflow> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("workflow_type", workflowType);
        return getOne(queryWrapper);
    }
    
    @Override
    public List<ApprovalWorkflow> getAllWorkflows() {
        return list();
    }

    @Override
    public PagedResult<ApprovalWorkflowWithNodesDto> searchWithDetails(int pageIndex, int pageSize) {
        boolean isAllData = (pageIndex == -1 || pageSize == -1);
        Page<ApprovalWorkflow> page;
        if (isAllData) {
            page = new Page<>(1, Integer.MAX_VALUE);
        } else {
            page = new Page<>(pageIndex + 1, pageSize);
        }

        QueryWrapper<ApprovalWorkflow> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("created_time");
        Page<ApprovalWorkflow> parentPage = this.page(page, wrapper);
        List<ApprovalWorkflow> parents = parentPage.getRecords();

        PagedResult<ApprovalWorkflowWithNodesDto> result = new PagedResult<>();
        if (parents.isEmpty()) {
            result.setItems(List.of());
            result.setTotalCount(parentPage.getTotal());
            result.setPageIndex(pageIndex);
            result.setPageSize(pageSize);
            return result;
        }

        List<Long> parentIds = parents.stream().map(ApprovalWorkflow::getId).collect(Collectors.toList());
        QueryWrapper<ApprovalNode> nodeWrapper = new QueryWrapper<>();
        nodeWrapper.in("workflow_id", parentIds);
        List<ApprovalNode> allNodes = approvalNodeMapper.selectList(nodeWrapper);
        Map<Long, List<ApprovalNode>> nodesMap = allNodes.stream()
                .collect(Collectors.groupingBy(ApprovalNode::getWorkflowId));

        List<ApprovalWorkflowWithNodesDto> dtos = parents.stream().map(parent -> {
            ApprovalWorkflowWithNodesDto dto = new ApprovalWorkflowWithNodesDto();
            BeanUtils.copyProperties(parent, dto);
            dto.setNodes(nodesMap.getOrDefault(parent.getId(), List.of()));
            return dto;
        }).collect(Collectors.toList());

        result.setItems(dtos);
        result.setTotalCount(parentPage.getTotal());
        result.setPageIndex(pageIndex);
        result.setPageSize(isAllData ? (int) parentPage.getTotal() : pageSize);
        return result;
    }
}




