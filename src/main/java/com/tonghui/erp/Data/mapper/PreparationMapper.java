package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Data.Entity.Preparation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 配制数据访问Mapper接口
 */
public interface PreparationMapper extends BaseMapper<Preparation> {

    /**
     * 根据制剂编码物理删除已软删除的记录（释放唯一键约束）
     *
     * @param preparationCode 制剂编码
     * @return 删除的记录数
     */
    @Delete("DELETE FROM preparation WHERE preparation_code = #{preparationCode} AND is_deleted = 1")
    int physicalDeleteByPreparationCode(@Param("preparationCode") String preparationCode);
}




