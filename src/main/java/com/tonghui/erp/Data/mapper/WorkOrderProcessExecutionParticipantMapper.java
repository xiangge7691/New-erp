package com.tonghui.erp.Data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import com.tonghui.erp.Data.Entity.WorkOrderProcessExecutionParticipant;

/**
 * 工单工序实际参与人数据访问Mapper接口
 */
public interface WorkOrderProcessExecutionParticipantMapper extends BaseMapper<WorkOrderProcessExecutionParticipant> {
    /**
     * 查询工序执行记录的参与人（含姓名、工号）
     *
     * @param executionIds 工序执行记录ID集合
     * @return 参与人列表
     */
    @Select("<script>SELECT ep.*, p.name AS personnelName, p.employee_no AS employeeNo,"
            + " (SELECT GROUP_CONCAT(DISTINCT t.team_name ORDER BY t.team_name SEPARATOR '、')"
            + "    FROM production_team_member tm"
            + "    JOIN production_team t ON t.id = tm.team_id AND t.is_deleted = 0"
            + "   WHERE tm.personnel_id = ep.personnel_id AND tm.is_deleted = 0) AS teamName"
            + " FROM work_order_process_execution_participant ep"
            + " LEFT JOIN personnel_file p ON p.personnel_file_id = ep.personnel_id AND p.is_deleted = 0"
            + " WHERE ep.is_deleted = 0 AND ep.execution_id IN"
            + " <foreach collection='executionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + " ORDER BY ep.execution_id, ep.is_primary DESC, ep.id</script>")
    List<com.tonghui.erp.Data.Entity.WorkOrderProcessExecutionParticipant> selectByExecutionIdsWithPersonnel(
            @Param("executionIds") List<Long> executionIds);

    /**
     * 物理删除工序执行记录下的参与人（全量覆盖保存时使用）
     *
     * @param executionId 工序执行记录ID
     * @return 删除行数
     */
    @Delete("DELETE FROM work_order_process_execution_participant WHERE execution_id = #{executionId}")
    int physicalDeleteByExecutionId(@Param("executionId") Long executionId);
}
