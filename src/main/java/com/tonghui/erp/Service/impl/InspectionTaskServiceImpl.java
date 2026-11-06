package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.Dto.BusinessFile.BusinessFileDto;
import com.tonghui.erp.Common.Dto.BusinessFile.BusinessFileSyncDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Quality.DestructionRecordDetailSaveDto;
import com.tonghui.erp.Common.Dto.Quality.DestructionRecordSaveDto;
import com.tonghui.erp.Common.Dto.QualityPages.InspectionTaskDtos;
import com.tonghui.erp.Common.utils.EntityUtils;
import com.tonghui.erp.Data.Entity.DestructionRecord;
import com.tonghui.erp.Data.Entity.InspectionPlan;
import com.tonghui.erp.Data.Entity.InspectionRecord;
import com.tonghui.erp.Data.Entity.InspectionRequest;
import com.tonghui.erp.Data.Entity.InspectionTask;
import com.tonghui.erp.Data.Entity.InspectionTaskStep;
import com.tonghui.erp.Data.Entity.ProductionPlan;
import com.tonghui.erp.Data.Entity.RetainedSample;
import com.tonghui.erp.Data.Entity.SamplingRecord;
import com.tonghui.erp.Data.Entity.WorkOrder;
import com.tonghui.erp.Data.mapper.InspectionTaskMapper;
import com.tonghui.erp.Data.mapper.InspectionTaskStepMapper;
import com.tonghui.erp.Data.mapper.ProductionPlanMapper;
import com.tonghui.erp.Data.mapper.WorkOrderMapper;
import com.tonghui.erp.Service.BusinessFileService;
import com.tonghui.erp.Service.DestructionRecordService;
import com.tonghui.erp.Service.InspectionPlanService;
import com.tonghui.erp.Service.InspectionRecordService;
import com.tonghui.erp.Service.InspectionRequestService;
import com.tonghui.erp.Service.InspectionTaskService;
import com.tonghui.erp.Service.PreparationService;
import com.tonghui.erp.Service.RetainedSampleService;
import com.tonghui.erp.Service.SamplingRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 检验任务服务实现
 * <p>
 * 六步流程：PLAN → REQUEST → SAMPLING → RETENTION → INSPECTION → DESTRUCTION → DONE。
 * 任务只保存流程索引，具体单据仍写入原有业务表，保证与现有检验页面兼容。
 * </p>
 */
@Service
public class InspectionTaskServiceImpl extends ServiceImpl<InspectionTaskMapper, InspectionTask>
        implements InspectionTaskService {

    // region 常量
    // ===================================
    // 常量
    // ===================================

    private static final String STEP_PLAN = "PLAN";
    private static final String STEP_REQUEST = "REQUEST";
    private static final String STEP_SAMPLING = "SAMPLING";
    private static final String STEP_RETENTION = "RETENTION";
    private static final String STEP_INSPECTION = "INSPECTION";
    private static final String STEP_DESTRUCTION = "DESTRUCTION";
    private static final String STEP_DONE = "DONE";

    private static final String STATUS_WAIT = "待开始";
    private static final String STATUS_RUN = "进行中";
    private static final String STATUS_DONE = "已完成";
    private static final String STATUS_CANCELLED = "已作废";

    private static final String STEP_STATUS_WAIT = "待办理";
    private static final String STEP_STATUS_RUN = "进行中";
    private static final String STEP_STATUS_DONE = "已完成";
    private static final String STEP_STATUS_SKIP = "已跳过";

    private static final List<String> STEP_ORDER = List.of(
            STEP_PLAN, STEP_REQUEST, STEP_SAMPLING, STEP_RETENTION, STEP_INSPECTION, STEP_DESTRUCTION
    );

    private static final Map<String, String> STEP_NAMES = Map.of(
            STEP_PLAN, "检验计划",
            STEP_REQUEST, "请检",
            STEP_SAMPLING, "取样",
            STEP_RETENTION, "留样",
            STEP_INSPECTION, "检验",
            STEP_DESTRUCTION, "销毁"
    );

    private static final Map<String, String> STEP_BIZ_TYPES = Map.of(
            STEP_PLAN, "INSPECTION_PLAN",
            STEP_REQUEST, "INSPECTION_REQUEST",
            STEP_SAMPLING, "SAMPLING_RECORD",
            STEP_RETENTION, "RETAINED_SAMPLE",
            STEP_INSPECTION, "INSPECTION_RECORD",
            STEP_DESTRUCTION, "DESTRUCTION_RECORD"
    );

    private static final Set<String> ITEM_CATEGORIES = Set.of("生产原料", "中间产品", "成品");

    // endregion

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    @Autowired
    private InspectionTaskStepMapper stepMapper;
    @Autowired
    private WorkOrderMapper workOrderMapper;
    @Autowired
    private ProductionPlanMapper productionPlanMapper;
    @Autowired
    private InspectionPlanService inspectionPlanService;
    @Autowired
    private InspectionRequestService inspectionRequestService;
    @Autowired
    private SamplingRecordService samplingRecordService;
    @Autowired
    private RetainedSampleService retainedSampleService;
    @Autowired
    private PreparationService preparationService;
    @Autowired
    private InspectionRecordService inspectionRecordService;
    @Autowired
    private DestructionRecordService destructionRecordService;
    @Autowired
    private BusinessFileService businessFileService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // endregion

    // region 查询
    // ===================================
    // 查询
    // ===================================

    @Override
    public PagedResult<InspectionTaskDtos.TaskSummary> queryTasks(String varietyCode, String batchNo,
                                                                  String itemCategory, String step,
                                                                  String status, String keyword,
                                                                  int pageIndex, int pageSize) {
        Page<InspectionTask> pageRequest = new Page<>(pageIndex + 1, pageSize);
        QueryWrapper<InspectionTask> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(varietyCode)) {
            wrapper.eq("preparation_code", varietyCode);
        }
        if (StringUtils.hasText(batchNo)) {
            wrapper.like("batch_no", batchNo);
        }
        if (StringUtils.hasText(itemCategory)) {
            wrapper.eq("item_category", itemCategory);
        }
        if (StringUtils.hasText(step)) {
            wrapper.eq("current_step", step);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("task_status", status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like("task_code", keyword)
                    .or().like("work_order_code", keyword)
                    .or().like("preparation_name", keyword)
                    .or().like("batch_no", keyword));
        }
        wrapper.orderByDesc("updated_time");
        Page<InspectionTask> result = page(pageRequest, wrapper);
        List<InspectionTaskDtos.TaskSummary> items = buildSummaries(result.getRecords());

        PagedResult<InspectionTaskDtos.TaskSummary> paged = new PagedResult<>();
        paged.setItems(items);
        paged.setTotalCount(result.getTotal());
        paged.setPageIndex(pageIndex);
        paged.setPageSize(pageSize);
        return paged;
    }

    private List<InspectionTaskDtos.TaskSummary> buildSummaries(List<InspectionTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, InspectionPlan> plans = loadMap(tasks.stream().map(InspectionTask::getPlanRefId),
                inspectionPlanService::listByIds, InspectionPlan::getId);
        Map<Long, InspectionRequest> requests = loadMap(tasks.stream().map(InspectionTask::getRequestRefId),
                inspectionRequestService::listByIds, InspectionRequest::getId);
        Map<Long, SamplingRecord> samplings = loadMap(tasks.stream().map(InspectionTask::getSamplingRefId),
                samplingRecordService::listByIds, SamplingRecord::getId);
        Map<Long, RetainedSample> retentions = loadMap(tasks.stream().map(InspectionTask::getRetentionRefId),
                retainedSampleService::listByIds, RetainedSample::getId);
        Map<Long, InspectionRecord> inspections = loadMap(tasks.stream().map(InspectionTask::getInspectionRefId),
                inspectionRecordService::listByIds, InspectionRecord::getId);
        Map<Long, DestructionRecord> destructions = loadMap(tasks.stream().map(InspectionTask::getDestructionRefId),
                destructionRecordService::listByIds, DestructionRecord::getId);

        return tasks.stream().map(t -> {
            InspectionTaskDtos.TaskSummary dto = new InspectionTaskDtos.TaskSummary();
            dto.setId(t.getId());
            dto.setTaskCode(t.getTaskCode());
            dto.setWorkOrderId(t.getWorkOrderId());
            dto.setWorkOrderCode(t.getWorkOrderCode());
            dto.setPlanNumber(t.getPlanNumber());
            dto.setPreparationCode(t.getPreparationCode());
            dto.setPreparationName(t.getPreparationName());
            dto.setBatchNo(t.getBatchNo());
            dto.setItemCategory(t.getItemCategory());
            dto.setObjectName(t.getObjectName());
            dto.setCurrentStep(t.getCurrentStep());
            dto.setTaskStatus(t.getTaskStatus());
            if (t.getPlanRefId() != null && plans.get(t.getPlanRefId()) != null) {
                dto.setPlanCode(plans.get(t.getPlanRefId()).getPlanCode());
            }
            if (t.getRequestRefId() != null && requests.get(t.getRequestRefId()) != null) {
                dto.setRequestCode(requests.get(t.getRequestRefId()).getInspectionCode());
            }
            if (t.getSamplingRefId() != null && samplings.get(t.getSamplingRefId()) != null) {
                dto.setSamplingCode(samplings.get(t.getSamplingRefId()).getSamplingCode());
            }
            if (t.getRetentionRefId() != null && retentions.get(t.getRetentionRefId()) != null) {
                dto.setRetentionCode(retentions.get(t.getRetentionRefId()).getRetainedCode());
                dto.setRetentionExpiryDate(retentions.get(t.getRetentionRefId()).getExpiryDate());
            }
            if (t.getInspectionRefId() != null && inspections.get(t.getInspectionRefId()) != null) {
                dto.setInspectionCode(inspections.get(t.getInspectionRefId()).getInspectionCode());
                dto.setInspectionConclusion(inspections.get(t.getInspectionRefId()).getConclusion());
            }
            if (t.getDestructionRefId() != null && destructions.get(t.getDestructionRefId()) != null) {
                dto.setDestructionCode(destructions.get(t.getDestructionRefId()).getDestructionCode());
            }
            return dto;
        }).collect(Collectors.toList());
    }

    private <T> Map<Long, T> loadMap(java.util.stream.Stream<Long> ids,
                                     Function<List<Long>, List<T>> loader,
                                     Function<T, Long> idGetter) {
        List<Long> idList = new ArrayList<>();
        ids.forEach(id -> {
            if (id != null && !idList.contains(id)) {
                idList.add(id);
            }
        });
        if (idList.isEmpty()) {
            return Collections.emptyMap();
        }
        List<T> list = loader.apply(idList);
        return list == null ? Collections.emptyMap()
                : list.stream().collect(Collectors.toMap(idGetter, Function.identity(), (a, b) -> a));
    }

    @Override
    public InspectionTaskDtos.TaskDetail getTaskDetail(Long id) {
        InspectionTask task = getTaskOrThrow(id);
        InspectionTaskDtos.TaskDetail detail = new InspectionTaskDtos.TaskDetail();
        detail.setId(task.getId());
        detail.setTaskCode(task.getTaskCode());
        detail.setWorkOrderId(task.getWorkOrderId());
        detail.setWorkOrderCode(task.getWorkOrderCode());
        detail.setPlanNumber(task.getPlanNumber());
        detail.setPreparationCode(task.getPreparationCode());
        detail.setPreparationName(task.getPreparationName());
        detail.setBatchNo(task.getBatchNo());
        detail.setItemCategory(task.getItemCategory());
        detail.setObjectName(task.getObjectName());
        detail.setSpec(task.getSpec());
        detail.setCurrentStep(task.getCurrentStep());
        detail.setTaskStatus(task.getTaskStatus());
        detail.setRetentionRequired(task.getRetentionRequired());
        detail.setDestructionRequired(task.getDestructionRequired());
        detail.setStartTime(task.getStartTime());
        detail.setCompleteTime(task.getCompleteTime());
        detail.setRemark(task.getRemark());

        Map<String, InspectionTaskStep> stepMap = listSteps(task.getId()).stream()
                .collect(Collectors.toMap(InspectionTaskStep::getStepCode, Function.identity(), (a, b) -> a));
        for (String stepCode : STEP_ORDER) {
            InspectionTaskStep step = stepMap.get(stepCode);
            InspectionTaskDtos.StepDetail dto = new InspectionTaskDtos.StepDetail();
            dto.setStepCode(stepCode);
            dto.setStepName(STEP_NAMES.get(stepCode));
            dto.setStepStatus(step == null ? STEP_STATUS_WAIT : step.getStepStatus());
            dto.setRefId(step == null ? null : step.getRefId());
            dto.setRefTable(step == null ? null : step.getRefTable());
            dto.setOperatorName(step == null ? null : step.getOperatorName());
            dto.setStartTime(step == null ? null : step.getStartTime());
            dto.setCompleteTime(step == null ? null : step.getCompleteTime());
            dto.setSkipReason(step == null ? null : step.getSkipReason());
            dto.setRemark(step == null ? null : step.getRemark());
            fillStepFields(dto, task, stepCode, step == null ? null : step.getRefId());
            detail.getSteps().add(dto);
        }
        return detail;
    }

    private void fillStepFields(InspectionTaskDtos.StepDetail dto, InspectionTask task,
                                String stepCode, Long refId) {
        if (refId == null) {
            return;
        }
        switch (stepCode) {
            case STEP_PLAN -> {
                InspectionPlan plan = inspectionPlanService.getById(refId);
                if (plan == null) return;
                dto.setCode(plan.getPlanCode());
                dto.getFields().add(new InspectionTaskDtos.FieldItem("planCode", "计划编号", plan.getPlanCode()));
                dto.getFields().add(new InspectionTaskDtos.FieldItem("planPeriod", "计划周期", plan.getPlanPeriod()));
                dto.getFields().add(new InspectionTaskDtos.FieldItem("inspectionType", "检验类型", plan.getInspectionType()));
                dto.getFields().add(new InspectionTaskDtos.FieldItem("objectName", "检验对象", plan.getObjectName()));
                dto.getFields().add(new InspectionTaskDtos.FieldItem("batchNo", "批号", plan.getBatchNo()));
                dto.getFields().add(new InspectionTaskDtos.FieldItem("spec", "规格", plan.getSpec()));
                dto.getFields().add(new InspectionTaskDtos.FieldItem("inspectionSummary", "检验项目", plan.getInspectionSummary()));
                dto.getFields().add(new InspectionTaskDtos.FieldItem("planTime", "计划时间", str(plan.getPlanTime())));
                loadFiles(dto, STEP_PLAN, refId);
            }
            case STEP_REQUEST -> {
                InspectionRequest request = inspectionRequestService.getById(refId);
                if (request == null) return;
                dto.setCode(request.getInspectionCode());
                dto.getFields().add(new InspectionTaskDtos.FieldItem("inspectionCode", "请检单号", request.getInspectionCode()));
                dto.getFields().add(new InspectionTaskDtos.FieldItem("requester", "请检人", request.getRequester()));
                dto.getFields().add(new InspectionTaskDtos.FieldItem("requestDepartment", "请检部门", request.getRequestDepartment()));
                dto.getFields().add(new InspectionTaskDtos.FieldItem("requestTime", "请检时间", str(request.getRequestTime())));
                dto.getFields().add(new InspectionTaskDtos.FieldItem("processName", "工序", request.getProcessName()));
                dto.getFields().add(new InspectionTaskDtos.FieldItem("inspectionItems", "检验项目", request.getInspectionItems()));
                loadFiles(dto, STEP_REQUEST, refId);
            }
            case STEP_SAMPLING -> {
                SamplingRecord sampling = samplingRecordService.getById(refId);
                if (sampling == null) return;
                dto.setCode(sampling.getSamplingCode());
                dto.getFields().add(new InspectionTaskDtos.FieldItem("samplingCode", "取样编号", sampling.getSamplingCode()));
                dto.getFields().add(new InspectionTaskDtos.FieldItem("sampler", "取样人", sampling.getSampler()));
                dto.getFields().add(new InspectionTaskDtos.FieldItem("samplingTime", "取样时间", str(sampling.getSamplingTime())));
                dto.getFields().add(new InspectionTaskDtos.FieldItem("samplingQuantity", "取样量", sampling.getSamplingQuantity()));
                dto.getFields().add(new InspectionTaskDtos.FieldItem("samplingLocation", "取样地点", sampling.getSamplingLocation()));
                dto.getFields().add(new InspectionTaskDtos.FieldItem("samplingMethod", "取样方法", sampling.getSamplingMethod()));
                loadFiles(dto, STEP_SAMPLING, refId);
            }
            case STEP_RETENTION -> {
                RetainedSample retained = retainedSampleService.getById(refId);
                if (retained == null) return;
                dto.setCode(retained.getRetainedCode());
                dto.getFields().add(new InspectionTaskDtos.FieldItem("retainedCode", "留样编号", retained.getRetainedCode()));
                dto.getFields().add(new InspectionTaskDtos.FieldItem("retainedQuantity", "留样量", retained.getRetainedQuantity()));
                dto.getFields().add(new InspectionTaskDtos.FieldItem("retainedDate", "留样日期", str(retained.getRetainedDate())));
                dto.getFields().add(new InspectionTaskDtos.FieldItem("expiryDate", "留样期限", str(retained.getExpiryDate())));
                dto.getFields().add(new InspectionTaskDtos.FieldItem("storageLocation", "存放位置", retained.getStorageLocation()));
                dto.getFields().add(new InspectionTaskDtos.FieldItem("status", "状态", retained.getStatus()));
                loadFiles(dto, STEP_RETENTION, refId);
            }
            case STEP_INSPECTION -> {
                InspectionRecord record = inspectionRecordService.getById(refId);
                if (record == null) return;
                dto.setCode(record.getInspectionCode());
                dto.getFields().add(new InspectionTaskDtos.FieldItem("inspectionCode", "检验编号", record.getInspectionCode()));
                dto.getFields().add(new InspectionTaskDtos.FieldItem("inspector", "检验人", record.getInspector()));
                dto.getFields().add(new InspectionTaskDtos.FieldItem("reviewer", "复核人", record.getReviewer()));
                dto.getFields().add(new InspectionTaskDtos.FieldItem("startTime", "开始时间", str(record.getStartTime())));
                dto.getFields().add(new InspectionTaskDtos.FieldItem("endTime", "结束时间", str(record.getEndTime())));
                dto.getFields().add(new InspectionTaskDtos.FieldItem("inspectionItem", "检验项目", record.getInspectionItem()));
                dto.getFields().add(new InspectionTaskDtos.FieldItem("conclusion", "结论", record.getConclusion()));
                loadFiles(dto, STEP_INSPECTION, refId);
            }
            case STEP_DESTRUCTION -> {
                DestructionRecord record = destructionRecordService.getById(refId);
                if (record == null) return;
                dto.setCode(record.getDestructionCode());
                dto.getFields().add(new InspectionTaskDtos.FieldItem("destructionCode", "销毁编号", record.getDestructionCode()));
                dto.getFields().add(new InspectionTaskDtos.FieldItem("recordType", "销毁类型", record.getRecordType()));
                dto.getFields().add(new InspectionTaskDtos.FieldItem("destroyDate", "销毁日期", str(record.getDestroyDate())));
                dto.getFields().add(new InspectionTaskDtos.FieldItem("destroyerName", "销毁人", record.getDestroyerName()));
                dto.getFields().add(new InspectionTaskDtos.FieldItem("destroyReason", "销毁原因", record.getDestroyReason()));
                dto.getFields().add(new InspectionTaskDtos.FieldItem("status", "状态", record.getStatus()));
                loadFiles(dto, STEP_DESTRUCTION, refId);
            }
            default -> {
            }
        }
    }

    private void loadFiles(InspectionTaskDtos.StepDetail dto, String stepCode, Long refId) {
        String bizType = STEP_BIZ_TYPES.get(stepCode);
        if (bizType == null) return;
        List<BusinessFileDto> files = businessFileService.list(bizType, refId, null, null);
        if (files == null) return;
        for (BusinessFileDto file : files) {
            InspectionTaskDtos.FileItem item = new InspectionTaskDtos.FileItem();
            item.setFileId(file.getFileId());
            item.setName(file.getOriginalName());
            item.setSize(file.getFileSize());
            item.setTime(str(file.getCreatedTime()));
            item.setUploader("");
            dto.getFiles().add(item);
        }
    }

    // endregion

    // region 创建与步骤办理
    // ===================================
    // 创建与步骤办理
    // ===================================

    @Override
    @Transactional
    public InspectionTaskDtos.TaskDetail createTask(InspectionTaskDtos.CreateTaskRequest request) {
        if (request == null || request.getWorkOrderId() == null) {
            throw new IllegalArgumentException("工单ID不能为空");
        }
        String itemCategory = StringUtils.hasText(request.getItemCategory()) ? request.getItemCategory() : "成品";
        if (!ITEM_CATEGORIES.contains(itemCategory)) {
            throw new IllegalArgumentException("被检品属性不合法");
        }
        WorkOrder workOrder = workOrderMapper.selectById(request.getWorkOrderId());
        if (workOrder == null) {
            throw new IllegalArgumentException("工单不存在");
        }
        String batchNo = workOrder.getBatchNumber() == null ? "" : workOrder.getBatchNumber();
        InspectionTask exist = getOne(new QueryWrapper<InspectionTask>()
                .eq("work_order_id", workOrder.getWorkOrderId())
                .eq("item_category", itemCategory)
                .eq("batch_no", batchNo), false);
        if (exist != null) {
            return getTaskDetail(exist.getId());
        }
        InspectionTask task = new InspectionTask();
        task.setTaskCode(generateTaskCode());
        task.setWorkOrderId(workOrder.getWorkOrderId());
        task.setWorkOrderCode(workOrder.getWorkOrderCode());
        task.setPlanId(workOrder.getPlanId() == null ? null : workOrder.getPlanId().longValue());
        ProductionPlan plan = workOrder.getPlanId() == null ? null : productionPlanMapper.selectById(workOrder.getPlanId());
        if (plan != null) {
            task.setPlanNumber(plan.getPlanNumber());
        }
        task.setPreparationId(workOrder.getPreparationId());
        task.setPreparationCode(workOrder.getPreparationCode());
        task.setPreparationName(workOrder.getPreparationName());
        task.setBatchNo(batchNo);
        task.setItemCategory(itemCategory);
        task.setObjectName(StringUtils.hasText(request.getObjectName()) ? request.getObjectName() : workOrder.getPreparationName());
        task.setSpec(workOrder.getPreparationId() == null ? null
                : java.util.Optional.ofNullable(preparationService.getById(workOrder.getPreparationId()))
                .map(p -> p.getSpec()).orElse(null));
        task.setCurrentStep(STEP_PLAN);
        task.setTaskStatus(STATUS_WAIT);
        task.setRetentionRequired(request.getRetentionRequired() == null ? 1 : request.getRetentionRequired());
        task.setDestructionRequired(request.getDestructionRequired() == null ? 1 : request.getDestructionRequired());
        task.setRemark(request.getRemark());
        task.setIsDeleted(0);
        task.setVersion(1);
        save(task);

        for (String step : STEP_ORDER) {
            InspectionTaskStep taskStep = new InspectionTaskStep();
            taskStep.setTaskId(task.getId());
            taskStep.setStepCode(step);
            taskStep.setStepStatus(STEP_STATUS_WAIT);
            taskStep.setIsDeleted(0);
            taskStep.setVersion(1);
            stepMapper.insert(taskStep);
        }
        markStepRunning(task.getId(), STEP_PLAN);
        return getTaskDetail(task.getId());
    }

    @Override
    @Transactional
    public InspectionTaskDtos.TaskDetail completePlan(Long id, InspectionTaskDtos.PlanStepRequest request) {
        InspectionTask task = getTaskOrThrow(id);
        ensureStep(task, STEP_PLAN);
        InspectionPlan plan = task.getPlanRefId() == null ? new InspectionPlan() : inspectionPlanService.getById(task.getPlanRefId());
        if (plan == null) plan = new InspectionPlan();
        if (!StringUtils.hasText(plan.getPlanCode())) {
            plan.setPlanCode(StringUtils.hasText(request.getPlanCode()) ? request.getPlanCode() : inspectionPlanService.generateCode());
        }
        plan.setPlanPeriod(request.getPlanPeriod());
        plan.setInspectionType(StringUtils.hasText(request.getInspectionType()) ? request.getInspectionType() : "成品检验");
        plan.setObjectName(StringUtils.hasText(request.getObjectName()) ? request.getObjectName() : task.getObjectName());
        plan.setBatchNo(StringUtils.hasText(request.getBatchNo()) ? request.getBatchNo() : task.getBatchNo());
        plan.setSpec(StringUtils.hasText(request.getSpec()) ? request.getSpec() : task.getSpec());
        plan.setInspectionSummary(request.getInspectionSummary());
        plan.setPlanTime(request.getPlanTime() == null ? LocalDate.now() : request.getPlanTime());
        plan.setStatus(STATUS_DONE);
        plan.setRemark(request.getRemark());
        plan.setIsDeleted(0);
        if (plan.getId() == null) {
            inspectionPlanService.save(plan);
        } else {
            inspectionPlanService.updateById(plan);
        }
        task.setPlanRefId(plan.getId());
        completeStepAndAdvance(task, STEP_PLAN, "inspection_plan", plan.getId(),
                "检验计划 " + plan.getPlanCode() + " 已完成");
        syncFiles(STEP_PLAN, plan.getId(), request.getFileIds());
        return getTaskDetail(task.getId());
    }

    @Override
    @Transactional
    public InspectionTaskDtos.TaskDetail completeRequest(Long id, InspectionTaskDtos.RequestStepRequest request) {
        InspectionTask task = getTaskOrThrow(id);
        ensureStep(task, STEP_REQUEST);
        if (task.getPlanRefId() == null) {
            throw new IllegalArgumentException("请先完成检验计划");
        }
        InspectionPlan plan = inspectionPlanService.getById(task.getPlanRefId());
        InspectionRequest entity = new InspectionRequest();
        entity.setInspectionCode(StringUtils.hasText(request.getInspectionCode()) ? request.getInspectionCode() : inspectionRequestService.generateCode());
        entity.setWorkOrderId(task.getWorkOrderId());
        entity.setWorkOrderCode(task.getWorkOrderCode());
        entity.setInspectionPlanCode(plan == null ? null : plan.getPlanCode());
        entity.setPreparationCode(task.getPreparationCode());
        entity.setPreparationName(task.getPreparationName());
        entity.setItemCategory(StringUtils.hasText(request.getItemCategory()) ? request.getItemCategory() : task.getItemCategory());
        entity.setInspectionItemName(StringUtils.hasText(request.getInspectionItemName()) ? request.getInspectionItemName() : task.getObjectName());
        entity.setBatchNumber(StringUtils.hasText(request.getBatchNumber()) ? request.getBatchNumber() : task.getBatchNo());
        entity.setSpec(StringUtils.hasText(request.getSpec()) ? request.getSpec() : task.getSpec());
        entity.setProcessName(StringUtils.hasText(request.getProcessName()) ? request.getProcessName() : "");
        entity.setConfigQuantity(StringUtils.hasText(request.getConfigQuantity()) ? request.getConfigQuantity() : "");
        entity.setRequestDepartment(StringUtils.hasText(request.getRequestDepartment()) ? request.getRequestDepartment() : "");
        entity.setRequester(StringUtils.hasText(request.getRequester()) ? request.getRequester() : "系统");
        entity.setRequestTime(request.getRequestTime() == null ? LocalDateTime.now() : request.getRequestTime());
        entity.setInspectionItems(StringUtils.hasText(request.getInspectionItems()) ? request.getInspectionItems() : "");
        entity.setRemark(request.getRemark());
        entity.setIsDeleted(0);
        inspectionRequestService.save(entity);
        task.setRequestRefId(entity.getId());
        completeStepAndAdvance(task, STEP_REQUEST, "inspection_request", entity.getId(),
                "请检单 " + entity.getInspectionCode() + " 已创建");
        syncFiles(STEP_REQUEST, entity.getId(), request.getFileIds());
        return getTaskDetail(task.getId());
    }

    @Override
    @Transactional
    public InspectionTaskDtos.TaskDetail completeSampling(Long id, InspectionTaskDtos.SamplingStepRequest request) {
        InspectionTask task = getTaskOrThrow(id);
        ensureStep(task, STEP_SAMPLING);
        if (task.getRequestRefId() == null) {
            throw new IllegalArgumentException("请先完成请检");
        }
        InspectionRequest requestEntity = inspectionRequestService.getById(task.getRequestRefId());
        SamplingRecord entity = new SamplingRecord();
        entity.setSamplingCode(StringUtils.hasText(request.getSamplingCode()) ? request.getSamplingCode() : samplingRecordService.generateCode());
        entity.setRelatedPlanCode(requestEntity == null ? null : requestEntity.getInspectionCode());
        entity.setObjectName(task.getObjectName());
        entity.setItemCategory(task.getItemCategory());
        entity.setBatchNo(task.getBatchNo());
        entity.setSpec(task.getSpec());
        entity.setSamplingLocation(StringUtils.hasText(request.getLocation()) ? request.getLocation() : "");
        entity.setSamplingQuantity(StringUtils.hasText(request.getQuantity()) ? request.getQuantity() : "");
        entity.setSamplingCount(request.getCount());
        entity.setSamplingMethod(request.getMethod());
        entity.setSampler(StringUtils.hasText(request.getSampler()) ? request.getSampler() : "系统");
        entity.setSamplingTime(request.getSamplingTime() == null ? LocalDateTime.now() : request.getSamplingTime());
        entity.setRemark(request.getRemark());
        entity.setIsDeleted(0);
        samplingRecordService.save(entity);
        task.setSamplingRefId(entity.getId());
        completeStepAndAdvance(task, STEP_SAMPLING, "sampling_record", entity.getId(),
                "取样记录 " + entity.getSamplingCode() + " 已创建");
        if (task.getRetentionRequired() != null && task.getRetentionRequired() == 0) {
            markStepSkipped(task.getId(), STEP_RETENTION, "该任务不要求留样");
            advanceTo(task, STEP_INSPECTION);
            markStepRunning(task.getId(), STEP_INSPECTION);
        }
        syncFiles(STEP_SAMPLING, entity.getId(), request.getFileIds());
        return getTaskDetail(task.getId());
    }

    @Override
    @Transactional
    public InspectionTaskDtos.TaskDetail completeRetention(Long id, InspectionTaskDtos.RetentionStepRequest request) {
        InspectionTask task = getTaskOrThrow(id);
        ensureStep(task, STEP_RETENTION);
        if (task.getSamplingRefId() == null) {
            throw new IllegalArgumentException("请先完成取样");
        }
        InspectionRequest requestEntity = task.getRequestRefId() == null ? null
                : inspectionRequestService.getById(task.getRequestRefId());
        RetainedSample entity = new RetainedSample();
        entity.setRetainedCode(StringUtils.hasText(request.getRetainedCode()) ? request.getRetainedCode() : retainedSampleService.generateCode());
        entity.setRelatedInspectionCode(requestEntity == null ? null : requestEntity.getInspectionCode());
        entity.setMaterialName(task.getObjectName());
        entity.setItemCategory(task.getItemCategory());
        entity.setBatchNo(task.getBatchNo());
        entity.setSpec(task.getSpec());
        entity.setRetainedQuantity(StringUtils.hasText(request.getQuantity()) ? request.getQuantity() : "");
        LocalDate retainedDate = request.getRetainedDate() == null ? LocalDate.now() : request.getRetainedDate();
        entity.setRetainedDate(retainedDate);
        entity.setExpiryDate(request.getExpiryDate() == null ? retainedDate.plusYears(2) : request.getExpiryDate());
        entity.setStorageLocation(StringUtils.hasText(request.getStorageLocation()) ? request.getStorageLocation() : "");
        entity.setObservationRecord(request.getObservationRecord());
        entity.setStatus("留样中");
        entity.setRemark(request.getRemark());
        entity.setIsDeleted(0);
        retainedSampleService.save(entity);
        task.setRetentionRefId(entity.getId());
        completeStepAndAdvance(task, STEP_RETENTION, "retained_sample", entity.getId(),
                "留样记录 " + entity.getRetainedCode() + " 已创建");
        syncFiles(STEP_RETENTION, entity.getId(), request.getFileIds());
        return getTaskDetail(task.getId());
    }

    @Override
    @Transactional
    public InspectionTaskDtos.TaskDetail completeInspection(Long id, InspectionTaskDtos.InspectionStepRequest request) {
        InspectionTask task = getTaskOrThrow(id);
        ensureStep(task, STEP_INSPECTION);
        if (task.getSamplingRefId() == null) {
            throw new IllegalArgumentException("请先完成取样");
        }
        SamplingRecord sampling = samplingRecordService.getById(task.getSamplingRefId());
        InspectionRequest requestEntity = task.getRequestRefId() == null ? null
                : inspectionRequestService.getById(task.getRequestRefId());
        InspectionRecord entity = task.getInspectionRefId() == null ? new InspectionRecord()
                : inspectionRecordService.getById(task.getInspectionRefId());
        if (entity == null) entity = new InspectionRecord();
        if (!StringUtils.hasText(entity.getInspectionCode())) {
            entity.setInspectionCode(StringUtils.hasText(request.getInspectionCode()) ? request.getInspectionCode() : inspectionRecordService.generateCode());
        }
        entity.setRelatedSamplingCode(sampling == null ? null : sampling.getSamplingCode());
        entity.setRelatedInspectionRequestCode(requestEntity == null ? null : requestEntity.getInspectionCode());
        entity.setWorkOrderId(task.getWorkOrderId());
        entity.setWorkOrderCode(task.getWorkOrderCode());
        entity.setObjectName(task.getObjectName());
        entity.setItemCategory(task.getItemCategory());
        entity.setPreparationName(task.getPreparationName());
        entity.setPreparationCode(task.getPreparationCode());
        entity.setBatchNo(task.getBatchNo());
        entity.setSpec(task.getSpec());
        entity.setInspectionBasis(StringUtils.hasText(request.getInspectionBasis()) ? request.getInspectionBasis() : "");
        entity.setInspectionItem(StringUtils.hasText(request.getInspectionItem()) ? request.getInspectionItem() : "");
        entity.setInspector(StringUtils.hasText(request.getInspector()) ? request.getInspector() : "");
        entity.setReviewer(StringUtils.hasText(request.getReviewer()) ? request.getReviewer() : "");
        entity.setStartTime(request.getStartTime() == null ? LocalDateTime.now() : request.getStartTime());
        entity.setEndTime(request.getEndTime() == null ? LocalDateTime.now() : request.getEndTime());
        entity.setConclusion(StringUtils.hasText(request.getConclusion()) ? request.getConclusion() : "待检");
        entity.setRemark(request.getRemark());
        entity.setIsDeleted(0);
        if (entity.getId() == null) {
            inspectionRecordService.save(entity);
        } else {
            inspectionRecordService.updateById(entity);
        }
        task.setInspectionRefId(entity.getId());
        completeStepAndAdvance(task, STEP_INSPECTION, "inspection_record", entity.getId(),
                "检验记录 " + entity.getInspectionCode() + " 已保存");
        if (task.getDestructionRequired() != null && task.getDestructionRequired() == 0) {
            markStepSkipped(task.getId(), STEP_DESTRUCTION, "该任务不要求销毁");
            advanceTo(task, STEP_DONE);
        }
        syncFiles(STEP_INSPECTION, entity.getId(), request.getFileIds());
        return getTaskDetail(task.getId());
    }

    @Override
    @Transactional
    public InspectionTaskDtos.TaskDetail completeDestruction(Long id, InspectionTaskDtos.DestructionStepRequest request) {
        InspectionTask task = getTaskOrThrow(id);
        ensureStep(task, STEP_DESTRUCTION);
        DestructionRecordSaveDto dto = new DestructionRecordSaveDto();
        dto.setRecordType(StringUtils.hasText(request.getRecordType()) ? request.getRecordType() : "留样及稳定性考察产品销毁");
        dto.setDestroyReason(request.getDestroyReason());
        dto.setDestroyMethod(request.getDestroyMethod());
        dto.setDestroyLocation(request.getDestroyLocation());
        dto.setApplicantName(request.getApplicantName());
        dto.setApplyDate(request.getApplyDate());
        dto.setApproverName(request.getApproverName());
        dto.setApproveDate(request.getApproveDate());
        dto.setDestroyerName(request.getDestroyerName());
        dto.setDestroyDate(request.getDestroyDate() == null ? LocalDate.now() : request.getDestroyDate());
        dto.setSupervisorName(request.getSupervisorName());
        dto.setSuperviseDate(request.getSuperviseDate());
        dto.setRemark(request.getRemark());
        DestructionRecordDetailSaveDto detail = new DestructionRecordDetailSaveDto();
        detail.setMaterialName(task.getObjectName());
        detail.setSpec(task.getSpec());
        detail.setBatchNo(task.getBatchNo());
        detail.setQuantity(BigDecimal.ONE);
        detail.setRetainedSampleId(task.getRetentionRefId());
        dto.setDetails(List.of(detail));
        dto.setAttachmentFileIds(request.getFileIds());
        DestructionRecord entity = destructionRecordService.create(dto);
        task.setDestructionRefId(entity.getId());
        completeStepAndAdvance(task, STEP_DESTRUCTION, "destruction_record", entity.getId(),
                "销毁记录 " + entity.getDestructionCode() + " 已创建");
        advanceTo(task, STEP_DONE);
        return getTaskDetail(task.getId());
    }

    @Override
    @Transactional
    public InspectionTaskDtos.TaskDetail skipStep(Long id, InspectionTaskDtos.SkipStepRequest request) {
        InspectionTask task = getTaskOrThrow(id);
        if (STEP_RETENTION.equals(request.getStepCode())) {
            ensureStep(task, STEP_RETENTION);
            markStepSkipped(task.getId(), STEP_RETENTION, request.getReason());
            advanceTo(task, STEP_INSPECTION);
            markStepRunning(task.getId(), STEP_INSPECTION);
        } else if (STEP_DESTRUCTION.equals(request.getStepCode())) {
            ensureStep(task, STEP_DESTRUCTION);
            markStepSkipped(task.getId(), STEP_DESTRUCTION, request.getReason());
            advanceTo(task, STEP_DONE);
        } else {
            throw new IllegalArgumentException("只允许跳过留样或销毁");
        }
        return getTaskDetail(task.getId());
    }

    @Override
    @Transactional
    public InspectionTaskDtos.TaskDetail cancelTask(Long id, InspectionTaskDtos.CancelRequest request) {
        InspectionTask task = getTaskOrThrow(id);
        if (STATUS_DONE.equals(task.getTaskStatus())) {
            throw new IllegalArgumentException("已完成的任务不能作废");
        }
        task.setTaskStatus(STATUS_CANCELLED);
        task.setRemark(request == null ? task.getRemark() : request.getReason());
        updateById(task);
        return getTaskDetail(task.getId());
    }

    // endregion

    // region 内部流程工具
    // ===================================
    // 内部流程工具
    // ===================================

    private InspectionTask getTaskOrThrow(Long id) {
        InspectionTask task = getById(id);
        if (task == null) {
            throw new IllegalArgumentException("检验任务不存在");
        }
        if (STATUS_CANCELLED.equals(task.getTaskStatus())) {
            throw new IllegalArgumentException("检验任务已作废");
        }
        return task;
    }

    private void ensureStep(InspectionTask task, String stepCode) {
        if (!stepCode.equals(task.getCurrentStep())) {
            throw new IllegalArgumentException("当前环节为 " + STEP_NAMES.getOrDefault(task.getCurrentStep(), task.getCurrentStep())
                    + "，不能办理 " + STEP_NAMES.get(stepCode));
        }
    }

    private void completeStepAndAdvance(InspectionTask task, String stepCode, String refTable, Long refId, String remark) {
        InspectionTaskStep step = getStep(task.getId(), stepCode);
        if (step != null) {
            step.setStepStatus(STEP_STATUS_DONE);
            step.setRefTable(refTable);
            step.setRefId(refId);
            step.setCompleteTime(LocalDateTime.now());
            step.setRemark(remark);
            if (step.getOperatorId() == null) step.setOperatorId(EntityUtils.getCurrentUserId());
            stepMapper.updateById(step);
        }
        if (task.getStartTime() == null) {
            task.setStartTime(LocalDateTime.now());
        }
        task.setTaskStatus(STATUS_RUN);
        String next = nextStep(stepCode);
        advanceTo(task, next);
        if (!STEP_DONE.equals(next)) {
            markStepRunning(task.getId(), next);
        }
    }

    private String nextStep(String stepCode) {
        int index = STEP_ORDER.indexOf(stepCode);
        return index < 0 || index == STEP_ORDER.size() - 1 ? STEP_DONE : STEP_ORDER.get(index + 1);
    }

    private void advanceTo(InspectionTask task, String stepCode) {
        task.setCurrentStep(stepCode);
        if (STEP_DONE.equals(stepCode)) {
            task.setTaskStatus(STATUS_DONE);
            task.setCompleteTime(LocalDateTime.now());
        } else {
            task.setTaskStatus(STATUS_RUN);
        }
        updateById(task);
    }

    private void markStepRunning(Long taskId, String stepCode) {
        InspectionTaskStep step = getStep(taskId, stepCode);
        if (step == null) return;
        step.setStepStatus(STEP_STATUS_RUN);
        step.setStartTime(LocalDateTime.now());
        if (step.getOperatorId() == null) step.setOperatorId(EntityUtils.getCurrentUserId());
        stepMapper.updateById(step);
    }

    private void markStepSkipped(Long taskId, String stepCode, String reason) {
        InspectionTaskStep step = getStep(taskId, stepCode);
        if (step == null) return;
        step.setStepStatus(STEP_STATUS_SKIP);
        step.setSkipReason(reason);
        step.setCompleteTime(LocalDateTime.now());
        stepMapper.updateById(step);
    }

    private InspectionTaskStep getStep(Long taskId, String stepCode) {
        return stepMapper.selectOne(new QueryWrapper<InspectionTaskStep>()
                .eq("task_id", taskId)
                .eq("step_code", stepCode)
                .last("LIMIT 1"));
    }

    private List<InspectionTaskStep> listSteps(Long taskId) {
        return stepMapper.selectList(new QueryWrapper<InspectionTaskStep>()
                .eq("task_id", taskId)
                .orderByAsc("id"));
    }

    private void syncFiles(String stepCode, Long bizId, List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        BusinessFileSyncDto dto = new BusinessFileSyncDto();
        dto.setBizType(STEP_BIZ_TYPES.get(stepCode));
        dto.setBizId(bizId);
        dto.setFileKind("DEFAULT");
        dto.setFileIds(fileIds);
        businessFileService.sync(dto);
    }

    private String generateTaskCode() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "JYRW-" + date + "-";
        String max = jdbcTemplate.queryForObject(
                "SELECT MAX(task_code) FROM inspection_task WHERE task_code LIKE ?", String.class, prefix + "%");
        int next = 1;
        if (StringUtils.hasText(max) && max.length() >= prefix.length() + 3) {
            try {
                next = Integer.parseInt(max.substring(max.length() - 3)) + 1;
            } catch (NumberFormatException ignored) {
                // 保持默认序号
            }
        }
        return prefix + String.format("%03d", next);
    }

    private String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    // endregion
}
