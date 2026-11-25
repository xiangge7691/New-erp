package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Constants.OperationType;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Performance.CoefficientPublishRequest;
import com.tonghui.erp.Common.Dto.Performance.CoefficientVersionDetail;
import com.tonghui.erp.Common.Dto.Performance.PerformanceManualRequest;
import com.tonghui.erp.Common.Dto.Performance.PerformanceModifyRequest;
import com.tonghui.erp.Common.annotation.OperationLog;
import com.tonghui.erp.Data.Entity.PerformanceCoefficientVersion;
import com.tonghui.erp.Data.Entity.PerformanceRecord;
import com.tonghui.erp.Service.PerformanceService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 人员绩效控制器
 * <p>
 * 提供绩效记录查询、修改、手动补录、作废、系数版本发布与导出。
 * </p>
 */
@RestController
@RequestMapping("/api/performance")
public class PerformanceController extends BaseController {

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    /** 人员绩效服务 */
    private final PerformanceService performanceService;

    public PerformanceController(PerformanceService performanceService) {
        this.performanceService = performanceService;
    }

    // endregion

    // region 查询
    // ===================================
    // 查询
    // ===================================

    /**
     * 分页查询绩效记录（支持关键字与工序粒度跨任务查询）
     *
     * 示例请求：
     * GET /api/performance/records?from=2026-11-01&to=2026-11-30&personnelId=71&source=系统生成&modifiedStatus=1&recordStatus=1&keyword=板蓝根&pageIndex=0&pageSize=20
     *
     * @param from          绩效归属日期起
     * @param to            绩效归属日期止
     * @param personnelId   人员档案ID
     * @param workOrderId   工单ID
     * @param teamId        班组ID
     * @param source        记录来源：系统生成/手动添加
     * @param modifiedStatus 修改状态：0未修改/1已修改
     * @param recordStatus  记录状态：1正常/0作废
     * @param keyword       工单号/工单名称/制剂名称/批号/计划名称/计划编号关键字
     * @param pageRequest   分页参数
     * @return 绩效记录分页
     */
    @GetMapping("/records")
    public ApiResponse<PagedResult<PerformanceRecord>> records(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long personnelId,
            @RequestParam(required = false) Long workOrderId,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) Integer modifiedStatus,
            @RequestParam(required = false) Integer recordStatus,
            @RequestParam(required = false) String keyword,
            @ModelAttribute PageRequestDto pageRequest) {
        try {
            return success(performanceService.query(from, to, personnelId, workOrderId, teamId, source,
                    modifiedStatus, recordStatus, keyword, pageRequest));
        } catch (Exception ex) {
            return exception(ex, "查询绩效记录");
        }
    }

    /**
     * 工序粒度数据：可按工单或全部工单查询
     *
     * 示例请求：
     * GET /api/performance/process-view?from=2026-11-01&to=2026-11-30&keyword=板蓝根&teamId=3
     *
     * @param workOrderId    工单ID（可选，不传则查全部任务）
     * @param teamId         班组ID（可选）
     * @param from           起始日期
     * @param to             结束日期
     * @param source         记录来源（可选）
     * @param modifiedStatus 修改状态（可选）
     * @param recordStatus   记录状态（可选，默认仅正常）
     * @param keyword        制剂/批号/工单号等关键字
     * @param pageRequest    分页参数
     * @return 绩效记录分页
     */
    @GetMapping("/process-view")
    public ApiResponse<PagedResult<PerformanceRecord>> processView(
            @RequestParam(required = false) Long workOrderId,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) Integer modifiedStatus,
            @RequestParam(required = false) Integer recordStatus,
            @RequestParam(required = false) String keyword,
            @ModelAttribute PageRequestDto pageRequest) {
        try {
            return success(performanceService.query(from, to, null, workOrderId, teamId, source,
                    modifiedStatus, recordStatus, keyword, pageRequest));
        } catch (Exception ex) {
            return exception(ex, "查询工序粒度绩效");
        }
    }

    /**
     * 人员粒度数据：按人查询
     *
     * 示例请求：
     * GET /api/performance/person-view?personnelId=71&from=2026-11-01&to=2026-11-30
     *
     * @param personnelId 人员档案ID（可选，不传则查全部人员）
     * @param from        起始日期
     * @param to          结束日期
     * @param pageRequest 分页参数
     * @return 绩效记录分页
     */
    @GetMapping("/person-view")
    public ApiResponse<PagedResult<PerformanceRecord>> personView(
            @RequestParam(required = false) Long personnelId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @ModelAttribute PageRequestDto pageRequest) {
        try {
            return success(performanceService.query(from, to, personnelId, null, null, null, null, null, null, pageRequest));
        } catch (Exception ex) {
            return exception(ex, "查询人员粒度绩效");
        }
    }

    /**
     * 汇总查询：班组/品种/日期
     *
     * 示例请求：
     * GET /api/performance/aggregate?from=2026-11-01&to=2026-11-30&groupBy=team
     *
     * @param from    起始日期
     * @param to      结束日期
     * @param groupBy team/product/date
     * @return 汇总行
     */
    @GetMapping("/aggregate")
    public ApiResponse<List<Map<String, Object>>> aggregate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "team") String groupBy) {
        try {
            return success(performanceService.aggregate(from, to, groupBy));
        } catch (Exception ex) {
            return exception(ex, "查询绩效汇总");
        }
    }

    // endregion

    // region 修改与补录
    // ===================================
    // 修改与补录
    // ===================================

    /**
     * 修改绩效工时（系数计算后的时长，直接覆盖）
     *
     * 示例请求：
     * PUT /api/performance/records/1
     * {
     *   "performanceHoursTotal": 1.8,
     *   "note": "复核后修正"
     * }
     *
     * @param id      绩效记录ID
     * @param request 修改请求
     * @return 修改后的记录
     */
    @OperationLog(module = "人员绩效", type = OperationType.UPDATE, desc = "修改绩效工时",
            businessType = "PERFORMANCE_RECORD", businessId = "#id")
    @PutMapping("/records/{id}")
    public ApiResponse<PerformanceRecord> modify(@PathVariable Long id,
                                                 @RequestBody PerformanceModifyRequest request) {
        try {
            return success(performanceService.modify(id, request), "修改成功");
        } catch (Exception ex) {
            return exception(ex, "修改绩效工时");
        }
    }

    /**
     * 手动添加绩效
     *
     * 示例请求：
     * POST /api/performance/manual
     * {
     *   "personnelId": 71,
     *   "performanceDate": "2026-11-14",
     *   "performanceHours": 2,
     *   "workHours": 2,
     *   "calcMode": "直接计入",
     *   "note": "临时协助装卸物料"
     * }
     *
     * @param request 补录请求
     * @return 新增的记录
     */
    @OperationLog(module = "人员绩效", type = OperationType.CREATE, desc = "手动添加绩效",
            businessType = "PERFORMANCE_RECORD", businessId = "#request.personnelId")
    @PostMapping("/manual")
    public ApiResponse<PerformanceRecord> manual(@RequestBody PerformanceManualRequest request) {
        try {
            return success(performanceService.manual(request), "添加成功");
        } catch (Exception ex) {
            return exception(ex, "手动添加绩效");
        }
    }

    /**
     * 作废手动添加的绩效记录
     *
     * 示例请求：
     * POST /api/performance/records/1/void?note=重复补录
     *
     * @param id   记录ID
     * @param note 作废原因
     * @return 作废后的记录
     */
    @OperationLog(module = "人员绩效", type = OperationType.UPDATE, desc = "作废绩效记录",
            businessType = "PERFORMANCE_RECORD", businessId = "#id")
    @PostMapping("/records/{id}/void")
    public ApiResponse<PerformanceRecord> voidRecord(@PathVariable Long id, @RequestParam String note) {
        try {
            return success(performanceService.voidRecord(id, note), "已作废");
        } catch (Exception ex) {
            return exception(ex, "作废绩效记录");
        }
    }

    // endregion

    // region 系数版本
    // ===================================
    // 系数版本
    // ===================================

    /**
     * 查询系数版本列表
     *
     * 示例请求：
     * GET /api/performance/coefficient-versions
     *
     * @return 版本列表
     */
    @GetMapping("/coefficient-versions")
    public ApiResponse<List<PerformanceCoefficientVersion>> versions() {
        try {
            return success(performanceService.listVersions());
        } catch (Exception ex) {
            return exception(ex, "查询系数版本");
        }
    }

    /**
     * 发布新系数版本
     *
     * 示例请求：
     * POST /api/performance/coefficient-versions
     * {
     *   "note": "上调灌装工序系数至 1.1",
     *   "processCoefficients": {"189": 1.1},
     *   "personCoefficients": {"71": 1.05}
     * }
     *
     * @param request 发布请求
     * @return 新版本
     */
    @OperationLog(module = "人员绩效", type = OperationType.UPDATE, desc = "发布系数版本",
            businessType = "PERFORMANCE_COEFFICIENT_VERSION")
    @PostMapping("/coefficient-versions")
    public ApiResponse<PerformanceCoefficientVersion> publish(@RequestBody CoefficientPublishRequest request) {
        try {
            return success(performanceService.publish(request), "新版本已发布");
        } catch (Exception ex) {
            return exception(ex, "发布系数版本");
        }
    }

    /**
     * 查询系数版本详情
     *
     * 示例请求：
     * GET /api/performance/coefficient-versions/1
     *
     * @param id 版本ID
     * @return 版本详情
     */
    @GetMapping("/coefficient-versions/{id}")
    public ApiResponse<CoefficientVersionDetail> versionDetail(@PathVariable Long id) {
        try {
            return success(performanceService.versionDetail(id));
        } catch (Exception ex) {
            return exception(ex, "查询系数版本详情");
        }
    }

    // endregion

    // region 导出
    // ===================================
    // 导出
    // ===================================

    /**
     * 导出绩效明细 Excel
     *
     * 示例请求：
     * GET /api/performance/export?from=2026-11-01&to=2026-11-30&personnelId=71&keyword=板蓝根
     *
     * @param from        起始日期
     * @param to          结束日期
     * @param personnelId 人员档案ID（可选）
     * @param keyword     关键字（可选）
     * @param teamId      班组ID（可选）
     * @param source      记录来源（可选）
     * @param modifiedStatus 修改状态（可选）
     * @param recordStatus   记录状态（可选，1正常/0作废）
     * @param response    HTTP响应
     */
    @OperationLog(module = "人员绩效", type = OperationType.EXPORT, desc = "导出绩效明细")
    @GetMapping("/export")
    public void export(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long personnelId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) Integer modifiedStatus,
            @RequestParam(required = false) Integer recordStatus,
            HttpServletResponse response) throws IOException {
        PageRequestDto pageRequest = new PageRequestDto();
        pageRequest.setPageIndex(-1);
        pageRequest.setPageSize(-1);
        List<PerformanceRecord> records = performanceService.query(from, to, personnelId, null,
                teamId, source, modifiedStatus, recordStatus, keyword, pageRequest).getItems();
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("人员绩效");
            String[] headers = {"人员", "班组", "绩效归属日期", "生产任务", "制剂", "批号", "工序", "批次",
                    "计划起止", "实际起止", "工序实际时长", "工时（绩效时长）", "绩效工时", "完成数量",
                    "计算方式", "记录来源", "修改状态", "备注", "修改人", "修改时间"};
            Row head = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                head.createCell(i).setCellValue(headers[i]);
            }
            int rowIndex = 1;
            for (PerformanceRecord record : records) {
                Row row = sheet.createRow(rowIndex++);
                int c = 0;
                row.createCell(c++).setCellValue(nullSafe(record.getPersonnelName()));
                row.createCell(c++).setCellValue(nullSafe(record.getTeamName()));
                row.createCell(c++).setCellValue(record.getPerformanceDate() == null ? "" : record.getPerformanceDate().toString());
                row.createCell(c++).setCellValue(nullSafe(record.getWorkOrderCode()));
                row.createCell(c++).setCellValue(nullSafe(record.getPreparationName()));
                row.createCell(c++).setCellValue(nullSafe(record.getBatchNumber()));
                row.createCell(c++).setCellValue(nullSafe(record.getProcessName()));
                row.createCell(c++).setCellValue(nullSafe(record.getPlanBatchName()));
                row.createCell(c++).setCellValue(period(record.getPlanStartTime(), record.getPlanEndTime()));
                row.createCell(c++).setCellValue(period(record.getActualStartTime(), record.getActualEndTime()));
                setDecimal(row.createCell(c++), record.getProcessActualHours());
                setDecimal(row.createCell(c++), record.getPerformanceHours());
                setDecimal(row.createCell(c++), record.getPerformanceHoursTotal());
                setDecimal(row.createCell(c++), record.getCompletedQty());
                row.createCell(c++).setCellValue(nullSafe(record.getCalcMode()));
                row.createCell(c++).setCellValue(nullSafe(record.getSource()));
                row.createCell(c++).setCellValue(Integer.valueOf(1).equals(record.getModifiedStatus()) ? "已修改" : "未修改");
                row.createCell(c++).setCellValue(nullSafe(record.getNote()));
                row.createCell(c++).setCellValue(nullSafe(record.getUpdatedByName()));
                row.createCell(c++).setCellValue(record.getUpdatedTime() == null ? "" : record.getUpdatedTime().toString());
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=performance.xlsx");
            workbook.write(response.getOutputStream());
        }
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    /**
     * 格式化起止时间区间
     */
    private String period(java.time.LocalDateTime start, java.time.LocalDateTime end) {
        if (start == null && end == null) {
            return "";
        }
        String startText = start == null ? "" : start.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        String endText = end == null ? "" : end.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        return startText + " ~ " + endText;
    }

    private void setDecimal(Cell cell, BigDecimal value) {
        cell.setCellValue(value == null ? "" : value.toPlainString());
    }

    // endregion
}
