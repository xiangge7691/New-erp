package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.QualityPages.ReleaseBatchDtos;
import com.tonghui.erp.Service.ReleaseBatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
 * │ 3  │ /api/release-review/batches/{workOrderId}  │ GET    │ 批次全批详情      │
 * │ 4  │ .../{workOrderId}/process-checks           │ POST   │ 保存工序检查      │
 * │ 5  │ .../{workOrderId}/approve                  │ POST   │ 通过放行          │
 * │ 6  │ .../{workOrderId}/reject                   │ POST   │ 拒绝放行          │
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
     * @param status      放行状态（可选：待审核/待放行/放行/拒绝放行）
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
     * 通过放行
     *
     * 示例请求：
     * POST /api/release-review/batches/463/approve
     * {"approver":"赵磊","reviewTime":"2026-11-06T10:00:00","opinion":"合规检查通过，同意放行。"}
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
     * {"reviewer":"赵磊","reviewTime":"2026-11-06T10:00:00","reason":"工序记录不符合"}
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
}
