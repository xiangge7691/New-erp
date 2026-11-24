package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Constants.OperationType;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.Scheduling.GanttItemDto;
import com.tonghui.erp.Common.Dto.Scheduling.PlanBatchDto;
import com.tonghui.erp.Common.Dto.Scheduling.ScheduleConflictDto;
import com.tonghui.erp.Common.Dto.Scheduling.ScheduleDetailDto;
import com.tonghui.erp.Common.Dto.Scheduling.ScheduleSaveRequest;
import com.tonghui.erp.Common.Dto.Scheduling.ScheduleSaveResult;
import com.tonghui.erp.Common.Dto.Scheduling.ScheduleTaskDto;
import com.tonghui.erp.Common.annotation.OperationLog;
import com.tonghui.erp.Service.WorkOrderScheduleService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单排产控制器
 * <p>
 * 提供任务列表、工序排产详情、自动顺排、草稿保存、正式保存、拆批次、冲突检查与甘特图查询。
 * 保存（草稿或正式）时存在冲突会被拒绝并且不落库，冲突明细随响应返回。
 * </p>
 */
@RestController
@RequestMapping("/api/scheduling")
public class WorkOrderScheduleController extends BaseController {

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    /** 工单排产服务 */
    private final WorkOrderScheduleService scheduleService;

    public WorkOrderScheduleController(WorkOrderScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    // endregion

    // region 查询
    // ===================================
    // 查询
    // ===================================

    /**
     * 查询未完成工单的排产任务列表
     *
     * 示例请求：
     * GET /api/scheduling/tasks?keyword=GD2026&status=生产中&configFrom=2026-11-01&configTo=2026-11-30
     *
     * @param keyword    工单编号/工单名称/制剂名称/计划名称/计划编号/批号关键字
     * @param status     生产状态：待生产/生产中/已完工，为空查全部
     * @param configFrom 配置日期起
     * @param configTo   配置日期止
     * @return 任务列表（含工序数、已排产工序数、冲突数、是否已保存）
     */
    @GetMapping("/tasks")
    public ApiResponse<List<ScheduleTaskDto>> tasks(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate configFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate configTo) {
        try {
            return success(scheduleService.listTasks(keyword, status, configFrom, configTo));
        } catch (Exception ex) {
            return exception(ex, "查询排产任务");
        }
    }

    /**
     * 查询单个工单的排产详情
     *
     * 示例请求：
     * GET /api/scheduling/tasks/470
     *
     * @param workOrderId 工单ID
     * @return 计划 + 模板 + 实际 + 冲突
     */
    @GetMapping("/tasks/{workOrderId}")
    public ApiResponse<ScheduleDetailDto> detail(@PathVariable Long workOrderId) {
        try {
            return success(scheduleService.getDetail(workOrderId));
        } catch (Exception ex) {
            return exception(ex, "查询排产详情");
        }
    }

    /**
     * 查询当前工单的冲突（草稿或已保存）
     *
     * 示例请求：
     * POST /api/scheduling/tasks/470/conflicts/check
     *
     * @param workOrderId 工单ID
     * @return 冲突列表
     */
    @PostMapping("/tasks/{workOrderId}/conflicts/check")
    public ApiResponse<List<ScheduleConflictDto>> check(@PathVariable Long workOrderId) {
        try {
            return success(scheduleService.getDetail(workOrderId).getConflicts());
        } catch (Exception ex) {
            return exception(ex, "检查排产冲突");
        }
    }

    /**
     * 查询甘特图数据（支持按任务号、设备、房间筛选）
     *
     * 示例请求：
     * GET /api/scheduling/gantt?view=task&from=2026-11-01T00:00:00&to=2026-11-30T23:59:59
     * GET /api/scheduling/gantt?view=equipment&from=2026-11-01T00:00:00&to=2026-11-30T23:59:59&taskNo=SC-2026&equipmentId=203&roomName=一车间
     * view 可选：task/equipment/room
     *
     * @param view          视图：task/equipment/room
     * @param from          开始时间
     * @param to            结束时间
     * @param taskNo        任务号（可选，工单编号模糊匹配）
     * @param equipmentId   设备ID（可选，精确匹配，批次设备优先于工序设备）
     * @param equipmentName 设备名称（可选，模糊匹配，批次设备优先于工序设备）
     * @param roomId        房间ID（可选，精确匹配，批次房间优先于工序房间）
     * @param roomName      房间名称（可选，模糊匹配，批次房间优先于工序房间）
     * @return 甘特条目
     */
    @GetMapping("/gantt")
    public ApiResponse<List<GanttItemDto>> gantt(
            @RequestParam(defaultValue = "task") String view,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss", fallbackPatterns = {"yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm"}) LocalDateTime from,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss", fallbackPatterns = {"yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm"}) LocalDateTime to,
            @RequestParam(required = false) String taskNo,
            @RequestParam(required = false) Integer equipmentId,
            @RequestParam(required = false) String equipmentName,
            @RequestParam(required = false) Integer roomId,
            @RequestParam(required = false) String roomName) {
        try {
            return success(scheduleService.gantt(view, from, to,
                    taskNo, equipmentId, equipmentName, roomId, roomName));
        } catch (Exception ex) {
            return exception(ex, "查询排产甘特图");
        }
    }

    // endregion

    // region 保存
    // ===================================
    // 保存
    // ===================================

    /**
     * 按模板标准工时自动顺排（保存为草稿；存在冲突时不保存并返回错误）
     *
     * 示例请求：
     * POST /api/scheduling/tasks/470/auto-plan
     *
     * @param workOrderId 工单ID
     * @return 排产详情
     */
    @OperationLog(module = "工序排产", type = OperationType.UPDATE, desc = "自动顺排",
            businessType = "WORK_ORDER_SCHEDULE", businessId = "#workOrderId")
    @PostMapping("/tasks/{workOrderId}/auto-plan")
    public ApiResponse<ScheduleDetailDto> autoPlan(@PathVariable Long workOrderId) {
        try {
            return success(scheduleService.autoPlan(workOrderId), "已按标准工时顺排");
        } catch (Exception ex) {
            return exception(ex, "自动顺排");
        }
    }

    /**
     * 保存排产草稿（存在冲突时不保存，返回冲突明细）
     *
     * 示例请求：
     * PUT /api/scheduling/tasks/470/plan
     * {
     *   "workOrderId": 470,
     *   "processes": [
     *     {
     *       "planId": 1,
     *       "processTypeId": 189,
     *       "stepOrder": 1,
     *       "planStartTime": "2026-11-20T08:00:00",
     *       "planEndTime": "2026-11-20T09:00:00",
     *       "planQty": 7000,
     *       "roomId": 104,
     *       "equipmentId": 203,
     *       "teamId": 1,
     *       "batches": [],
     *       "participants": [{"personnelId": 71}]
     *     }
     *   ]
     * }
     *
     * @param workOrderId 工单ID
     * @param request     排产请求
     * @return 保存结果；存在冲突时 code=400，data.conflicts 返回冲突明细且不落库
     */
    @OperationLog(module = "工序排产", type = OperationType.UPDATE, desc = "保存排产草稿",
            businessType = "WORK_ORDER_SCHEDULE", businessId = "#workOrderId")
    @PutMapping("/tasks/{workOrderId}/plan")
    public ApiResponse<ScheduleSaveResult> saveDraft(@PathVariable Long workOrderId,
                                                     @RequestBody ScheduleSaveRequest request) {
        try {
            request.setWorkOrderId(workOrderId);
            ScheduleSaveResult result = scheduleService.save(workOrderId, request, false);
            if (!result.isSaved()) {
                ApiResponse<ScheduleSaveResult> response = new ApiResponse<>();
                response.setCode(ApiResponse.BAD_REQUEST);
                response.setMessage("存在 " + result.getConflicts().size() + " 项冲突，排产未保存");
                response.setData(result);
                return response;
            }
            return success(result, "草稿已保存");
        } catch (Exception ex) {
            return exception(ex, "保存排产草稿");
        }
    }

    /**
     * 正式保存排产（存在冲突时拒绝保存）
     *
     * 示例请求：
     * POST /api/scheduling/tasks/470/plan/save
     * 请求体同草稿保存。
     *
     * @param workOrderId 工单ID
     * @param request     排产请求
     * @return 保存结果；存在冲突时 code=400，data.conflicts 返回冲突明细
     */
    @OperationLog(module = "工序排产", type = OperationType.UPDATE, desc = "正式保存排产",
            businessType = "WORK_ORDER_SCHEDULE", businessId = "#workOrderId")
    @PostMapping("/tasks/{workOrderId}/plan/save")
    public ApiResponse<ScheduleSaveResult> saveFormal(@PathVariable Long workOrderId,
                                                      @RequestBody ScheduleSaveRequest request) {
        try {
            request.setWorkOrderId(workOrderId);
            ScheduleSaveResult result = scheduleService.save(workOrderId, request, true);
            if (!result.isSaved()) {
                ApiResponse<ScheduleSaveResult> response = new ApiResponse<>();
                response.setCode(ApiResponse.BAD_REQUEST);
                response.setMessage("存在 " + result.getConflicts().size() + " 项冲突，排产未保存");
                response.setData(result);
                return response;
            }
            return success(result, "排产已保存");
        } catch (Exception ex) {
            return exception(ex, "正式保存排产");
        }
    }

    /**
     * 拆分工序批次
     *
     * 示例请求：
     * POST /api/scheduling/plans/1/split
     * [
     *   {"batchNo":1,"batchName":"批次 1","planStartTime":"2026-11-20T08:00:00","planEndTime":"2026-11-20T10:00:00","planQty":3500},
     *   {"batchNo":2,"batchName":"批次 2","planStartTime":"2026-11-20T10:00:00","planEndTime":"2026-11-20T12:00:00","planQty":3500}
     * ]
     *
     * @param planId  计划行ID
     * @param batches 批次列表
     * @return 保存结果
     */
    @OperationLog(module = "工序排产", type = OperationType.UPDATE, desc = "拆分工序批次",
            businessType = "WORK_ORDER_SCHEDULE", businessId = "#planId")
    @PostMapping("/plans/{planId}/split")
    public ApiResponse<ScheduleSaveResult> split(@PathVariable Long planId,
                                                 @RequestBody List<PlanBatchDto> batches) {
        try {
            ScheduleSaveResult result = scheduleService.split(planId, batches);
            if (!result.isSaved()) {
                ApiResponse<ScheduleSaveResult> response = new ApiResponse<>();
                response.setCode(ApiResponse.BAD_REQUEST);
                response.setMessage("存在 " + result.getConflicts().size() + " 项冲突，批次未保存");
                response.setData(result);
                return response;
            }
            return success(result, "批次已保存");
        } catch (Exception ex) {
            return exception(ex, "拆分工序批次");
        }
    }

    /**
     * 删除误拆的批次行
     *
     * 示例请求：
     * DELETE /api/scheduling/plans/1/batches/12
     *
     * 批次已完工或已登记实际生产时拒绝删除；删除后剩余批次自动重排序号，
     * 工序计划时间同步为剩余批次的最早开始与最晚结束。
     *
     * @param planId  计划行ID
     * @param batchId 批次ID
     * @return 删除后的排产结果；processes 为最新工序行，conflicts 为最新冲突明细
     */
    @OperationLog(module = "工序排产", type = OperationType.DELETE, desc = "删除排产批次",
            businessType = "WORK_ORDER_SCHEDULE", businessId = "#planId")
    @DeleteMapping("/plans/{planId}/batches/{batchId}")
    public ApiResponse<ScheduleSaveResult> deleteBatch(@PathVariable Long planId,
                                                       @PathVariable Long batchId) {
        try {
            return success(scheduleService.deleteBatch(planId, batchId), "批次已删除");
        } catch (Exception ex) {
            return exception(ex, "删除排产批次");
        }
    }

    /**
     * 删除误加的整道排产工序行
     *
     * 示例请求：
     * DELETE /api/scheduling/plans/1
     *
     * 工序下已登记实际生产（存在执行记录）或存在已完工批次时拒绝删除；
     * 删除后不压缩其余工序的序号，与制剂工序模板及执行记录保持一致。
     *
     * @param planId 计划行ID
     * @return 删除后的排产结果；processes 为最新工序行，conflicts 为最新冲突明细
     */
    @OperationLog(module = "工序排产", type = OperationType.DELETE, desc = "删除排产工序行",
            businessType = "WORK_ORDER_SCHEDULE", businessId = "#planId")
    @DeleteMapping("/plans/{planId}")
    public ApiResponse<ScheduleSaveResult> deletePlan(@PathVariable Long planId) {
        try {
            return success(scheduleService.deletePlan(planId), "工序行已删除");
        } catch (Exception ex) {
            return exception(ex, "删除排产工序行");
        }
    }

    // endregion
}
