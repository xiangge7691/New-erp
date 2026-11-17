package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Dto.Scheduling.GanttItemDto;
import com.tonghui.erp.Common.Dto.Scheduling.PlanBatchDto;
import com.tonghui.erp.Common.Dto.Scheduling.ScheduleDetailDto;
import com.tonghui.erp.Common.Dto.Scheduling.ScheduleSaveRequest;
import com.tonghui.erp.Common.Dto.Scheduling.ScheduleSaveResult;
import com.tonghui.erp.Common.Dto.Scheduling.ScheduleTaskDto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单排产服务接口
 */
public interface WorkOrderScheduleService {

    /**
     * 查询未完成工单的排产任务列表
     *
     * @param keyword 工单编号/制剂名称关键字
     * @return 任务列表
     */
    List<ScheduleTaskDto> listTasks(String keyword);

    /**
     * 查询单个工单的排产详情
     *
     * @param workOrderId 工单ID
     * @return 排产详情（计划 + 模板 + 实际 + 冲突）
     */
    ScheduleDetailDto getDetail(Long workOrderId);

    /**
     * 按工序模板标准工时自动顺排（保存为草稿）
     *
     * @param workOrderId 工单ID
     * @return 排产详情
     */
    ScheduleDetailDto autoPlan(Long workOrderId);

    /**
     * 保存排产
     *
     * @param workOrderId 工单ID
     * @param request     排产请求
     * @param formal      是否正式保存（true 时有冲突拒绝保存）
     * @return 保存结果
     */
    ScheduleSaveResult save(Long workOrderId, ScheduleSaveRequest request, boolean formal);

    /**
     * 拆分工序批次
     *
     * @param planId  计划行ID
     * @param batches 批次
     * @return 保存结果
     */
    ScheduleSaveResult split(Long planId, List<PlanBatchDto> batches);

    /**
     * 查询甘特图数据
     *
     * @param view 视图：task/equipment/room
     * @param from 开始时间
     * @param to   结束时间
     * @return 甘特条目
     */
    List<GanttItemDto> gantt(String view, LocalDateTime from, LocalDateTime to);
}
