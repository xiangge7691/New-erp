package com.tonghui.erp.Data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tonghui.erp.Data.Entity.Customer;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 客户信息数据访问Mapper接口
 * <p>
 * 提供客户表的基础CRUD及客户编号生成所需的原生SQL查询
 * </p>
 */
public interface CustomerMapper extends BaseMapper<Customer> {

    /**
     * 查询客户表中最大的客户编号（绕过软删除过滤，包含已删除记录）
     * <p>
     * 用于客户编号生成，避免软删除后编号重复触发唯一约束
     * </p>
     *
     * @return 最大的客户编号，无记录时返回null
     */
    @Select("SELECT customer_code FROM customer WHERE customer_code IS NOT NULL ORDER BY customer_code DESC LIMIT 1")
    String selectMaxCode();

    /**
     * 统计指定客户编号的记录数（绕过软删除过滤，包含已删除记录）
     * <p>
     * 用于编号唯一性校验，避免已软删除记录占用的编号被误判为可用，
     * 从而在插入时触发唯一索引冲突
     * </p>
     *
     * @param code      客户编号
     * @param excludeId 需要排除的记录ID（修改时传入，新增时传null）
     * @return 匹配记录数
     */
    @Select("SELECT COUNT(*) FROM customer WHERE customer_code = #{code} AND (#{excludeId} IS NULL OR customer_id <> #{excludeId})")
    long countByCodeIncludeDeleted(@Param("code") String code, @Param("excludeId") Long excludeId);
}
