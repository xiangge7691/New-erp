package com.tonghui.erp.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.Maintenance.MaintenancePendingDto;
import com.tonghui.erp.Common.Dto.Maintenance.MaintenancePendingSummaryDto;
import com.tonghui.erp.Common.Dto.Maintenance.MaintenanceRecordListItemDto;
import com.tonghui.erp.Common.Dto.Maintenance.MaintenanceRecordSaveDto;
import com.tonghui.erp.Common.Dto.Maintenance.MaintenanceRecordWithDetailsDto;
import com.tonghui.erp.Common.Dto.Maintenance.MaintenanceStockItemDto;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.FileInfo;
import com.tonghui.erp.Data.Entity.MaintenanceRecord;
import com.tonghui.erp.Service.MaintenanceRecordService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 养护记录控制器
 * <p>
 * 提供库存养护记录（养护台账）的CRUD操作、条件分页查询、编号生成、
 * 库存明细选择、待养护查询与统计、台账Excel导出，
 * 用于库存管理模块的GMP养护台账与周期提醒
 * </p>
 * <p>
 * 登记养护本身不产生库存增减；下次养护日期=本次实际检查日期+物料养护周期快照
 * </p>
 *
 * 接口清单：
 * ┌────┬────────────────────────────────────────────┬────────┬──────────────────────────────────┐
 * │ #  │ 接口                                       │ 方法   │ 说明                             │
 * ├────┼────────────────────────────────────────────┼────────┼──────────────────────────────────┤
 * │ 1  │ /api/maintenance-record                    │ GET    │ 分页查询养护记录列表             │
 * │ 2  │ /api/maintenance-record/{id}               │ GET    │ 查询养护记录详情（含明细附件）   │
 * │ 3  │ /api/maintenance-record/generateCode       │ GET    │ 获取自动生成的养护记录编号       │
 * │ 4  │ /api/maintenance-record/{id}/attachments   │ GET    │ 查询主单附件列表                 │
 * │ 5  │ /api/maintenance-record/stock-details      │ GET    │ 选择库存物料（带出养护要求）     │
 * │ 6  │ /api/maintenance-record/pending            │ GET    │ 分页查询待养护库存明细           │
 * │ 7  │ /api/maintenance-record/pending/summary    │ GET    │ 待养护统计（逾期/今日/即将到期） │
 * │ 8  │ /api/maintenance-record/export             │ GET    │ 导出养护台账Excel                │
 * │ 9  │ /api/maintenance-record                    │ POST   │ 新增养护记录（主单+明细）        │
 * │ 10 │ /api/maintenance-record/{id}               │ PUT    │ 修改养护记录（明细整体重建）     │
 * │ 11 │ /api/maintenance-record/{id}               │ DELETE │ 删除养护记录                     │
 * └────┴────────────────────────────────────────────┴────────┴──────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/maintenance-record")
public class MaintenanceRecordController extends BaseController {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 养护记录服务
     */
    @Autowired
    private MaintenanceRecordService maintenanceRecordService;

    /**
     * Jackson对象映射器（导出失败时写统一JSON错误）
     */
    @Autowired
    private ObjectMapper objectMapper;

    // endregion

    // region 查询接口
    // ===================================
    // 查询接口
    // ===================================

    /**
     * 分页查询养护记录列表
     *
     * 示例请求：
     * GET /api/maintenance-record?keyword=YH&warehouseId=1&material=VC&batchNumber=20260701&inCode=IN2026&checkType=日常检查&checkerName=张三&hasAbnormal=true&startDate=2026-10-01&endDate=2026-10-12&pageIndex=0&pageSize=10
     *
     * @param keyword     关键字（可选，模糊匹配养护记录编号）
     * @param warehouseId 仓库ID（可选，精确匹配）
     * @param material    物料关键字（可选，模糊匹配明细物料编码/名称）
     * @param batchNumber 批号（可选，模糊匹配明细批号）
     * @param inCode      入库编号（可选，模糊匹配明细入库编号）
     * @param checkType   检查类型（可选，精确匹配：日常检查/定期检查/特殊天气检查/复查）
     * @param checkerName 检查人员姓名（可选，模糊匹配）
     * @param hasAbnormal 是否发现异常（可选，true仅返回有异常明细的记录）
     * @param startDate   检查时间起始（可选，格式：yyyy-MM-dd）
     * @param endDate     检查时间结束（可选，格式：yyyy-MM-dd）
     * @param pageIndex   页码索引，从0开始（默认0）
     * @param pageSize    每页数量（默认10）
     * @return ApiResponse&lt;PagedResult&lt;MaintenanceRecordListItemDto&gt;&gt; 分页结果（列表项含明细数detailCount、异常明细数abnormalCount与创建人姓名createdByName）
     */
    @GetMapping
    public ApiResponse<PagedResult<MaintenanceRecordListItemDto>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String material,
            @RequestParam(required = false) String batchNumber,
            @RequestParam(required = false) String inCode,
            @RequestParam(required = false) String checkType,
            @RequestParam(required = false) String checkerName,
            @RequestParam(required = false) Boolean hasAbnormal,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            PageRequestDto pageRequest = new PageRequestDto();
            pageRequest.setPageIndex(pageIndex);
            pageRequest.setPageSize(pageSize);
            return success(maintenanceRecordService.search(keyword, warehouseId, material, batchNumber,
                    inCode, checkType, checkerName, hasAbnormal, startDate, endDate, pageRequest));
        } catch (Exception e) {
            return exception(e, "查询养护记录");
        }
    }

    /**
     * 查询养护记录详情（含全部库存检查明细、明细附件、主单附件与创建人/更新人姓名）
     *
     * 示例请求：
     * GET /api/maintenance-record/1
     *
     * @param id 养护记录ID（路径参数）
     * @return ApiResponse&lt;MaintenanceRecordWithDetailsDto&gt; 养护记录详情（不存在时返回错误）
     */
    @GetMapping("/{id}")
    public ApiResponse<MaintenanceRecordWithDetailsDto> getDetail(@PathVariable Long id) {
        try {
            MaintenanceRecordWithDetailsDto detail = maintenanceRecordService.getDetail(id);
            if (detail == null) {
                return error("养护记录不存在");
            }
            return success(detail);
        } catch (Exception e) {
            return exception(e, "查询养护记录详情");
        }
    }

    /**
     * 获取自动生成的养护记录编号（格式YH-YYYYMMDD-NNN）
     *
     * 示例请求：
     * GET /api/maintenance-record/generateCode?checkDate=2026-10-15
     *
     * @param checkDate 实际检查日期（可选，格式yyyy-MM-dd，默认当天）
     * @return ApiResponse&lt;String&gt; 自动生成的养护记录编号
     */
    @GetMapping("/generateCode")
    public ApiResponse<String> generateCode(@RequestParam(required = false) String checkDate) {
        try {
            return success(maintenanceRecordService.generateCode(checkDate));
        } catch (Exception e) {
            return exception(e, "生成养护记录编号");
        }
    }

    /**
     * 查询养护记录主单附件列表
     *
     * 示例请求：
     * GET /api/maintenance-record/1/attachments
     *
     * @param id 养护记录ID（路径参数）
     * @return ApiResponse&lt;List&lt;FileInfo&gt;&gt; 附件文件列表
     */
    @GetMapping("/{id}/attachments")
    public ApiResponse<List<FileInfo>> getAttachments(@PathVariable Long id) {
        try {
            return success(maintenanceRecordService.getAttachments(id));
        } catch (Exception e) {
            return exception(e, "查询养护记录附件");
        }
    }

    // endregion

    // region 库存选择与提醒接口
    // ===================================
    // 库存选择与提醒接口
    // ===================================

    /**
     * 选择库存物料（新增养护记录弹窗，带出入库编号与物料养护要求）
     *
     * 示例请求：
     * GET /api/maintenance-record/stock-details?warehouse=原药库&keyword=VC&showZero=false
     *
     * @param warehouse 仓库名称（必填）
     * @param keyword   关键字（可选，模糊匹配物料编码/名称/批号/入库编号）
     * @param showZero  是否显示零库存（可选，默认false仅显示有库存明细）
     * @return ApiResponse&lt;List&lt;MaintenanceStockItemDto&gt;&gt; 库存选择项列表
     */
    @GetMapping("/stock-details")
    public ApiResponse<List<MaintenanceStockItemDto>> getStockDetails(
            @RequestParam String warehouse,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean showZero) {
        try {
            return success(maintenanceRecordService.getStockDetails(warehouse, keyword, showZero));
        } catch (Exception e) {
            return exception(e, "查询库存明细");
        }
    }

    /**
     * 分页查询待养护库存明细（逾期优先、下次养护日期升序，排除零库存）
     *
     * 示例请求：
     * GET /api/maintenance-record/pending?keyword=VC&warehouseId=1&pendingStatus=OVERDUE&pageIndex=0&pageSize=10
     *
     * @param keyword       关键字（可选，模糊匹配物料编码/名称/批号/入库编号）
     * @param warehouseId   仓库ID（可选，精确匹配库存所在生产单位）
     * @param pendingStatus 提醒状态（可选：OVERDUE已逾期/TODAY今日到期/SOON即将到期）
     * @param pageIndex     页码索引，从0开始（默认0）
     * @param pageSize      每页数量（默认10）
     * @return ApiResponse&lt;PagedResult&lt;MaintenancePendingDto&gt;&gt; 分页结果（含逾期天数overdueDays与最近检查编号lastMaintenanceCode）
     */
    @GetMapping("/pending")
    public ApiResponse<PagedResult<MaintenancePendingDto>> searchPending(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String pendingStatus,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            return success(maintenanceRecordService.searchPending(keyword, warehouseId,
                    pendingStatus, pageIndex, pageSize));
        } catch (Exception e) {
            return exception(e, "查询待养护明细");
        }
    }

    /**
     * 查询待养护统计（首页展示逾期/今日到期/即将到期数量与提前提醒天数）
     *
     * 示例请求：
     * GET /api/maintenance-record/pending/summary
     *
     * @return ApiResponse&lt;MaintenancePendingSummaryDto&gt; 待养护统计数据
     */
    @GetMapping("/pending/summary")
    public ApiResponse<MaintenancePendingSummaryDto> getPendingSummary() {
        try {
            return success(maintenanceRecordService.getPendingSummary());
        } catch (Exception e) {
            return exception(e, "查询待养护统计");
        }
    }

    // endregion

    // region 新增、修改与删除接口
    // ===================================
    // 新增、修改与删除接口
    // ===================================

    /**
     * 新增养护记录（主单+明细一次保存）
     *
     * 示例请求：
     * POST /api/maintenance-record
     * Content-Type: application/json
     * {
     *   "checkTime": "2026-10-15 09:30:00",
     *   "checkType": "日常检查",
     *   "warehouseId": 1,
     *   "checkerId": 5,
     *   "envObserveInfo": "一号库温湿度记录仪旁",
     *   "temperature": 22.5,
     *   "humidity": 55.0,
     *   "summary": "整体状况良好",
     *   "details": [
     *     {
     *       "stockId": 101,
     *       "shelfLocation": "A区3架",
     *       "checkScope": "全部",
     *       "packagingStatus": 2,
     *       "moistureStatus": 2,
     *       "moistureContent": 12.5,
     *       "measures": "通风",
     *       "operatorId": 5,
     *       "attachmentFileIds": [301]
     *     }
     *   ],
     *   "attachmentFileIds": [201]
     * }
     *
     * 必填字段：
     *   checkTime   实际检查时间（必填，编号按此日期生成）
     *   checkType   检查类型（必填：日常检查/定期检查/特殊天气检查/复查）
     *   warehouseId 仓库ID（必填）
     *   checkerId   检查人员ID（必填）
     *   details     库存检查明细（必填，至少1行，明细stockId必填）
     * 其他说明：
     *   maintenanceCode由服务端自动生成（YH-YYYYMMDD-NNN），不可传入
     *   物料信息/批号/入库编号/数量由服务端按stockId快照，明细检查项四态默认未见异常
     *   下次养护日期=本次实际检查日期+物料养护周期快照（周期为空则不提醒）
     *   以下字段选填：envObserveInfo、temperature、humidity、summary、relatedMaintenanceId、attachmentFileIds
     *
     * @param dto 保存请求体（主单字段+明细列表）
     * @return ApiResponse&lt;MaintenanceRecord&gt; 新增的养护记录（含自动生成的编号）
     */
    @PostMapping
    public ApiResponse<MaintenanceRecord> create(@RequestBody MaintenanceRecordSaveDto dto) {
        try {
            return success(maintenanceRecordService.create(dto), "新增成功");
        } catch (Exception e) {
            return exception(e, "新增养护记录");
        }
    }

    /**
     * 修改养护记录（主单+明细一次保存，明细按ID原地更新保留ID）
     *
     * 示例请求：
     * PUT /api/maintenance-record/1
     * Content-Type: application/json
     * {
     *   "checkTime": "2026-10-15 10:00:00",
     *   "checkType": "定期检查",
     *   "warehouseId": 1,
     *   "checkerId": 5,
     *   "summary": "补充说明：已安排翻垛",
     *   "details": [
     *     { "id": 88, "stockId": 101, "shelfLocation": "A区3架", "moistureStatus": 3, "abnormalDescription": "垛位底部轻微受潮" }
     *   ],
     *   "attachmentFileIds": [201, 202]
     * }
     *
     * 必填字段说明：同新增接口；养护记录编号不可修改，服务端保留原值
     * 明细说明：details 中已有行须回传原 id（服务端按 id 原地更新，保留明细ID与按行附件）；
     *           不传 id 的行按新增写入；本单中未被 details 覆盖的既有行将被物理删除
     * 其他说明：主单附件同步解绑/绑定
     *
     * @param id  养护记录ID（路径参数）
     * @param dto 更新的养护记录信息（主单字段+明细列表）
     * @return ApiResponse&lt;MaintenanceRecord&gt; 修改后的养护记录
     */
    @PutMapping("/{id}")
    public ApiResponse<MaintenanceRecord> update(@PathVariable Long id, @RequestBody MaintenanceRecordSaveDto dto) {
        try {
            return success(maintenanceRecordService.update(id, dto), "修改成功");
        } catch (Exception e) {
            return exception(e, "修改养护记录");
        }
    }

    /**
     * 删除养护记录（主单软删除，明细物理删除并解绑附件）
     *
     * 示例请求：
     * DELETE /api/maintenance-record/1
     *
     * @param id 养护记录ID（路径参数）
     * @return ApiResponse&lt;Void&gt; 删除结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        try {
            maintenanceRecordService.delete(id);
            return success(null, "删除成功");
        } catch (Exception e) {
            return exception(e, "删除养护记录");
        }
    }

    // endregion

    // region 导出接口
    // ===================================
    // 导出接口
    // ===================================

    /**
     * 按列表筛选条件导出养护台账Excel（主单+明细平铺，一行一物料）
     * <p>
     * 先在内存生成，参数校验失败时返回统一JSON错误（不污染下载头）
     * </p>
     *
     * 示例请求：
     * GET /api/maintenance-record/export
     * GET /api/maintenance-record/export?keyword=YH&warehouseId=1&checkType=日常检查&startDate=2026-10-01&endDate=2026-10-12
     *
     * @param keyword     关键字（可选，同列表查询）
     * @param warehouseId 仓库ID（可选）
     * @param material    物料关键字（可选）
     * @param batchNumber 批号（可选）
     * @param inCode      入库编号（可选）
     * @param checkType   检查类型（可选）
     * @param checkerName 检查人员姓名（可选）
     * @param hasAbnormal 是否发现异常（可选）
     * @param startDate   检查时间起始（可选，格式：yyyy-MM-dd）
     * @param endDate     检查时间结束（可选，格式：yyyy-MM-dd）
     * @param response    HTTP响应（成功时Content-Type为xlsx，Content-Disposition附件下载）
     */
    @GetMapping("/export")
    public void export(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String material,
            @RequestParam(required = false) String batchNumber,
            @RequestParam(required = false) String inCode,
            @RequestParam(required = false) String checkType,
            @RequestParam(required = false) String checkerName,
            @RequestParam(required = false) Boolean hasAbnormal,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletResponse response) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            // 服务层先生成（失败时尚未写响应，可安全回JSON错误）
            maintenanceRecordService.exportToExcel(keyword, warehouseId, material, batchNumber,
                    inCode, checkType, checkerName, hasAbnormal, startDate, endDate, buffer);

            String filename = "养护台账导出_" + java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition",
                    "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8));
            response.getOutputStream().write(buffer.toByteArray());
        } catch (Exception e) {
            if (!response.isCommitted()) {
                writeExportError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "导出失败: " + e.getMessage());
            }
        }
    }

    /**
     * 导出失败时写统一格式JSON错误响应
     *
     * @param response HTTP响应
     * @param status   HTTP状态码
     * @param message  错误消息
     */
    private void writeExportError(HttpServletResponse response, int status, String message) {
        try {
            response.reset();
            response.setStatus(status);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(
                    ApiResponse.errorResponse(status, message)));
        } catch (IOException ignored) {
            // 错误响应写入失败时忽略（客户端可能已断开）
        }
    }

    // endregion
}
