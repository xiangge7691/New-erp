package com.tonghui.erp.Data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tonghui.erp.Data.Entity.SystemConfig;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 系统配置数据访问Mapper接口
 * <p>
 * 提供系统配置表的基础CRUD及配置键唯一性校验所需的原生SQL查询
 * </p>
 */
public interface SystemConfigMapper extends BaseMapper<SystemConfig> {

    /**
     * 统计指定配置键的记录数（绕过软删除过滤，包含已删除记录）
     * <p>
     * 用于配置键唯一性校验，避免已软删除记录占用的键被误判为可用，
     * 从而在插入时触发唯一索引冲突
     * </p>
     *
     * @param configKey 配置键
     * @param excludeId 需要排除的记录ID（修改时传入，新增时传null）
     * @return 匹配记录数
     */
    @Select("SELECT COUNT(*) FROM system_config WHERE config_key = #{configKey} AND (#{excludeId} IS NULL OR config_id <> #{excludeId})")
    long countByConfigKeyIncludeDeleted(@Param("configKey") String configKey, @Param("excludeId") Long excludeId);
}
