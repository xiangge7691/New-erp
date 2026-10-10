package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.ApiResponse;
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
     */
    @PostMapping("/batch")
    public ApiResponse<Boolean> batchSave(@RequestParam Long workOrderId,
                                          @RequestBody List<WorkOrderProcessExecution> executions) {
        try {
            workOrderProcessExecutionService.batchSave(workOrderId, executions);
            return success(true, "保存成功");
        } catch (Exception ex) {
            return exception(ex, "保存工序执行记录失败");
        }
    }
}
