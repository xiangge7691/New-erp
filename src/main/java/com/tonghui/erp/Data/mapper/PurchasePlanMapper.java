package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Data.Entity.PurchasePlan;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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

    /**
     * 根据编号前缀查询最大采购计划编号（用于自动生成编号）
     * <p>
     * 使用原生SQL查询，绕过全局软删除过滤，避免与已软删除计划的编号冲突
     * </p>
     *
     * @param prefix 编号前缀，如 CGJH20260803
     * @return 最大采购计划编号，无记录时返回null
     */
    @Select("SELECT plan_code FROM purchase_plan WHERE plan_code LIKE CONCAT(#{prefix}, '%') ORDER BY plan_code DESC LIMIT 1")
    String selectMaxPlanCodeByPrefix(@Param("prefix") String prefix);
}
