package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.SupplierAudit;
import com.tonghui.erp.Data.mapper.SupplierAuditMapper;
import com.tonghui.erp.Service.SupplierAuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 供应商审核记录服务实现类
 * <p>
 * 实现SupplierAuditService接口，提供供应商审核记录相关的业务逻辑处理，
 * 包括增删改查、高级查询、周期计算及到期提醒等功能
 * </p>
 */
@Service
public class SupplierAuditServiceImpl extends ServiceImpl<SupplierAuditMapper, SupplierAudit>
        implements SupplierAuditService {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    @Autowired
    private SupplierAuditMapper supplierAuditMapper;

    // endregion

    // region 基础CRUD操作
    // ===================================
    // 基础CRUD操作
    // ===================================

    /**
     * 新增供应商审核记录
     *
     * @param supplierAudit 供应商审核记录实体
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean addSupplierAudit(SupplierAudit supplierAudit) {
        // 计算下次审核日期
        if (supplierAudit.getAuditDate() != null && supplierAudit.getAuditCycle() != null) {
            supplierAudit.setNextAuditDate(
                    calculateNextAuditDate(supplierAudit.getAuditDate(), supplierAudit.getAuditCycle())
            );
        }

        return this.save(supplierAudit);
    }

    /**
     * 更新供应商审核记录
     *
     * @param supplierAudit 供应商审核记录实体
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean updateSupplierAudit(SupplierAudit supplierAudit) {
        // 重新计算下次审核日期
        if (supplierAudit.getAuditDate() != null && supplierAudit.getAuditCycle() != null) {
            supplierAudit.setNextAuditDate(
                    calculateNextAuditDate(supplierAudit.getAuditDate(), supplierAudit.getAuditCycle())
            );
        }

        return this.updateById(supplierAudit);
    }

    /**
     * 删除供应商审核记录（软删除）
     *
     * @param id 供应商审核记录ID
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean deleteSupplierAudit(Long id) {
        SupplierAudit supplierAudit = this.getById(id);
        if (supplierAudit != null) {
            supplierAudit.setIsDeleted(1);
            return this.updateById(supplierAudit);
        }
        return false;
    }

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
    @Override
    public SupplierAudit getSupplierAuditById(Long id) {
        return this.getById(id);
    }

    /**
     * 获取供应商审核记录列表（分页）
     *
     * @param pageIndex 页码
     * @param pageSize  每页大小
     * @return 分页结果
     */
    @Override
    public Page<SupplierAudit> getSupplierAuditList(int pageIndex, int pageSize) {
        Page<SupplierAudit> page = new Page<>(pageIndex, pageSize);
        QueryWrapper<SupplierAudit> wrapper = new QueryWrapper<>();
        wrapper.eq("is_deleted", 0);
        wrapper.orderByDesc("audit_date");
        return this.page(page, wrapper);
    }

    // endregion

    // region 高级查询
    // ===================================
    // 高级查询
    // ===================================

    /**
     * 高级查询供应商审核记录（支持多条件 + 分页）
     *
     * @param supplierAudit 查询条件
     * @param pageIndex     页码
     * @param pageSize      每页大小
     * @return 分页结果
     */
    @Override
    public Page<SupplierAudit> querySupplierAudits(SupplierAudit supplierAudit, int pageIndex, int pageSize) {
        Page<SupplierAudit> page = new Page<>(pageIndex, pageSize);
        QueryWrapper<SupplierAudit> wrapper = new QueryWrapper<>();

        // 未删除条件
        wrapper.eq("is_deleted", 0);

        // 供应商ID精确匹配
        if (supplierAudit.getSupplierId() != null) {
            wrapper.eq("supplier_id", supplierAudit.getSupplierId());
        }

        // 供应类型精确匹配
        if (StringUtils.hasText(supplierAudit.getSupplyType())) {
            wrapper.eq("supply_type", supplierAudit.getSupplyType());
        }

        // 审核结果精确匹配
        if (StringUtils.hasText(supplierAudit.getAuditResult())) {
            wrapper.eq("audit_result", supplierAudit.getAuditResult());
        }

        // 审核日期范围查询
        if (supplierAudit.getAuditDate() != null) {
            wrapper.ge("audit_date", supplierAudit.getAuditDate());
        }

        // 按审核日期降序排列
        wrapper.orderByDesc("audit_date");

        return this.page(page, wrapper);
    }

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
    @Override
    public Date calculateNextAuditDate(Date auditDate, Integer cycleMonths) {
        if (auditDate == null || cycleMonths == null) {
            return null;
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(auditDate);
        cal.add(Calendar.MONTH, cycleMonths);
        return cal.getTime();
    }

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
    @Override
    public List<SupplierAudit> getExpiringAudits(int days) {
        Date today = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(today);
        cal.add(Calendar.DAY_OF_MONTH, days);
        Date warningDate = cal.getTime();

        return supplierAuditMapper.selectExpiringAudits(today, warningDate);
    }

    // endregion
}
