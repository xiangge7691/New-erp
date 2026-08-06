package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Stock.ExpiryWarningDTO;
import com.tonghui.erp.Common.Dto.Stock.ExpiryWarningStatsDTO;
import com.tonghui.erp.Common.Dto.Stock.StockGroupedDto;
import com.tonghui.erp.Common.Dto.Stock.StockWithDetailsDto;
import com.tonghui.erp.Data.Entity.Stock;
import com.tonghui.erp.Data.Entity.StockIn;
import com.tonghui.erp.Data.Entity.StockInDetail;
import com.tonghui.erp.Data.Entity.StockOut;
import com.tonghui.erp.Data.Entity.StockOutDetail;
import com.tonghui.erp.Data.Entity.StockTransaction;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 库存服务接口
 * <p>
 * 提供库存相关的业务逻辑接口，包括库存的高级查询、带子表关联查询、
 * 库存预警查询、预警统计等功能
 * </p>
 */
public interface StockService extends IService<Stock> {

    /**
     * 高级查询库存（支持分页）
     *
     * @param stock     查询条件
     * @param createdTimeStart 创建时间起始
     * @param createdTimeEnd 创建时间结束
     * @param updatedTimeStart 更新时间起始
     * @param updatedTimeEnd 更新时间结束
     * @param pageNum   页码
     * @param pageSize  每页大小
     * @return 分页结果
     */
    Page<Stock> queryStocks(Stock stock, LocalDateTime createdTimeStart, LocalDateTime createdTimeEnd, LocalDateTime updatedTimeStart, LocalDateTime updatedTimeEnd, int pageNum, int pageSize);

    /**
     * 高级查询库存（支持分页）
     *
     * @param stock     查询条件
     * @param pageNum   页码
     * @param pageSize  每页大小
     * @return 分页结果
     */
    Page<Stock> queryStocks(Stock stock, int pageNum, int pageSize);

    /**
     * 高级查询库存（包含关联子表数据）
     *
     * @param stock     查询条件
     * @param pageNum   页码
     * @param pageSize  每页大小
     * @return 分页结果（包含关联子表）
     */
    PagedResult<StockWithDetailsDto> searchWithDetails(Stock stock, int pageNum, int pageSize);

    /**
     * 获取即将过期的库存列表（基于 FIFO 先进先出计算实际剩余数量）
     *
     * @param warningDays 预警天数（如 7、30、90）
     * @return 预警库存列表
     */
    List<ExpiryWarningDTO> getExpiringStocks(int warningDays);

    /**
     * 获取有效期预警统计
     *
     * @return 预警统计数据
     */
    ExpiryWarningStatsDTO getExpiryWarningStats();

    /**
     * 高级查询即将过期的库存（支持分页和筛选）
     *
     * @param warningDays 预警天数
     * @param itemType 物品类型（可选）
     * @param prodUnitId 生产单位ID（可选）
     * @param warningLevel 预警级别（可选: urgent/warning/info）
     * @param pageIndex 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    Page<ExpiryWarningDTO> queryExpiringStocks(int warningDays, String itemType, 
                                                Long prodUnitId, String warningLevel,
                                                int pageIndex, int pageSize);

    // endregion

    // region 库存联动（入库/出库确认）
    // ===================================
    // 库存联动（入库/出库确认）
    // ===================================

    /**
     * 入库库存联动：按明细逐行 upsert 库存表并写入库存流水
     * <p>
     * 库存唯一键：物品编码 + 生产单位 + 批号；存在则数量累加并覆盖单价/有效期，不存在则新增
     * 供入库单确认、货物验收检验合格入库共用
     * </p>
     *
     * @param stockIn 入库单信息（inType作为流水类型，prodUnitId为仓库）
     * @param details 入库明细列表
     */
    void applyInbound(StockIn stockIn, List<StockInDetail> details);

    /**
     * 出库库存联动：按明细逐行扣减库存并写入库存流水
     * <p>
     * 通过明细中的stockId定位库存批次，校验库存充足（不足抛异常整体回滚），
     * 扣减后数量小于等于0时删除库存记录
     * </p>
     *
     * @param stockOut 出库单信息（outType作为流水类型）
     * @param details  出库明细列表（须携带stockId）
     */
    void applyOutbound(StockOut stockOut, List<StockOutDetail> details);

    /**
     * 入库取消回滚：按明细逐行扣减已入库的库存并写入调整流水
     * <p>
     * 定位键与入库一致（物品编码+生产单位+批号），数量不足抛异常整体回滚，
     * 扣减后数量小于等于0时删除库存记录。对应原数据库触发器 after_stock_in_cancel 的逻辑
     * </p>
     *
     * @param stockIn 入库单信息
     * @param details 入库明细列表
     */
    void rollbackInbound(StockIn stockIn, List<StockInDetail> details);

    /**
     * 出库取消回滚：按明细逐行恢复库存并写入调整流水
     * <p>
     * 通过明细中的stockId定位库存批次，存在则数量累加；若该批次已因出库清零被删除则按明细信息重建。
     * 对应原数据库触发器 after_stock_out_cancel 的逻辑
     * </p>
     *
     * @param stockOut 出库单信息
     * @param details  出库明细列表（须携带stockId）
     */
    void rollbackOutbound(StockOut stockOut, List<StockOutDetail> details);

    /**
     * 根据库存ID查询库存流水列表
     *
     * @param stockId 库存ID
     * @return 流水列表
     */
    List<StockTransaction> getTransactionsByStockId(Long stockId);

    // endregion

    // region 分组查询
    // ===================================
    // 分组查询
    // ===================================

    /**
     * 按物料编码分组查询库存（支持筛选与分页）
     * <p>
     * 返回每个物料的总库存及批次明细（含仓库名称），用于库存查询页面
     * 按"物料分组 + 批次展开"的展示模式
     * </p>
     *
     * @param itemCode     物料编码（模糊匹配）
     * @param itemName     物料名称（模糊匹配）
     * @param categoryName 分类名称（等值匹配）
     * @param prodUnitId   仓库（生产单位ID，等值匹配）
     * @param stockStatus  库存状态（等值匹配：合格/待检/不合格）
     * @param showZero     是否显示零库存
     * @param pageIndex    页码，从0开始
     * @param pageSize     每页大小
     * @return 分组分页结果
     */
    PagedResult<StockGroupedDto> groupedSearch(String itemCode, String itemName, String categoryName,
                                               Long prodUnitId, String stockStatus, boolean showZero,
                                               int pageIndex, int pageSize);

    // endregion
}
