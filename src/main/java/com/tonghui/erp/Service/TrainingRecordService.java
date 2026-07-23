package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Data.Entity.TrainingRecord;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * 培训记录服务接口
 * <p>
 * 提供培训记录的CRUD操作、查询、编号生成、周期计算及到期提醒功能
 * </p>
 */
public interface TrainingRecordService extends IService<TrainingRecord> {

    // region 基础操作
    // ===================================
    // 基础操作
    // ===================================

    /**
     * 新增培训记录
     *
     * @param trainingRecord 培训记录实体
     * @return 是否成功
     */
    boolean addTrainingRecord(TrainingRecord trainingRecord);

    /**
     * 更新培训记录
     *
     * @param trainingRecord 培训记录实体
     * @return 是否成功
     */
    boolean updateTrainingRecord(TrainingRecord trainingRecord);

    /**
     * 删除培训记录
     *
     * @param id 培训记录ID
     * @return 是否成功
     */
    boolean deleteTrainingRecord(Long id);

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
    TrainingRecord getTrainingRecordById(Long id);

    /**
     * 根据培训编号查询培训记录
     *
     * @param trainingNo 培训编号
     * @return 培训记录实体
     */
    TrainingRecord getTrainingRecordByNo(String trainingNo);

    /**
     * 获取培训记录列表（分页）
     *
     * @param pageIndex 页码
     * @param pageSize  每页大小
     * @return 分页结果
     */
    Page<TrainingRecord> getTrainingRecordList(int pageIndex, int pageSize);

    // endregion

    // region 高级查询
    // ===================================
    // 高级查询
    // ===================================

    /**
     * 高级查询培训记录（支持多条件 + 分页）
     *
     * @param trainingRecord    查询条件
     * @param trainingDateStart 培训日期开始
     * @param trainingDateEnd   培训日期结束
     * @param pageIndex         页码
     * @param pageSize          每页大小
     * @return 分页结果
     */
    Page<TrainingRecord> queryTrainingRecords(TrainingRecord trainingRecord,
                                               LocalDateTime trainingDateStart, LocalDateTime trainingDateEnd,
                                               int pageIndex, int pageSize);

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
    String generateTrainingNo();

    /**
     * 计算下次培训日期
     *
     * @param trainingDate 培训日期
     * @param cycleMonths  培训周期（月）
     * @return 下次培训日期
     */
    Date calculateNextTrainingDate(Date trainingDate, Integer cycleMonths);

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
    List<TrainingRecord> getExpiringTrainings(int days);

    /**
     * 更新预警状态
     *
     * @param id     培训记录ID
     * @param status 预警状态（0未处理/1已处理/2不再提醒）
     * @return 是否成功
     */
    boolean updateReminderStatus(Long id, Integer status);

    // endregion
}
