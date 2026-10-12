package com.tonghui.erp.Data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tonghui.erp.Data.Entity.DestructionRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 销毁记录主表数据访问Mapper接口
 * <p>
 * 提供销毁记录主表的基础CRUD及销毁单号生成所需的原生SQL查询
 * </p>
 */
public interface DestructionRecordMapper extends BaseMapper<DestructionRecord> {

    /**
     * 查询指定前缀下最大的销毁单号（绕过软删除过滤，包含已删除记录）
     * <p>
     * 用于销毁单号生成，避免软删除后单号被复用触发唯一约束
     * </p>
     *
     * @param prefix 单号前缀，如 "BPXH-20261012" 或 "LYXH-20261012"
     * @return 最大的销毁单号，无记录时返回null
     */
    @Select("SELECT destruction_code FROM destruction_record WHERE destruction_code LIKE CONCAT(#{prefix}, '%') ORDER BY destruction_code DESC LIMIT 1")
    String selectMaxCodeByPrefix(@Param("prefix") String prefix);

    /**
     * 统计指定销毁单号的记录数（绕过软删除过滤，包含已删除记录）
     * <p>
     * 用于单号唯一性校验，避免已软删除记录占用的单号被误判为可用
     * </p>
     *
     * @param code      销毁单号
     * @param excludeId 需要排除的记录ID（修改时传入，新增时传null）
     * @return 匹配记录数
     */
    @Select("SELECT COUNT(*) FROM destruction_record WHERE destruction_code = #{code} AND (#{excludeId} IS NULL OR id <> #{excludeId})")
    long countByCodeIncludeDeleted(@Param("code") String code, @Param("excludeId") Long excludeId);
}
