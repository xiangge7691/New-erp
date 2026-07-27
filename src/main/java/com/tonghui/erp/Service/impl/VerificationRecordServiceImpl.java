package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.VerificationRecord;
import com.tonghui.erp.Data.mapper.VerificationRecordMapper;
import com.tonghui.erp.Service.VerificationRecordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 验证记录服务实现类
 * <p>
 * 实现VerificationRecordService接口，提供验证记录相关的业务逻辑处理，
 * 包括增删改查、高级查询、到期提醒等功能
 * </p>
 */
@Service
public class VerificationRecordServiceImpl extends ServiceImpl<VerificationRecordMapper, VerificationRecord>
        implements VerificationRecordService {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    // endregion

    // region 数据清理接口
    // ===================================
    // 数据清理接口
    // ===================================

    /**
     * 清理指定验证编号下已被软删除的记录（释放唯一键约束）
     *
     * @param verificationNo 验证编号
     * @return 清理的记录数
     */
    public int cleanSoftDeletedByVerificationNo(String verificationNo) {
        return baseMapper.physicalDeleteByVerificationNo(verificationNo);
    }

    // endregion

    // region 基础CRUD操作
    // ===================================
    // 基础CRUD操作
    // ===================================

    /**
     * 新增验证记录
     *
     * @param verificationRecord 验证记录实体
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean addVerificationRecord(VerificationRecord verificationRecord) {
        // 如果验证编号不为空，清理已软删除的相同编号记录（避免唯一键冲突）
        if (verificationRecord.getVerificationNo() != null && !verificationRecord.getVerificationNo().isEmpty()) {
            cleanSoftDeletedByVerificationNo(verificationRecord.getVerificationNo());
        }
        return this.save(verificationRecord);
    }

    /**
     * 更新验证记录
     *
     * @param verificationRecord 验证记录实体
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean updateVerificationRecord(VerificationRecord verificationRecord) {
        return this.updateById(verificationRecord);
    }

    /**
     * 删除验证记录（软删除）
     *
     * @param id 验证记录ID
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean deleteVerificationRecord(Long id) {
        return this.removeById(id);
    }

    // endregion

    // region 查询操作
    // ===================================
    // 查询操作
    // ===================================

    /**
     * 根据ID查询验证记录
     *
     * @param id 验证记录ID
     * @return 验证记录实体
     */
    @Override
    public VerificationRecord getVerificationRecordById(Long id) {
        return this.getById(id);
    }

    /**
     * 获取验证记录列表（分页）
     *
     * @param pageIndex 页码
     * @param pageSize  每页大小
     * @return 分页结果
     */
    @Override
    public Page<VerificationRecord> getVerificationRecordList(int pageIndex, int pageSize) {
        Page<VerificationRecord> page = new Page<>(pageIndex, pageSize);
        QueryWrapper<VerificationRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("is_deleted", 0);
        wrapper.orderByDesc("execute_date");
        return this.page(page, wrapper);
    }

    // endregion

    // region 高级查询
    // ===================================
    // 高级查询
    // ===================================

    /**
     * 高级查询验证记录（支持多条件 + 分页）
     *
     * @param verificationRecord 查询条件
     * @param executeDateStart   执行日期开始
     * @param executeDateEnd     执行日期结束
     * @param nextVerifyDateStart 下次验证日期开始
     * @param nextVerifyDateEnd   下次验证日期结束
     * @param createdTimeStart   创建时间开始
     * @param createdTimeEnd     创建时间结束
     * @param updatedTimeStart   更新时间开始
     * @param updatedTimeEnd     更新时间结束
     * @param pageIndex          页码
     * @param pageSize           每页大小
     * @return 分页结果
     */
    @Override
    public Page<VerificationRecord> queryVerificationRecords(VerificationRecord verificationRecord,
                                                              LocalDateTime executeDateStart, LocalDateTime executeDateEnd,
                                                              LocalDateTime nextVerifyDateStart, LocalDateTime nextVerifyDateEnd,
                                                              LocalDateTime createdTimeStart, LocalDateTime createdTimeEnd,
                                                              LocalDateTime updatedTimeStart, LocalDateTime updatedTimeEnd,
                                                              int pageIndex, int pageSize) {
        Page<VerificationRecord> page = new Page<>(pageIndex, pageSize);
        QueryWrapper<VerificationRecord> wrapper = new QueryWrapper<>();

        wrapper.eq("is_deleted", 0);

        if (StringUtils.hasText(verificationRecord.getCategory())) {
            wrapper.eq("category", verificationRecord.getCategory());
        }

        if (StringUtils.hasText(verificationRecord.getVerificationNo())) {
            wrapper.like("verification_no", verificationRecord.getVerificationNo());
        }

        if (StringUtils.hasText(verificationRecord.getVerificationName())) {
            wrapper.like("verification_name", verificationRecord.getVerificationName());
        }

        if (StringUtils.hasText(verificationRecord.getRelatedObject())) {
            wrapper.like("related_object", verificationRecord.getRelatedObject());
        }

        if (StringUtils.hasText(verificationRecord.getExecutor())) {
            wrapper.like("executor", verificationRecord.getExecutor());
        }

        if (StringUtils.hasText(verificationRecord.getAuditor())) {
            wrapper.like("auditor", verificationRecord.getAuditor());
        }

        // 执行日期范围查询
        if (executeDateStart != null) {
            wrapper.ge("execute_date", executeDateStart);
        }
        if (executeDateEnd != null) {
            wrapper.le("execute_date", executeDateEnd);
        }

        // 下次验证日期范围查询
        if (nextVerifyDateStart != null) {
            wrapper.ge("next_verify_date", nextVerifyDateStart);
        }
        if (nextVerifyDateEnd != null) {
            wrapper.le("next_verify_date", nextVerifyDateEnd);
        }

        // 创建时间范围查询
        if (createdTimeStart != null) {
            wrapper.ge("created_time", createdTimeStart);
        }
        if (createdTimeEnd != null) {
            wrapper.le("created_time", createdTimeEnd);
        }

        // 更新时间范围查询
        if (updatedTimeStart != null) {
            wrapper.ge("updated_time", updatedTimeStart);
        }
        if (updatedTimeEnd != null) {
            wrapper.le("updated_time", updatedTimeEnd);
        }

        wrapper.orderByDesc("execute_date");

        return this.page(page, wrapper);
    }

    // endregion

    // region 到期提醒
    // ===================================
    // 到期提醒
    // ===================================

    /**
     * 查询即将到期的验证记录
     *
     * @param days 提前天数
     * @return 到期的验证记录列表
     */
    @Override
    public List<VerificationRecord> getExpiringVerifications(int days) {
        Date today = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(today);
        cal.add(Calendar.DAY_OF_MONTH, days);
        Date warningDate = cal.getTime();

        return baseMapper.selectExpiringVerifications(warningDate);
    }

    /**
     * 更新预警状态
     *
     * @param id     验证记录ID
     * @param status 预警状态（0未处理/1已处理/2不再提醒）
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean updateReminderStatus(Long id, Integer status) {
        VerificationRecord record = this.getById(id);
        if (record == null) {
            return false;
        }
        record.setReminderStatus(status);
        return this.updateById(record);
    }

    // endregion
}
