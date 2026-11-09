package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tonghui.erp.Common.Dto.QualityPages.InspectionTaskDtos;
import com.tonghui.erp.Data.Entity.InspectionTask;
import com.tonghui.erp.Data.Entity.WorkOrder;
import com.tonghui.erp.Data.mapper.InspectionTaskMapper;
import com.tonghui.erp.Data.mapper.WorkOrderMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 检验任务流程顺序测试（事务回滚，不留下测试数据）
 * <p>
 * 新流程：请检 → 检验计划 → 取样 → 检验记录 → 留样 → 销毁（非必须）。
 * 销毁不要求时，完成留样后直接完成任务。
 * </p>
 */
@SpringBootTest
@Transactional
public class InspectionFlowOrderTest {

    @Autowired
    private InspectionTaskService inspectionTaskService;

    @Autowired
    private WorkOrderMapper workOrderMapper;

    @Autowired
    private InspectionTaskMapper inspectionTaskMapper;

    @Test
    public void newFlowShouldFinishAfterRetentionWhenDestructionNotRequired() {
        WorkOrder workOrder = findFreeWorkOrder();
        Assumptions.assumeTrue(workOrder != null, "测试库没有可用的空闲工单，跳过流程顺序测试");

        InspectionTaskDtos.CreateTaskRequest create = new InspectionTaskDtos.CreateTaskRequest();
        create.setWorkOrderId(workOrder.getWorkOrderId());
        create.setItemCategory("成品");
        create.setRetentionRequired(1);
        create.setDestructionRequired(0);

        InspectionTaskDtos.TaskDetail task = inspectionTaskService.createTask(create);
        Long id = task.getId();
        Assertions.assertEquals("REQUEST", task.getCurrentStep(), "任务应从请检开始");

        inspectionTaskService.completeRequest(id, requestStep());
        task = inspectionTaskService.getTaskDetail(id);
        Assertions.assertEquals("PLAN", task.getCurrentStep(), "请检后应进入检验计划");

        inspectionTaskService.completePlan(id, planStep());
        task = inspectionTaskService.getTaskDetail(id);
        Assertions.assertEquals("SAMPLING", task.getCurrentStep(), "检验计划后应进入取样");

        inspectionTaskService.completeSampling(id, samplingStep());
        task = inspectionTaskService.getTaskDetail(id);
        Assertions.assertEquals("INSPECTION", task.getCurrentStep(), "取样后应进入检验记录");

        inspectionTaskService.completeInspection(id, inspectionStep());
        task = inspectionTaskService.getTaskDetail(id);
        Assertions.assertEquals("RETENTION", task.getCurrentStep(), "检验记录后应进入留样");

        inspectionTaskService.completeRetention(id, retentionStep());
        task = inspectionTaskService.getTaskDetail(id);
        Assertions.assertEquals("DONE", task.getCurrentStep(), "销毁非必须时，留样后应完成任务");
        Assertions.assertEquals("已完成", task.getTaskStatus(), "任务状态应为已完成");
    }

    @Test
    public void destructionShouldBeTheLastOptionalStepWhenRequired() {
        WorkOrder workOrder = findFreeWorkOrder();
        Assumptions.assumeTrue(workOrder != null, "测试库没有可用的空闲工单，跳过销毁流程测试");

        InspectionTaskDtos.CreateTaskRequest create = new InspectionTaskDtos.CreateTaskRequest();
        create.setWorkOrderId(workOrder.getWorkOrderId());
        create.setItemCategory("成品");
        create.setRetentionRequired(1);
        create.setDestructionRequired(1);

        InspectionTaskDtos.TaskDetail task = inspectionTaskService.createTask(create);
        Long id = task.getId();
        inspectionTaskService.completeRequest(id, requestStep());
        inspectionTaskService.completePlan(id, planStep());
        inspectionTaskService.completeSampling(id, samplingStep());
        inspectionTaskService.completeInspection(id, inspectionStep());
        inspectionTaskService.completeRetention(id, retentionStep());

        task = inspectionTaskService.getTaskDetail(id);
        Assertions.assertEquals("DESTRUCTION", task.getCurrentStep(), "要求销毁时，留样后应进入销毁");

        inspectionTaskService.completeDestruction(id, destructionStep());
        task = inspectionTaskService.getTaskDetail(id);
        Assertions.assertEquals("DONE", task.getCurrentStep(), "销毁后任务应完成");
        Assertions.assertEquals("已完成", task.getTaskStatus(), "任务状态应为已完成");
    }

    /**
     * 优先找没有成品检验任务的工单，避免唯一键冲突
     */
    private WorkOrder findFreeWorkOrder() {
        List<WorkOrder> orders = workOrderMapper.selectList(new QueryWrapper<WorkOrder>().last("LIMIT 100"));
        for (WorkOrder order : orders) {
            String batchNo = order.getBatchNumber() == null ? "" : order.getBatchNumber();
            Long count = inspectionTaskMapper.selectCount(new QueryWrapper<InspectionTask>()
                    .eq("work_order_id", order.getWorkOrderId())
                    .eq("item_category", "成品")
                    .eq("batch_no", batchNo));
            if (count == null || count == 0) {
                return order;
            }
        }
        return null;
    }

    private InspectionTaskDtos.RequestStepRequest requestStep() {
        InspectionTaskDtos.RequestStepRequest request = new InspectionTaskDtos.RequestStepRequest();
        request.setRequester("流程测试");
        request.setRequestDepartment("质量部");
        request.setProcessName("成品检验");
        request.setInspectionItems("性状、鉴别");
        request.setRequestTime(LocalDateTime.now());
        return request;
    }

    private InspectionTaskDtos.PlanStepRequest planStep() {
        InspectionTaskDtos.PlanStepRequest request = new InspectionTaskDtos.PlanStepRequest();
        request.setInspectionType("成品检验");
        request.setInspectionSummary("性状、鉴别");
        request.setPlanTime(LocalDate.now());
        return request;
    }

    private InspectionTaskDtos.SamplingStepRequest samplingStep() {
        InspectionTaskDtos.SamplingStepRequest request = new InspectionTaskDtos.SamplingStepRequest();
        request.setSampler("流程测试");
        request.setSamplingTime(LocalDateTime.now());
        request.setQuantity("10g×3份");
        return request;
    }

    private InspectionTaskDtos.InspectionStepRequest inspectionStep() {
        InspectionTaskDtos.InspectionStepRequest request = new InspectionTaskDtos.InspectionStepRequest();
        request.setInspector("流程测试");
        request.setReviewer("流程测试");
        request.setStartTime(LocalDateTime.now());
        request.setEndTime(LocalDateTime.now());
        request.setConclusion("合格");
        request.setInspectionItem("性状、鉴别");
        return request;
    }

    private InspectionTaskDtos.DestructionStepRequest destructionStep() {
        InspectionTaskDtos.DestructionStepRequest request = new InspectionTaskDtos.DestructionStepRequest();
        request.setDestroyReason("流程测试");
        request.setDestroyMethod("委托销毁");
        request.setDestroyLocation("销毁区");
        request.setApplicantName("流程测试");
        request.setApplyDate(LocalDate.now());
        request.setApproverName("流程测试");
        request.setApproveDate(LocalDate.now());
        request.setDestroyerName("流程测试");
        request.setDestroyDate(LocalDate.now());
        request.setSupervisorName("流程测试");
        request.setSuperviseDate(LocalDate.now());
        return request;
    }

    private InspectionTaskDtos.RetentionStepRequest retentionStep() {
        InspectionTaskDtos.RetentionStepRequest request = new InspectionTaskDtos.RetentionStepRequest();
        request.setQuantity("10g×3份");
        request.setRetainedDate(LocalDate.now());
        request.setStorageLocation("流程测试留样位");
        return request;
    }
}
