package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Constants.OperationType;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.annotation.OperationLog;
import com.tonghui.erp.Data.Entity.WorkOrderProcessExecution;
import com.tonghui.erp.Service.WorkOrderProcessExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工单工序执行记录控制器
 */
@RestController
@RequestMapping("/api/work-order-process-execution")
public class WorkOrderProcessExecutionController extends BaseController {

    @Autowired
    private WorkOrderProcessExecutionService workOrderProcessExecutionService;

    /**
     * 根据工单ID查询工序执行记录列表
     *
     * 示例请求：
     * GET /api/work-order-process-execution/by-work-order/1
     *
     * 每条记录除执行信息外，还返回关联的制剂工序模版ID（templateId），
     * 按工单制剂+工序类型+工序顺序匹配，无匹配模版时为null。
     *
     * @param workOrderId 工单ID
     * @return 工序执行记录列表（含工序类型名称、房间名称、设备名称、模版ID等关联字段）
     */
    @GetMapping("/by-work-order/{workOrderId}")
    public ApiResponse<List<WorkOrderProcessExecution>> getByWorkOrderId(@PathVariable Long workOrderId) {
        try {
            List<WorkOrderProcessExecution> list = workOrderProcessExecutionService.getByWorkOrderId(workOrderId);
            return success(list);
        } catch (Exception ex) {
            return exception(ex, "查询工序执行记录失败");
        }
    }

    /**
     * 批量保存工序执行记录
     * <p>
     * 已有行按 id 原地更新（保留ID，便于按行附件关联），无 id 的行新增，
     * 数据库中未被提交的行将被删除。请确保已有行携带原 id，新增行不携带。
     * </p>
     * <p>
     * 保存前会校验设备使用合法性：设备必须为"正常"状态、同一设备的开始/结束时间不得重叠、
     * 使用日期不得与设备维保日期重叠。校验不通过返回 400，message 为具体冲突明细。
     * </p>
     * <p>
     * 工序状态由后端按时间推导并落库，前端无需传 status：
     * 开始与结束时间齐全=已完成，仅有开始时间=执行中，均未填写=待执行。
     * 只填开始时间表示工序执行中，此时该工序按开放区间持续占用设备。
     * </p>
     *
     * 示例请求：
     * POST /api/work-order-process-execution/batch?workOrderId=1
     * Content-Type: application/json
     * [
     *   {
     *     "id": 10,
     *     "processTypeId": 1,
     *     "stepOrder": 1,
     *     "operatorName": "张三",
     *     "startTime": "2026-11-10T08:00:00",
     *     "endTime": "2026-11-10T12:00:00"
     *   },
     *   {
     *     "processTypeId": 2,
     *     "stepOrder": 2,
     *     "operatorName": "李四",
     *     "startTime": "2026-11-10T13:00:00"
     *   }
     * ]
     *
     * @param workOrderId 工单ID（请求参数）
     * @param executions  工序执行记录列表（已有行带 id，新增行不带）
     * @return ApiResponse&lt;List&lt;WorkOrderProcessExecution&gt;&gt; 保存后的工序执行记录列表（含自增ID与推导后的状态）
     */
    @OperationLog(module = "工单工序执行", type = OperationType.UPDATE, desc = "批量保存工单工序执行",
            businessType = "WORK_ORDER_PROCESS_EXECUTION", businessId = "#workOrderId")
    @PostMapping("/batch")
    public ApiResponse<List<WorkOrderProcessExecution>> batchSave(@RequestParam Long workOrderId,
                                                                  @RequestBody List<WorkOrderProcessExecution> executions) {
        try {
            workOrderProcessExecutionService.batchSave(workOrderId, executions);
            return success(workOrderProcessExecutionService.getByWorkOrderId(workOrderId), "保存成功");
        } catch (Exception ex) {
            return exception(ex, "保存工序执行记录失败");
        }
    }
}
