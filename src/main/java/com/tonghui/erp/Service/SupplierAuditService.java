package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Data.Entity.SupplierAudit;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * 供应商审核记录服务接口
 * <p>
 * 提供供应商审核记录的CRUD操作、查询、周期计算及到期提醒功能
 * </p>
 */
public interface SupplierAuditService extends IService<SupplierAudit> {

    // region 基础操作
    // ===================================
    // 基础操作
    // ===================================

    /**
     * 新增供应商审核记录
     *
     * @param supplierAudit 供应商审核记录实体
     * @return 是否成功
     */
    boolean addSupplierAudit(SupplierAudit supplierAudit);

    /**
     * 更新供应商审核记录
     *
     * @param supplierAudit 供应商审核记录实体
     * @return 是否成功
     */
    boolean updateSupplierAudit(SupplierAudit supplierAudit);

    /**
     * 删除供应商审核记录
     *
     * @param id 供应商审核记录ID
     * @return 是否成功
     */
    boolean deleteSupplierAudit(Long id);

    // endregion

    // region 查询操作
    // ===================================
    // 查询操作
    // ===================================

    /**
     * 根据ID查询供应商审核记录
     *
     * @param id 供应商审核记录ID
     * @return 供应商审核记录实体
     */
    SupplierAudit getSupplierAuditById(Long id);

    /**
     * 获取供应商审核记录列表（分页）
     *
     * @param pageIndex 页码
     * @param pageSize  每页大小
     * @return 分页结果
     */
    Page<SupplierAudit> getSupplierAuditList(int pageIndex, int pageSize);

    // endregion

    // region 高级查询
    // ===================================
    // 高级查询
    // ===================================

    /**
     * 高级查询供应商审核记录（支持多条件 + 分页）
     *
     * @param supplierAudit  查询条件
     * @param auditDateStart 审核日期开始
     * @param auditDateEnd   审核日期结束
     * @param pageIndex      页码
     * @param pageSize       每页大小
     * @return 分页结果
     */
    Page<SupplierAudit> querySupplierAudits(SupplierAudit supplierAudit,
                                             LocalDateTime auditDateStart, LocalDateTime auditDateEnd,
                                             int pageIndex, int pageSize);

    // endregion

    // region 周期计算
    // ===================================
    // 周期计算
    // ===================================

    /**
     * 计算下次审核日期
     *
     * @param auditDate   审核日期
     * @param cycleMonths 审核周期（月）
     * @return 下次审核日期
     */
    Date calculateNextAuditDate(Date auditDate, Integer cycleMonths);

    // endregion

    // region 到期提醒
    // ===================================
    // 到期提醒
    // ===================================

    /**
     * 查询即将到期的审核记录
     *
     * @param days 提前天数
     * @return 即将到期的审核记录列表
     */
    List<SupplierAudit> getExpiringAudits(int days);

    // endregion
}
