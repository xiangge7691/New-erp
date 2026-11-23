package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.tonghui.erp.Common.Dto.Performance.CoefficientPublishRequest;
import com.tonghui.erp.Common.Dto.Performance.CoefficientVersionDetail;
import com.tonghui.erp.Common.Dto.Performance.PerformanceManualRequest;
import com.tonghui.erp.Common.Dto.Performance.PerformanceModifyRequest;
import com.tonghui.erp.Common.Dto.Performance.PersonCoefficientItem;
import com.tonghui.erp.Common.Dto.Performance.ProcessCoefficientItem;
import com.tonghui.erp.Common.utils.EntityUtils;
import com.tonghui.erp.Data.Entity.PerformanceCoefficientVersion;
import com.tonghui.erp.Data.Entity.PerformancePersonCoefficient;
import com.tonghui.erp.Data.Entity.PerformanceProcessCoefficient;
import com.tonghui.erp.Data.Entity.PerformanceRecord;
import com.tonghui.erp.Data.Entity.PersonnelFile;
import com.tonghui.erp.Data.Entity.ProcessType;
import com.tonghui.erp.Data.Entity.ProductionTeam;
import com.tonghui.erp.Data.Entity.ProductionTeamMember;
import com.tonghui.erp.Data.Entity.User;
import com.tonghui.erp.Data.Entity.ProductionPlan;
import com.tonghui.erp.Data.Entity.WorkOrder;
import com.tonghui.erp.Data.Entity.WorkOrderProcessPlan;
import com.tonghui.erp.Data.Entity.WorkOrderProcessPlanBatch;
import com.tonghui.erp.Data.Entity.WorkOrderProcessExecution;
import com.tonghui.erp.Data.mapper.PerformanceCoefficientVersionMapper;
import com.tonghui.erp.Data.mapper.PerformancePersonCoefficientMapper;
import com.tonghui.erp.Data.mapper.PerformanceProcessCoefficientMapper;
import com.tonghui.erp.Data.mapper.PerformanceRecordMapper;
import com.tonghui.erp.Data.mapper.PersonnelFileMapper;
import com.tonghui.erp.Data.mapper.ProcessTypeMapper;
import com.tonghui.erp.Data.mapper.ProductionTeamMapper;
import com.tonghui.erp.Data.mapper.ProductionTeamMemberMapper;
import com.tonghui.erp.Data.mapper.UserMapper;
import com.tonghui.erp.Data.mapper.ProductionPlanMapper;
import com.tonghui.erp.Data.mapper.WorkOrderMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessExecutionMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessPlanBatchMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessPlanMapper;
import com.tonghui.erp.Service.PerformanceService;
import com.tonghui.erp.Service.PerformanceGenerationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 人员绩效服务实现类
 */
@Service
public class PerformanceServiceImpl implements PerformanceService {

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    /** 绩效记录 Mapper */
    @Autowired
    private PerformanceRecordMapper recordMapper;

    /** 系数版本 Mapper */
    @Autowired
    private PerformanceCoefficientVersionMapper versionMapper;

    /** 版本内工序系数 Mapper */
    @Autowired
    private PerformanceProcessCoefficientMapper processCoefficientMapper;

    /** 版本内人员系数 Mapper */
    @Autowired
    private PerformancePersonCoefficientMapper personCoefficientMapper;

    /** 人员档案 Mapper */
    @Autowired
    private PersonnelFileMapper personnelFileMapper;

    /** 工单 Mapper */
    @Autowired
    private WorkOrderMapper workOrderMapper;

    /** 生产计划 Mapper（计划编号关键字匹配） */
    @Autowired
    private ProductionPlanMapper productionPlanMapper;

    /** 工序类型 Mapper */
    @Autowired
    private ProcessTypeMapper processTypeMapper;

    /** 排产批次 Mapper */
    @Autowired
    private WorkOrderProcessPlanBatchMapper planBatchMapper;

    /** 排产计划 Mapper */
    @Autowired
    private WorkOrderProcessPlanMapper planMapper;

    /** 实际执行记录 Mapper */
    @Autowired
    private WorkOrderProcessExecutionMapper executionMapper;

    /** 班组成员 Mapper */
    @Autowired
    private ProductionTeamMemberMapper teamMemberMapper;

    /** 班组 Mapper */
    @Autowired
    private ProductionTeamMapper teamMapper;

    /** 用户 Mapper（回填修改人姓名） */
    @Autowired
    private UserMapper userMapper;

    /** 绩效生成服务（历史执行记录补生成） */
    @Autowired
    private PerformanceGenerationService performanceGenerationService;

    // endregion

    // region 查询
    // ===================================
    // 查询
    // ===================================

    /**
     * 分页查询绩效记录
     */
    @Override
    public PagedResult<PerformanceRecord> query(LocalDate from, LocalDate to, Long personnelId, Long workOrderId,
                                                Long teamId, String source, Integer modifiedStatus,
                                                Integer recordStatus, String keyword, PageRequestDto pageRequest) {
        if (pageRequest == null) {
            pageRequest = new PageRequestDto();
        }
        pageRequest.validateAndFix();
        // 历史执行记录（PDA 结束打点、实际生产保存等）完工时没有生成绩效，
        // 查询指定区间前先幂等补生成，保证工序/人员粒度视图能看到历史数据；单边日期也补生成
        if (from != null || to != null) {
            performanceGenerationService.generateForRange(from, to);
        }
        LambdaQueryWrapper<PerformanceRecord> wrapper = new LambdaQueryWrapper<>();
        if (from != null) wrapper.ge(PerformanceRecord::getPerformanceDate, from);
        if (to != null) wrapper.le(PerformanceRecord::getPerformanceDate, to);
        if (personnelId != null) wrapper.eq(PerformanceRecord::getPersonnelId, personnelId);
        if (workOrderId != null) wrapper.eq(PerformanceRecord::getWorkOrderId, workOrderId);
        if (StringUtils.hasText(keyword)) {
            final List<Integer> matchedPlanIds = productionPlanMapper.selectList(
                            new LambdaQueryWrapper<ProductionPlan>()
                                    .like(ProductionPlan::getPlanNumber, keyword))
                    .stream().map(ProductionPlan::getId).toList();
            LambdaQueryWrapper<WorkOrder> orderWrapper = new LambdaQueryWrapper<>();
            orderWrapper.and(q -> {
                q.like(WorkOrder::getWorkOrderCode, keyword)
                        .or().like(WorkOrder::getWorkOrderName, keyword)
                        .or().like(WorkOrder::getPreparationCode, keyword)
                        .or().like(WorkOrder::getPreparationName, keyword)
                        .or().like(WorkOrder::getPlanName, keyword)
                        .or().like(WorkOrder::getBatchNumber, keyword);
                if (!matchedPlanIds.isEmpty()) {
                    q.or().in(WorkOrder::getPlanId, matchedPlanIds);
                }
            });
            List<Long> orderIds = workOrderMapper.selectList(orderWrapper).stream()
                    .map(WorkOrder::getWorkOrderId).toList();
            if (CollectionUtils.isEmpty(orderIds)) {
                return PagedResult.empty();
            }
            wrapper.in(PerformanceRecord::getWorkOrderId, orderIds);
        }
        if (teamId != null) {
            Set<Long> memberIds = teamMemberMapper.selectList(
                            new LambdaQueryWrapper<ProductionTeamMember>()
                                    .eq(ProductionTeamMember::getTeamId, teamId))
                    .stream().map(ProductionTeamMember::getPersonnelId)
                    .filter(Objects::nonNull).collect(Collectors.toSet());
            if (memberIds.isEmpty()) {
                return PagedResult.empty();
            }
            wrapper.in(PerformanceRecord::getPersonnelId, memberIds);
        }
        if (StringUtils.hasText(source)) wrapper.eq(PerformanceRecord::getSource, source);
        if (modifiedStatus != null) wrapper.eq(PerformanceRecord::getModifiedStatus, modifiedStatus);
        if (recordStatus != null) wrapper.eq(PerformanceRecord::getRecordStatus, recordStatus);
        wrapper.orderByDesc(PerformanceRecord::getPerformanceDate).orderByDesc(PerformanceRecord::getId);
        Page<PerformanceRecord> page = recordMapper.selectPage(PagedResult.toMybatisPage(pageRequest), wrapper);
        enrich(page.getRecords());
        return PagedResult.fromPage(page, pageRequest);
    }

    /**
     * 补充人员、工单、制剂、工序、批次、班组、计划/实际时间与系数版本
     */
    private void enrich(List<PerformanceRecord> records) {
        if (CollectionUtils.isEmpty(records)) {
            return;
        }
        Set<Long> personnelIds = records.stream().map(PerformanceRecord::getPersonnelId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> workOrderIds = records.stream().map(PerformanceRecord::getWorkOrderId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> processIds = records.stream().map(PerformanceRecord::getProcessTypeId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> batchIds = records.stream().map(PerformanceRecord::getPlanBatchId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> planIds = records.stream().map(PerformanceRecord::getPlanId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> executionIds = records.stream().map(PerformanceRecord::getExecutionId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> versionIds = records.stream().map(PerformanceRecord::getCoefficientVersionId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, PersonnelFile> people = personnelIds.isEmpty() ? Map.of()
                : personnelFileMapper.selectBatchIds(personnelIds).stream()
                .collect(Collectors.toMap(PersonnelFile::getPersonnelFileId, Function.identity(), (a, b) -> a));
        Map<Long, WorkOrder> orders = workOrderIds.isEmpty() ? Map.of()
                : workOrderMapper.selectBatchIds(workOrderIds).stream()
                .collect(Collectors.toMap(WorkOrder::getWorkOrderId, Function.identity(), (a, b) -> a));
        Map<Long, ProcessType> processes = processIds.isEmpty() ? Map.of()
                : processTypeMapper.selectBatchIds(processIds).stream()
                .collect(Collectors.toMap(x -> x.getProcessId().longValue(), Function.identity(), (a, b) -> a));
        Map<Long, WorkOrderProcessPlanBatch> batches = batchIds.isEmpty() ? Map.of()
                : planBatchMapper.selectBatchIds(batchIds).stream()
                .collect(Collectors.toMap(WorkOrderProcessPlanBatch::getId, Function.identity(), (a, b) -> a));
        Map<Long, WorkOrderProcessPlan> plans = planIds.isEmpty() ? Map.of()
                : planMapper.selectBatchIds(planIds).stream()
                .collect(Collectors.toMap(WorkOrderProcessPlan::getId, Function.identity(), (a, b) -> a));
        Map<Long, WorkOrderProcessExecution> executions = executionIds.isEmpty() ? Map.of()
                : executionMapper.selectBatchIds(executionIds).stream()
                .collect(Collectors.toMap(WorkOrderProcessExecution::getId, Function.identity(), (a, b) -> a));
        Map<Long, PerformanceCoefficientVersion> versions = versionIds.isEmpty() ? Map.of()
                : versionMapper.selectBatchIds(versionIds).stream()
                .collect(Collectors.toMap(PerformanceCoefficientVersion::getId, Function.identity(), (a, b) -> a));
        Set<Long> userIds = records.stream().map(PerformanceRecord::getUpdatedBy)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> users = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getUserId, Function.identity(), (a, b) -> a));
        Map<Long, String> personTeams = personTeamNames(personnelIds);
        for (PerformanceRecord record : records) {
            PersonnelFile person = record.getPersonnelId() == null ? null : people.get(record.getPersonnelId());
            record.setPersonnelName(person == null ? null : person.getName());
            record.setTeamName(record.getPersonnelId() == null ? null : personTeams.get(record.getPersonnelId()));
            WorkOrder order = record.getWorkOrderId() == null ? null : orders.get(record.getWorkOrderId());
            if (order != null) {
                record.setWorkOrderCode(order.getWorkOrderCode());
                record.setPreparationName(order.getPreparationName());
                record.setBatchNumber(order.getBatchNumber());
            }
            ProcessType process = record.getProcessTypeId() == null ? null : processes.get(record.getProcessTypeId());
            record.setProcessName(process == null ? null : process.getProcessName());
            WorkOrderProcessPlanBatch batch = record.getPlanBatchId() == null
                    ? null : batches.get(record.getPlanBatchId());
            record.setPlanBatchName(batch == null ? null : batch.getBatchName());
            WorkOrderProcessPlan plan = record.getPlanId() == null ? null : plans.get(record.getPlanId());
            if (batch != null) {
                record.setPlanStartTime(batch.getPlanStartTime() != null
                        ? batch.getPlanStartTime() : plan == null ? null : plan.getPlanStartTime());
                record.setPlanEndTime(batch.getPlanEndTime() != null
                        ? batch.getPlanEndTime() : plan == null ? null : plan.getPlanEndTime());
                record.setPlanDurationMinutes(batch.getPlanDurationMinutes() != null
                        ? batch.getPlanDurationMinutes() : plan == null ? null : plan.getPlanDurationMinutes());
                record.setPlanQty(batch.getPlanQty() != null
                        ? batch.getPlanQty() : plan == null ? null : plan.getPlanQty());
            } else if (plan != null) {
                record.setPlanStartTime(plan.getPlanStartTime());
                record.setPlanEndTime(plan.getPlanEndTime());
                record.setPlanDurationMinutes(plan.getPlanDurationMinutes());
                record.setPlanQty(plan.getPlanQty());
            }
            WorkOrderProcessExecution execution = record.getExecutionId() == null
                    ? null : executions.get(record.getExecutionId());
            if (execution != null) {
                record.setActualStartTime(execution.getStartTime());
                record.setActualEndTime(execution.getEndTime());
            }
            PerformanceCoefficientVersion version = record.getCoefficientVersionId() == null
                    ? null : versions.get(record.getCoefficientVersionId());
            record.setCoefficientVersionNo(version == null ? null : version.getVersionNo());
            User updater = record.getUpdatedBy() == null ? null : users.get(record.getUpdatedBy());
            if (updater != null) {
                record.setUpdatedByName(StringUtils.hasText(updater.getUserName())
                        ? updater.getUserName() : updater.getUserAccount());
            }
        }
    }

    // endregion

    // region 修改与补录
    // ===================================
    // 修改与补录
    // ===================================

    /**
     * 修改绩效时长
     */
    @Override
    public PerformanceRecord modify(Long id, PerformanceModifyRequest request) {
        PerformanceRecord record = recordMapper.selectById(id);
        if (record == null) {
            throw new IllegalArgumentException("绩效记录不存在");
        }
        if (Integer.valueOf(0).equals(record.getRecordStatus())) {
            throw new IllegalArgumentException("作废记录不能修改");
        }
        if (request == null || request.getPerformanceHours() == null
                || request.getPerformanceHours().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("绩效时长必须大于 0");
        }
        if (!StringUtils.hasText(request.getNote())) {
            throw new IllegalArgumentException("修改备注必填");
        }
        if (record.getOriginalPerformanceHours() == null) {
            record.setOriginalPerformanceHours(record.getPerformanceHours());
        }
        record.setPerformanceHours(scale2(request.getPerformanceHours()));
        if ("手动添加".equals(record.getSource()) && request.getCompletedQty() != null) {
            record.setCompletedQty(request.getCompletedQty());
        }
        record.setPerformanceHoursTotal(calcTotal(record));
        record.setModifiedStatus(1);
        record.setNote(request.getNote().trim());
        recordMapper.updateById(record);
        enrich(List.of(record));
        return record;
    }

    /**
     * 手动添加绩效
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PerformanceRecord manual(PerformanceManualRequest request) {
        if (request == null || request.getPersonnelId() == null) {
            throw new IllegalArgumentException("被考核人不能为空");
        }
        if (request.getPerformanceDate() == null) {
            throw new IllegalArgumentException("绩效归属日期不能为空");
        }
        if (request.getPerformanceDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("绩效归属日期不能晚于当天");
        }
        if (request.getPerformanceHours() == null
                || request.getPerformanceHours().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("绩效时长必须大于 0");
        }
        if (!StringUtils.hasText(request.getNote())) {
            throw new IllegalArgumentException("补录备注必填");
        }
        PersonnelFile person = personnelFileMapper.selectById(request.getPersonnelId());
        if (person == null) {
            throw new IllegalArgumentException("人员档案不存在");
        }
        boolean linked = request.getWorkOrderId() != null || request.getPlanBatchId() != null
                || request.getExecutionId() != null;
        String calcMode = StringUtils.hasText(request.getCalcMode()) ? request.getCalcMode()
                : (linked ? "按系数计算" : "直接计入");
        PerformanceRecord record = new PerformanceRecord();
        record.setPersonnelId(request.getPersonnelId());
        record.setPerformanceDate(request.getPerformanceDate());
        record.setWorkHours(request.getWorkHours() != null ? scale2(request.getWorkHours())
                : scale2(request.getPerformanceHours()));
        record.setPerformanceHours(scale2(request.getPerformanceHours()));
        record.setCompletedQty(request.getCompletedQty());
        record.setNote(request.getNote().trim());
        record.setSource("手动添加");
        record.setModifiedStatus(0);
        record.setRecordStatus(1);
        record.setCalcMode(calcMode);
        if (linked) {
            // 人员绩效以实际执行为准：关联已完工的工序执行记录
            WorkOrderProcessExecution execution = resolveCompletedExecution(request);
            if (execution == null) {
                throw new IllegalArgumentException("未找到已完工的工序执行记录，无法关联补录");
            }
            if (execution.getEndTime() == null) {
                throw new IllegalArgumentException("只能关联已完工的工序执行记录");
            }
            record.setWorkOrderId(execution.getWorkOrderId());
            record.setProcessTypeId(execution.getProcessTypeId());
            record.setPlanId(execution.getPlanId());
            record.setPlanBatchId(execution.getPlanBatchId());
            record.setBatchNo(execution.getBatchNo());
            if (execution.getStartTime() != null && execution.getEndTime() != null) {
                long minutes = Math.max(Duration.between(execution.getStartTime(), execution.getEndTime()).toMinutes(), 0);
                record.setProcessActualHours(BigDecimal.valueOf(minutes)
                        .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP));
            }
            applyCoefficientSnapshot(record);
            // 同一执行记录同一人已有系统记录时，补录记录不再挂 executionId，避免唯一键冲突
            Long execPersonCount = recordMapper.selectCount(new LambdaQueryWrapper<PerformanceRecord>()
                    .eq(PerformanceRecord::getExecutionId, execution.getId())
                    .eq(PerformanceRecord::getPersonnelId, record.getPersonnelId())
                    .eq(PerformanceRecord::getRecordStatus, 1));
            if (execPersonCount == null || execPersonCount == 0) {
                record.setExecutionId(execution.getId());
            }
            if (!Boolean.TRUE.equals(request.getAllowDuplicate())) {
                Long exists = recordMapper.selectCount(new LambdaQueryWrapper<PerformanceRecord>()
                        .eq(PerformanceRecord::getPersonnelId, record.getPersonnelId())
                        .eq(PerformanceRecord::getWorkOrderId, record.getWorkOrderId())
                        .eq(PerformanceRecord::getProcessTypeId, record.getProcessTypeId())
                        .eq(PerformanceRecord::getRecordStatus, 1));
                if (exists != null && exists > 0) {
                    throw new IllegalArgumentException("该人对同一生产任务同一工序已有记录；确认继续添加请在原因确认后传 allowDuplicate=true");
                }
            }
        } else {
            record.setProcessActualHours(null);
            record.setProcessCoefficient(null);
            record.setPersonCoefficient(null);
            record.setCoefficientVersionId(null);
            record.setCalcMode("直接计入");
            record.setPerformanceHoursTotal(scale2(record.getPerformanceHours()));
        }
        recordMapper.insert(record);
        enrich(List.of(record));
        return record;
    }

    /**
     * 作废手动添加的绩效记录
     */
    @Override
    public PerformanceRecord voidRecord(Long id, String note) {
        PerformanceRecord record = recordMapper.selectById(id);
        if (record == null) {
            throw new IllegalArgumentException("绩效记录不存在");
        }
        if (!"手动添加".equals(record.getSource())) {
            throw new IllegalArgumentException("系统生成记录不可作废，只能修改绩效时长");
        }
        if (!StringUtils.hasText(note)) {
            throw new IllegalArgumentException("作废原因必填");
        }
        record.setRecordStatus(0);
        record.setNote("作废：" + note.trim());
        recordMapper.updateById(record);
        enrich(List.of(record));
        return record;
    }

    /**
     * 查找可关联的已完工执行记录：优先指定 executionId，否则按工单+工序取最近完工的一条
     */
    private WorkOrderProcessExecution resolveCompletedExecution(PerformanceManualRequest request) {
        if (request.getExecutionId() != null) {
            WorkOrderProcessExecution execution = executionMapper.selectById(request.getExecutionId());
            if (execution == null || Integer.valueOf(1).equals(execution.getIsDeleted())) {
                return null;
            }
            if (request.getWorkOrderId() != null && !request.getWorkOrderId().equals(execution.getWorkOrderId())) {
                return null;
            }
            if (request.getProcessTypeId() != null && !request.getProcessTypeId().equals(execution.getProcessTypeId())) {
                return null;
            }
            return execution;
        }
        if (request.getWorkOrderId() == null && request.getPlanBatchId() == null) {
            return null;
        }
        return executionMapper.selectOne(new LambdaQueryWrapper<WorkOrderProcessExecution>()
                .eq(request.getWorkOrderId() != null, WorkOrderProcessExecution::getWorkOrderId, request.getWorkOrderId())
                .eq(request.getProcessTypeId() != null, WorkOrderProcessExecution::getProcessTypeId, request.getProcessTypeId())
                .eq(request.getPlanBatchId() != null, WorkOrderProcessExecution::getPlanBatchId, request.getPlanBatchId())
                .eq(WorkOrderProcessExecution::getIsDeleted, 0)
                .isNotNull(WorkOrderProcessExecution::getEndTime)
                .orderByDesc(WorkOrderProcessExecution::getEndTime)
                .last("LIMIT 1"));
    }

    /**
     * 填充当前生效版本的系数快照
     */
    private void applyCoefficientSnapshot(PerformanceRecord record) {
        PerformanceCoefficientVersion version = activeVersion();
        BigDecimal processCoef = processCoefficient(version, record.getProcessTypeId());
        BigDecimal personCoef = personCoefficient(version, record.getPersonnelId());
        record.setProcessCoefficient(processCoef);
        record.setPersonCoefficient(personCoef);
        record.setCoefficientVersionId(version == null ? null : version.getId());
        record.setCalcMode("按系数计算");
        record.setPerformanceHoursTotal(calcTotal(record));
    }

    private BigDecimal calcTotal(PerformanceRecord record) {
        if (!"按系数计算".equals(record.getCalcMode())) {
            return scale2(record.getPerformanceHours());
        }
        BigDecimal process = record.getProcessCoefficient() == null ? BigDecimal.ONE : record.getProcessCoefficient();
        BigDecimal person = record.getPersonCoefficient() == null ? BigDecimal.ONE : record.getPersonCoefficient();
        return record.getPerformanceHours().multiply(process).multiply(person).setScale(2, RoundingMode.HALF_UP);
    }

    // endregion

    // region 系数版本
    // ===================================
    // 系数版本
    // ===================================

    /**
     * 查询系数版本列表
     */
    @Override
    public List<PerformanceCoefficientVersion> listVersions() {
        List<PerformanceCoefficientVersion> versions = versionMapper.selectList(
                new LambdaQueryWrapper<PerformanceCoefficientVersion>()
                        .orderByDesc(PerformanceCoefficientVersion::getId));
        Set<Long> operatorIds = versions.stream().map(PerformanceCoefficientVersion::getOperatorId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (!operatorIds.isEmpty()) {
            Map<Long, User> users = userMapper.selectBatchIds(operatorIds).stream()
                    .collect(Collectors.toMap(User::getUserId, Function.identity(), (a, b) -> a));
            versions.forEach(v -> {
                User user = v.getOperatorId() == null ? null : users.get(v.getOperatorId());
                v.setOperatorName(user == null ? null : user.getUserName());
            });
        }
        return versions;
    }

    /**
     * 发布新系数版本（整体快照）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PerformanceCoefficientVersion publish(CoefficientPublishRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("发布参数不能为空");
        }
        // 兼容两种提交方式：id -> 系数 映射，或明细数组
        Map<Long, BigDecimal> processCoefficients = parseCoefficientNode(
                request.getProcessCoefficients(), "工序类型ID");
        if (request.getProcessItems() != null) {
            for (ProcessCoefficientItem item : request.getProcessItems()) {
                if (item != null && item.getProcessTypeId() != null && item.getCoefficient() != null
                        && !processCoefficients.containsKey(item.getProcessTypeId())) {
                    processCoefficients.put(item.getProcessTypeId(), item.getCoefficient());
                }
            }
        }
        Map<Long, BigDecimal> personCoefficients = parseCoefficientNode(
                request.getPersonCoefficients(), "人员档案ID");
        if (request.getPersonItems() != null) {
            for (PersonCoefficientItem item : request.getPersonItems()) {
                if (item != null && item.getPersonnelId() != null && item.getCoefficient() != null
                        && !personCoefficients.containsKey(item.getPersonnelId())) {
                    personCoefficients.put(item.getPersonnelId(), item.getCoefficient());
                }
            }
        }
        validateRange(processCoefficients, "工序");
        validateRange(personCoefficients, "人员");
        List<PerformanceCoefficientVersion> versions = listVersions();
        versions.forEach(v -> {
            if ("生效".equals(v.getStatus())) {
                v.setStatus("归档");
                versionMapper.updateById(v);
            }
        });
        String versionNo = nextVersionNo(versions);
        PerformanceCoefficientVersion version = new PerformanceCoefficientVersion();
        version.setVersionNo(versionNo);
        version.setEffectiveTime(LocalDateTime.now());
        version.setOperatorId(EntityUtils.getCurrentUserId());
        String note = StringUtils.hasText(request.getNote()) ? request.getNote().trim() : null;
        if (note != null && note.length() > 500) {
            note = note.substring(0, 500);
        }
        version.setNote(note);
        version.setStatus("生效");
        version.setIsDeleted(0);
        version.setVersion(0);
        try {
            versionMapper.insert(version);
        } catch (DuplicateKeyException ex) {
            // 并发或历史软删除记录占用版本号时顺延重试一次
            int next = Integer.parseInt(versionNo.substring(1)) + 1;
            version.setVersionNo("V" + next);
            versionMapper.insert(version);
        }

        List<ProcessType> processTypes = processTypeMapper.selectList(null);
        for (ProcessType processType : processTypes) {
            BigDecimal coef = processCoefficients.get(processType.getProcessId().longValue()) != null
                    ? processCoefficients.get(processType.getProcessId().longValue())
                    : (processType.getCoefficient() != null ? processType.getCoefficient() : BigDecimal.ONE);
            PerformanceProcessCoefficient entity = new PerformanceProcessCoefficient();
            entity.setVersionId(version.getId());
            entity.setProcessTypeId(processType.getProcessId().longValue());
            entity.setCoefficient(coef.setScale(2, RoundingMode.HALF_UP));
            entity.setIsDeleted(0);
            entity.setVersion(0);
            processCoefficientMapper.insert(entity);
            processType.setCoefficient(coef.setScale(2, RoundingMode.HALF_UP));
            processTypeMapper.updateById(processType);
        }
        List<PersonnelFile> personnel = personnelFileMapper.selectList(
                new LambdaQueryWrapper<PersonnelFile>().eq(PersonnelFile::getStatus, 1));
        for (PersonnelFile person : personnel) {
            BigDecimal coef = personCoefficients.get(person.getPersonnelFileId()) != null
                    ? personCoefficients.get(person.getPersonnelFileId())
                    : personCoefficient(versions.isEmpty() ? null : versions.get(0), person.getPersonnelFileId());
            PerformancePersonCoefficient entity = new PerformancePersonCoefficient();
            entity.setVersionId(version.getId());
            entity.setPersonnelId(person.getPersonnelFileId());
            entity.setCoefficient(coef.setScale(2, RoundingMode.HALF_UP));
            entity.setIsDeleted(0);
            entity.setVersion(0);
            personCoefficientMapper.insert(entity);
        }
        return version;
    }

    /**
     * 查询系数版本详情
     */
    @Override
    public CoefficientVersionDetail versionDetail(Long id) {
        PerformanceCoefficientVersion version = versionMapper.selectById(id);
        if (version == null) {
            throw new IllegalArgumentException("系数版本不存在");
        }
        CoefficientVersionDetail detail = new CoefficientVersionDetail();
        detail.setId(version.getId());
        detail.setVersionNo(version.getVersionNo());
        detail.setEffectiveTime(version.getEffectiveTime());
        detail.setOperatorId(version.getOperatorId());
        if (version.getOperatorId() != null) {
            User operator = userMapper.selectById(version.getOperatorId());
            detail.setOperatorName(operator == null ? null : operator.getUserName());
        }
        detail.setNote(version.getNote());
        detail.setStatus(version.getStatus());
        List<PerformanceProcessCoefficient> processRows = processCoefficientMapper.selectList(
                new LambdaQueryWrapper<PerformanceProcessCoefficient>()
                        .eq(PerformanceProcessCoefficient::getVersionId, id));
        detail.setProcessCoefficients(processRows.stream()
                .collect(Collectors.toMap(PerformanceProcessCoefficient::getProcessTypeId,
                        PerformanceProcessCoefficient::getCoefficient, (a, b) -> a, LinkedHashMap::new)));
        if (!processRows.isEmpty()) {
            Set<Integer> processIds = processRows.stream()
                    .map(x -> x.getProcessTypeId().intValue()).collect(Collectors.toSet());
            Map<Integer, ProcessType> processTypes = processTypeMapper.selectBatchIds(processIds).stream()
                    .collect(Collectors.toMap(ProcessType::getProcessId, Function.identity(), (a, b) -> a));
            List<ProcessCoefficientItem> processItems = new ArrayList<>();
            for (PerformanceProcessCoefficient row : processRows) {
                ProcessCoefficientItem item = new ProcessCoefficientItem();
                item.setProcessTypeId(row.getProcessTypeId());
                item.setCoefficient(row.getCoefficient());
                ProcessType type = processTypes.get(row.getProcessTypeId().intValue());
                if (type != null) {
                    item.setProcessCode(type.getProcessCode());
                    item.setProcessName(type.getProcessName());
                }
                processItems.add(item);
            }
            processItems.sort(Comparator.comparing(ProcessCoefficientItem::getProcessTypeId));
            detail.setProcessItems(processItems);
        }
        List<PerformancePersonCoefficient> personRows = personCoefficientMapper.selectList(
                new LambdaQueryWrapper<PerformancePersonCoefficient>()
                        .eq(PerformancePersonCoefficient::getVersionId, id));
        detail.setPersonCoefficients(personRows.stream()
                .collect(Collectors.toMap(PerformancePersonCoefficient::getPersonnelId,
                        PerformancePersonCoefficient::getCoefficient, (a, b) -> a, LinkedHashMap::new)));
        if (!personRows.isEmpty()) {
            Set<Long> personnelIds = personRows.stream()
                    .map(PerformancePersonCoefficient::getPersonnelId).collect(Collectors.toSet());
            Map<Long, PersonnelFile> people = personnelFileMapper.selectBatchIds(personnelIds).stream()
                    .collect(Collectors.toMap(PersonnelFile::getPersonnelFileId, Function.identity(), (a, b) -> a));
            Map<Long, String> teams = personTeamNames(personnelIds);
            List<PersonCoefficientItem> personItems = new ArrayList<>();
            for (PerformancePersonCoefficient row : personRows) {
                PersonCoefficientItem item = new PersonCoefficientItem();
                item.setPersonnelId(row.getPersonnelId());
                item.setCoefficient(row.getCoefficient());
                PersonnelFile person = people.get(row.getPersonnelId());
                if (person != null) {
                    item.setPersonnelName(person.getName());
                    item.setEmployeeNo(person.getEmployeeNo());
                }
                item.setTeamName(teams.get(row.getPersonnelId()));
                personItems.add(item);
            }
            personItems.sort(Comparator.comparing(PersonCoefficientItem::getPersonnelId));
            detail.setPersonItems(personItems);
        }
        return detail;
    }

    private String nextVersionNo(List<PerformanceCoefficientVersion> versions) {
        int max = 0;
        Integer dbMax = versionMapper.maxVersionNumber();
        if (dbMax != null) {
            max = dbMax;
        }
        for (PerformanceCoefficientVersion version : versions) {
            String no = version.getVersionNo();
            if (no != null && no.startsWith("V")) {
                try {
                    max = Math.max(max, Integer.parseInt(no.substring(1)));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return "V" + (max + 1);
    }

    /**
     * 解析系数 JSON：兼容 {"id": 系数} 与 [{id字段, coefficient}] 两种结构；
     * 键也兼容工序编码、人员工号，值兼容数字与数字字符串
     */
    private Map<Long, BigDecimal> parseCoefficientNode(JsonNode node, String idLabel) {
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        if (node == null || node.isNull()) {
            return result;
        }
        boolean process = "工序类型ID".equals(idLabel);
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                BigDecimal value = decimalValue(entry.getValue());
                if (value == null) {
                    return;
                }
                result.put(resolveCoefficientId(entry.getKey(), process), value);
            });
            return result;
        }
        if (node.isArray()) {
            String[] idFields = process
                    ? new String[]{"processTypeId", "processId", "id"}
                    : new String[]{"personnelId", "personnelFileId", "id"};
            for (JsonNode item : node) {
                JsonNode idNode = null;
                for (String field : idFields) {
                    idNode = item.get(field);
                    if (idNode != null && !idNode.isNull()) {
                        break;
                    }
                }
                BigDecimal value = decimalValue(item.get("coefficient"));
                if (idNode == null || idNode.isNull() || value == null) {
                    continue;
                }
                if (idNode.isNumber()) {
                    result.put(idNode.asLong(), value);
                } else {
                    result.put(resolveCoefficientId(idNode.asText(), process), value);
                }
            }
            return result;
        }
        throw new IllegalArgumentException("系数格式不正确：应为对象或数组");
    }

    /**
     * 数字/数字字符串转 BigDecimal
     */
    private BigDecimal decimalValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.decimalValue();
        }
        if (node.isTextual()) {
            try {
                return new BigDecimal(node.asText().trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    /**
     * 把系数键解析为ID：优先按ID，其次按工序编码/人员工号匹配
     */
    private Long resolveCoefficientId(String key, boolean process) {
        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException(process ? "工序系数键不能为空" : "人员系数键不能为空");
        }
        String text = key.trim();
        try {
            return Long.valueOf(text);
        } catch (NumberFormatException ignored) {
            // 继续按编码匹配
        }
        if (process) {
            ProcessType type = processTypeMapper.selectOne(new LambdaQueryWrapper<ProcessType>()
                    .eq(ProcessType::getProcessCode, text).last("LIMIT 1"));
            if (type != null) {
                return type.getProcessId().longValue();
            }
            throw new IllegalArgumentException("未找到工序编码：" + text);
        }
        PersonnelFile person = personnelFileMapper.selectOne(new LambdaQueryWrapper<PersonnelFile>()
                .eq(PersonnelFile::getEmployeeNo, text).last("LIMIT 1"));
        if (person != null) {
            return person.getPersonnelFileId();
        }
        throw new IllegalArgumentException("未找到人员工号：" + text);
    }

    private void validateRange(Map<Long, BigDecimal> coefficients, String label) {
        if (coefficients == null) {
            return;
        }
        for (Map.Entry<Long, BigDecimal> entry : coefficients.entrySet()) {
            BigDecimal value = entry.getValue();
            // 空值表示该项不修改，沿用现有系数
            if (value == null) {
                continue;
            }
            if (value.compareTo(new BigDecimal("0.5")) < 0
                    || value.compareTo(new BigDecimal("2.0")) > 0) {
                throw new IllegalArgumentException(label + "系数必须在 0.5–2.0 之间：" + entry.getKey());
            }
        }
    }

    private PerformanceCoefficientVersion activeVersion() {
        return versionMapper.selectOne(new LambdaQueryWrapper<PerformanceCoefficientVersion>()
                .eq(PerformanceCoefficientVersion::getStatus, "生效")
                .orderByDesc(PerformanceCoefficientVersion::getId).last("LIMIT 1"));
    }

    private BigDecimal processCoefficient(PerformanceCoefficientVersion version, Long processTypeId) {
        if (processTypeId == null) {
            return BigDecimal.ONE;
        }
        if (version != null) {
            PerformanceProcessCoefficient entity = processCoefficientMapper.selectOne(
                    new LambdaQueryWrapper<PerformanceProcessCoefficient>()
                            .eq(PerformanceProcessCoefficient::getVersionId, version.getId())
                            .eq(PerformanceProcessCoefficient::getProcessTypeId, processTypeId));
            if (entity != null && entity.getCoefficient() != null) {
                return entity.getCoefficient();
            }
        }
        ProcessType type = processTypeMapper.selectById(processTypeId);
        return type != null && type.getCoefficient() != null ? type.getCoefficient() : BigDecimal.ONE;
    }

    private BigDecimal personCoefficient(PerformanceCoefficientVersion version, Long personnelId) {
        if (personnelId == null) {
            return BigDecimal.ONE;
        }
        if (version != null) {
            PerformancePersonCoefficient entity = personCoefficientMapper.selectOne(
                    new LambdaQueryWrapper<PerformancePersonCoefficient>()
                            .eq(PerformancePersonCoefficient::getVersionId, version.getId())
                            .eq(PerformancePersonCoefficient::getPersonnelId, personnelId));
            if (entity != null && entity.getCoefficient() != null) {
                return entity.getCoefficient();
            }
        }
        return BigDecimal.ONE;
    }

    // endregion

    // region 汇总
    // ===================================
    // 汇总
    // ===================================

    /**
     * 汇总查询
     */
    @Override
    public List<Map<String, Object>> aggregate(LocalDate from, LocalDate to, String groupBy) {
        // 汇总同样按区间补生成历史绩效，避免总览与明细数据不一致；单边日期也补生成
        if (from != null || to != null) {
            performanceGenerationService.generateForRange(from, to);
        }
        LambdaQueryWrapper<PerformanceRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PerformanceRecord::getRecordStatus, 1);
        if (from != null) wrapper.ge(PerformanceRecord::getPerformanceDate, from);
        if (to != null) wrapper.le(PerformanceRecord::getPerformanceDate, to);
        List<PerformanceRecord> records = recordMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(records)) {
            return List.of();
        }
        enrich(records);
        Map<String, List<PerformanceRecord>> grouped = new LinkedHashMap<>();
        if ("team".equalsIgnoreCase(groupBy)) {
            Map<Long, String> personTeam = personTeamNames();
            for (PerformanceRecord record : records) {
                String key = personTeam.getOrDefault(record.getPersonnelId(), "未分组");
                grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(record);
            }
        } else if ("product".equalsIgnoreCase(groupBy)) {
            for (PerformanceRecord record : records) {
                grouped.computeIfAbsent(record.getPreparationName() == null ? "未关联制剂"
                        : record.getPreparationName(), k -> new ArrayList<>()).add(record);
            }
        } else {
            for (PerformanceRecord record : records) {
                grouped.computeIfAbsent(record.getPerformanceDate().toString(), k -> new ArrayList<>()).add(record);
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<PerformanceRecord>> entry : grouped.entrySet()) {
            List<PerformanceRecord> list = entry.getValue();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("groupKey", entry.getKey());
            row.put("recordCount", list.size());
            row.put("personCount", list.stream().map(PerformanceRecord::getPersonnelId).distinct().count());
            row.put("performanceHours", list.stream().map(PerformanceRecord::getPerformanceHours)
                    .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
            row.put("performanceHoursTotal", list.stream().map(PerformanceRecord::getPerformanceHoursTotal)
                    .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
            row.put("completedQty", list.stream().map(PerformanceRecord::getCompletedQty)
                    .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
            result.add(row);
        }
        result.sort(Comparator.comparing((Map<String, Object> m) ->
                (BigDecimal) m.get("performanceHoursTotal")).reversed());
        return result;
    }

    private Map<Long, String> personTeamNames() {
        return personTeamNames(null);
    }

    /**
     * 查询人员所属班组名称
     *
     * @param personnelIds 人员档案ID集合，为空时查询全部
     * @return 人员ID -> 班组名称（多个班组用、连接）
     */
    private Map<Long, String> personTeamNames(Set<Long> personnelIds) {
        if (personnelIds != null && personnelIds.isEmpty()) {
            return Map.of();
        }
        LambdaQueryWrapper<ProductionTeamMember> wrapper = new LambdaQueryWrapper<>();
        if (!CollectionUtils.isEmpty(personnelIds)) {
            wrapper.in(ProductionTeamMember::getPersonnelId, personnelIds);
        }
        List<ProductionTeamMember> members = teamMemberMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(members)) {
            return Map.of();
        }
        Set<Long> teamIds = members.stream().map(ProductionTeamMember::getTeamId).collect(Collectors.toSet());
        Map<Long, String> teamNames = teamMapper.selectBatchIds(teamIds).stream()
                .collect(Collectors.toMap(ProductionTeam::getId, ProductionTeam::getTeamName));
        Map<Long, Set<String>> personTeams = new HashMap<>();
        for (ProductionTeamMember member : members) {
            String name = teamNames.get(member.getTeamId());
            if (name != null) {
                personTeams.computeIfAbsent(member.getPersonnelId(), k -> new java.util.LinkedHashSet<>()).add(name);
            }
        }
        Map<Long, String> result = new HashMap<>();
        personTeams.forEach((k, v) -> result.put(k, String.join("、", v)));
        return result;
    }

    private BigDecimal scale2(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    // endregion
}
