package com.tonghui.erp.Data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tonghui.erp.Data.Entity.PreparationProcessTemplate;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 制剂工序模版Mapper接口
 */
@Mapper
public interface PreparationProcessTemplateMapper extends BaseMapper<PreparationProcessTemplate> {

    /**
     * 按模版ID集合物理删除（批量保存时删除被移除的行）
     * <p>绕过全局软删除配置，直接执行物理删除，保留未被移除行的ID</p>
     *
     * @param ids 模版ID集合
     * @return 删除的行数
     */
    @Delete("<script>DELETE FROM preparation_process_template WHERE template_id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    int physicalDeleteByIds(@Param("ids") List<Long> ids);
}
