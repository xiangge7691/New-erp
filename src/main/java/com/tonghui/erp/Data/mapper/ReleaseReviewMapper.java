package com.tonghui.erp.Data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tonghui.erp.Data.Entity.ReleaseReview;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 审核放行数据访问Mapper接口
 * <p>
 * 提供审核放行表的基础CRUD及放行编号生成所需的原生SQL查询
 * </p>
 */
public interface ReleaseReviewMapper extends BaseMapper<ReleaseReview> {

    /**
     * 查询指定前缀下最大的放行编号（绕过软删除过滤，包含已删除记录）
     * <p>
     * 用于放行编号生成，避免软删除后编号重复触发唯一约束
     * </p>
     *
     * @param prefix 编号前缀，如 "FX-20260731"
     * @return 最大的放行编号，无记录时返回null
     */
    @Select("SELECT release_code FROM release_review WHERE release_code LIKE CONCAT(#{prefix}, '%') ORDER BY release_code DESC LIMIT 1")
    String selectMaxCodeByPrefix(@Param("prefix") String prefix);

    /**
     * 统计指定放行编号的记录数（绕过软删除过滤，包含已删除记录）
     * <p>
     * 用于编号唯一性校验，避免已软删除记录占用的编号被误判为可用，
     * 从而在插入时触发唯一索引冲突
     * </p>
     *
     * @param code      放行编号
     * @param excludeId 需要排除的记录ID（修改时传入，新增时传null）
     * @return 匹配记录数
     */
    @Select("SELECT COUNT(*) FROM release_review WHERE release_code = #{code} AND (#{excludeId} IS NULL OR id <> #{excludeId})")
    long countByCodeIncludeDeleted(@Param("code") String code, @Param("excludeId") Long excludeId);
}