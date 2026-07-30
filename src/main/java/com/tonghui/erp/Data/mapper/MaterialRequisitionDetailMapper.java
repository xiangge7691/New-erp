package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Data.Entity.MaterialRequisitionDetail;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 领料明细数据访问Mapper接口
 */
public interface MaterialRequisitionDetailMapper extends BaseMapper<MaterialRequisitionDetail> {

    /**
     * 根据领料申请ID物理删除领料明细
     *
     * @param requisitionId 领料申请ID
     * @return 删除的记录数
     */
    @Delete("DELETE FROM material_requisition_detail WHERE requisition_id = #{requisitionId}")
    int physicalDeleteByRequisitionId(@Param("requisitionId") Long requisitionId);
}
