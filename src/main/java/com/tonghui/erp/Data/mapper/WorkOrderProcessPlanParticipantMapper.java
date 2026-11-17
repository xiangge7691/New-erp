package com.tonghui.erp.Data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import com.tonghui.erp.Data.Entity.WorkOrderProcessPlanParticipant;

/**
 * 排产计划参与人数据访问Mapper接口
 */
public interface WorkOrderProcessPlanParticipantMapper extends BaseMapper<WorkOrderProcessPlanParticipant> {
    /**
     * 查询计划参与人（含姓名）
     *
     * @param planIds 计划行ID集合
     * @return 参与人列表
     */
    @Select("<script>SELECT pp.*, p.name AS personnelName"
            + " FROM work_order_process_plan_participant pp"
            + " LEFT JOIN personnel_file p ON p.personnel_file_id = pp.personnel_id AND p.is_deleted = 0"
            + " WHERE pp.is_deleted = 0 AND pp.plan_id IN"
            + " <foreach collection='planIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + " ORDER BY pp.plan_id, pp.batch_id, pp.id</script>")
    List<com.tonghui.erp.Data.Entity.WorkOrderProcessPlanParticipant> selectByPlanIdsWithPersonnel(@Param("planIds") List<Long> planIds);

    /**
     * 物理删除计划行下的参与人（全量覆盖保存时使用）
     *
     * @param planId 计划行ID
     * @return 删除行数
     */
    @Delete("DELETE FROM work_order_process_plan_participant WHERE plan_id = #{planId}")
    int physicalDeleteByPlanId(@Param("planId") Long planId);

    /**
     * 物理删除计划行下的批次参与人（保留工序级参与人）
     *
     * @param planId 计划行ID
     * @return 删除行数
     */
    @Delete("DELETE FROM work_order_process_plan_participant WHERE plan_id = #{planId} AND batch_id IS NOT NULL")
    int physicalDeleteBatchParticipants(@Param("planId") Long planId);
}
