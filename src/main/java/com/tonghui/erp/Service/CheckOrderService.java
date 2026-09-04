package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Common.Dto.Warehouse.CheckOrderCreateDto;
import com.tonghui.erp.Common.Dto.Warehouse.CheckOrderDetailDto;
import com.tonghui.erp.Common.Dto.Warehouse.StockDetailItemDto;
import com.tonghui.erp.Data.Entity.CheckOrder;

import java.util.List;

/**
 * 盘点单服务接口
 * <p>
 * 提供盘点单分页查询、盘点仓库列表、仓库库存明细查询及提交盘点
 * （自动计算差异、调整库存并生成盘点流水）等业务功能
 * </p>
 */
public interface CheckOrderService extends IService<CheckOrder> {

    /**
     * 分页查询盘点单列表
     *
     * @param warehouse 仓库名称筛选（可选）
     * @param keyword   搜索关键词（盘点单号/物料名称，可选）
     * @param startTime 创建时间起始（可选）
     * @param endTime   创建时间结束（可选）
     * @param pageIndex 页码（从0开始）
     * @param pageSize  每页数量
     * @return 分页结果（主表信息列表）
     */
    Page<CheckOrder> queryCheckOrders(String warehouse, String keyword, String startTime, String endTime, int pageIndex, int pageSize);

    /**
     * 获取所有仓库名称列表（生产单位名称）
     *
     * @return 仓库名称列表
     */
    List<String> getWarehouseList();

    /**
     * 获取仓库库存明细（盘点用）
     *
     * @param warehouse 仓库名称（必填）
     * @param showZero  是否显示零库存（默认false）
     * @param keyword   搜索关键词（物料名称/批号/编码，可选）
     * @return 库存明细列表
     */
    List<StockDetailItemDto> getStockDetails(String warehouse, Boolean showZero, String keyword);

    /**
     * 提交盘点
     * <p>
     * 自动计算差异（实盘-系统）并生成盘点结果（盘盈/盘亏/盘平），
     * 对有差异的物料调整库存并生成盘点调整流水，保存盘点单主表与明细
     * </p>
     *
     * @param dto 盘点请求（仓库+明细）
     * @return 创建后的盘点单（含单号与统计结果）
     */
    CheckOrder createCheckOrder(CheckOrderCreateDto dto);

    /**
     * 查询盘点单详情（含明细）
     *
     * @param id 盘点单ID
     * @return 盘点单详情（主表+明细）
     */
    CheckOrderDetailDto getCheckOrderDetail(Long id);
}