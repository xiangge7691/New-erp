package com.tonghui.erp.Data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tonghui.erp.Data.Entity.PreparationProcessTemplate;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 制剂工序模版Mapper接口
 */
@Mapper
public interface PreparationProcessTemplateMapper extends BaseMapper<PreparationProcessTemplate> {

    /**
     * 物理删除指定制剂下的所有工序模版
     * <p>绕过全局软删除配置，直接执行物理删除</p>
     *
     * @param preparationId 制剂ID
     * @return 删除的行数
     */
    @Delete("DELETE FROM preparation_process_template WHERE preparation_id = #{preparationId}")
    int physicalDeleteByPreparationId(@Param("preparationId") Long preparationId);
}
