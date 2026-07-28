package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Common.Dto.Stock.ExpiryWarningDTO;
import com.tonghui.erp.Data.Entity.Stock;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 库存数据访问Mapper接口
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

    /**
     * 根据生产单位ID物理删除库存记录（绕过逻辑删除）
     *
     * @param prodUnitId 生产单位ID
     * @return 删除的记录数
     */
    @Delete("DELETE FROM stock WHERE prod_unit_id = #{prodUnitId}")
    int physicalDeleteByProdUnitId(@Param("prodUnitId") Long prodUnitId);

    /**
     * 根据生产单位ID软删除库存记录（绕过MyBatis-Plus逻辑删除配置）
     *
     * @param prodUnitId 生产单位ID
     * @return 更新的记录数
     */
    @Update("UPDATE stock SET is_deleted = 1 WHERE prod_unit_id = #{prodUnitId} AND is_deleted = 0")
    int softDeleteByProdUnitId(@Param("prodUnitId") Long prodUnitId);

}




