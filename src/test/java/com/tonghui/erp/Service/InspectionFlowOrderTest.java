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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 检验任务流程顺序测试（事务回滚，不留下测试数据）
 * <p>
 * 新流程：请检 → 检验计划 → 取样 → 检验记录 → 留样 → 销毁（非必须）。
 * 留样完成后固定进入销毁，不再根据 destructionRequired 自动跳过并完成任务；
 * 销毁不要求时需显式跳过，或通过 nextStep 指定直接完成。
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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void retentionShouldAdvanceToDestructionEvenWhenNotRequired() {
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
        Assertions.assertEquals("DESTRUCTION", task.getCurrentStep(),
                "留样后应进入销毁，不能自动跳过并完成任务");
        Assertions.assertEquals("进行中", task.getTaskStatus(), "留样后任务状态应为进行中");

        inspectionTaskService.skipStep(id, skipStep("DESTRUCTION", "该任务不要求销毁"));
        task = inspectionTaskService.getTaskDetail(id);
        Assertions.assertEquals("DONE", task.getCurrentStep(), "跳过销毁后任务应完成");
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
     * 留样不要求时，检验记录完成后仍应停留在留样，不能自动置为已完成
     */
    @Test
    public void inspectionShouldStayAtRetentionWhenRetentionNotRequired() {
        WorkOrder workOrder = prepareFreeWorkOrder("中间产品");
        Assumptions.assumeTrue(workOrder != null, "测试库没有可用的空闲工单，跳过留样回归测试");

        InspectionTaskDtos.CreateTaskRequest create = new InspectionTaskDtos.CreateTaskRequest();
        create.setWorkOrderId(workOrder.getWorkOrderId());
        create.setItemCategory("中间产品");
        create.setRetentionRequired(0);
        create.setDestructionRequired(0);

        InspectionTaskDtos.TaskDetail task = inspectionTaskService.createTask(create);
        Long id = task.getId();
        inspectionTaskService.completeRequest(id, requestStep());
        inspectionTaskService.completePlan(id, planStep());
        inspectionTaskService.completeSampling(id, samplingStep());
        inspectionTaskService.completeInspection(id, inspectionStep());

        task = inspectionTaskService.getTaskDetail(id);
        Assertions.assertEquals("RETENTION", task.getCurrentStep(),
                "留样不要求时，检验记录后仍应停留在留样");
        Assertions.assertEquals("进行中", task.getTaskStatus(), "任务状态不应变成已完成");
        Assertions.assertNull(task.getCompleteTime(), "任务不应写入完成时间");
        InspectionTaskDtos.StepDetail retention = task.getSteps().stream()
                .filter(step -> "RETENTION".equals(step.getStepCode()))
                .findFirst()
                .orElseThrow();
        Assertions.assertEquals("进行中", retention.getStepStatus(), "留样环节应为进行中");
    }

    /**
     * 前端通过 nextStep 指定直接完成时，未办理环节应记为已跳过
     */
    @Test
    public void nextStepDoneShouldFinishTaskAndSkipPendingSteps() {
        WorkOrder workOrder = findFreeWorkOrder();
        Assumptions.assumeTrue(workOrder != null, "测试库没有可用的空闲工单，跳过 nextStep 完成测试");

        InspectionTaskDtos.CreateTaskRequest create = new InspectionTaskDtos.CreateTaskRequest();
        create.setWorkOrderId(workOrder.getWorkOrderId());
        create.setItemCategory("成品");
        create.setRetentionRequired(1);
        create.setDestructionRequired(0);

        InspectionTaskDtos.TaskDetail task = inspectionTaskService.createTask(create);
        Long id = task.getId();
        inspectionTaskService.completeRequest(id, requestStep());
        inspectionTaskService.completePlan(id, planStep());
        inspectionTaskService.completeSampling(id, samplingStep());
        inspectionTaskService.completeInspection(id, inspectionStep());

        InspectionTaskDtos.RetentionStepRequest retention = retentionStep();
        retention.setNextStep("DONE");
        inspectionTaskService.completeRetention(id, retention);

        task = inspectionTaskService.getTaskDetail(id);
        Assertions.assertEquals("DONE", task.getCurrentStep(), "nextStep=DONE 时任务应直接完成");
        Assertions.assertEquals("已完成", task.getTaskStatus(), "任务状态应为已完成");
        Assertions.assertNotNull(task.getCompleteTime(), "任务应写入完成时间");
        InspectionTaskDtos.StepDetail destruction = task.getSteps().stream()
                .filter(step -> "DESTRUCTION".equals(step.getStepCode()))
                .findFirst()
                .orElseThrow();
        Assertions.assertEquals("已跳过", destruction.getStepStatus(), "未办理的销毁环节应记为已跳过");
    }

    /**
     * 前端指定跳过非必办留样直接进入销毁时，留样环节应记为已跳过
     */
    @Test
    public void nextStepDestructionShouldSkipOptionalRetention() {
        WorkOrder workOrder = prepareFreeWorkOrder("中间产品");
        Assumptions.assumeTrue(workOrder != null, "测试库没有可用的空闲工单，跳过跨环节测试");

        InspectionTaskDtos.CreateTaskRequest create = new InspectionTaskDtos.CreateTaskRequest();
        create.setWorkOrderId(workOrder.getWorkOrderId());
        create.setItemCategory("中间产品");
        create.setRetentionRequired(0);
        create.setDestructionRequired(1);

        InspectionTaskDtos.TaskDetail task = inspectionTaskService.createTask(create);
        Long id = task.getId();
        inspectionTaskService.completeRequest(id, requestStep());
        inspectionTaskService.completePlan(id, planStep());
        inspectionTaskService.completeSampling(id, samplingStep());

        InspectionTaskDtos.InspectionStepRequest inspection = inspectionStep();
        inspection.setNextStep("DESTRUCTION");
        inspectionTaskService.completeInspection(id, inspection);

        task = inspectionTaskService.getTaskDetail(id);
        Assertions.assertEquals("DESTRUCTION", task.getCurrentStep(), "应直接进入销毁环节");
        InspectionTaskDtos.StepDetail retention = task.getSteps().stream()
                .filter(step -> "RETENTION".equals(step.getStepCode()))
                .findFirst()
                .orElseThrow();
        Assertions.assertEquals("已跳过", retention.getStepStatus(), "非必办留样应记为已跳过");
        InspectionTaskDtos.StepDetail destruction = task.getSteps().stream()
                .filter(step -> "DESTRUCTION".equals(step.getStepCode()))
                .findFirst()
                .orElseThrow();
        Assertions.assertEquals("进行中", destruction.getStepStatus(), "销毁环节应为进行中");
    }

    /**
     * 留样被跳过后，仍可办理销毁（销毁记录不依赖留样记录）
     */
    @Test
    public void destructionShouldWorkAfterRetentionSkipped() {
        WorkOrder workOrder = prepareFreeWorkOrder("中间产品");
        Assumptions.assumeTrue(workOrder != null, "测试库没有可用的空闲工单，跳过留样后销毁测试");

        InspectionTaskDtos.CreateTaskRequest create = new InspectionTaskDtos.CreateTaskRequest();
        create.setWorkOrderId(workOrder.getWorkOrderId());
        create.setItemCategory("中间产品");
        create.setRetentionRequired(0);
        create.setDestructionRequired(1);

        InspectionTaskDtos.TaskDetail task = inspectionTaskService.createTask(create);
        Long id = task.getId();
        inspectionTaskService.completeRequest(id, requestStep());
        inspectionTaskService.completePlan(id, planStep());
        inspectionTaskService.completeSampling(id, samplingStep());
        inspectionTaskService.completeInspection(id, inspectionStep());

        inspectionTaskService.skipStep(id, skipStep("RETENTION", "该任务不要求留样"));
        task = inspectionTaskService.getTaskDetail(id);
        Assertions.assertEquals("DESTRUCTION", task.getCurrentStep(), "跳过留样后应进入销毁");

        inspectionTaskService.completeDestruction(id, destructionStep());
        task = inspectionTaskService.getTaskDetail(id);
        Assertions.assertEquals("DONE", task.getCurrentStep(), "销毁后任务应完成");
        Assertions.assertEquals("已完成", task.getTaskStatus(), "任务状态应为已完成");
    }

    /**
     * nextStep 与当前环节相同时，任务停留在当前环节，步骤保持已完成
     */
    @Test
    public void nextStepSameAsCurrentShouldKeepTaskAtCurrentStep() {
        WorkOrder workOrder = findFreeWorkOrder();
        Assumptions.assumeTrue(workOrder != null, "测试库没有可用的空闲工单，跳过停留环节测试");

        InspectionTaskDtos.CreateTaskRequest create = new InspectionTaskDtos.CreateTaskRequest();
        create.setWorkOrderId(workOrder.getWorkOrderId());
        create.setItemCategory("成品");
        create.setRetentionRequired(1);
        create.setDestructionRequired(0);

        InspectionTaskDtos.TaskDetail task = inspectionTaskService.createTask(create);
        Long id = task.getId();
        inspectionTaskService.completeRequest(id, requestStep());
        inspectionTaskService.completePlan(id, planStep());
        inspectionTaskService.completeSampling(id, samplingStep());

        InspectionTaskDtos.InspectionStepRequest inspection = inspectionStep();
        inspection.setNextStep("INSPECTION");
        inspectionTaskService.completeInspection(id, inspection);

        task = inspectionTaskService.getTaskDetail(id);
        Assertions.assertEquals("INSPECTION", task.getCurrentStep(), "nextStep 与当前环节相同时应停留在原环节");
        Assertions.assertEquals("进行中", task.getTaskStatus(), "任务状态应为进行中");
        InspectionTaskDtos.StepDetail inspectionStep = task.getSteps().stream()
                .filter(step -> "INSPECTION".equals(step.getStepCode()))
                .findFirst()
                .orElseThrow();
        Assertions.assertEquals("已完成", inspectionStep.getStepStatus(), "当前环节步骤应保持已完成");
    }

    /**
     * 回退到已完成环节应被拒绝
     */
    @Test
    public void backwardNextStepShouldBeRejected() {
        WorkOrder workOrder = findFreeWorkOrder();
        Assumptions.assumeTrue(workOrder != null, "测试库没有可用的空闲工单，跳过回退校验测试");

        InspectionTaskDtos.CreateTaskRequest create = new InspectionTaskDtos.CreateTaskRequest();
        create.setWorkOrderId(workOrder.getWorkOrderId());
        create.setItemCategory("成品");
        create.setRetentionRequired(1);
        create.setDestructionRequired(0);

        InspectionTaskDtos.TaskDetail task = inspectionTaskService.createTask(create);
        Long id = task.getId();
        inspectionTaskService.completeRequest(id, requestStep());

        InspectionTaskDtos.PlanStepRequest plan = planStep();
        plan.setNextStep("REQUEST");
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> inspectionTaskService.completePlan(id, plan));
        Assertions.assertTrue(ex.getMessage().contains("不能回退"), "应提示不能回退环节");
    }

    /**
     * 非法目标环节应被拒绝
     */
    @Test
    public void invalidNextStepShouldBeRejected() {
        WorkOrder workOrder = findFreeWorkOrder();
        Assumptions.assumeTrue(workOrder != null, "测试库没有可用的空闲工单，跳过非法环节测试");

        InspectionTaskDtos.CreateTaskRequest create = new InspectionTaskDtos.CreateTaskRequest();
        create.setWorkOrderId(workOrder.getWorkOrderId());
        create.setItemCategory("成品");
        create.setRetentionRequired(1);
        create.setDestructionRequired(0);

        InspectionTaskDtos.TaskDetail task = inspectionTaskService.createTask(create);
        Long id = task.getId();
        inspectionTaskService.completeRequest(id, requestStep());

        InspectionTaskDtos.PlanStepRequest plan = planStep();
        plan.setNextStep("UNKNOWN");
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> inspectionTaskService.completePlan(id, plan));
        Assertions.assertTrue(ex.getMessage().contains("不合法"), "应提示目标环节不合法");
    }

    @Test
    public void planTypeShouldFollowItemCategory() {
        WorkOrder workOrder = findFreeWorkOrder();
        Assumptions.assumeTrue(workOrder != null, "测试库没有可用的空闲工单，跳过检验类型推导测试");

        InspectionTaskDtos.CreateTaskRequest create = new InspectionTaskDtos.CreateTaskRequest();
        create.setWorkOrderId(workOrder.getWorkOrderId());
        create.setItemCategory("成品");
        create.setRetentionRequired(0);
        create.setDestructionRequired(0);

        InspectionTaskDtos.TaskDetail task = inspectionTaskService.createTask(create);
        Long id = task.getId();
        inspectionTaskService.completeRequest(id, requestStep());

        // 模拟前端传错检验类型，后端应按被检品属性强制修正
        InspectionTaskDtos.PlanStepRequest plan = planStep();
        plan.setInspectionType("生产原料检验");
        inspectionTaskService.completePlan(id, plan);

        InspectionTaskDtos.TaskDetail detail = inspectionTaskService.getTaskDetail(id);
        InspectionTaskDtos.StepDetail planStep = detail.getSteps().stream()
                .filter(step -> "PLAN".equals(step.getStepCode()))
                .findFirst()
                .orElseThrow();
        String type = null;
        String category = null;
        for (InspectionTaskDtos.FieldItem field : planStep.getFields()) {
            if ("inspectionType".equals(field.getKey())) {
                type = field.getValue();
            }
            if ("itemCategory".equals(field.getKey())) {
                category = field.getValue();
            }
        }
        Assertions.assertEquals("成品", category, "检验计划应展示任务被检品属性");
        Assertions.assertEquals("成品检验", type, "检验类型应按被检品属性自动推导");
    }

    /**
     * 优先找没有指定被检品属性任务的工单，避免建任务时命中已有任务
     */
    private WorkOrder findFreeWorkOrder() {
        return findFreeWorkOrder("成品");
    }

    private WorkOrder findFreeWorkOrder(String itemCategory) {
        List<WorkOrder> orders = workOrderMapper.selectList(new QueryWrapper<WorkOrder>().last("LIMIT 100"));
        for (WorkOrder order : orders) {
            String batchNo = order.getBatchNumber() == null ? "" : order.getBatchNumber();
            Long count = inspectionTaskMapper.selectCount(new QueryWrapper<InspectionTask>()
                    .eq("work_order_id", order.getWorkOrderId())
                    .eq("item_category", itemCategory)
                    .eq("batch_no", batchNo));
            if (count == null || count == 0) {
                return order;
            }
        }
        return null;
    }

    /**
     * 准备一个不会被 createTask 复用的工单
     * <p>
     * 优先找没有同品类任务的工单；测试库中所有工单都已有任务时，
     * 临时把未关联请检单的任务置为已归档，保证 createTask 新建任务（事务回滚）。
     * </p>
     */
    private WorkOrder prepareFreeWorkOrder(String itemCategory) {
        List<WorkOrder> orders = workOrderMapper.selectList(new QueryWrapper<WorkOrder>().last("LIMIT 200"));
        for (WorkOrder order : orders) {
            String batchNo = order.getBatchNumber() == null ? "" : order.getBatchNumber();
            Long count = inspectionTaskMapper.selectCount(new QueryWrapper<InspectionTask>()
                    .eq("work_order_id", order.getWorkOrderId())
                    .eq("item_category", itemCategory)
                    .eq("batch_no", batchNo)
                    .isNull("request_ref_id")
                    .notIn("task_status", List.of("已作废", "已归档")));
            if (count == null || count == 0) {
                return order;
            }
        }
        if (orders.isEmpty()) {
            return null;
        }
        WorkOrder fallback = orders.get(0);
        String batchNo = fallback.getBatchNumber() == null ? "" : fallback.getBatchNumber();
        jdbcTemplate.update("UPDATE inspection_task SET task_status = '已归档' "
                        + "WHERE work_order_id = ? AND item_category = ? AND batch_no = ? "
                        + "AND request_ref_id IS NULL AND is_deleted = 0 "
                        + "AND task_status NOT IN ('已作废', '已归档')",
                fallback.getWorkOrderId(), itemCategory, batchNo);
        return fallback;
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

    private InspectionTaskDtos.SkipStepRequest skipStep(String stepCode, String reason) {
        InspectionTaskDtos.SkipStepRequest request = new InspectionTaskDtos.SkipStepRequest();
        request.setStepCode(stepCode);
        request.setReason(reason);
        return request;
    }
}
