package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.PurchaseSuppliers;

import java.util.List;

/**
* @author 87954
* @description 针对表【purchase_suppliers(采购供应商信息表)】的数据库操作Service
* @createDate 2025-10-30 10:16:11
*/
public interface PurchaseSuppliersService extends IService<PurchaseSuppliers> {
    
    // #region 基础操作

    /**
     * 新增采购供应商
     *
     * @param purchaseSuppliers 采购供应商实体
     * @return 是否成功
     */
    boolean addPurchaseSupplier(PurchaseSuppliers purchaseSuppliers);

    /**
     * 更新采购供应商
     *
     * @param purchaseSuppliers 采购供应商实体
     * @return 是否成功
     */
    boolean updatePurchaseSupplier(PurchaseSuppliers purchaseSuppliers);

    /**
     * 删除采购供应商
     *
     * @param id 采购供应商ID
     * @return 是否成功
     */
    boolean deletePurchaseSupplier(Long id);

    // #endregion

    // #region 查询操作

    /**
     * 根据ID查询采购供应商
     *
     * @param id 采购供应商ID
     * @return 采购供应商实体
     */
    PurchaseSuppliers getPurchaseSupplierById(Long id);

    /**
     * 根据编号查询采购供应商
     *
     * @param supplierNumber 采购供应商编号
     * @return 采购供应商实体
     */
    PurchaseSuppliers getPurchaseSupplierByNumber(String supplierNumber);

    /**
     * 查询所有启用状态的采购供应商
     *
     * @return 采购供应商集合
     */
    List<PurchaseSuppliers> getEnabledPurchaseSuppliers();

    /**
     * 查询所有采购供应商
     *
     * @return 采购供应商集合
     */
    List<PurchaseSuppliers> getAllPurchaseSuppliers();

    /**
     * 获取采购供应商列表（分页）
     *
     * @param pageRequestDto 分页请求参数
     * @return 分页结果
     */
    PagedResult<PurchaseSuppliers> getPurchaseSupplierList(PageRequestDto pageRequestDto);

    // #endregion

    // #region 高级查询

    /**
     * 高级查询采购供应商（支持分页）
     *
     * @param purchaseSuppliers 查询条件
     * @param pageNum           页码
     * @param pageSize          每页大小
     * @return 分页结果
     */
    Page<PurchaseSuppliers> queryPurchaseSuppliers(PurchaseSuppliers purchaseSuppliers, int pageNum, int pageSize);

    // #endregion
}
