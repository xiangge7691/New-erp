package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.SupplierAudit;
import com.tonghui.erp.Service.SupplierAuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 供应商审核记录控制器
 * <p>
 * 提供供应商审核记录的CRUD操作、高级查询及到期提醒等功能
 * </p>
 *
 * 接口清单：
 * ┌────┬────────────────────────────────────┬────────┬──────────────────────────────┐
 * │ #  │ 接口                               │ 方法   │ 说明                         │
 * ├────┼────────────────────────────────────┼────────┼──────────────────────────────┤
 * │ 1  │ /api/supplier-audit                │ GET    │ 分页查询审核记录列表         │
 * │ 2  │ /api/supplier-audit/{id}           │ GET    │ 获取审核记录详情             │
 * │ 3  │ /api/supplier-audit                │ POST   │ 新增审核记录                 │
 * │ 4  │ /api/supplier-audit/{id}           │ PUT    │ 修改审核记录                 │
 * │ 5  │ /api/supplier-audit/{id}           │ DELETE │ 删除审核记录                 │
 * │ 6  │ /api/supplier-audit/search         │ GET    │ 高级查询审核记录             │
 * │ 7  │ /api/supplier-audit/warning        │ GET    │ 到期提醒查询                 │
 * └────┴────────────────────────────────────┴────────┴──────────────────────────────┘
 */
@RestController
@RequestMapping("/api/supplier-audit")
public class SupplierAuditController extends BaseCrudController<SupplierAudit, SupplierAudit, Long> {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 供应商审核记录服务
     */
    @Autowired
    private SupplierAuditService supplierAuditService;

    // endregion

    // region CRUD操作实现方法
    // ===================================
    // CRUD操作实现方法
    // ===================================

    /**
     * 获取所有供应商审核记录数据（分页）
     *
     * @param pageIndex 页码
     * @param pageSize  每页大小
     * @return 分页结果
     */
    @Override
    protected PagedResult<SupplierAudit> getAllData(int pageIndex, int pageSize) {
        int safePageIndex = Math.max(0, pageIndex);
        int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);

        Page<SupplierAudit> pageResult = supplierAuditService.getSupplierAuditList(safePageIndex, safePageSize);

        PagedResult<SupplierAudit> pagedResult = new PagedResult<>();
        pagedResult.setItems(pageResult.getRecords());
        pagedResult.setTotalCount(pageResult.getTotal());
        pagedResult.setPageIndex(safePageIndex);
        pagedResult.setPageSize((int) pageResult.getSize());

        return pagedResult;
    }

    /**
     * 根据ID获取供应商审核记录
     *
     * @param id 供应商审核记录ID
     * @return 供应商审核记录实体
     */
    @Override
    protected SupplierAudit getDataById(Long id) {
        return supplierAuditService.getSupplierAuditById(id);
    }

    /**
     * 创建供应商审核记录
     *
     * @param supplierAudit 供应商审核记录信息
     * @return 创建后的供应商审核记录
     */
    @Override
    protected SupplierAudit doCreate(SupplierAudit supplierAudit) {
        supplierAuditService.addSupplierAudit(supplierAudit);
        return supplierAudit;
    }

    /**
     * 更新供应商审核记录
     *
     * @param id             供应商审核记录ID
     * @param supplierAudit  供应商审核记录信息
     * @return 更新后的供应商审核记录
     */
    @Override
    protected SupplierAudit doUpdate(Long id, SupplierAudit supplierAudit) {
        supplierAudit.setId(id);
        supplierAuditService.updateSupplierAudit(supplierAudit);
        return supplierAudit;
    }

    /**
     * 删除供应商审核记录
     *
     * @param id 供应商审核记录ID
     * @return 是否删除成功
     */
    @Override
    protected boolean doDelete(Long id) {
        return supplierAuditService.deleteSupplierAudit(id);
    }

    // endregion

    // region 高级查询
    // ===================================
    // 高级查询
    // ===================================

    /**
     * 高级查询供应商审核记录（支持多条件 + 分页）
     *
     * 可选查询条件：
     * - supplierId：供应商ID
     * - supplyType：供应类型
     * - auditResult：审核结果
     * - auditDate：审核日期（范围查询，查询大于等于该日期的记录）
     *
     * 示例请求：
     * GET /api/supplier-audit/search?pageIndex=1&pageSize=20&supplierId=1&supplyType=包材&auditResult=合格
     *
     * @param supplierAudit 查询条件（自动从query参数映射）
     * @param pageIndex     页码
     * @param pageSize      每页大小
     * @return 分页结果
     */
    @GetMapping("/search")
    public ApiResponse<PagedResult<SupplierAudit>> querySupplierAudits(SupplierAudit supplierAudit,
                                                                       @RequestParam int pageIndex,
                                                                       @RequestParam int pageSize) {
        try {
            int safePageIndex = Math.max(0, pageIndex);
            int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);

            Page<SupplierAudit> pageResult = supplierAuditService.querySupplierAudits(supplierAudit, safePageIndex, safePageSize);

            PagedResult<SupplierAudit> pagedResult = new PagedResult<>();
            pagedResult.setItems(pageResult.getRecords());
            pagedResult.setTotalCount(pageResult.getTotal());
            pagedResult.setPageIndex(safePageIndex);
            pagedResult.setPageSize((int) pageResult.getSize());

            return success(pagedResult);
        } catch (Exception ex) {
            return exception(ex, "查询审核记录");
        }
    }

    // endregion

    // region 到期提醒
    // ===================================
    // 到期提醒
    // ===================================

    /**
     * 查询即将到期的审核记录
     *
     * 示例请求：
     * GET /api/supplier-audit/warning?days=30
     *
     * @param days 提前天数，默认30天
     * @return 即将到期的审核记录列表
     */
    @GetMapping("/warning")
    public ApiResponse<List<SupplierAudit>> getExpiringAudits(
            @RequestParam(value = "days", defaultValue = "30") int days) {
        try {
            List<SupplierAudit> expiringList = supplierAuditService.getExpiringAudits(days);
            return success(expiringList);
        } catch (Exception ex) {
            return exception(ex, "查询到期审核");
        }
    }

    // endregion
}
