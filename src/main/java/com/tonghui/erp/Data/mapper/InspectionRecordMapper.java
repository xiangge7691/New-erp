package com.tonghui.erp.Data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tonghui.erp.Data.Entity.InspectionRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 检验记录数据访问Mapper接口
 * <p>
 * 提供检验记录表的基础CRUD及检验编号生成所需的原生SQL查询
 * </p>
 */
public interface InspectionRecordMapper extends BaseMapper<InspectionRecord> {

    /**
     * 查询指定前缀下最大的检验编号（绕过软删除过滤，包含已删除记录）
     * <p>
     * 用于检验编号生成，避免软删除后编号重复触发唯一约束
     * </p>
     *
     * @param prefix 编号前缀，如 "JY-20260731"
     * @return 最大的检验编号，无记录时返回null
     */
    @Select("SELECT inspection_code FROM inspection_record WHERE inspection_code LIKE CONCAT(#{prefix}, '%') ORDER BY inspection_code DESC LIMIT 1")
    String selectMaxCodeByPrefix(@Param("prefix") String prefix);
}