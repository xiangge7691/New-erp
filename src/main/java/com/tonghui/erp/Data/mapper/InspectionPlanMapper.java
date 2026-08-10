package com.tonghui.erp.Data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tonghui.erp.Data.Entity.InspectionPlan;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 检验计划数据访问Mapper接口
 * <p>
 * 提供检验计划表的基础CRUD及计划编号生成所需的原生SQL查询
 * </p>
 */
public interface InspectionPlanMapper extends BaseMapper<InspectionPlan> {

    /**
     * 查询指定前缀下最大的计划编号（绕过软删除过滤，包含已删除记录）
     * <p>
     * 用于计划编号生成，避免软删除后编号重复触发唯一约束
     * </p>
     *
     * @param prefix 编号前缀，如 "JH-20260731"
     * @return 最大的计划编号，无记录时返回null
     */
    @Select("SELECT plan_code FROM inspection_plan WHERE plan_code LIKE CONCAT(#{prefix}, '%') ORDER BY plan_code DESC LIMIT 1")
    String selectMaxCodeByPrefix(@Param("prefix") String prefix);
}