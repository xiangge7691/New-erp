package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
        boolean linked = request.getWorkOrderId() != null || request.getPlanBatchId() != null;
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
            WorkOrderProcessPlanBatch batch = request.getPlanBatchId() == null ? null
                    : planBatchMapper.selectById(request.getPlanBatchId());
            WorkOrderProcessPlan plan = batch == null ? findPlanByWorkOrder(request.getWorkOrderId(),
                    request.getProcessTypeId()) : planMapper.selectById(batch.getPlanId());
            if (plan == null) {
                throw new IllegalArgumentException("关联的工序计划不存在");
            }
            record.setWorkOrderId(plan.getWorkOrderId());
            record.setProcessTypeId(request.getProcessTypeId() != null ? request.getProcessTypeId() : plan.getProcessTypeId());
            record.setPlanId(plan.getId());
            record.setPlanBatchId(request.getPlanBatchId());
            record.setBatchNo(batch == null ? null : batch.getBatchNo());
            applyCoefficientSnapshot(record);
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

    private WorkOrderProcessPlan findPlanByWorkOrder(Long workOrderId, Long processTypeId) {
        if (workOrderId == null) {
            return null;
        }
        LambdaQueryWrapper<WorkOrderProcessPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrderProcessPlan::getWorkOrderId, workOrderId);
        if (processTypeId != null) {
            wrapper.eq(WorkOrderProcessPlan::getProcessTypeId, processTypeId);
        }
        wrapper.orderByAsc(WorkOrderProcessPlan::getStepOrder).last("LIMIT 1");
        return planMapper.selectOne(wrapper);
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
        validateRange(request.getProcessCoefficients(), "工序");
        validateRange(request.getPersonCoefficients(), "人员");
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
        version.setNote(StringUtils.hasText(request.getNote()) ? request.getNote().trim() : null);
        version.setStatus("生效");
        version.setIsDeleted(0);
        version.setVersion(0);
        versionMapper.insert(version);

        List<ProcessType> processTypes = processTypeMapper.selectList(null);
        for (ProcessType processType : processTypes) {
            BigDecimal coef = request.getProcessCoefficients() != null
                    && request.getProcessCoefficients().get(processType.getProcessId()) != null
                    ? request.getProcessCoefficients().get(processType.getProcessId())
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
            BigDecimal coef = request.getPersonCoefficients() != null
                    && request.getPersonCoefficients().get(person.getPersonnelFileId()) != null
                    ? request.getPersonCoefficients().get(person.getPersonnelFileId())
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

    private void validateRange(Map<Long, BigDecimal> coefficients, String label) {
        if (coefficients == null) {
            return;
        }
        for (Map.Entry<Long, BigDecimal> entry : coefficients.entrySet()) {
            BigDecimal value = entry.getValue();
            if (value == null || value.compareTo(new BigDecimal("0.5")) < 0
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
