package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Data.Entity.MaterialRequisitionSlipDetail;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 物料领料单明细数据访问Mapper接口
 */
public interface MaterialRequisitionSlipDetailMapper extends BaseMapper<MaterialRequisitionSlipDetail> {

    /**
     * 根据领料单ID物理删除所有明细
     *
     * @param slipId 领料单ID
     * @return 删除的记录数
     */
    @Delete("DELETE FROM material_requisition_slip_detail WHERE slip_id = #{slipId}")
    int physicalDeleteBySlipId(@Param("slipId") Long slipId);
}
