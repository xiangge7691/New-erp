package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Data.Entity.MaterialRequisition;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 领料申请数据访问Mapper接口
 */
public interface MaterialRequisitionMapper extends BaseMapper<MaterialRequisition> {

    /**
     * 根据工单ID物理删除领料申请
     *
     * @param workOrderId 工单ID
     * @return 删除的记录数
     */
    @Delete("DELETE FROM material_requisition WHERE work_order_id = #{workOrderId}")
    int physicalDeleteByWorkOrderId(@Param("workOrderId") Long workOrderId);
}
