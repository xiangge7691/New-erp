package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Data.Entity.StockOut;
import com.tonghui.erp.Data.Entity.StockOutDetail;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 出库单业务接口
 */
public interface StockOutService extends IService<StockOut> {

    // #region 基础操作

    /**
     * 新增出库单（包含明细）
     *
     * @param stockOut 出库单实体
     * @param details 出库明细列表
     */
    void addStockOut(StockOut stockOut, List<StockOutDetail> details);

    /**
     * 更新出库单（包含明细）
     *
     * @param stockOut 出库单实体
     * @param details 出库明细列表
     */
    void updateStockOut(StockOut stockOut, List<StockOutDetail> details);

    /**
     * 部分更新出库单（只更新非 null 字段）
     *
     * @param stockOut 出库单实体
     */
    void partialUpdateStockOut(StockOut stockOut);

    /**
     * 删除出库单（同时删除明细）
     *
     * @param stockOutId 出库单 ID
     */
    void deleteStockOut(Long stockOutId);

    // #endregion

    // #region 查询操作

    /**
     * 根据ID查询出库单
     *
     * @param stockOutId 出库单ID
     * @return 出库单实体
     */
    StockOut getStockOutById(Long stockOutId);

    /**
     * 根据出库单号查询出库单
     *
     * @param stockOutCode 出库单号
     * @return 出库单实体
     */
    StockOut getStockOutByCode(String stockOutCode);

    /**
     * 查询所有出库单
     *
     * @return 出库单集合
     */
    List<StockOut> getAllStockOuts();

    // #endregion

    // #region 出库单明细操作

    /**
     * 根据出库单ID获取明细列表
     *
     * @param stockOutId 出库单ID
     * @return 明细列表
     */
    List<StockOutDetail> getStockOutDetailsByStockOutId(Long stockOutId);

    /**
     * 添加出库明细
     *
     * @param detail 出库明细
     */
    void addStockOutDetail(StockOutDetail detail);

    /**
     * 批量添加出库明细
     *
     * @param details 出库明细列表
     */
    void addStockOutDetails(List<StockOutDetail> details);

    /**
     * 更新出库明细
     *
     * @param detail 出库明细
     */
    void updateStockOutDetail(StockOutDetail detail);

    /**
     * 删除出库明细
     *
     * @param detailId 明细ID
     */
    void deleteStockOutDetail(Long detailId);

    // #endregion

    // #region 单号生成

    /**
     * 生成出库单号
     *
     * @return 出库单号
     */
    String generateStockOutCode();

    // #endregion

    // #region 高级查询

    /**
     * 高级查询出库单（支持分页）
     *
     * @param stockOut  查询条件
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
    Page<StockOut> queryStockOuts(StockOut stockOut, LocalDateTime createdTimeStart, LocalDateTime createdTimeEnd, LocalDateTime updatedTimeStart, LocalDateTime updatedTimeEnd, LocalDate startDate, LocalDate endDate, int pageNum, int pageSize);

    // #endregion
}
