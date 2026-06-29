package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Common.Dto.Stock.ExpiryWarningDTO;
import com.tonghui.erp.Data.Entity.Stock;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
* @author 87954
* @description 针对表【stock(库存表（统一管理物料和制剂库存，按生产单位分配）)】的数据库操作Mapper
* @createDate 2025-10-22 10:42:53
* @Entity com.tonghui.erp.Data.Entity.Stock
*/
public interface StockMapper extends BaseMapper<Stock> {

    /**
     * 查询即将过期的库存（基于 FIFO 先进先出计算实际剩余数量）
     *
     * @param startDate 开始日期（通常是今天）
     * @param endDate 结束日期（今天+N天）
     * @param itemType 物品类型（可选）
     * @param prodUnitId 生产单位ID（可选）
     * @return 预警库存列表
     */
    List<ExpiryWarningDTO> selectExpiringStocksWithDetail(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("itemType") String itemType,
            @Param("prodUnitId") Long prodUnitId
    );
    
    /**
     * 统计各预警级别的库存批次数量
     *
     * @return 统计数据 Map
     */
    Map<String, Object> countExpiringStocksByLevel();

}




