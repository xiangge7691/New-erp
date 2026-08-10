package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Purchase.PurchaseOrdersWithItemsDto;
import com.tonghui.erp.Data.Entity.PurchaseOrders;

import java.util.List;

/**
 * 采购订单主表业务接口
 */
public interface PurchaseOrdersService extends IService<PurchaseOrders> {

    // region 基础操作
    // ===================================
    // 基础操作
    // ===================================

    /**
     * 生成采购订单编号（CGDH + yyyyMMdd + 4位流水号）
     * <p>替代原数据库触发器 trg_auto_generate_purchase_number 的逻辑，由后端统一生成</p>
     *
     * @return 采购订单编号
     */
    String generateOrderNumber();

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

    // endregion

    // region 查询操作
    // ===================================
    // 查询操作
    // ===================================

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

    // endregion

    // region 高级查询
    // ===================================
    // 高级查询
    // ===================================

    /**
     * 高级查询采购订单（支持分页）
     *
     * @param purchaseOrders 查询条件
     * @param keyword        关键字（对采购编号、采购标题进行模糊匹配，可选）
     * @param processingDateStart 处理开始日期（可选，格式：yyyy-MM-dd）
     * @param processingDateEnd 处理结束日期（可选，格式：yyyy-MM-dd）
     * @param desiredDeliveryDateStart 期望到货开始日期（可选，格式：yyyy-MM-dd）
     * @param desiredDeliveryDateEnd 期望到货结束日期（可选，格式：yyyy-MM-dd）
     * @param expectedDeliveryDateStart 预计到货开始日期（可选，格式：yyyy-MM-dd）
     * @param expectedDeliveryDateEnd 预计到货结束日期（可选，格式：yyyy-MM-dd）
     * @param pageNum        页码
     * @param pageSize       每页大小
     * @return 分页结果
     */
    Page<PurchaseOrders> queryPurchaseOrders(PurchaseOrders purchaseOrders, String keyword, 
            String processingDateStart, String processingDateEnd, 
            String desiredDeliveryDateStart, String desiredDeliveryDateEnd,
            String expectedDeliveryDateStart, String expectedDeliveryDateEnd,
            int pageNum, int pageSize);

    // endregion

    // region 带子表查询
    // ===================================
    // 带子表查询
    // ===================================

    /**
     * 高级查询采购订单（包含明细子表）
     *
     * @param purchaseOrders 查询条件
     * @param keyword        关键字（对采购编号、采购标题进行模糊匹配，可选）
     * @param processingDateStart 处理开始日期（可选，格式：yyyy-MM-dd）
     * @param processingDateEnd 处理结束日期（可选，格式：yyyy-MM-dd）
     * @param desiredDeliveryDateStart 期望到货开始日期（可选，格式：yyyy-MM-dd）
     * @param desiredDeliveryDateEnd 期望到货结束日期（可选，格式：yyyy-MM-dd）
     * @param expectedDeliveryDateStart 预计到货开始日期（可选，格式：yyyy-MM-dd）
     * @param expectedDeliveryDateEnd 预计到货结束日期（可选，格式：yyyy-MM-dd）
     * @param pageNum        页码
     * @param pageSize       每页大小
     * @return 分页结果（包含明细）
     */
    PagedResult<PurchaseOrdersWithItemsDto> searchWithDetails(PurchaseOrders purchaseOrders, String keyword,
            String processingDateStart, String processingDateEnd,
            String desiredDeliveryDateStart, String desiredDeliveryDateEnd,
            String expectedDeliveryDateStart, String expectedDeliveryDateEnd,
            int pageNum, int pageSize);

    // endregion
}
