package com.tonghui.erp.Data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tonghui.erp.Common.annotation.DataChangeAudit;
import com.tonghui.erp.Data.Entity.PreparationProcessTemplate;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 制剂工序模版Mapper接口
 */
@Mapper
public interface PreparationProcessTemplateMapper extends BaseMapper<PreparationProcessTemplate> {

    /**
     * 查询指定制剂下的全部工序模版（包含已软删除记录）
     * <p>
     * 用于变更历史：工序模版后来被删除时，历史记录仍需展示工序名称
     * </p>
     *
     * @param preparationId 制剂ID
     * @return 工序模版列表（含工序类型名称）
     */
    @Select("SELECT t.template_id AS templateId, t.preparation_id AS preparationId,"
            + " t.process_type_id AS processTypeId, t.step_order AS stepOrder,"
            + " p.process_name AS processTypeName, p.process_code AS processCode"
            + " FROM preparation_process_template t"
            + " LEFT JOIN process_type p ON p.process_id = t.process_type_id"
            + " WHERE t.preparation_id = #{preparationId}"
            + " ORDER BY t.step_order ASC, t.template_id ASC")
    List<PreparationProcessTemplate> selectByPreparationIdIncludeDeleted(@Param("preparationId") Long preparationId);

    /**
     * 按模版ID物理删除单行（批量保存时删除被移除的行）
     * <p>逐行删除便于数据变更日志记录每一行的删除历史</p>
     *
     * @param id 模版ID
     * @return 删除的行数
     */
    @DataChangeAudit(table = "preparation_process_template", whereColumn = "template_id",
            param = "id", primaryKeyColumn = "template_id", changeType = "DELETE")
    @Delete("DELETE FROM preparation_process_template WHERE template_id = #{id}")
    int physicalDeleteById(@Param("id") Long id);

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
