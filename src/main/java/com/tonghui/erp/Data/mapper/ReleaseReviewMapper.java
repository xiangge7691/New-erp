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
}