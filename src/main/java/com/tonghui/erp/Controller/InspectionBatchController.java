package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.QualityPages.InspectionTaskDtos;
import com.tonghui.erp.Service.InspectionTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 检验批次控制器
 * <p>
 * 以工单/批次为单位汇总检验任务，供检验管理页“按批次”视图使用。
 * 一个批次下可包含成品、中间产品、生产原料等多条检验任务。
 * </p>
 *
 * 接口清单：
 * ┌────┬────────────────────────────────────────────────────┬────────┬──────────────────────┐
 * │ #  │ 接口                                               │ 方法   │ 说明                 │
 * ├────┼────────────────────────────────────────────────────┼────────┼──────────────────────┤
 * │ 1  │ /api/inspection/batches                            │ GET    │ 分页查询检验批次      │
 * │ 2  │ /api/inspection/batches/stats                      │ GET    │ 检验看板统计          │
 * │ 3  │ /api/inspection/batches/{workOrderId}              │ GET    │ 批次汇总详情          │
 * │ 4  │ /api/inspection/batches/{workOrderId}/tasks        │ GET    │ 查询批次下全部任务    │
 * └────┴────────────────────────────────────────────────────┴────────┴──────────────────────┘
 */
@RestController
@RequestMapping("/api/inspection/batches")
public class InspectionBatchController extends BaseController {

    /** 检验任务服务 */
    @Autowired
    private InspectionTaskService inspectionTaskService;

    /**
     * 分页查询检验批次
     *
     * 示例请求：
     * GET /api/inspection/batches?varietyCode=Z04-011&batchNo=260901&itemCategory=成品&step=INSPECTION&pageIndex=0&pageSize=20
     *
     * @param varietyCode  品种编码（可选）
     * @param batchNo      批号（可选，模糊匹配）
     * @param itemCategory 被检品属性（可选）
     * @param step         当前环节（可选）
     * @param status       任务状态（可选）
     * @param keyword      关键字（任务号/工单号/品种/批号）
     * @param pageIndex    页码，从0开始
     * @param pageSize     每页数量
     * @return 分页批次汇总，每项包含该批次下的检验任务
     */
    @GetMapping
    public ApiResponse<PagedResult<InspectionTaskDtos.BatchSummary>> list(
            @RequestParam(required = false) String varietyCode,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) String itemCategory,
            @RequestParam(required = false) String step,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "20") int pageSize) {
        try {
            return success(inspectionTaskService.queryBatches(varietyCode, batchNo, itemCategory,
                    step, status, keyword, pageIndex, pageSize));
        } catch (Exception e) {
            return exception(e, "查询检验批次");
        }
    }

    /**
     * 查询检验看板统计
     *
     * 示例请求：
     * GET /api/inspection/batches/stats?varietyCode=Z04-011&batchNo=260901&itemCategory=成品
     *
     * @param varietyCode  品种编码（可选）
     * @param batchNo      批号（可选）
     * @param itemCategory 被检品属性（可选）
     * @param status       任务状态（可选）
     * @param keyword      关键字
     * @return 批次/任务计数与六步环节计数
     */
    @GetMapping("/stats")
    public ApiResponse<InspectionTaskDtos.BoardStats> stats(
            @RequestParam(required = false) String varietyCode,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) String itemCategory,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        try {
            return success(inspectionTaskService.queryBoardStats(varietyCode, batchNo, itemCategory, status, keyword));
        } catch (Exception e) {
            return exception(e, "查询检验看板统计");
        }
    }

    /**
     * 查询批次汇总详情
     *
     * 示例请求：GET /api/inspection/batches/463
     *
     * @param workOrderId 工单ID
     * @return 批次汇总（含任务明细）
     */
    @GetMapping("/{workOrderId}")
    public ApiResponse<InspectionTaskDtos.BatchSummary> batch(@PathVariable Long workOrderId) {
        try {
            return success(inspectionTaskService.queryBatch(workOrderId));
        } catch (Exception e) {
            return exception(e, "查询检验批次详情");
        }
    }

    /**
     * 查询指定工单/批次下的全部检验任务
     *
     * 示例请求：GET /api/inspection/batches/463/tasks
     *
     * @param workOrderId 工单ID
     * @return 检验任务列表
     */
    @GetMapping("/{workOrderId}/tasks")
    public ApiResponse<List<InspectionTaskDtos.TaskSummary>> tasks(@PathVariable Long workOrderId) {
        try {
            return success(inspectionTaskService.queryBatchTasks(workOrderId));
        } catch (Exception e) {
            return exception(e, "查询批次检验任务");
        }
    }
}
