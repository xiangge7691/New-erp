package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Data.Entity.PurchasePlanDetail;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 采购计划明细数据访问Mapper接口
 */
public interface PurchasePlanDetailMapper extends BaseMapper<PurchasePlanDetail> {

    /**
     * 根据采购计划ID物理删除明细记录
     *
     * @param planId 采购计划ID
     * @return 删除的记录数
     */
    @Delete("DELETE FROM purchase_plan_detail WHERE plan_id = #{planId}")
    int physicalDeleteByPlanId(@Param("planId") Long planId);
}
