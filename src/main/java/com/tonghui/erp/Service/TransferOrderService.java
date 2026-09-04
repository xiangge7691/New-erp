package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Common.Dto.Warehouse.MaterialBatchDto;
import com.tonghui.erp.Common.Dto.Warehouse.TransferOrderCreateDto;
import com.tonghui.erp.Common.Dto.Warehouse.TransferOrderDetailDto;
import com.tonghui.erp.Common.Dto.Warehouse.TransferOrderListItemDto;
import com.tonghui.erp.Common.Dto.Warehouse.WarehouseMaterialDto;
import com.tonghui.erp.Data.Entity.TransferOrder;

import java.util.List;

/**
 * 调拨单服务接口
 * <p>
 * 提供调拨单的分页查询、仓库列表、仓库可用物料、物料批次详情查询，
 * 以及新增调拨（自动更新两端库存并生成调拨出入库流水）等业务功能
 * </p>
 */
public interface TransferOrderService extends IService<TransferOrder> {

    /**
     * 分页查询调拨单列表
     *
     * @param type      类型筛选：调拨出库（按调出仓库模糊匹配keyword）/调拨入库（按调入仓库模糊匹配keyword），可选
     * @param keyword   搜索关键词（调拨单号/物料名称，keyword为空且type不为空时作为仓库名模糊匹配），可选
     * @param startTime 创建时间起始（可选）
     * @param endTime   创建时间结束（可选）
     * @param pageIndex 页码（从0开始）
     * @param pageSize  每页数量
     * @return 分页结果（主表信息列表）
     */
    Page<TransferOrder> queryTransferOrders(String type, String keyword, String startTime, String endTime, int pageIndex, int pageSize);

    /**
     * 获取所有仓库名称列表（生产单位名称）
     *
     * @return 仓库名称列表
     */
    List<String> getWarehouseList();

    /**
     * 获取指定仓库的可用物料列表（含批次数量）
     *
     * @param warehouse 仓库名称
     * @param keyword   搜索关键词（按物料编码/物料名称模糊匹配，可选）
     * @return 物料列表（按物料编码分组）
     */
    List<WarehouseMaterialDto> getWarehouseMaterials(String warehouse, String keyword);

    /**
     * 获取指定仓库某物料的批次库存详情
     *
     * @param warehouse    仓库名称
     * @param materialCode 物料编码
     * @return 批次详情列表
     */
    List<MaterialBatchDto> getMaterialBatches(String warehouse, String materialCode);

    /**
     * 新增调拨单
     * <p>
     * 校验仓库与库存后，扣减调出库存、增加调入库存（无同批次则新增库存记录），
     * 生成调拨出库与调拨入库两条流水，并保存调拨单主表与明细
     * </p>
     *
     * @param dto 调拨单请求（仓库+明细）
     * @return 创建后的调拨单（含单号与汇总数据）
     */
    TransferOrder createTransferOrder(TransferOrderCreateDto dto);

    /**
     * 查询调拨单详情（含明细）
     *
     * @param id 调拨单ID
     * @return 调拨单详情（主表+明细）
     */
    TransferOrderDetailDto getTransferOrderDetail(Long id);
}