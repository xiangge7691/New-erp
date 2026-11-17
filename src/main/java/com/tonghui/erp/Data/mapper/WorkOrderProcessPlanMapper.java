package com.tonghui.erp.Data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import com.tonghui.erp.Data.Entity.WorkOrderProcessPlan;

/**
 * 工单工序排产计划数据访问Mapper接口
 */
public interface WorkOrderProcessPlanMapper extends BaseMapper<WorkOrderProcessPlan> {
    /**
     * 查询工单下的排产计划（含工序、房间、设备、班组名称）
     *
     * @param workOrderId 工单ID
     * @return 计划列表
     */
    @Select("SELECT p.*, pt.process_name AS processTypeName,"
            + " r.room_name AS roomName, eq.equipment_name AS equipmentName, t.team_name AS teamName,"
            + " w.work_order_code AS workOrderCode"
            + " FROM work_order_process_plan p"
            + " LEFT JOIN process_type pt ON pt.process_id = p.process_type_id AND pt.is_deleted = 0"
            + " LEFT JOIN room_info r ON r.room_id = p.room_id AND r.is_deleted = 0"
            + " LEFT JOIN equipment eq ON eq.equipment_id = p.equipment_id AND eq.is_deleted = 0"
            + " LEFT JOIN production_team t ON t.id = p.team_id AND t.is_deleted = 0"
            + " LEFT JOIN work_order w ON w.work_order_id = p.work_order_id AND w.is_deleted = 0"
            + " WHERE p.work_order_id = #{workOrderId} AND p.is_deleted = 0"
            + " ORDER BY p.step_order ASC")
    List<com.tonghui.erp.Data.Entity.WorkOrderProcessPlan> selectByWorkOrderIdWithDetails(@Param("workOrderId") Long workOrderId);

    /**
     * 查询与时间窗重叠的已保存计划（不含草稿、不含指定工单）
     *
     * @param minStart         最早开始时间
     * @param maxEnd           最晚结束时间
     * @param excludeWorkOrderId 排除的工单ID
     * @return 重叠计划列表
     */
    @Select("SELECT p.*, pt.process_name AS processTypeName,"
            + " r.room_name AS roomName, eq.equipment_name AS equipmentName, t.team_name AS teamName,"
            + " w.work_order_code AS workOrderCode"
            + " FROM work_order_process_plan p"
            + " LEFT JOIN process_type pt ON pt.process_id = p.process_type_id AND pt.is_deleted = 0"
            + " LEFT JOIN room_info r ON r.room_id = p.room_id AND r.is_deleted = 0"
            + " LEFT JOIN equipment eq ON eq.equipment_id = p.equipment_id AND eq.is_deleted = 0"
            + " LEFT JOIN production_team t ON t.id = p.team_id AND t.is_deleted = 0"
            + " LEFT JOIN work_order w ON w.work_order_id = p.work_order_id AND w.is_deleted = 0"
            + " WHERE p.is_deleted = 0 AND p.status <> '草稿'"
            + "   AND p.plan_start_time IS NOT NULL AND p.plan_end_time IS NOT NULL"
            + "   AND p.work_order_id <> #{excludeWorkOrderId}"
            + "   AND p.plan_start_time < #{maxEnd} AND p.plan_end_time > #{minStart}"
            + " ORDER BY p.plan_start_time ASC")
    List<com.tonghui.erp.Data.Entity.WorkOrderProcessPlan> selectOverlappingSavedPlans(
            @Param("minStart") LocalDateTime minStart,
            @Param("maxEnd") LocalDateTime maxEnd,
            @Param("excludeWorkOrderId") Long excludeWorkOrderId);
}
