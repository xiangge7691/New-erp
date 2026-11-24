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
    List<ScheduleTaskDto> listTasks(String keyword, String status,
                                    java.time.LocalDate configFrom, java.time.LocalDate configTo);

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
     * 删除误拆的批次行
     * <p>批次已完工或已登记实际生产时拒绝删除，避免执行记录悬挂</p>
     *
     * @param planId  计划行ID
     * @param batchId 批次ID
     * @return 删除后的排产结果（含最新冲突明细）
     */
    ScheduleSaveResult deleteBatch(Long planId, Long batchId);

    /**
     * 删除误加的整道排产工序行
     * <p>工序下已登记实际生产或存在已完工批次时拒绝删除</p>
     *
     * @param planId 计划行ID
     * @return 删除后的排产结果（含最新冲突明细）
     */
    ScheduleSaveResult deletePlan(Long planId);

    /**
     * 查询甘特图数据
     *
     * @param view          视图：task/equipment/room
     * @param from          开始时间
     * @param to            结束时间
     * @param taskNo        任务号（可选，工单编号模糊匹配）
     * @param equipmentId   设备ID（可选，精确匹配，批次设备优先于工序设备）
     * @param equipmentName 设备名称（可选，模糊匹配，批次设备优先于工序设备）
     * @param roomId        房间ID（可选，精确匹配，批次房间优先于工序房间）
     * @param roomName      房间名称（可选，模糊匹配，批次房间优先于工序房间）
     * @return 甘特条目（冲突仍按工单全部计划计算，筛选只影响返回条数）
     */
    List<GanttItemDto> gantt(String view, LocalDateTime from, LocalDateTime to,
                             String taskNo, Integer equipmentId, String equipmentName,
                             Integer roomId, String roomName);
}
