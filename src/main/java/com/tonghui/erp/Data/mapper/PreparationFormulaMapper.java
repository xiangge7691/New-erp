package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Data.Entity.PreparationFormula;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 配制配方数据访问Mapper接口
 */
@Mapper
public interface PreparationFormulaMapper extends BaseMapper<PreparationFormula> {

    /**
     * 查询指定制剂下的全部处方行（包含已软删除记录）
     * <p>
     * 用于变更历史解析处方行名称：处方行后来被删除时，历史记录仍需要显示物料名称
     * </p>
     *
     * @param preparationId 制剂ID
     * @return 处方行（仅 formulaId 与 materialName）
     */
    @Select("SELECT formula_id AS formulaId, material_name AS materialName"
            + " FROM preparation_formula WHERE preparation_id = #{preparationId}")
    List<PreparationFormula> selectByPreparationIdIncludeDeleted(@Param("preparationId") Long preparationId);

    /**
     * 物理删除指定制剂下的所有处方
     * <p>绕过全局软删除配置，直接执行物理删除</p>
     *
     * @param preparationId 制剂ID
     * @return 删除的行数
     */
    @Delete("DELETE FROM preparation_formula WHERE preparation_id = #{preparationId}")
    int physicalDeleteByPreparationId(@Param("preparationId") Long preparationId);
}
