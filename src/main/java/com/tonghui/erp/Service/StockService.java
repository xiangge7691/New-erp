package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Stock.ExpiryWarningDTO;
import com.tonghui.erp.Common.Dto.Stock.ExpiryWarningStatsDTO;
import com.tonghui.erp.Common.Dto.Stock.StockWithDetailsDto;
import com.tonghui.erp.Data.Entity.Stock;
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

}
