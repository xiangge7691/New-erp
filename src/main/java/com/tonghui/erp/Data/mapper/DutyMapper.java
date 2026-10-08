package com.tonghui.erp.Data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tonghui.erp.Data.Entity.Duty;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 职务信息数据访问Mapper接口
 * <p>
 * 提供职务表的基础CRUD及职务编码唯一性校验所需的原生SQL查询
 * </p>
 */
public interface DutyMapper extends BaseMapper<Duty> {

    /**
     * 统计指定职务编码的记录数（绕过软删除过滤，包含已删除记录）
     * <p>
     * 用于职务编码唯一性校验，避免已软删除记录占用的编码被误判为可用，
     * 从而在插入时触发唯一索引冲突
     * </p>
     *
     * @param code      职务编码
     * @param excludeId 需要排除的记录ID（修改时传入，新增时传null）
     * @return 匹配记录数
     */
    @Select("SELECT COUNT(*) FROM duty WHERE duty_code = #{code} AND (#{excludeId} IS NULL OR duty_id <> #{excludeId})")
    long countByCodeIncludeDeleted(@Param("code") String code, @Param("excludeId") Long excludeId);
}