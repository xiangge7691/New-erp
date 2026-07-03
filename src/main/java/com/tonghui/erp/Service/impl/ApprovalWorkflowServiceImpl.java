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
 * 审批流程服务实现类
 * <p>
 * 实现ApprovalWorkflowService接口，提供审批流程定义的CRUD操作及带节点子表的联合查询
 * </p>
 */
@Service
public class ApprovalWorkflowServiceImpl extends ServiceImpl<ApprovalWorkflowMapper, ApprovalWorkflow>
    implements ApprovalWorkflowService{

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 审批节点Mapper
     */
    @Autowired
    private ApprovalNodeMapper approvalNodeMapper;

    // endregion

    // region 基础查询方法
    // ===================================
    // 基础查询方法
    // ===================================

    /**
     * 根据流程类型获取审批流程
     *
     * @param workflowType 流程类型
     * @return 审批流程
     */
    @Override
    public ApprovalWorkflow getByWorkflowType(String workflowType) {
        QueryWrapper<ApprovalWorkflow> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("workflow_type", workflowType);
        return getOne(queryWrapper);
    }
    
    /**
     * 获取所有审批流程
     *
     * @return 审批流程列表
     */
    @Override
    public List<ApprovalWorkflow> getAllWorkflows() {
        return list();
    }

    // endregion

    // region 带子表查询方法
    // ===================================
    // 带子表查询方法
    // ===================================

    /**
     * 查询审批流程（包含节点子表）
     *
     * @param pageIndex 页码索引，-1为全量
     * @param pageSize  每页数量，-1为全量
     * @return 包含审批节点的分页结果
     */
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

        // 批量查询关联的审批节点
        List<Long> parentIds = parents.stream().map(ApprovalWorkflow::getId).collect(Collectors.toList());
        QueryWrapper<ApprovalNode> nodeWrapper = new QueryWrapper<>();
        nodeWrapper.in("workflow_id", parentIds);
        List<ApprovalNode> allNodes = approvalNodeMapper.selectList(nodeWrapper);
        Map<Long, List<ApprovalNode>> nodesMap = allNodes.stream()
                .collect(Collectors.groupingBy(ApprovalNode::getWorkflowId));

        // 组装DTO
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

    // endregion
}
