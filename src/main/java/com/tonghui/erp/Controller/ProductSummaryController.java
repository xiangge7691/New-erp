package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Constants.OperationType;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.annotation.OperationLog;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.QualityPages.ProductSummaryDtos;
import com.tonghui.erp.Service.ProductSummaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * 产品信息汇总控制器
 * <p>
 * 按品种/时间筛选批次，返回计划 → 采购 → 验收入库 → 生产任务 → 生产领料 →
 * 生产过程 → 检验 → 审核放行 → 成品入库 → 成品出库 全链路、槽位统计、
 * 单据明细、文件分组和缺件清单。
 * </p>
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────────────────┬────────┬──────────────────┐
 * │ #  │ 接口                                         │ 方法   │ 说明             │
 * ├────┼──────────────────────────────────────────────┼────────┼──────────────────┤
 * │ 1  │ /api/product-summary/overview                │ GET    │ 页面聚合概览      │
 * │ 2  │ /api/product-summary/batches                 │ GET    │ 批次汇总列表      │
 * │ 3  │ /api/product-summary/batches/{workOrderId}   │ GET    │ 批次全链路详情    │
 * │ 4  │ /api/product-summary/plans/{planId}          │ GET    │ 未下达计划详情    │
 * │ 5  │ /api/product-summary/cards                   │ GET    │ 品种卡片视图      │
 * │ 6  │ /api/product-summary/export                  │ POST   │ 集中导出 ZIP/CSV  │
 * └────┴──────────────────────────────────────────────┴────────┴──────────────────┘
 */
@RestController
@RequestMapping("/api/product-summary")
public class ProductSummaryController extends BaseController {

    /** 产品信息汇总服务 */
    @Autowired
    private ProductSummaryService productSummaryService;

    /**
     * 查询页面聚合概览
     *
     * 示例请求：
     * GET /api/product-summary/overview?varietyCode=Z04-011&dateType=planDate&startDate=2026-09-01&endDate=2026-10-31&keyword=&onlyMissing=false
     *
     * @param varietyCode 品种编码（可选）
     * @param dateType    日期类型
     * @param startDate   开始日期
     * @param endDate     结束日期
     * @param keyword     关键字
     * @param onlyMissing 只看缺件
     * @return 结论、KPI、待办、文件健康度、制剂卡片
     */
    @GetMapping("/overview")
    public ApiResponse<ProductSummaryDtos.Overview> overview(
            @RequestParam(required = false) String varietyCode,
            @RequestParam(required = false) String dateType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "false") boolean onlyMissing) {
        try {
            return success(productSummaryService.getOverview(varietyCode, dateType, startDate, endDate, keyword, onlyMissing));
        } catch (Exception e) {
            return exception(e, "查询产品信息概览");
        }
    }

    /**
     * 查询品种卡片视图数据（不分页）
     *
     * 示例请求：
     * GET /api/product-summary/cards?varietyCode=Z04-011&dateType=planDate&startDate=2026-09-01&endDate=2026-10-31&keyword=&onlyMissing=false
     *
     * @param varietyCode 品种编码（可选）
     * @param dateType    日期类型：planDate/workOrderDate/inboundDate/inspectionDate
     * @param startDate   开始日期
     * @param endDate     结束日期
     * @param keyword     关键字（工单号/批号/品种/计划号）
     * @param onlyMissing 只看缺件
     * @return 批次卡片列表，字段与批次列表一致
     */
    @GetMapping("/cards")
    public ApiResponse<List<ProductSummaryDtos.BatchSummary>> cards(
            @RequestParam(required = false) String varietyCode,
            @RequestParam(required = false) String dateType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "false") boolean onlyMissing) {
        try {
            return success(productSummaryService.queryCards(varietyCode, dateType, startDate, endDate, keyword, onlyMissing));
        } catch (Exception e) {
            return exception(e, "查询产品卡片");
        }
    }

    /**
     * 查询未下达工单的计划详情
     *
     * 示例请求：GET /api/product-summary/plans/315
     *
     * @param planId 生产计划ID
     * @return 计划详情
     */
    @GetMapping("/plans/{planId}")
    public ApiResponse<ProductSummaryDtos.BatchDetail> plan(@PathVariable Long planId) {
        try {
            return success(productSummaryService.getPlanDetail(planId));
        } catch (Exception e) {
            return exception(e, "查询生产计划详情");
        }
    }

    /**
     * 分页查询产品批次汇总
     *
     * 示例请求：
     * GET /api/product-summary/batches?varietyCode=Z04-011&dateType=planDate&startDate=2026-09-01&endDate=2026-10-31&keyword=260901&onlyMissing=false&pageIndex=0&pageSize=20
     *
     * @param varietyCode 品种编码（可选）
     * @param dateType    日期类型：planDate/workOrderDate/inboundDate/inspectionDate
     * @param startDate   开始日期
     * @param endDate     结束日期
     * @param keyword     关键字（工单号/批号/品种）
     * @param onlyMissing 只看缺件
     * @param pageIndex   页码，从0开始
     * @param pageSize    每页数量
     * @return 分页批次汇总
     */
    @GetMapping("/batches")
    public ApiResponse<PagedResult<ProductSummaryDtos.BatchSummary>> list(
            @RequestParam(required = false) String varietyCode,
            @RequestParam(required = false) String dateType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "false") boolean onlyMissing,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "20") int pageSize) {
        try {
            return success(productSummaryService.queryBatches(varietyCode, dateType, startDate, endDate,
                    keyword, onlyMissing, pageIndex, pageSize));
        } catch (Exception e) {
            return exception(e, "查询产品信息汇总");
        }
    }

    /**
     * 查询批次全链路详情
     *
     * 示例请求：GET /api/product-summary/batches/463
     *
     * @param workOrderId 工单ID
     * @return 批次全链路详情
     */
    @GetMapping("/batches/{workOrderId}")
    public ApiResponse<ProductSummaryDtos.BatchDetail> detail(@PathVariable Long workOrderId) {
        try {
            return success(productSummaryService.getBatchDetail(workOrderId));
        } catch (Exception e) {
            return exception(e, "查询产品批次详情");
        }
    }

    /**
     * 集中导出批次文件
     *
     * 示例请求：
     * POST /api/product-summary/export
     * {
     *   "scope": "FILTERED",
     *   "filters": {"varietyCode":"Z04-011","dateType":"planDate","startDate":"2026-09-01","endDate":"2026-10-31","onlyMissing":false},
     *   "includeProductFiles": true,
     *   "requirements": ["REQUIRED","CONDITIONAL"],
     *   "includeManifest": true,
     *   "format": "ZIP"
     * }
     *
     * @param request 导出范围（SELECTED/FILTERED/ALL）、批次/计划 ID、筛选条件、文件开关、ZIP/CSV 格式
     * @return ZIP 文件（application/zip）或 CSV 清单（text/csv）
     */
    @OperationLog(module = "产品信息汇总", type = OperationType.EXPORT, desc = "导出产品信息汇总",
            businessType = "PRODUCT_SUMMARY")
    @PostMapping("/export")
    public ResponseEntity<byte[]> export(@RequestBody(required = false) ProductSummaryDtos.ExportRequest request) {
        try {
            byte[] data = productSummaryService.export(request);
            boolean csv = request != null && "CSV".equalsIgnoreCase(request.getFormat());
            String ext = csv ? "csv" : "zip";
            String baseName = request != null && request.getFileName() != null && !request.getFileName().isBlank()
                    ? request.getFileName() : "产品信息汇总_" + LocalDate.now();
            String filename = baseName.endsWith("." + ext) ? baseName : baseName + "." + ext;
            String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
            MediaType mediaType = csv
                    ? MediaType.parseMediaType("text/csv;charset=UTF-8")
                    : MediaType.APPLICATION_OCTET_STREAM;
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                    .contentType(mediaType)
                    .body(data);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
