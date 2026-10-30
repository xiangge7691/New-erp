package com.tonghui.erp.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.Equipment.EquipmentUsageRecordDto;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Service.EquipmentUsageService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 设备使用记录控制器
 * <p>
 * 设备使用记录来源于工单工序执行记录（生产工序填写的设备 + 开始/结束时间），
 * 提供按设备/设备类型/时间范围/关键字的分页查询与按设备导出 Excel。
 * </p>
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────────┬────────┬────────────────────────────────────────────┐
 * │ #  │ 接口                                 │ 方法   │ 说明                                       │
 * ├────┼──────────────────────────────────────┼────────┼────────────────────────────────────────────┤
 * │ 1  │ /api/equipment-usage/page            │ GET    │ 分页查询设备使用记录                       │
 * │ 2  │ /api/equipment-usage/export          │ GET    │ 导出设备使用记录（按设备/全部设备多sheet） │
 * └────┴──────────────────────────────────────┴────────┴────────────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/equipment-usage")
public class EquipmentUsageController extends BaseController {

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    /**
     * 设备使用记录服务
     */
    @Autowired
    private EquipmentUsageService equipmentUsageService;

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
     * 分页查询设备使用记录
     * <p>
     * 时间范围按"与使用区间有交集"过滤；设备使用记录来源于工单工序执行记录。
     * </p>
     *
     * 示例请求：
     * GET /api/equipment-usage/page?equipmentId=180&startDate=2026-10-01&endDate=2026-10-31&pageIndex=0&pageSize=10
     * GET /api/equipment-usage/page?equipmentType=生产设备&keyword=GD2026&pageIndex=0&pageSize=10
     *
     * @param equipmentId   设备ID（可选）
     * @param equipmentType 设备类型（可选，精确匹配：生产设备/检验设备/环境设备/计量器具/压力容器/其他设备）
     * @param keyword       关键字（可选，模糊匹配工单编号/批号/制剂名称/工序名称/设备名称/固定资产编号）
     * @param status        执行状态（可选，精确匹配：待执行/已完成；按是否填写使用时间判定）
     * @param startDate     使用开始日期（可选，格式 yyyy-MM-dd）
     * @param endDate       使用结束日期（可选，格式 yyyy-MM-dd）
     * @param pageRequest   分页参数（pageIndex 从 0 开始，-1 表示全量）
     * @return 设备使用记录分页结果
     */
    @GetMapping("/page")
    public ApiResponse<PagedResult<EquipmentUsageRecordDto>> page(
            @RequestParam(required = false) Integer equipmentId,
            @RequestParam(required = false) String equipmentType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @ModelAttribute PageRequestDto pageRequest) {
        try {
            pageRequest = processPageRequest(pageRequest);
            PagedResult<EquipmentUsageRecordDto> result = equipmentUsageService.search(
                    equipmentId, equipmentType, keyword, status, startDate, endDate, pageRequest);
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "查询设备使用记录");
        }
    }

    // endregion

    // region 导出接口
    // ===================================
    // 导出接口
    // ===================================

    /**
     * 导出设备使用记录 Excel
     * <p>
     * 传入 equipmentId 时为单设备导出（单 sheet）；不传时按设备分 sheet 导出全部设备，
     * 并附加"汇总"sheet（使用次数、累计使用时长、首次/最近使用时间、维保次数等）。
     * 设备在导出范围内有维保记录时，会额外追加"设备名-维保"sheet，导出维保时间段与维保内容。
     * 先在内存生成，参数校验失败时返回统一 JSON 错误，不污染下载头。
     * </p>
     *
     * 示例请求：
     * GET /api/equipment-usage/export?equipmentId=180&startDate=2026-10-01&endDate=2026-10-31
     * GET /api/equipment-usage/export?equipmentType=生产设备&startDate=2026-10-01
     *
     * @param equipmentId   设备ID（可选，按设备导出时必填）
     * @param equipmentType 设备类型（可选）
     * @param keyword       关键字（可选）
     * @param status        执行状态（可选）
     * @param startDate     使用开始日期（可选，格式 yyyy-MM-dd）
     * @param endDate       使用结束日期（可选，格式 yyyy-MM-dd）
     * @param response      HTTP响应（成功时为 xlsx 附件下载）
     */
    @GetMapping("/export")
    public void export(
            @RequestParam(required = false) Integer equipmentId,
            @RequestParam(required = false) String equipmentType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletResponse response) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            // 服务层先生成（失败时尚未写响应，可安全回JSON错误）
            equipmentUsageService.exportToExcel(
                    equipmentId, equipmentType, keyword, status, startDate, endDate, buffer);

            String scope = equipmentId == null ? "全部设备" : "设备" + equipmentId;
            String filename = "设备使用记录_" + scope + "_" + LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition",
                    "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8));
            response.getOutputStream().write(buffer.toByteArray());
        } catch (Exception e) {
            if (!response.isCommitted()) {
                int errorStatus = e instanceof IllegalArgumentException
                        ? HttpServletResponse.SC_BAD_REQUEST
                        : HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                writeExportError(response, errorStatus, "导出失败: " + e.getMessage());
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
