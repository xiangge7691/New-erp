package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Data.Entity.PreparationDocument;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 制剂文档Mapper接口
 */
@Mapper
public interface PreparationDocumentMapper extends BaseMapper<PreparationDocument> {

    /**
     * 物理删除指定制剂下的所有文档
     * <p>绕过全局软删除配置，直接执行物理删除</p>
     *
     * @param preparationId 制剂ID
     * @return 删除的行数
     */
    @Delete("DELETE FROM preparation_document WHERE preparation_id = #{preparationId}")
    int physicalDeleteByPreparationId(@Param("preparationId") Long preparationId);
}
