package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tonghui.erp.Common.Constants.ProcessExecutionStatus;
import com.tonghui.erp.Data.Entity.PerformanceCoefficientVersion;
import com.tonghui.erp.Data.Entity.PerformancePersonCoefficient;
import com.tonghui.erp.Data.Entity.PerformanceProcessCoefficient;
import com.tonghui.erp.Data.Entity.PerformanceRecord;
import com.tonghui.erp.Data.Entity.ProcessType;
import com.tonghui.erp.Data.Entity.WorkOrderProcessExecution;
import com.tonghui.erp.Data.Entity.WorkOrderProcessExecutionParticipant;
import com.tonghui.erp.Data.mapper.PerformanceCoefficientVersionMapper;
import com.tonghui.erp.Data.mapper.PerformancePersonCoefficientMapper;
import com.tonghui.erp.Data.mapper.PerformanceProcessCoefficientMapper;
import com.tonghui.erp.Data.mapper.PerformanceRecordMapper;
import com.tonghui.erp.Data.mapper.ProcessTypeMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessExecutionMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessExecutionParticipantMapper;
import com.tonghui.erp.Service.PerformanceGenerationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

/**
 * 绩效生成服务实现类
 */
@Service
public class PerformanceGenerationServiceImpl implements PerformanceGenerationService {

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    /** 工序执行 Mapper */
    @Autowired
    private WorkOrderProcessExecutionMapper executionMapper;

    /** 实际参与人 Mapper */
    @Autowired
    private WorkOrderProcessExecutionParticipantMapper participantMapper;

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

    /** 工序类型 Mapper */
    @Autowired
    private ProcessTypeMapper processTypeMapper;

    // endregion

    // region 生成
    // ===================================
    // 生成
    // ===================================

    /**
     * 按工序执行记录与实际参与人生成个人绩效记录
     *
     * @param executionId 工序执行记录ID
     * @return 生成的记录数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int generateForExecution(Long executionId) {
        WorkOrderProcessExecution execution = executionMapper.selectById(executionId);
        if (execution == null || execution.getStartTime() == null || execution.getEndTime() == null) {
            return 0;
        }
        List<WorkOrderProcessExecutionParticipant> participants =
                participantMapper.selectByExecutionIdsWithPersonnel(List.of(executionId));
        if (CollectionUtils.isEmpty(participants)) {
            return 0;
        }
        BigDecimal processActualHours = durationHours(execution);
        PerformanceCoefficientVersion version = activeVersion();
        int generated = 0;
        for (WorkOrderProcessExecutionParticipant participant : participants) {
            if (participant.getPersonnelId() == null) {
                continue;
            }
            long exists = recordMapper.selectCount(new LambdaQueryWrapper<PerformanceRecord>()
                    .eq(PerformanceRecord::getExecutionId, executionId)
                    .eq(PerformanceRecord::getPersonnelId, participant.getPersonnelId()));
            if (exists > 0) {
                continue;
            }
            BigDecimal workHours = participant.getWorkHours() == null
                    || participant.getWorkHours().compareTo(BigDecimal.ZERO) <= 0
                    ? processActualHours : participant.getWorkHours();
            BigDecimal processCoef = processCoefficient(version, execution.getProcessTypeId());
            BigDecimal personCoef = personCoefficient(version, participant.getPersonnelId());

            PerformanceRecord record = new PerformanceRecord();
            record.setPersonnelId(participant.getPersonnelId());
            record.setWorkOrderId(execution.getWorkOrderId());
            record.setProcessTypeId(execution.getProcessTypeId());
            record.setPlanId(execution.getPlanId());
            record.setPlanBatchId(execution.getPlanBatchId());
            record.setExecutionId(executionId);
            record.setBatchNo(execution.getBatchNo());
            record.setPerformanceDate(execution.getEndTime().toLocalDate());
            record.setProcessActualHours(processActualHours);
            record.setWorkHours(scale2(workHours));
            record.setPerformanceHours(scale2(workHours));
            record.setProcessCoefficient(processCoef);
            record.setPersonCoefficient(personCoef);
            record.setCoefficientVersionId(version == null ? null : version.getId());
            record.setCalcMode("按系数计算");
            record.setPerformanceHoursTotal(scale2(workHours.multiply(processCoef).multiply(personCoef)));
            record.setCompletedQty(participant.getCompletedQty());
            record.setSource("系统生成");
            record.setModifiedStatus(0);
            record.setRecordStatus(1);
            record.setIsDeleted(0);
            record.setVersion(0);
            recordMapper.insert(record);
            generated++;
        }
        return generated;
    }

    private BigDecimal durationHours(WorkOrderProcessExecution execution) {
        long minutes = Duration.between(execution.getStartTime(), execution.getEndTime()).toMinutes();
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    /**
     * 按完工日期区间补生成绩效记录
     * <p>历史执行记录（PDA 结束打点、实际生产保存、未走完工锁定的旧数据）不会在完工时生成绩效，
     * 这里按结束时间扫描已完成的执行记录逐条补生成；已有记录的执行记录由
     * {@link #generateForExecution(Long)} 自动跳过，因此可重复调用。</p>
     *
     * @param from 完工日期起（可为空）
     * @param to   完工日期止（可为空）
     * @return 新生成的记录数
     */
    @Override
    public int generateForRange(LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return 0;
        }
        LambdaQueryWrapper<WorkOrderProcessExecution> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(WorkOrderProcessExecution::getStartTime)
                .isNotNull(WorkOrderProcessExecution::getEndTime)
                .eq(WorkOrderProcessExecution::getStatus, ProcessExecutionStatus.COMPLETED);
        if (from != null) {
            wrapper.ge(WorkOrderProcessExecution::getEndTime, from.atStartOfDay());
        }
        if (to != null) {
            wrapper.lt(WorkOrderProcessExecution::getEndTime, to.plusDays(1).atStartOfDay());
        }
        List<Long> executionIds = executionMapper.selectList(wrapper).stream()
                .map(WorkOrderProcessExecution::getId).toList();
        int generated = 0;
        for (Long executionId : executionIds) {
            try {
                generated += generateForExecution(executionId);
            } catch (DuplicateKeyException ex) {
                // 并发补生成时唯一索引（执行记录+人员）冲突，说明记录已存在，跳过即可
            }
        }
        return generated;
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

    private BigDecimal scale2(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    // endregion
}
