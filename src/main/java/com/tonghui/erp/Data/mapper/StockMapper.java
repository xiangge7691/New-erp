package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Common.Dto.FinishedStock.FinishedStockAggDto;
import com.tonghui.erp.Common.Dto.Stock.ExpiryWarningDTO;
import com.tonghui.erp.Data.Entity.Stock;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
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

    /**
     * 按物料编码汇总库存数量（用于制剂处方查询时展示各物料的库存总量）
     * <p>
     * 汇总条件：物料类型（item_type='material'）、未删除（is_deleted=0）、编码在给定集合内，
     * 按 item_code 分组求和 quantity
     * </p>
     *
     * @param itemCodes 物料编码集合（非空）
     * @return 汇总结果列表，每项含 item_code 与 total_quantity（数量）
     */
    @Select("<script>" +
            "SELECT item_code, COALESCE(SUM(quantity), 0) AS total_quantity FROM stock " +
            "WHERE item_type = 'material' AND is_deleted = 0 AND item_code IN " +
            "<foreach collection='itemCodes' item='code' open='(' separator=',' close=')'>#{code}</foreach> " +
            "GROUP BY item_code" +
            "</script>")
    List<Map<String, Object>> sumQuantityByMaterialCodes(@Param("itemCodes") List<String> itemCodes);

    /**
     * 按物料编码与入库单ID查询库存记录（绕过逻辑删除过滤）
     * <p>
     * 用于退库重建场景：原库存行可能已因出库清零被软删除，
     * 但出库明细仍通过 stock_id 引用它，需要从被软删除的行反查出库明细
     * </p>
     *
     * @param itemCode   物料编码
     * @param stockInId  入库单ID
     * @return 库存记录（含软删除行，可能为null）
     */
    @Select("SELECT * FROM stock WHERE item_code = #{itemCode} AND stock_in_id = #{stockInId} LIMIT 1")
    Stock selectByItemAndStockInIncludeDeleted(@Param("itemCode") String itemCode, @Param("stockInId") Long stockInId);

    /**
     * 按库存ID批量查询库存记录（绕过逻辑删除过滤）
     * <p>
     * 用于库存流水查询：原批次可能已清零被逻辑删除，但仍需展示物品编码/名称/分类/单位/仓库等信息。
     * </p>
     *
     * @param ids 库存ID集合（非空）
     * @return 库存记录列表（含已逻辑删除行）
     */
    @Select("<script>" +
            "SELECT * FROM stock WHERE stock_id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<Stock> selectBatchIdsIncludeDeleted(@Param("ids") List<Long> ids);

    /**
     * 按批次号、物料编码查询库存记录（绕过逻辑删除过滤）
     * <p>
     * 用于按批次查询库存流水：批次清零后库存行被逻辑删除，若走普通查询会遗漏该批次的出入库流水。
     * </p>
     *
     * @param batchNumber 批次号（可选，模糊匹配）
     * @param itemCode    物料编码（可选，模糊匹配）
     * @return 库存记录列表（含已逻辑删除行）
     */
    @Select("<script>" +
            "SELECT * FROM stock WHERE 1 = 1 " +
            "<if test=\"batchNumber != null and batchNumber != ''\">" +
            " AND batch_number LIKE CONCAT('%', #{batchNumber}, '%')" +
            "</if>" +
            "<if test=\"itemCode != null and itemCode != ''\">" +
            " AND item_code LIKE CONCAT('%', #{itemCode}, '%')" +
            "</if>" +
            "</script>")
    List<Stock> selectByBatchIncludeDeleted(@Param("batchNumber") String batchNumber,
                                            @Param("itemCode") String itemCode);

    /**
     * 按品种编码汇总成品库存（数量大于 0 的成品批次）
     * <p>
     * 成品库存识别规则：category_name='成品' 或 item_type='preparation'，
     * 兼容历史数据中成品库存 item_type 仍为 material 的情况。
     * totalStock 统计全部状态库存；qualifiedStock 仅统计合格库存；
     * 近效期库存与已过期库存只在合格库存范围内统计，
     * 供成品库存预警计算可用库存时按需剔除。
     * </p>
     *
     * @param nearExpiryDate 近效期截止日期（今天 + 近效期天数）
     * @return 按品种编码汇总的成品库存列表
     */
    @Select("""
            SELECT
                item_code AS preparationCode,
                MAX(item_name) AS preparationName,
                MAX(unit_name) AS unitName,
                COALESCE(SUM(quantity), 0) AS totalStock,
                COALESCE(SUM(CASE WHEN stock_status IS NULL OR stock_status = '合格'
                                  THEN quantity ELSE 0 END), 0) AS qualifiedStock,
                COALESCE(SUM(CASE WHEN (stock_status IS NULL OR stock_status = '合格')
                                       AND expiry_date IS NOT NULL AND expiry_date <= #{nearExpiryDate}
                                  THEN quantity ELSE 0 END), 0) AS nearExpiryStock,
                COALESCE(SUM(CASE WHEN (stock_status IS NULL OR stock_status = '合格')
                                       AND expiry_date IS NOT NULL AND expiry_date < CURDATE()
                                  THEN quantity ELSE 0 END), 0) AS expiredStock
            FROM stock
            WHERE is_deleted = 0
              AND quantity > 0
              AND (category_name = '成品' OR item_type = 'preparation')
            GROUP BY item_code
            """)
    List<FinishedStockAggDto> selectFinishedStockAgg(@Param("nearExpiryDate") LocalDate nearExpiryDate);

}




