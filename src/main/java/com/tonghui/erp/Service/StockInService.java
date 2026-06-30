package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Stock.StockInWithDetailsDto;
import com.tonghui.erp.Data.Entity.StockIn;
import com.tonghui.erp.Data.Entity.StockInDetail;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 入库单业务接口
 */
public interface StockInService extends IService<StockIn> {

    // #region 基础操作

    /**
     * 新增入库单（包含明细）
     *
     * @param stockIn 入库单实体
     * @param details 入库明细列表
     */
    void addStockIn(StockIn stockIn, List<StockInDetail> details);

    /**
     * 更新入库单（包含明细）
     *
     * @param stockIn 入库单实体
     * @param details 入库明细列表
     */
    void updateStockIn(StockIn stockIn, List<StockInDetail> details);

    /**
     * 部分更新入库单（只更新非 null 字段）
     *
     * @param stockIn 入库单实体
     */
    void partialUpdateStockIn(StockIn stockIn);

    /**
     * 删除入库单（同时删除明细）
     *
     * @param stockInId 入库单 ID
     */
    void deleteStockIn(Long stockInId);

    // #endregion

    // #region 查询操作

    /**
     * 根据ID查询入库单
     *
     * @param stockInId 入库单ID
     * @return 入库单实体
     */
    StockIn getStockInById(Long stockInId);

    /**
     * 根据入库单号查询入库单
     *
     * @param stockInCode 入库单号
     * @return 入库单实体
     */
    StockIn getStockInByCode(String stockInCode);

    /**
     * 查询所有入库单
     *
     * @return 入库单集合
     */
    List<StockIn> getAllStockIns();

    // #endregion

    // #region 入库单明细操作

    /**
     * 根据入库单ID获取明细列表
     *
     * @param stockInId 入库单ID
     * @return 明细列表
     */
    List<StockInDetail> getStockInDetailsByStockInId(Long stockInId);

    /**
     * 添加入库明细
     *
     * @param detail 入库明细
     */
    void addStockInDetail(StockInDetail detail);

    /**
     * 批量添加入库明细
     *
     * @param details 入库明细列表
     */
    void addStockInDetails(List<StockInDetail> details);

    /**
     * 更新入库明细
     *
     * @param detail 入库明细
     */
    void updateStockInDetail(StockInDetail detail);

    /**
     * 删除入库明细
     *
     * @param detailId 明细ID
     */
    void deleteStockInDetail(Long detailId);

    // #endregion

    // #region 单号生成

    /**
     * 生成入库单号
     *
     * @return 入库单号
     */
    String generateStockInCode();

    // #endregion

    // #region 高级查询

    /**
     * 高级查询入库单（支持分页）
     *
     * @param stockIn   查询条件
     * @param createdTimeStart 创建时间起始
     * @param createdTimeEnd 创建时间结束
     * @param updatedTimeStart 更新时间起始
     * @param updatedTimeEnd 更新时间结束
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param pageNum   页码
     * @param pageSize  每页大小
     * @return 分页结果
     */
    Page<StockIn> queryStockIns(StockIn stockIn, LocalDateTime createdTimeStart, LocalDateTime createdTimeEnd, LocalDateTime updatedTimeStart, LocalDateTime updatedTimeEnd, LocalDate startDate, LocalDate endDate, int pageNum, int pageSize);

    // #endregion

    // #region 带子表查询

    /**
     * 高级查询入库单（包含明细子表）
     *
     * @param stockIn   查询条件
     * @param createdTimeStart 创建时间起始
     * @param createdTimeEnd 创建时间结束
     * @param updatedTimeStart 更新时间起始
     * @param updatedTimeEnd 更新时间结束
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param pageNum   页码
     * @param pageSize  每页大小
     * @return 分页结果（包含明细）
     */
    PagedResult<StockInWithDetailsDto> searchWithDetails(StockIn stockIn, LocalDateTime createdTimeStart, LocalDateTime createdTimeEnd, LocalDateTime updatedTimeStart, LocalDateTime updatedTimeEnd, LocalDate startDate, LocalDate endDate, int pageNum, int pageSize);

    // #endregion
}
