package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Data.Entity.InspectionRequest;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 请检记录数据访问Mapper接口
 */
public interface InspectionRequestMapper extends BaseMapper<InspectionRequest> {

    /**
     * 根据ID物理删除请检记录
     *
     * @param id 请检记录ID
     * @return 删除的记录数
     */
    @Delete("DELETE FROM inspection_request WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);

    /**
     * 统计指定请检编号的记录数（绕过软删除过滤，包含已删除记录）
     * <p>
     * 用于编号唯一性校验，避免已软删除记录占用的编号被误判为可用，
     * 从而在插入时触发唯一索引冲突
     * </p>
     *
     * @param code      请检编号
     * @param excludeId 需要排除的记录ID（修改时传入，新增时传null）
     * @return 匹配记录数
     */
    @Select("SELECT COUNT(*) FROM inspection_request WHERE inspection_code = #{code} AND (#{excludeId} IS NULL OR id <> #{excludeId})")
    long countByCodeIncludeDeleted(@Param("code") String code, @Param("excludeId") Long excludeId);
}
