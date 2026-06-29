package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.ApprovalWorkflow;
import com.tonghui.erp.Service.ApprovalWorkflowService;
import com.tonghui.erp.Data.mapper.ApprovalWorkflowMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author 87954
* @description 针对表【approval_workflow(审批流程定义)】的数据库操作Service实现
* @createDate 2025-12-18 09:50:00
*/
@Service
public class ApprovalWorkflowServiceImpl extends ServiceImpl<ApprovalWorkflowMapper, ApprovalWorkflow>
    implements ApprovalWorkflowService{

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
}




