package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Data.Entity.Material;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 物料数据访问Mapper接口
 */
public interface MaterialMapper extends BaseMapper<Material> {

    /**
     * 根据前缀查询最大的物料编码（包含已软删除的记录，避免编码冲突）
     *
     * @param prefix 编码前缀（如Y、F、B）
     * @return 最大的物料编码，不存在则返回null
     */
    @Select("SELECT MAX(material_code) FROM material WHERE material_code LIKE CONCAT(#{prefix}, '%')")
    String getMaxCodeByPrefix(@Param("prefix") String prefix);
}




