package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Constants.OperationType;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.annotation.OperationLog;
import com.tonghui.erp.Data.Entity.WorkOrderProcessExecution;
import com.tonghui.erp.Service.WorkOrderProcessExecutionService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

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
     * 查询已完工（锁定）的工序执行记录
     *
     * 示例请求：
     * GET /api/work-order-process-execution/completed?from=2026-11-01&to=2026-11-30&workOrderId=470
     *
     * 返回记录已锁定，且包含参与人、计划起止/数量、已生成绩效记录数，
     * 供「完工记录」页面直接展示。
     *
     * @param from        完工日期起（可选）
     * @param to          完工日期止（可选）
     * @param workOrderId 工单ID（可选）
     * @return 已完工工序执行记录列表
     */
    @GetMapping("/completed")
    public ApiResponse<List<WorkOrderProcessExecution>> completed(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long workOrderId) {
        try {
            return success(workOrderProcessExecutionService.listCompleted(from, to, workOrderId));
        } catch (Exception ex) {
            return exception(ex, "查询完工记录失败");
        }
    }

    /**
     * 导出已完工的工序执行记录
     *
     * 示例请求：
     * GET /api/work-order-process-execution/completed/export?from=2026-11-01&to=2026-11-30
     *
     * @param from        完工日期起（可选）
     * @param to          完工日期止（可选）
     * @param workOrderId 工单ID（可选）
     * @param response    HTTP响应
     */
    @OperationLog(module = "工单工序执行", type = OperationType.EXPORT, desc = "导出完工记录")
    @GetMapping("/completed/export")
    public void exportCompleted(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long workOrderId,
            HttpServletResponse response) throws IOException {
        List<WorkOrderProcessExecution> list =
                workOrderProcessExecutionService.listCompleted(from, to, workOrderId);
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("完工记录");
            String[] headers = {"生产任务", "批号", "制剂", "工序", "批次", "计划起止", "实际起止",
                    "实际数量", "参与人", "生成绩效数", "完工时间"};
            Row head = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                head.createCell(i).setCellValue(headers[i]);
            }
            int rowIndex = 1;
            for (WorkOrderProcessExecution execution : list) {
                Row row = sheet.createRow(rowIndex++);
                int c = 0;
                row.createCell(c++).setCellValue(nullSafe(execution.getWorkOrderCode()));
                row.createCell(c++).setCellValue(nullSafe(execution.getBatchNumber()));
                row.createCell(c++).setCellValue(nullSafe(execution.getPreparationName()));
                row.createCell(c++).setCellValue(nullSafe(execution.getProcessTypeName()));
                row.createCell(c++).setCellValue(nullSafe(execution.getPlanBatchName()));
                row.createCell(c++).setCellValue(period(execution.getPlanStartTime(), execution.getPlanEndTime()));
                row.createCell(c++).setCellValue(period(execution.getStartTime(), execution.getEndTime()));
                row.createCell(c++).setCellValue(decimal(execution.getProcessQty()));
                String participants = execution.getParticipants() == null ? "" : execution.getParticipants().stream()
                        .map(p -> p.getPersonnelName() == null ? "" : p.getPersonnelName())
                        .filter(name -> !name.isEmpty())
                        .collect(Collectors.joining("、"));
                row.createCell(c++).setCellValue(participants);
                row.createCell(c++).setCellValue(execution.getPerformanceRecordCount() == null
                        ? 0 : execution.getPerformanceRecordCount());
                row.createCell(c).setCellValue(period(execution.getCompletedTime(), null));
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=completed-executions.xlsx");
            workbook.write(response.getOutputStream());
        }
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private String decimal(BigDecimal value) {
        return value == null ? "" : value.toPlainString();
    }

    /**
     * 格式化起止时间区间
     */
    private String period(LocalDateTime start, LocalDateTime end) {
        if (start == null && end == null) {
            return "";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String startText = start == null ? "" : start.format(formatter);
        String endText = end == null ? "" : end.format(formatter);
        return startText + " ~ " + endText;
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
     * 同时校验实际时间：不得早于任务配置时间、不得晚于任务配置完成时间（未填写时只校验下界），
     * 且不得早于前序工序的实际结束时间（前序工序未完工时不允许开始）。
     * </p>
     * <p>
     * 工序状态由后端按时间推导并落库，前端无需传 status：
     * 开始与结束时间齐全=已完成，仅有开始时间=执行中，均未填写=待执行。
     * 只填开始时间表示工序执行中，此时该工序按开放区间持续占用设备；
     * 结束实际生产请调用 POST /{executionId}/complete，不要用保存接口补结束时间。
     * 更新既有行时，请求未携带的开始/结束时间、数量等执行数据沿用原值，不会因局部保存被清空。
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

    /**
     * 工序/批次完工（登记实际结束时间并锁定）
     * <p>
     * 「实际生产」保存只登记实际开始时间（状态=执行中），完工时调用本接口：
     * 结束时间可空，为空则取当前时间；传入时必须晚于实际开始时间，并落在工单配置时间窗内。
     * 完工后锁定实际执行记录，按实际参与人与个人工作时间生成人员绩效；
     * 已锁定的工序不可重复完工。
     * </p>
     *
     * 示例请求：
     * POST /api/work-order-process-execution/123/complete
     * POST /api/work-order-process-execution/123/complete?endTime=2026-11-19T18:00:00
     *
     * @param executionId 工序执行记录ID
     * @param endTime     实际结束时间（可选，ISO 格式；为空取当前时间）
     * @return 完工后的执行记录（含参与人）
     */
    @OperationLog(module = "工单工序执行", type = OperationType.UPDATE, desc = "工序完工",
            businessType = "WORK_ORDER_PROCESS_EXECUTION", businessId = "#executionId")
    @PostMapping("/{executionId}/complete")
    public ApiResponse<WorkOrderProcessExecution> complete(
            @PathVariable Long executionId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            return success(workOrderProcessExecutionService.complete(executionId, endTime), "完工成功");
        } catch (Exception ex) {
            return exception(ex, "工序完工");
        }
    }

    /**
     * 删除误拆的工序执行行
     *
     * 示例请求：
     * DELETE /api/work-order-process-execution/123
     *
     * 仅允许删除未完工锁定、未通过 PDA 提交、未生成绩效的执行行；
     * 删除时同时清理该行的参与人与照片附件。
     *
     * @param executionId 工序执行记录ID
     * @return 删除后该工单的最新工序执行记录列表
     */
    @OperationLog(module = "工单工序执行", type = OperationType.DELETE, desc = "删除工序执行行",
            businessType = "WORK_ORDER_PROCESS_EXECUTION", businessId = "#executionId")
    @DeleteMapping("/{executionId}")
    public ApiResponse<List<WorkOrderProcessExecution>> delete(@PathVariable Long executionId) {
        try {
            Long workOrderId = workOrderProcessExecutionService.deleteExecution(executionId);
            return success(workOrderProcessExecutionService.getByWorkOrderId(workOrderId), "已删除");
        } catch (Exception ex) {
            return exception(ex, "删除工序执行记录失败");
        }
    }
}
