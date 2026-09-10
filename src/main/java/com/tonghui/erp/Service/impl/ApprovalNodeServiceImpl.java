package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.Dto.Approval.ApprovalNodeWithRecordsDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.ApprovalNode;
import com.tonghui.erp.Data.Entity.ApprovalRecord;
import com.tonghui.erp.Data.mapper.ApprovalNodeMapper;
import com.tonghui.erp.Data.mapper.ApprovalRecordMapper;
import com.tonghui.erp.Service.ApprovalNodeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 审批节点服务实现类
 * <p>
 * 实现ApprovalNodeService接口，提供审批节点的CRUD操作、条件查询及带子表的联合查询功能
 * </p>
 */
@Service
public class ApprovalNodeServiceImpl extends ServiceImpl<ApprovalNodeMapper, ApprovalNode>
    implements ApprovalNodeService {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 审批记录Mapper
     */
    @Autowired
    private ApprovalRecordMapper approvalRecordMapper;

    // endregion

    // region 基础查询方法
    // ===================================
    // 基础查询方法
    // ===================================

    /**
     * 根据流程ID获取审批节点列表
     *
     * @param workflowId 流程ID
     * @return 审批节点列表，按nodeOrder升序
     */
    @Override
    public List<ApprovalNode> getNodesByWorkflowId(Long workflowId) {
        QueryWrapper<ApprovalNode> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("workflow_id", workflowId);
        queryWrapper.orderByAsc("node_order");
        return list(queryWrapper);
    }

    /**
     * 根据流程ID和节点顺序获取审批节点
     *
     * @param workflowId 流程ID
     * @param nodeOrder  节点顺序
     * @return 审批节点
     */
    @Override
    public ApprovalNode getNodeByWorkflowIdAndOrder(Long workflowId, Integer nodeOrder) {
        QueryWrapper<ApprovalNode> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("workflow_id", workflowId);
        queryWrapper.eq("node_order", nodeOrder);
        return getOne(queryWrapper);
    }

    /**
     * 获取审批节点列表（分页）
     *
     * @param pageIndex 页码索引
     * @param pageSize  每页数量
     * @return 分页结果
     */
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

    // endregion

    // region 条件查询方法
    // ===================================
    // 条件查询方法
    // ===================================

    /**
     * 条件查询审批节点（分页）
     *
     * @param approvalNode 查询条件对象
     * @param pageNum      页码（从0开始）
     * @param pageSize     每页数量
     * @return MyBatis Plus分页结果
     */
    @Override
    public Page<ApprovalNode> queryApprovalNodes(ApprovalNode approvalNode, int pageNum, int pageSize) {
        int actualPageNum = pageNum + 1;
        Page<ApprovalNode> page = new Page<>(actualPageNum, pageSize);
        QueryWrapper<ApprovalNode> wrapper = new QueryWrapper<>();

        if (approvalNode.getId() != null) {
            wrapper.eq("id", approvalNode.getId());
        }
        if (approvalNode.getWorkflowId() != null) {
            wrapper.eq("workflow_id", approvalNode.getWorkflowId());
        }
        if (StringUtils.hasText(approvalNode.getNodeName())) {
            wrapper.like("node_name", approvalNode.getNodeName());
        }
        if (approvalNode.getNodeOrder() != null) {
            wrapper.eq("node_order", approvalNode.getNodeOrder());
        }
        if (approvalNode.getRoleId() != null) {
            wrapper.eq("role_id", approvalNode.getRoleId());
        }

        return this.page(page, wrapper);
    }

    // endregion

    // region 带子表查询方法
    // ===================================
    // 带子表查询方法
    // ===================================

    /**
     * 查询审批节点（包含审批记录子表）
     *
     * @param approvalNode 查询条件对象
     * @param pageNum      页码（从0开始）
     * @param pageSize     每页数量
     * @return 包含审批记录的分页结果
     */
    @Override
    public PagedResult<ApprovalNodeWithRecordsDto> searchWithDetails(ApprovalNode approvalNode, int pageNum, int pageSize) {
        Page<ApprovalNode> parentPage = queryApprovalNodes(approvalNode, pageNum, pageSize);
        List<ApprovalNode> parents = parentPage.getRecords();

        PagedResult<ApprovalNodeWithRecordsDto> result = new PagedResult<>();
        if (parents.isEmpty()) {
            result.setItems(List.of());
            result.setTotalCount(parentPage.getTotal());
            result.setPageIndex(pageNum);
            result.setPageSize(pageSize);
            return result;
        }

        // 批量查询关联的审批记录
        List<Long> nodeIds = parents.stream().map(ApprovalNode::getId).collect(Collectors.toList());
        QueryWrapper<ApprovalRecord> recordWrapper = new QueryWrapper<>();
        recordWrapper.in("node_id", nodeIds);
        List<ApprovalRecord> allRecords = approvalRecordMapper.selectList(recordWrapper);
        Map<Long, List<ApprovalRecord>> recordsMap = allRecords.stream()
                .collect(Collectors.groupingBy(ApprovalRecord::getNodeId));

        // 组装DTO
        List<ApprovalNodeWithRecordsDto> dtos = parents.stream().map(parent -> {
            ApprovalNodeWithRecordsDto dto = new ApprovalNodeWithRecordsDto();
            BeanUtils.copyProperties(parent, dto);
            dto.setRecords(recordsMap.getOrDefault(parent.getId(), List.of()));
            return dto;
        }).collect(Collectors.toList());

        result.setItems(dtos);
        result.setTotalCount(parentPage.getTotal());
        result.setPageIndex(pageNum);
        result.setPageSize(pageSize);
        return result;
    }

    // endregion
}
