package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Data.Entity.PreparationFormula;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 配制配方数据访问Mapper接口
 */
@Mapper
public interface PreparationFormulaMapper extends BaseMapper<PreparationFormula> {

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
