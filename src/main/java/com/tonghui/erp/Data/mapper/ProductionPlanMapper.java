package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Data.Entity.ProductionPlan;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 生产计划数据访问Mapper接口
 */
public interface ProductionPlanMapper extends BaseMapper<ProductionPlan> {

    /**
     * 按前缀查询最大计划编号（原生SQL，绕过全局软删除过滤）
     * <p>
     * 取号时必须看到已软删除的计划编号，否则会复用已删单号并触发唯一索引冲突。
     * 与 MP 的 getOne 查询（自动追加 is_deleted=0）不同，本方法返回结果包含软删除行。
     * </p>
     *
     * @param prefix 计划编号前缀（如 Plan20260101）
     * @return 该前缀下的最大计划编号，无记录返回null
     */
    @Select("SELECT plan_number FROM production_plan WHERE plan_number LIKE CONCAT(#{prefix}, '%') "
            + "ORDER BY plan_number DESC LIMIT 1")
    String selectMaxPlanNumberByPrefix(@Param("prefix") String prefix);

    /**
     * 统计指定计划编号的记录数（包含已软删除的记录，绕过全局软删除过滤）
     * <p>
     * 用于编号唯一性校验：数据库唯一索引对所有行生效（含 is_deleted=1 的行），
     * MyBatis-Plus 查询会自动追加 is_deleted=0 导致看不见软删除行，必须使用本方法
     * </p>
     *
     * @param planNumber 计划编号
     * @param excludeId  需要排除的记录ID（修改时传入当前记录ID，新增时传null）
     * @return 匹配的记录数（含软删除）
     */
    @Select("SELECT COUNT(*) FROM production_plan WHERE plan_number = #{planNumber} "
            + "AND (#{excludeId} IS NULL OR id != #{excludeId})")
    Long countByPlanNumberIncludeDeleted(@Param("planNumber") String planNumber,
                                         @Param("excludeId") Integer excludeId);
}
