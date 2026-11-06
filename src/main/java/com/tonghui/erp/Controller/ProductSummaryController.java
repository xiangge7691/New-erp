package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.ApiResponse;
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

/**
 * 产品信息汇总控制器
 * <p>
 * 按品种/时间筛选批次，返回计划 → 采购/领料 → 工单 → 生产 → 入库 → 出库 → 检验
 * 全链路、槽位统计、单据明细、文件分组和缺件清单。
 * </p>
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────────────────┬────────┬──────────────────┐
 * │ #  │ 接口                                         │ 方法   │ 说明             │
 * ├────┼──────────────────────────────────────────────┼────────┼──────────────────┤
 * │ 1  │ /api/product-summary/batches                 │ GET    │ 批次汇总列表      │
 * │ 2  │ /api/product-summary/batches/{workOrderId}   │ GET    │ 批次全链路详情    │
 * └────┴──────────────────────────────────────────────┴────────┴──────────────────┘
 */
@RestController
@RequestMapping("/api/product-summary")
public class ProductSummaryController extends BaseController {

    /** 产品信息汇总服务 */
    @Autowired
    private ProductSummaryService productSummaryService;

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
     * 导出批次文件 zip
     *
     * 示例请求：
     * POST /api/product-summary/export
     * {"workOrderIds":[463,462],"includeProductFiles":true}
     *
     * @param request 导出请求
     * @return zip 文件
     */
    @PostMapping("/export")
    public ResponseEntity<byte[]> export(@RequestBody(required = false) ProductSummaryDtos.ExportRequest request) {
        try {
            byte[] data = productSummaryService.export(request);
            String filename = "产品信息汇总_" + LocalDate.now() + ".zip";
            String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(data);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
