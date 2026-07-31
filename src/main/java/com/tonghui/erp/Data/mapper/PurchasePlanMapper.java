package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Data.Entity.PurchasePlan;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 采购计划数据访问Mapper接口
 */
public interface PurchasePlanMapper extends BaseMapper<PurchasePlan> {

    /**
     * 根据采购计划编码物理删除已软删除的记录（释放唯一键约束）
     *
     * @param planCode 采购计划编码
     * @return 删除的记录数
     */
    @Delete("DELETE FROM purchase_plan WHERE plan_code = #{planCode} AND is_deleted = 1")
    int physicalDeleteByPlanCode(@Param("planCode") String planCode);
}
