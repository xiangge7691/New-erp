package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.PurchaseOrders;

import java.util.List;

/**
 * 采购订单主表业务接口
 */
public interface PurchaseOrdersService extends IService<PurchaseOrders> {

    // #region 基础操作

    /**
     * 新增采购订单
     *
     * @param purchaseOrders 采购订单实体
     * @return 是否成功
     */
    boolean addPurchaseOrder(PurchaseOrders purchaseOrders);

    /**
     * 更新采购订单
     *
     * @param purchaseOrders 采购订单实体
     * @return 是否成功
     */
    boolean updatePurchaseOrder(PurchaseOrders purchaseOrders);

    /**
     * 删除采购订单
     *
     * @param orderId 采购订单ID
     * @return 是否成功
     */
    boolean deletePurchaseOrder(Long orderId);

    // #endregion

    // #region 查询操作

    /**
     * 根据ID查询采购订单
     *
     * @param orderId 采购订单ID
     * @return 采购订单实体
     */
    PurchaseOrders getPurchaseOrderById(Long orderId);

    /**
     * 获取采购订单列表（分页）
     *
     * @param pageRequestDto 分页请求参数
     * @return 分页结果
     */
    PagedResult<PurchaseOrders> getPurchaseOrderList(PageRequestDto pageRequestDto);

    // #endregion

    // #region 高级查询

    /**
     * 高级查询采购订单（支持分页）
     *
     * @param purchaseOrders 查询条件
     * @param pageNum        页码
     * @param pageSize       每页大小
     * @return 分页结果
     */
    Page<PurchaseOrders> queryPurchaseOrders(PurchaseOrders purchaseOrders, int pageNum, int pageSize);

    // #endregion
}
