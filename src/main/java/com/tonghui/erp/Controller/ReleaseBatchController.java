package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.QualityPages.ReleaseBatchDtos;
import com.tonghui.erp.Service.ReleaseBatchService;
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
 * 批次审核放行控制器
 * <p>
 * 以工单/批次为单位聚合全批内容与文件，提供合规列表、详情、工序检查保存、
 * 通过/拒绝放行。工序要求读取现有工序页配置，不新增配置页面。
 * </p>
 *
 * 接口清单：
 * ┌────┬────────────────────────────────────────────┬────────┬──────────────────┐
 * │ #  │ 接口                                       │ 方法   │ 说明             │
 * ├────┼────────────────────────────────────────────┼────────┼──────────────────┤
 * │ 1  │ /api/release-review/batches                │ GET    │ 批次审核列表      │
 * │ 2  │ /api/release-review/batches/stats          │ GET    │ 审核看板统计      │
 * │ 3  │ /api/release-review/batches/varieties      │ GET    │ 品种下拉选项      │
 * │ 4  │ /api/release-review/batches/{workOrderId}  │ GET    │ 批次全批详情      │
 * │ 5  │ .../{workOrderId}/process-checks           │ POST   │ 保存工序检查      │
 * │ 6  │ .../{workOrderId}/draft                    │ POST   │ 保存放行草稿      │
 * │ 7  │ .../{workOrderId}/approve                  │ POST   │ 通过放行          │
 * │ 8  │ .../{workOrderId}/reject                   │ POST   │ 拒绝放行          │
 * │ 9  │ /api/release-review/batches/export         │ POST   │ 导出待审核批次    │
 * └────┴────────────────────────────────────────────┴────────┴──────────────────┘
 */
@RestController
@RequestMapping("/api/release-review/batches")
public class ReleaseBatchController extends BaseController {

    /** 批次审核放行服务 */
    @Autowired
    private ReleaseBatchService releaseBatchService;

    /**
     * 分页查询批次审核列表
     *
     * 示例请求：
     * GET /api/release-review/batches?varietyCode=Z04-011&batchNo=260901&status=待审核&pageIndex=0&pageSize=20
     *
     * @param varietyCode 品种编码（可选）
     * @param batchNo     批号/工单号（可选）
     * @param status      放行状态（可选：待审核/待放行/已放行/已拒绝）
     * @param keyword     关键字（工单号/批号/品种）
     * @param pageIndex   页码，从0开始
     * @param pageSize    每页数量
     * @return 分页批次审核列表
     */
    @GetMapping
    public ApiResponse<PagedResult<ReleaseBatchDtos.BatchSummary>> list(
            @RequestParam(required = false) String varietyCode,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "20") int pageSize) {
        try {
            return success(releaseBatchService.queryBatches(varietyCode, batchNo, status, keyword, pageIndex, pageSize));
        } catch (Exception e) {
            return exception(e, "查询批次审核列表");
        }
    }

    /**
     * 查询审核放行看板统计
     *
     * 示例请求：
     * GET /api/release-review/batches/stats?varietyCode=Z04-011&batchNo=260901
     *
     * @param varietyCode 品种编码（可选）
     * @param batchNo     批号/工单号（可选）
     * @param status      放行状态（可选）
     * @param keyword     关键字（可选）
     * @return 待审核、可放行、存在阻塞等计数
     */
    @GetMapping("/stats")
    public ApiResponse<ReleaseBatchDtos.BoardStats> stats(
            @RequestParam(required = false) String varietyCode,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        try {
            return success(releaseBatchService.queryBoardStats(varietyCode, batchNo, status, keyword));
        } catch (Exception e) {
            return exception(e, "查询审核放行看板统计");
        }
    }

    /**
     * 查询品种下拉选项
     * <p>取有批号的工单品种，按品种编码去重，供列表筛选栏使用。</p>
     *
     * 示例请求：
     * GET /api/release-review/batches/varieties
     *
     * @return 品种编码/名称列表
     */
    @GetMapping("/varieties")
    public ApiResponse<List<ReleaseBatchDtos.VarietyOption>> varieties() {
        try {
            return success(releaseBatchService.listVarieties());
        } catch (Exception e) {
            return exception(e, "查询品种选项");
        }
    }

    /**
     * 查询批次全批审核详情
     *
     * 示例请求：GET /api/release-review/batches/463
     *
     * @param workOrderId 工单ID
     * @return 批次详情
     */
    @GetMapping("/{workOrderId}")
    public ApiResponse<ReleaseBatchDtos.BatchDetail> detail(@PathVariable Long workOrderId) {
        try {
            return success(releaseBatchService.getBatchDetail(workOrderId));
        } catch (Exception e) {
            return exception(e, "查询批次审核详情");
        }
    }

    /**
     * 保存工序符合性检查
     *
     * 示例请求：
     * POST /api/release-review/batches/463/process-checks
     * {"items":[{"executionId":1,"processTypeId":63,"processName":"粉碎","checkResult":"符合","inspectorName":"丁圣芳","checkDate":"2026-11-06"}]}
     *
     * @param workOrderId 工单ID
     * @param request     检查项
     * @return 操作结果
     */
    @PostMapping("/{workOrderId}/process-checks")
    public ApiResponse<Void> saveProcessChecks(
            @PathVariable Long workOrderId,
            @RequestBody ReleaseBatchDtos.ProcessCheckSaveRequest request) {
        try {
            releaseBatchService.saveProcessChecks(workOrderId, request);
            return success(null, "工序检查已保存");
        } catch (Exception e) {
            return exception(e, "保存工序检查");
        }
    }

    /**
     * 保存放行审核草稿
     * <p>草稿不校验合规，结论固定为“待补充”，用于记录审核意见和待补资料。</p>
     *
     * 示例请求：
     * POST /api/release-review/batches/463/draft
     * {"conclusion":"待补充","approver":"赵磊","reviewDate":"2026-11-06","opinion":"验收检验报告待补"}
     *
     * @param workOrderId 工单ID
     * @param request     草稿请求
     * @return 批次详情
     */
    @PostMapping("/{workOrderId}/draft")
    public ApiResponse<ReleaseBatchDtos.BatchDetail> draft(
            @PathVariable Long workOrderId,
            @RequestBody(required = false) ReleaseBatchDtos.DraftRequest request) {
        try {
            return success(releaseBatchService.saveDraft(workOrderId, request), "草稿已保存");
        } catch (Exception e) {
            return exception(e, "保存放行草稿");
        }
    }

    /**
     * 通过放行
     *
     * 示例请求：
     * POST /api/release-review/batches/463/approve
     * {"approver":"赵磊","reviewDate":"2026-11-06","opinion":"合规检查通过，同意放行。"}
     *
     * @param workOrderId 工单ID
     * @param request     放行请求
     * @return 放行后的批次详情
     */
    @PostMapping("/{workOrderId}/approve")
    public ApiResponse<ReleaseBatchDtos.BatchDetail> approve(
            @PathVariable Long workOrderId,
            @RequestBody(required = false) ReleaseBatchDtos.ApproveRequest request) {
        try {
            return success(releaseBatchService.approve(workOrderId, request), "已通过放行");
        } catch (Exception e) {
            return exception(e, "通过放行");
        }
    }

    /**
     * 拒绝放行
     *
     * 示例请求：
     * POST /api/release-review/batches/463/reject
     * {"reviewer":"赵磊","reviewDate":"2026-11-06","reason":"工序记录不符合"}
     *
     * @param workOrderId 工单ID
     * @param request     拒绝请求
     * @return 拒绝后的批次详情
     */
    @PostMapping("/{workOrderId}/reject")
    public ApiResponse<ReleaseBatchDtos.BatchDetail> reject(
            @PathVariable Long workOrderId,
            @RequestBody(required = false) ReleaseBatchDtos.RejectRequest request) {
        try {
            return success(releaseBatchService.reject(workOrderId, request), "已拒绝放行");
        } catch (Exception e) {
            return exception(e, "拒绝放行");
        }
    }

    /**
     * 导出待审核批次 CSV
     * <p>按当前筛选条件导出列表数据，status 为空时默认“待审核”。</p>
     *
     * 示例请求：
     * POST /api/release-review/batches/export
     * {"varietyCode":"Z04-011","batchNo":"260901","status":"待审核","keyword":""}
     *
     * @param request 导出筛选条件
     * @return CSV 文件流
     */
    @PostMapping("/export")
    public ResponseEntity<byte[]> export(@RequestBody(required = false) ReleaseBatchDtos.ExportRequest request) {
        try {
            byte[] data = releaseBatchService.exportCsv(request);
            String filename = "审核放行_" + LocalDate.now() + ".csv";
            String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                    .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                    .body(data);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
