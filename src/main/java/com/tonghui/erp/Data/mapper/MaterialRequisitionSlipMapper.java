package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Data.Entity.MaterialRequisitionSlip;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 物料领料单主表数据访问Mapper接口
 */
public interface MaterialRequisitionSlipMapper extends BaseMapper<MaterialRequisitionSlip> {

    /**
     * 根据ID物理删除领料单主表
     *
     * @param id 领料单ID
     * @return 删除的记录数
     */
    @Delete("DELETE FROM material_requisition_slip WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);
}
