package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Purchase.PurchaseOrdersWithItemsDto;
import com.tonghui.erp.Data.Entity.PurchaseOrders;

import java.io.IOException;
import java.io.OutputStream;
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

    /**
     * 确认采购（待采购 → 运输中）
     * <p>
     * 前置校验：每条明细必须已填写供应商；确认后主单状态改为"运输中"，
     * 并自动生成一条对应验收单（状态"运输中"，等待货物验收页确认到货）
     * </p>
     *
     * @param orderId 采购订单ID
     */
    void confirmPurchase(Long orderId);

    /**
     * 作废采购订单（整单 → 已作废，终态）
     * <p>
     * 仅待采购/运输中/验收中/已结束状态可作废；已入库订单（已产生库存）与已作废订单不可作废；
     * 作废后联动将关联验收单（未入库/未作废）一并置为"已作废"
     * </p>
     *
     * @param orderId 采购订单ID
     */
    void voidPurchase(Long orderId);

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

    // region 采购数据导出
    // ===================================
    // 采购数据导出
    // ===================================

    /**
     * 导出采购数据 Excel（仅采购数据，不含领料数据）
     * <p>
     * 以采购订单明细为行（purchase_orders + purchase_order_items，关联物料主数据），
     * 输出采购订单编号、生产计划编号、工单标题、制剂名称、批量、处方倍数、物料编码、
     * 物料名称、规格、计量单位、标准处方、采购数量、单价、总价、标准量差值、
     * 采购订单状态、发票号、供应商、创建时间等列；
     * 支持按关键字、订单状态、处理日期范围筛选
     * </p>
     *
     * @param keyword   关键字（对采购订单编号、工单标题模糊匹配，可选）
     * @param status    采购订单状态（精确匹配，可选）
     * @param startDate 开始日期（yyyy-MM-dd，可选）
     * @param endDate   结束日期（yyyy-MM-dd，可选）
     * @param out       输出流（HTTP 响应流或字节流）
     * @throws IllegalArgumentException 日期格式非法或开始日期晚于结束日期
     * @throws IOException              写流失败
     */
    void exportPurchaseData(String keyword, String status,
                            String startDate, String endDate,
                            OutputStream out) throws IOException;

    // endregion
}
