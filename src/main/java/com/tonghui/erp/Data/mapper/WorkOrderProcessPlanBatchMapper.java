package com.tonghui.erp.Data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import com.tonghui.erp.Data.Entity.WorkOrderProcessPlanBatch;

/**
 * 工单工序排产批次数据访问Mapper接口
 */
public interface WorkOrderProcessPlanBatchMapper extends BaseMapper<WorkOrderProcessPlanBatch> {
    /**
     * 查询计划行下的批次
     *
     * @param planIds 计划行ID集合
     * @return 批次列表
     */
    @Select("<script>SELECT b.*, r.room_name AS roomName, eq.equipment_name AS equipmentName, t.team_name AS teamName"
            + " FROM work_order_process_plan_batch b"
            + " LEFT JOIN room_info r ON r.room_id = b.room_id AND r.is_deleted = 0"
            + " LEFT JOIN equipment eq ON eq.equipment_id = b.equipment_id AND eq.is_deleted = 0"
            + " LEFT JOIN production_team t ON t.id = b.team_id AND t.is_deleted = 0"
            + " WHERE b.is_deleted = 0 AND b.plan_id IN"
            + " <foreach collection='planIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + " ORDER BY b.plan_id, b.batch_no</script>")
    List<com.tonghui.erp.Data.Entity.WorkOrderProcessPlanBatch> selectByPlanIds(@Param("planIds") List<Long> planIds);

    /**
     * 物理删除计划行下的批次（全量覆盖保存时使用）
     *
     * @param planId 计划行ID
     * @return 删除行数
     */
    @Delete("DELETE FROM work_order_process_plan_batch WHERE plan_id = #{planId}")
    int physicalDeleteByPlanId(@Param("planId") Long planId);

    /**
     * 物理删除单个批次（删除误拆的批次行时使用）
     *
     * @param id 批次ID
     * @return 删除行数
     */
    @Delete("DELETE FROM work_order_process_plan_batch WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);
}
