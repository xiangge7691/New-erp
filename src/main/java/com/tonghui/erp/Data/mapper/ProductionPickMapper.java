package com.tonghui.erp.Data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tonghui.erp.Data.Entity.ProductionPick;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 生产领料单主表数据访问层
 */
public interface ProductionPickMapper extends BaseMapper<ProductionPick> {

    /**
     * 根据ID物理删除生产领料单主表
     *
     * @param id 生产领料单ID
     * @return 删除的记录数
     */
    @Delete("DELETE FROM production_pick WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);
}
