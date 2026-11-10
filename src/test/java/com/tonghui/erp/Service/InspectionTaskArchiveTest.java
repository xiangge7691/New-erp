package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tonghui.erp.Common.Dto.PagedResult;
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
 * 检验任务归档状态测试（事务回滚，不留下测试数据）
 * <p>
 * 验证：待开始/进行中任务可归档；归档后只读、默认查询隐藏、看板不计入；
 * 已完成/已作废任务不可归档；作废接口可正常返回已作废详情。
 * </p>
 */
@SpringBootTest
@Transactional
public class InspectionTaskArchiveTest {

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    @Autowired
    private InspectionTaskService inspectionTaskService;

    @Autowired
    private WorkOrderMapper workOrderMapper;

    @Autowired
    private InspectionTaskMapper inspectionTaskMapper;

    // endregion

    // region 归档场景
    // ===================================
    // 归档场景
    // ===================================

    /**
     * 待开始任务归档：写归档元数据、默认视图隐藏、按状态可查、看板不计入、不可再办理
     */
    @Test
    public void waitingTaskShouldArchiveAndDisappearFromDefaultViews() {
        WorkOrder workOrder = findWorkOrderWithoutFinishedTask();
        Assumptions.assumeTrue(workOrder != null, "测试库没有可用工单，跳过归档测试");

        InspectionTaskDtos.BoardStats before = inspectionTaskService.queryBoardStats(
                null, null, null, null, null);
        InspectionTaskDtos.TaskDetail task = createTask(workOrder);
        Long id = task.getId();
        Assertions.assertEquals("待开始", task.getTaskStatus(), "新建任务状态应为待开始");

        InspectionTaskDtos.TaskDetail archived = inspectionTaskService.archiveTask(id);
        Assertions.assertEquals("已归档", archived.getTaskStatus(), "任务状态应为已归档");
        Assertions.assertNotNull(archived.getArchiveTime(), "归档时间不应为空");
        Assertions.assertNotNull(archived.getArchiveBy(), "归档人不应为空");

        // 详情可查看
        Assertions.assertEquals("已归档", inspectionTaskService.getTaskDetail(id).getTaskStatus(),
                "已归档任务详情应可查看");

        // 默认任务列表不返回，按状态筛选可查
        PagedResult<InspectionTaskDtos.TaskSummary> defaultList = inspectionTaskService.queryTasks(
                null, null, null, null, null, task.getTaskCode(), 0, 10);
        Assertions.assertEquals(0, defaultList.getItems().size(), "默认列表不应返回已归档任务");
        PagedResult<InspectionTaskDtos.TaskSummary> archivedList = inspectionTaskService.queryTasks(
                null, null, null, null, "已归档", task.getTaskCode(), 0, 10);
        Assertions.assertEquals(1, archivedList.getItems().size(), "按已归档状态应能查到任务");

        // 批次列表和批次详情不包含归档任务
        PagedResult<InspectionTaskDtos.BatchSummary> batches = inspectionTaskService.queryBatches(
                null, null, null, null, null, null, 0, 200);
        boolean inBatchList = batches.getItems().stream()
                .flatMap(batch -> batch.getTasks().stream())
                .anyMatch(item -> id.equals(item.getId()));
        Assertions.assertFalse(inBatchList, "批次列表不应包含已归档任务");
        try {
            InspectionTaskDtos.BatchSummary batch = inspectionTaskService.queryBatch(workOrder.getWorkOrderId());
            Assertions.assertFalse(batch.getTasks().stream().anyMatch(item -> id.equals(item.getId())),
                    "批次详情不应包含已归档任务");
        } catch (IllegalArgumentException e) {
            Assertions.assertTrue(e.getMessage().contains("检验批次不存在"),
                    "批次下任务全部归档时应视为批次不存在");
        }

        // 看板不计入归档任务
        InspectionTaskDtos.BoardStats after = inspectionTaskService.queryBoardStats(
                null, null, null, null, null);
        Assertions.assertEquals(before.getTaskTotal(), after.getTaskTotal(),
                "看板任务总数不应计入已归档任务");
        Assertions.assertEquals(before.getTodoTasks(), after.getTodoTasks(),
                "看板待办数不应计入已归档任务");

        // 归档后不可办理
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> inspectionTaskService.completePlan(id, planStep()));
        Assertions.assertTrue(ex.getMessage().contains("已归档"), "办理已归档任务应被拒绝");
    }

    /**
     * 进行中任务归档：办理过一个环节后仍可归档，归档后办理被拒绝
     */
    @Test
    public void runningTaskShouldArchive() {
        WorkOrder workOrder = findWorkOrderWithoutFinishedTask();
        Assumptions.assumeTrue(workOrder != null, "测试库没有可用工单，跳过归档测试");

        InspectionTaskDtos.TaskDetail task = createTask(workOrder);
        Long id = task.getId();
        inspectionTaskService.completeRequest(id, requestStep());
        Assertions.assertEquals("进行中", inspectionTaskService.getTaskDetail(id).getTaskStatus(),
                "办理请检后状态应为进行中");

        InspectionTaskDtos.TaskDetail archived = inspectionTaskService.archiveTask(id);
        Assertions.assertEquals("已归档", archived.getTaskStatus(), "进行中任务应可归档");
        Assertions.assertNotNull(archived.getArchiveTime(), "归档时间不应为空");

        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> inspectionTaskService.completePlan(id, planStep()));
        Assertions.assertTrue(ex.getMessage().contains("已归档"), "归档后办理应被拒绝");
    }

    /**
     * 已完成任务不允许归档
     */
    @Test
    public void finishedTaskShouldNotArchive() {
        WorkOrder workOrder = findWorkOrderWithoutFinishedTask();
        Assumptions.assumeTrue(workOrder != null, "测试库没有可用工单，跳过归档测试");

        InspectionTaskDtos.TaskDetail task = createTask(workOrder);
        Long id = task.getId();
        finishTask(id);
        Assertions.assertEquals("已完成", inspectionTaskService.getTaskDetail(id).getTaskStatus(),
                "完整流程后任务状态应为已完成");

        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> inspectionTaskService.archiveTask(id));
        Assertions.assertTrue(ex.getMessage().contains("已完成"), "已完成任务不应允许归档");
    }

    /**
     * 已作废任务不允许归档；作废接口应正常返回详情（回归作废回滚缺陷）
     */
    @Test
    public void cancelledTaskShouldNotArchiveAndDetailShouldBeViewable() {
        WorkOrder workOrder = findWorkOrderWithoutFinishedTask();
        Assumptions.assumeTrue(workOrder != null, "测试库没有可用工单，跳过归档测试");

        InspectionTaskDtos.TaskDetail task = createTask(workOrder);
        Long id = task.getId();
        InspectionTaskDtos.CancelRequest cancel = new InspectionTaskDtos.CancelRequest();
        cancel.setReason("归档测试作废");

        InspectionTaskDtos.TaskDetail cancelled = inspectionTaskService.cancelTask(id, cancel);
        Assertions.assertEquals("已作废", cancelled.getTaskStatus(), "作废接口应正常返回已作废详情");
        InspectionTaskDtos.TaskDetail detail = inspectionTaskService.getTaskDetail(id);
        Assertions.assertEquals("已作废", detail.getTaskStatus(), "已作废任务详情应可查看");
        Assertions.assertEquals("归档测试作废", detail.getRemark(), "作废原因应写入备注");

        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> inspectionTaskService.archiveTask(id));
        Assertions.assertTrue(ex.getMessage().contains("已作废"), "已作废任务不应允许归档");
    }

    /**
     * 新建任务不复用已归档任务
     */
    @Test
    public void createTaskShouldNotReuseArchivedTask() {
        WorkOrder workOrder = findWorkOrderWithoutFinishedTask();
        Assumptions.assumeTrue(workOrder != null, "测试库没有可用工单，跳过归档测试");

        InspectionTaskDtos.TaskDetail first = createTask(workOrder);
        inspectionTaskService.archiveTask(first.getId());

        InspectionTaskDtos.TaskDetail second = createTask(workOrder);
        Assertions.assertNotEquals(first.getId(), second.getId(), "创建任务不应复用已归档任务");
        Assertions.assertEquals("待开始", second.getTaskStatus(), "新任务状态应为待开始");
    }

    // endregion

    // region 测试辅助
    // ===================================
    // 测试辅助
    // ===================================

    /**
     * 优先找没有成品检验任务的工单，避免建任务时命中已有任务
     */
    private WorkOrder findWorkOrderWithoutFinishedTask() {
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

    private InspectionTaskDtos.TaskDetail createTask(WorkOrder workOrder) {
        InspectionTaskDtos.CreateTaskRequest create = new InspectionTaskDtos.CreateTaskRequest();
        create.setWorkOrderId(workOrder.getWorkOrderId());
        create.setItemCategory("成品");
        create.setRetentionRequired(1);
        create.setDestructionRequired(0);
        return inspectionTaskService.createTask(create);
    }

    private void finishTask(Long id) {
        inspectionTaskService.completeRequest(id, requestStep());
        inspectionTaskService.completePlan(id, planStep());
        inspectionTaskService.completeSampling(id, samplingStep());
        inspectionTaskService.completeInspection(id, inspectionStep());
        inspectionTaskService.completeRetention(id, retentionStep());
        // 留样完成后固定进入销毁，销毁不要求时需显式跳过
        InspectionTaskDtos.SkipStepRequest skip = new InspectionTaskDtos.SkipStepRequest();
        skip.setStepCode("DESTRUCTION");
        skip.setReason("归档测试不要求销毁");
        inspectionTaskService.skipStep(id, skip);
    }

    private InspectionTaskDtos.RequestStepRequest requestStep() {
        InspectionTaskDtos.RequestStepRequest request = new InspectionTaskDtos.RequestStepRequest();
        request.setRequester("归档测试");
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
        request.setSampler("归档测试");
        request.setSamplingTime(LocalDateTime.now());
        request.setQuantity("10g×3份");
        return request;
    }

    private InspectionTaskDtos.InspectionStepRequest inspectionStep() {
        InspectionTaskDtos.InspectionStepRequest request = new InspectionTaskDtos.InspectionStepRequest();
        request.setInspector("归档测试");
        request.setReviewer("归档测试");
        request.setStartTime(LocalDateTime.now());
        request.setEndTime(LocalDateTime.now());
        request.setConclusion("合格");
        request.setInspectionItem("性状、鉴别");
        return request;
    }

    private InspectionTaskDtos.RetentionStepRequest retentionStep() {
        InspectionTaskDtos.RetentionStepRequest request = new InspectionTaskDtos.RetentionStepRequest();
        request.setQuantity("10g×3份");
        request.setRetainedDate(LocalDate.now());
        request.setStorageLocation("归档测试留样位");
        return request;
    }

    // endregion
}
