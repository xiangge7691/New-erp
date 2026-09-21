package com.tonghui.erp.Data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tonghui.erp.Data.Entity.SalesOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
}
