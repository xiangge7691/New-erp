package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.TrainingRecord;
import com.tonghui.erp.Data.mapper.TrainingRecordMapper;
import com.tonghui.erp.Service.TrainingRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 培训记录服务实现类
 * <p>
 * 实现TrainingRecordService接口，提供培训记录相关的业务逻辑处理，
 * 包括增删改查、高级查询、编号生成、周期计算及到期提醒等功能
 * </p>
 */
@Service
public class TrainingRecordServiceImpl extends ServiceImpl<TrainingRecordMapper, TrainingRecord>
        implements TrainingRecordService {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    @Autowired
    private TrainingRecordMapper trainingRecordMapper;

    // endregion

    // region 基础CRUD操作
    // ===================================
    // 基础CRUD操作
    // ===================================

    /**
     * 新增培训记录
     *
     * @param trainingRecord 培训记录实体
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean addTrainingRecord(TrainingRecord trainingRecord) {
        // 自动生成培训编号
        if (!StringUtils.hasText(trainingRecord.getTrainingNo())) {
            trainingRecord.setTrainingNo(generateTrainingNo());
        } else {
            // 检查编号是否已存在
            TrainingRecord existing = getTrainingRecordByNo(trainingRecord.getTrainingNo());
            if (existing != null) {
                throw new RuntimeException("培训编号已存在：" + trainingRecord.getTrainingNo());
            }
        }

        // 计算下次培训日期
        if (trainingRecord.getTrainingDate() != null && trainingRecord.getTrainingCycle() != null) {
            trainingRecord.setNextTrainingDate(
                    calculateNextTrainingDate(trainingRecord.getTrainingDate(), trainingRecord.getTrainingCycle())
            );
        }

        return this.save(trainingRecord);
    }

    /**
     * 更新培训记录
     *
     * @param trainingRecord 培训记录实体
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean updateTrainingRecord(TrainingRecord trainingRecord) {
        // 重新计算下次培训日期
        if (trainingRecord.getTrainingDate() != null && trainingRecord.getTrainingCycle() != null) {
            trainingRecord.setNextTrainingDate(
                    calculateNextTrainingDate(trainingRecord.getTrainingDate(), trainingRecord.getTrainingCycle())
            );
        }

        return this.updateById(trainingRecord);
    }

    /**
     * 删除培训记录（软删除）
     *
     * @param id 培训记录ID
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean deleteTrainingRecord(Long id) {
        return this.removeById(id);
    }

    // endregion

    // region 查询操作
    // ===================================
    // 查询操作
    // ===================================

    /**
     * 根据ID查询培训记录
     *
     * @param id 培训记录ID
     * @return 培训记录实体
     */
    @Override
    public TrainingRecord getTrainingRecordById(Long id) {
        return this.getById(id);
    }

    /**
     * 根据培训编号查询培训记录
     *
     * @param trainingNo 培训编号
     * @return 培训记录实体
     */
    @Override
    public TrainingRecord getTrainingRecordByNo(String trainingNo) {
        QueryWrapper<TrainingRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("training_no", trainingNo);
        wrapper.eq("is_deleted", 0);
        return this.getOne(wrapper);
    }

    /**
     * 获取培训记录列表（分页）
     *
     * @param pageIndex 页码
     * @param pageSize  每页大小
     * @return 分页结果
     */
    @Override
    public Page<TrainingRecord> getTrainingRecordList(int pageIndex, int pageSize) {
        Page<TrainingRecord> page = new Page<>(pageIndex, pageSize);
        QueryWrapper<TrainingRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("is_deleted", 0);
        wrapper.orderByDesc("training_date");
        return this.page(page, wrapper);
    }

    // endregion

    // region 高级查询
    // ===================================
    // 高级查询
    // ===================================

    /**
     * 高级查询培训记录（支持多条件 + 分页）
     *
     * @param trainingRecord 查询条件
     * @param pageIndex      页码
     * @param pageSize       每页大小
     * @return 分页结果
     */
    @Override
    public Page<TrainingRecord> queryTrainingRecords(TrainingRecord trainingRecord, int pageIndex, int pageSize) {
        Page<TrainingRecord> page = new Page<>(pageIndex, pageSize);
        QueryWrapper<TrainingRecord> wrapper = new QueryWrapper<>();

        // 未删除条件
        wrapper.eq("is_deleted", 0);

        // 培训名称模糊查询
        if (StringUtils.hasText(trainingRecord.getTrainingName())) {
            wrapper.like("training_name", trainingRecord.getTrainingName());
        }

        // 培训类别精确匹配
        if (StringUtils.hasText(trainingRecord.getTrainingCategory())) {
            wrapper.eq("training_category", trainingRecord.getTrainingCategory());
        }

        // 培训形式精确匹配
        if (StringUtils.hasText(trainingRecord.getTrainingForm())) {
            wrapper.eq("training_form", trainingRecord.getTrainingForm());
        }

        // 培训日期范围查询
        if (trainingRecord.getTrainingDate() != null) {
            wrapper.ge("training_date", trainingRecord.getTrainingDate());
        }

        // 按培训日期降序排列
        wrapper.orderByDesc("training_date");

        return this.page(page, wrapper);
    }

    // endregion

    // region 编号生成与周期计算
    // ===================================
    // 编号生成与周期计算
    // ===================================

    /**
     * 生成培训编号
     * <p>格式：TRAIN-年-序号（如TRAIN-2026-001）</p>
     *
     * @return 培训编号
     */
    @Override
    public String generateTrainingNo() {
        SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy");
        String year = yearFormat.format(new Date());
        String prefix = "TRAIN-" + year + "-";

        // 查询当年最大序号
        Integer maxSeq = trainingRecordMapper.getMaxSeqByYear(year);
        int nextSeq = (maxSeq == null) ? 1 : maxSeq + 1;

        return prefix + String.format("%03d", nextSeq);
    }

    /**
     * 计算下次培训日期
     *
     * @param trainingDate 培训日期
     * @param cycleMonths  培训周期（月）
     * @return 下次培训日期
     */
    @Override
    public Date calculateNextTrainingDate(Date trainingDate, Integer cycleMonths) {
        if (trainingDate == null || cycleMonths == null) {
            return null;
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(trainingDate);
        cal.add(Calendar.MONTH, cycleMonths);
        return cal.getTime();
    }

    // endregion

    // region 到期提醒
    // ===================================
    // 到期提醒
    // ===================================

    /**
     * 查询即将到期的培训记录
     *
     * @param days 提前天数
     * @return 即将到期的培训记录列表
     */
    @Override
    public List<TrainingRecord> getExpiringTrainings(int days) {
        Date today = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(today);
        cal.add(Calendar.DAY_OF_MONTH, days);
        Date warningDate = cal.getTime();

        return trainingRecordMapper.selectExpiringTrainings(warningDate);
    }

    /**
     * 更新预警状态
     *
     * @param id     培训记录ID
     * @param status 预警状态（0未处理/1已处理/2不再提醒）
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean updateReminderStatus(Long id, Integer status) {
        TrainingRecord trainingRecord = this.getById(id);
        if (trainingRecord == null) {
            return false;
        }
        trainingRecord.setReminderStatus(status);
        return this.updateById(trainingRecord);
    }

    // endregion
}
