package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Constants.OperationType;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.Pda.PdaExecutionSaveRequest;
import com.tonghui.erp.Common.Dto.Pda.PdaOptionDto;
import com.tonghui.erp.Common.Dto.Pda.PdaProcessDto;
import com.tonghui.erp.Common.Dto.Pda.PdaPunchRequest;
import com.tonghui.erp.Common.Dto.Pda.PdaRecordDto;
import com.tonghui.erp.Common.Dto.Pda.PdaTaskDto;
import com.tonghui.erp.Common.annotation.OperationLog;
import com.tonghui.erp.Service.PdaProductionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * PDA 生产工序执行控制器
 * <p>
 * 仅提供移动端页面所需接口；不做接口级权限校验，由 JWT 认证与前端菜单控制访问。
 * </p>
 */
@RestController
@RequestMapping("/api/pda/production")
public class PdaProductionController extends BaseController {

    @Autowired
    private PdaProductionService pdaProductionService;

    /**
     * PDA 任务列表（任务—工序两级）
     *
     * 示例请求：
     * GET /api/pda/production/tasks?status=进行中&personId=12&processTypeId=3&keyword=复方
     *
     * @param status        任务状态：未开始/进行中/已完成
     * @param personId      操作人档案ID
     * @param processTypeId 工序类型ID
     * @param workshop      车间（预留）
     * @param keyword       制剂名称/批号/工单号
     * @param todoOnly      true 时只返回仍有待办工序的工单（查看“已完成”时除外）
     * @return 任务列表
     */
    @GetMapping("/tasks")
    public ApiResponse<List<PdaTaskDto>> tasks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long personId,
            @RequestParam(required = false) Long processTypeId,
            @RequestParam(required = false) String workshop,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean todoOnly) {
        try {
            return success(pdaProductionService.listTasks(status, personId, processTypeId, workshop,
                    keyword, Boolean.TRUE.equals(todoOnly)));
        } catch (Exception ex) {
            return exception(ex, "查询PDA生产任务");
        }
    }

    /**
     * 单个 PDA 任务详情
     *
     * 示例请求：
     * GET /api/pda/production/tasks/470
     *
     * @param workOrderId 工单ID
     * @return 任务详情
     */
    @GetMapping("/tasks/{workOrderId}")
    public ApiResponse<PdaTaskDto> task(@PathVariable Long workOrderId) {
        try {
            return success(pdaProductionService.getTask(workOrderId));
        } catch (Exception ex) {
            return exception(ex, "查询PDA生产任务详情");
        }
    }

    /**
     * 确保拆分批次已有执行记录（不存在则创建）
     * <p>PDA 任务详情按批次展开后，尚未登记实际生产的批次行没有 executionId，
     * 开始计时或保存前先调用本接口取得批次执行记录。</p>
     *
     * 示例请求：
     * POST /api/pda/production/tasks/480/batches/146/execution
     *
     * @param workOrderId 工单ID
     * @param planBatchId 排产批次ID
     * @return 批次执行行（含 executionId）
     */
    @OperationLog(module = "PDA生产执行", type = OperationType.CREATE, desc = "准备批次执行记录",
            businessType = "WORK_ORDER_PROCESS_EXECUTION", businessId = "#planBatchId")
    @PostMapping("/tasks/{workOrderId}/batches/{planBatchId}/execution")
    public ApiResponse<PdaProcessDto> ensureBatchExecution(@PathVariable Long workOrderId,
                                                           @PathVariable Long planBatchId) {
        try {
            return success(pdaProductionService.ensureBatchExecution(workOrderId, planBatchId),
                    "已准备批次执行记录");
        } catch (Exception ex) {
            return exception(ex, "准备批次执行记录");
        }
    }

    /**
     * 工序开始打点
     *
     * 示例请求：
     * POST /api/pda/production/executions/123/start
     * {"equipmentId": 5, "deviceNo": "PDA-A03"}
     *
     * @param executionId 执行记录ID
     * @param request     请求
     * @return 更新后的工序
     */
    @OperationLog(module = "PDA生产执行", type = OperationType.UPDATE, desc = "工序开始打点",
            businessType = "WORK_ORDER_PROCESS_EXECUTION", businessId = "#executionId")
    @PostMapping("/executions/{executionId}/start")
    public ApiResponse<PdaProcessDto> start(@PathVariable Long executionId,
                                            @RequestBody PdaPunchRequest request) {
        try {
            return success(pdaProductionService.start(executionId, request), "已开始计时");
        } catch (Exception ex) {
            return exception(ex, "工序开始打点");
        }
    }

    /**
     * 工序结束打点
     *
     * 示例请求：
     * POST /api/pda/production/executions/123/end
     * {"deviceNo": "PDA-A03"}
     *
     * @param executionId 执行记录ID
     * @param request     请求
     * @return 更新后的工序
     */
    @OperationLog(module = "PDA生产执行", type = OperationType.UPDATE, desc = "工序结束打点",
            businessType = "WORK_ORDER_PROCESS_EXECUTION", businessId = "#executionId")
    @PostMapping("/executions/{executionId}/end")
    public ApiResponse<PdaProcessDto> end(@PathVariable Long executionId,
                                          @RequestBody PdaPunchRequest request) {
        try {
            return success(pdaProductionService.end(executionId, request), "已结束计时");
        } catch (Exception ex) {
            return exception(ex, "工序结束打点");
        }
    }

    /**
     * 暂存工序执行信息
     *
     * 示例请求：
     * PUT /api/pda/production/executions/123/draft
     * {"equipmentId":5,"startTime":"2026-11-19T08:00:00","processQty":100,
     *  "participants":[{"personnelId":12,"completedQty":100}],"deviceNo":"PDA-A03"}
     *
     * @param executionId 执行记录ID
     * @param request     请求
     * @return 更新后的工序
     */
    @OperationLog(module = "PDA生产执行", type = OperationType.UPDATE, desc = "工序暂存",
            businessType = "WORK_ORDER_PROCESS_EXECUTION", businessId = "#executionId")
    @PutMapping("/executions/{executionId}/draft")
    public ApiResponse<PdaProcessDto> draft(@PathVariable Long executionId,
                                            @RequestBody PdaExecutionSaveRequest request) {
        try {
            return success(pdaProductionService.saveDraft(executionId, request), "已暂存");
        } catch (Exception ex) {
            return exception(ex, "工序暂存");
        }
    }

    /**
     * 提交工序执行信息
     *
     * 示例请求：
     * POST /api/pda/production/executions/123/submit
     * {"equipmentId":5,"startTime":"2026-11-19T08:00:00","endTime":"2026-11-19T10:00:00",
     *  "processQty":100,"participants":[{"personnelId":12,"completedQty":100}],
     *  "deviceNo":"PDA-A03","requestId":"uuid"}
     *
     * @param executionId 执行记录ID
     * @param request     请求
     * @return 更新后的工序
     */
    @OperationLog(module = "PDA生产执行", type = OperationType.UPDATE, desc = "工序提交",
            businessType = "WORK_ORDER_PROCESS_EXECUTION", businessId = "#executionId")
    @PostMapping("/executions/{executionId}/submit")
    public ApiResponse<PdaProcessDto> submit(@PathVariable Long executionId,
                                             @RequestBody PdaExecutionSaveRequest request) {
        try {
            return success(pdaProductionService.submit(executionId, request), "提交成功");
        } catch (Exception ex) {
            return exception(ex, "工序提交");
        }
    }

    /**
     * PDA 提交记录
     *
     * 示例请求：
     * GET /api/pda/production/records?keyword=复方&processTypeId=3&personId=12
     *
     * @param keyword       关键字
     * @param processTypeId 工序类型ID
     * @param personId      操作人档案ID
     * @return 提交记录
     */
    @GetMapping("/records")
    public ApiResponse<List<PdaRecordDto>> records(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long processTypeId,
            @RequestParam(required = false) Long personId) {
        try {
            return success(pdaProductionService.listRecords(keyword, processTypeId, personId));
        } catch (Exception ex) {
            return exception(ex, "查询PDA提交记录");
        }
    }

    /**
     * 启用设备选项
     *
     * 示例请求：
     * GET /api/pda/production/equipment/options?keyword=F-301&roomId=104
     *
     * @param keyword 名称/编号关键字
     * @param roomId  配置室ID，传入时只返回该房间的设备
     * @return 设备选项
     */
    @GetMapping("/equipment/options")
    public ApiResponse<List<PdaOptionDto>> equipmentOptions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer roomId) {
        try {
            return success(pdaProductionService.equipmentOptions(keyword, roomId));
        } catch (Exception ex) {
            return exception(ex, "查询PDA设备选项");
        }
    }

    /**
     * 在职人员选项
     *
     * 示例请求：
     * GET /api/pda/production/personnel/options?keyword=张
     *
     * @param keyword 姓名/工号关键字
     * @return 人员选项
     */
    @GetMapping("/personnel/options")
    public ApiResponse<List<PdaOptionDto>> personnelOptions(
            @RequestParam(required = false) String keyword) {
        try {
            return success(pdaProductionService.personnelOptions(keyword));
        } catch (Exception ex) {
            return exception(ex, "查询PDA人员选项");
        }
    }
}
