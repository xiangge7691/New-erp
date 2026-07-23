package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Data.Entity.VerificationRecord;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 验证记录服务接口
 * <p>
 * 提供验证记录的CRUD操作、高级查询、到期提醒等功能
 * </p>
 */
public interface VerificationRecordService extends IService<VerificationRecord> {

    // region 基础操作
    // ===================================
    // 基础操作
    // ===================================

    /**
     * 新增验证记录
     *
     * @param verificationRecord 验证记录实体
     * @return 是否成功
     */
    boolean addVerificationRecord(VerificationRecord verificationRecord);

    /**
     * 更新验证记录
     *
     * @param verificationRecord 验证记录实体
     * @return 是否成功
     */
    boolean updateVerificationRecord(VerificationRecord verificationRecord);

    /**
     * 删除验证记录（软删除）
     *
     * @param id 验证记录ID
     * @return 是否成功
     */
    boolean deleteVerificationRecord(Long id);

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
    VerificationRecord getVerificationRecordById(Long id);

    /**
     * 获取验证记录列表（分页）
     *
     * @param pageIndex 页码
     * @param pageSize  每页大小
     * @return 分页结果
     */
    Page<VerificationRecord> getVerificationRecordList(int pageIndex, int pageSize);

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
    Page<VerificationRecord> queryVerificationRecords(VerificationRecord verificationRecord,
                                                       LocalDateTime executeDateStart, LocalDateTime executeDateEnd,
                                                       LocalDateTime nextVerifyDateStart, LocalDateTime nextVerifyDateEnd,
                                                       LocalDateTime createdTimeStart, LocalDateTime createdTimeEnd,
                                                       LocalDateTime updatedTimeStart, LocalDateTime updatedTimeEnd,
                                                       int pageIndex, int pageSize);

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
    List<VerificationRecord> getExpiringVerifications(int days);

    /**
     * 更新预警状态
     *
     * @param id     验证记录ID
     * @param status 预警状态（0未处理/1已处理/2不再提醒）
     * @return 是否成功
     */
    boolean updateReminderStatus(Long id, Integer status);

    // endregion
}
