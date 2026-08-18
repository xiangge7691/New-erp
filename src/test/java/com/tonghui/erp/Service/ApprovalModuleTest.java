package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.ApprovalWorkflow;
import com.tonghui.erp.Data.Entity.ApprovalNode;
import com.tonghui.erp.Data.Entity.ApprovalRecord;
import com.tonghui.erp.Data.Entity.ApprovalInstance;
import com.tonghui.erp.Data.Entity.Role;
import com.tonghui.erp.Data.mapper.ApprovalNodeMapper;
import com.tonghui.erp.Data.mapper.ApprovalRecordMapper;
import com.tonghui.erp.Data.mapper.ApprovalWorkflowMapper;
import com.tonghui.erp.Data.mapper.ApprovalInstanceMapper;
import com.tonghui.erp.Service.ApprovalWorkflowService;
import com.tonghui.erp.Service.ApprovalNodeService;
import com.tonghui.erp.Service.ApprovalRecordService;
import com.tonghui.erp.Service.ApprovalInstanceService;
import com.tonghui.erp.Service.RoleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 审批域综合测试
 * <p>
 * 覆盖审批工作流、审批节点、审批记录、审批实例等核心业务逻辑，
 * 全部用例使用事务回滚，不污染数据库
 * </p>
 */
@SpringBootTest
public class ApprovalModuleTest {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 审批工作流服务
     */
    @Autowired
    private ApprovalWorkflowService approvalWorkflowService;

    /**
     * 审批节点服务
     */
    @Autowired
    private ApprovalNodeService approvalNodeService;

    /**
     * 审批记录服务
     */
    @Autowired
    private ApprovalRecordService approvalRecordService;

    /**
     * 审批实例服务
     */
    @Autowired
    private ApprovalInstanceService approvalInstanceService;

    /**
     * 角色服务（创建测试角色供审批节点绑定）
     */
    @Autowired
    private RoleService roleService;

    /**
     * 审批工作流数据访问层
     */
    @Autowired
    private ApprovalWorkflowMapper approvalWorkflowMapper;

    /**
     * 审批节点数据访问层
     */
    @Autowired
    private ApprovalNodeMapper approvalNodeMapper;

    /**
     * 审批记录数据访问层
     */
    @Autowired
    private ApprovalRecordMapper approvalRecordMapper;

    /**
     * 审批实例数据访问层
     */
    @Autowired
    private ApprovalInstanceMapper approvalInstanceMapper;

    // endregion

    // region 测试用例
    // ===================================
    // 测试用例
    // ===================================

    /**
     * 测试审批工作流管理：新增→按类型查询→启用列表→详情分页
     */
    @Test
    @Transactional
    public void testWorkflowCrud() {
        String workflowType = "TSTTYPE" + System.currentTimeMillis();
        ApprovalWorkflow workflow = new ApprovalWorkflow();
        workflow.setWorkflowName("测试审批流程");
        workflow.setWorkflowType(workflowType);
        workflow.setStatus(1);
        assertTrue(approvalWorkflowService.save(workflow), "新增审批工作流应成功");
        Long workflowId = workflow.getId();

        // 按类型查询
        ApprovalWorkflow found = approvalWorkflowService.getByWorkflowType(workflowType);
        assertNotNull(found, "按类型应查到审批工作流");
        assertEquals(workflowId, found.getId(), "工作流ID应一致");

        // 全部工作流
        assertTrue(approvalWorkflowService.getAllWorkflows().stream().anyMatch(w -> w.getId().equals(workflowId)),
                "工作流列表应包含新建设工作流");

        // 详情分页查询
        PagedResult<com.tonghui.erp.Common.Dto.Approval.ApprovalWorkflowWithNodesDto> page =
                approvalWorkflowService.searchWithDetails(1, 10);
        assertNotNull(page, "工作流详情分页不应为空");

        // 更新
        workflow.setWorkflowName("更新后的审批流程");
        assertTrue(approvalWorkflowService.updateById(workflow), "更新工作流应成功");
        assertEquals("更新后的审批流程", approvalWorkflowService.getById(workflowId).getWorkflowName(),
                "工作流名称应已更新");
    }

    /**
     * 测试审批节点管理：新增节点→按工作流查询→按顺序查询→分页
     */
    @Test
    @Transactional
    public void testNodeCrud() {
        String workflowType = "TSTNODE" + System.currentTimeMillis();
        ApprovalWorkflow workflow = new ApprovalWorkflow();
        workflow.setWorkflowName("节点测试流程");
        workflow.setWorkflowType(workflowType);
        workflow.setStatus(1);
        approvalWorkflowService.save(workflow);
        Long workflowId = workflow.getId();

        // 创建角色供节点绑定
        Role role = new Role();
        role.setRoleName("TSTNODEROLE" + System.currentTimeMillis());
        role.setRoleStatus(1);
        roleService.save(role);

        // 新增两个节点
        ApprovalNode node1 = new ApprovalNode();
        node1.setWorkflowId(workflowId);
        node1.setNodeName("部门审批");
        node1.setNodeOrder(1);
        node1.setRoleId(role.getRoleId());
        assertTrue(approvalNodeService.save(node1), "新增审批节点应成功");

        ApprovalNode node2 = new ApprovalNode();
        node2.setWorkflowId(workflowId);
        node2.setNodeName("总经理审批");
        node2.setNodeOrder(2);
        node2.setRoleId(role.getRoleId());
        assertTrue(approvalNodeService.save(node2), "新增审批节点应成功");

        // 按工作流查询节点（按顺序排序）
        List<ApprovalNode> nodes = approvalNodeService.getNodesByWorkflowId(workflowId);
        assertEquals(2, nodes.size(), "应查到2个节点");
        assertEquals("部门审批", nodes.get(0).getNodeName(), "第1个节点应为部门审批");

        // 按工作流+顺序查询
        ApprovalNode byOrder = approvalNodeService.getNodeByWorkflowIdAndOrder(workflowId, 2);
        assertNotNull(byOrder, "按顺序应查到节点");
        assertEquals("总经理审批", byOrder.getNodeName(), "第2个节点应为总经理审批");

        // 分页查询
        PagedResult<ApprovalNode> page = approvalNodeService.getNodes(1, 10);
        assertNotNull(page, "节点分页不应为空");
        assertTrue(page.getTotalCount() >= 2, "节点总数应>=2");

        // 更新节点
        node1.setNodeName("更新后的部门审批");
        assertTrue(approvalNodeService.updateById(node1), "更新节点应成功");
        assertEquals("更新后的部门审批", approvalNodeService.getById(node1.getId()).getNodeName(),
                "节点名称应已更新");
    }

    /**
     * 测试审批记录管理：新增记录→按实例查询→按节点查询
     */
    @Test
    @Transactional
    public void testApprovalRecordCrud() {
        String workflowType = "TSTREC" + System.currentTimeMillis();
        ApprovalWorkflow workflow = new ApprovalWorkflow();
        workflow.setWorkflowName("记录测试流程");
        workflow.setWorkflowType(workflowType);
        workflow.setStatus(1);
        approvalWorkflowService.save(workflow);
        Long workflowId = workflow.getId();

        Role role = new Role();
        role.setRoleName("TSTRECROLE" + System.currentTimeMillis());
        role.setRoleStatus(1);
        roleService.save(role);

        // 建节点
        ApprovalNode node = new ApprovalNode();
        node.setWorkflowId(workflowId);
        node.setNodeName("审批节点");
        node.setNodeOrder(1);
        node.setRoleId(role.getRoleId());
        approvalNodeService.save(node);

        // 建审批实例
        ApprovalInstance instance = new ApprovalInstance();
        instance.setWorkflowId(workflowId);
        instance.setRelatedId(100001L);
        instance.setRelatedType("采购计划");
        instance.setCurrentNodeId(node.getId());
        instance.setInitiatorId(1L);
        instance.setStatus("待审批");
        assertTrue(approvalInstanceService.save(instance), "新增审批实例应成功");
        Long instanceId = instance.getId();

        // 建审批记录
        ApprovalRecord record = new ApprovalRecord();
        record.setInstanceId(instanceId);
        record.setNodeId(node.getId());
        record.setApproverId(1L);
        record.setAction("同意");
        record.setComment("同意审批");
        assertTrue(approvalRecordService.save(record), "新增审批记录应成功");
        Long recordId = record.getId();

        // 按实例查询记录
        List<ApprovalRecord> byInstance = approvalRecordService.getRecordsByInstanceId(instanceId);
        assertEquals(1, byInstance.size(), "按实例应查到1条审批记录");
        assertEquals("同意", byInstance.get(0).getAction(), "审批动作应一致");

        // 按节点查询记录
        List<ApprovalRecord> byNode = approvalRecordService.getRecordsByNodeId(node.getId());
        assertTrue(byNode.stream().anyMatch(r -> r.getId().equals(recordId)), "按节点应查到审批记录");

        // 实例状态查询（按实例ID查询工作流名称）
        assertNotNull(approvalInstanceService.getById(instanceId), "审批实例应存在");
    }

    // endregion
}