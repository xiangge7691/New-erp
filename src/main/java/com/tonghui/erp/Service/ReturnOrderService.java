package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Common.Dto.Warehouse.AvailableOutOrderDto;
import com.tonghui.erp.Common.Dto.Warehouse.OutOrderMaterialDto;
import com.tonghui.erp.Common.Dto.Warehouse.ReturnOrderCreateDto;
import com.tonghui.erp.Common.Dto.Warehouse.ReturnOrderDetailDto;
import com.tonghui.erp.Data.Entity.ReturnOrder;

import java.util.List;

/**
 * 退库单服务接口
 * <p>
 * 提供退库单分页查询、可退库出库单列表、出库单物料明细（含可退数量）查询，
 * 以及新增退库（校验可退额度、回增库存并生成退库流水）等业务功能
 * </p>
 */
public interface ReturnOrderService extends IService<ReturnOrder> {

    /**
     * 分页查询退库单列表
     *
     * @param keyword   搜索关键词（退库单号/出库单号/物料名称，可选）
     * @param pageIndex 页码（从0开始）
     * @param pageSize  每页数量
     * @return 分页结果（主表信息列表）
     */
    Page<ReturnOrder> queryReturnOrders(String keyword, int pageIndex, int pageSize);

    /**
     * 获取可退库的出库单列表
     * <p>
     * 仅展示"生产领料出库"且仍有可退额度的出库单
     * </p>
     *
     * @return 可退库出库单列表（含可退总量）
     */
    List<AvailableOutOrderDto> getAvailableOutOrders();

    /**
     * 获取出库单物料明细（含可退数量）
     *
     * @param outOrderNo 出库单号
     * @return 物料明细列表（含出库数量、已退数量、可退数量）
     */
    List<OutOrderMaterialDto> getOutOrderMaterials(String outOrderNo);

    /**
     * 新增退库单
     * <p>
     * 校验退库数量不超过可退数量后回增库存（无同批次库存则新增库存记录），
     * 生成退库流水，并保存退库单主表与明细
     * </p>
     *
     * @param dto 退库请求（出库单号+明细）
     * @return 创建后的退库单（含单号与汇总数据）
     */
    ReturnOrder createReturnOrder(ReturnOrderCreateDto dto);

    /**
     * 查询退库单详情（含明细）
     *
     * @param id 退库单ID
     * @return 退库单详情（主表+明细）
     */
    ReturnOrderDetailDto getReturnOrderDetail(Long id);
}