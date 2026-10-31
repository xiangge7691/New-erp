package com.tonghui.erp.Data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tonghui.erp.Common.Dto.FinishedStock.FinishedOutboundMonthlyDto;
import com.tonghui.erp.Data.Entity.SalesOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 成品出库台账数据访问Mapper接口
 * <p>
 * 提供成品出库台账表的基础CRUD及单号生成所需的原生SQL查询
 * </p>
 */
public interface SalesOrderMapper extends BaseMapper<SalesOrder> {

    /**
     * 查询当天最大的成品出库单号（绕过软删除过滤，包含已删除记录）
     * <p>
     * 用于单号生成，避免软删除后单号重复触发唯一约束
     * </p>
     *
     * @param prefix 单号前缀（如 CPCK-20260918）
     * @return 最大的单号，无记录时返回null
     */
    @Select("SELECT sales_order_code FROM sales_order WHERE sales_order_code LIKE CONCAT(#{prefix}, '%') ORDER BY sales_order_code DESC LIMIT 1")
    String selectMaxCodeByPrefix(@Param("prefix") String prefix);

    /**
     * 统计指定成品出库单号的记录数（绕过软删除过滤，包含已删除记录）
     * <p>
     * 用于单号唯一性校验，避免已软删除记录占用的单号被误判为可用，
     * 从而在插入时触发唯一索引冲突
     * </p>
     *
     * @param code      成品出库单号
     * @param excludeId 需要排除的记录ID（修改时传入，新增时传null）
     * @return 匹配记录数
     */
    @Select("SELECT COUNT(*) FROM sales_order WHERE sales_order_code = #{code} AND (#{excludeId} IS NULL OR sales_order_id <> #{excludeId})")
    long countByCodeIncludeDeleted(@Param("code") String code, @Param("excludeId") Long excludeId);

    /**
     * 按品种编码 + 自然月汇总成品已出库数量
     * <p>
     * 仅统计状态为“已出库”且未删除的台账记录，
     * 用于成品库存预警计算前 N 个完整自然月的月平均出库量
     * </p>
     *
     * @param startTime 统计开始时间（含）
     * @param endTime   统计结束时间（不含，通常为当月 1 日 00:00:00）
     * @return 按品种编码 + 月份汇总的已出库数量列表
     */
    @Select("""
            SELECT
                preparation_code AS preparationCode,
                MAX(preparation_name) AS preparationName,
                DATE_FORMAT(sales_order_date, '%Y-%m') AS month,
                COALESCE(SUM(quantity), 0) AS quantity
            FROM sales_order
            WHERE is_deleted = 0
              AND status = '已出库'
              AND sales_order_date >= #{startTime}
              AND sales_order_date < #{endTime}
            GROUP BY preparation_code, DATE_FORMAT(sales_order_date, '%Y-%m')
            """)
    List<FinishedOutboundMonthlyDto> selectFinishedOutboundMonthly(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}
