package com.tonghui.erp.Data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tonghui.erp.Data.Entity.ProductionPickDetail;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 生产领料单明细数据访问层
 */
public interface ProductionPickDetailMapper extends BaseMapper<ProductionPickDetail> {

    /**
     * 根据生产领料单ID物理删除全部明细
     *
     * @param pickId 生产领料单ID
     * @return 删除的记录数
     */
    @Delete("DELETE FROM production_pick_detail WHERE pick_id = #{pickId}")
    int physicalDeleteByPickId(@Param("pickId") Long pickId);
}
