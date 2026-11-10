package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.Log.DataChangeLogDetailDto;
import com.tonghui.erp.Common.Dto.Log.DataChangeLogDto;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.utils.LogQueryTimeUtils;
import com.tonghui.erp.Service.DataChangeLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据变更日志控制器
 *
 * 接口清单：
 * ┌────┬────────────────────────────────────────────┬────────┬──────────────────────────────┐
 * │ #  │ 接口                                       │ 方法   │ 说明                         │
 * ├────┼────────────────────────────────────────────┼────────┼──────────────────────────────┤
 * │ 1  │ /api/system/data-change-log/search         │ GET    │ 分页查询数据变更日志         │
 * │ 2  │ /api/system/data-change-log/{id}           │ GET    │ 查询数据变更详情             │
 * └────┴────────────────────────────────────────────┴────────┴──────────────────────────────┘
 */
@RestController
@RequestMapping("/api/system/data-change-log")
public class DataChangeLogController extends BaseController {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 数据变更日志服务
     */
    @Autowired
    private DataChangeLogService dataChangeLogService;

    // endregion

    // region 查询接口
    // ===================================
    // 查询接口
    // ===================================

    /**
     * 分页查询数据变更日志
     *
     * 示例请求：
     * GET /api/system/data-change-log/search?pageIndex=0&pageSize=20&tableName=purchase_orders&businessId=1024
     *
     * @param operationLogId 操作日志ID（可选）
     * @param traceId        变更TraceID（可选）
     * @param tableName      表名（精确匹配，可选）
     * @param businessId     业务记录主键（可选）
     * @param changeType     变更类型：INSERT/UPDATE/DELETE（可选）
     * @param userId         操作人ID（可选）
     * @param userAccount    操作人账号（精确匹配，可选）
     * @param startTime      起始时间，支持 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss（可选）
     * @param endTime        截止时间，支持 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss（可选）
     * @param pageRequest    分页参数
     * @return ApiResponse&lt;PagedResult&lt;DataChangeLogDto&gt;&gt; 分页变更日志
     */
    @GetMapping("/search")
    public ApiResponse<PagedResult<DataChangeLogDto>> search(
            @RequestParam(required = false) Long operationLogId,
            @RequestParam(required = false) String traceId,
            @RequestParam(required = false) String tableName,
            @RequestParam(required = false) String businessId,
            @RequestParam(required = false) String changeType,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String userAccount,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @ModelAttribute PageRequestDto pageRequest) {
        try {
            PagedResult<DataChangeLogDto> result = dataChangeLogService.search(
                    operationLogId, traceId, tableName, businessId, changeType, userId, userAccount,
                    LogQueryTimeUtils.parseStart(startTime),
                    LogQueryTimeUtils.parseEnd(endTime),
                    processLogPageRequest(pageRequest));
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "查询数据变更日志");
        }
    }

    /**
     * 查询数据变更详情
     *
     * 返回完整修改前/后整行快照与字段差异。
     *
     * 示例请求：
     * GET /api/system/data-change-log/501
     *
     * @param id 变更日志ID
     * @return ApiResponse&lt;DataChangeLogDetailDto&gt; 变更详情
     */
    @GetMapping("/{id}")
    public ApiResponse<DataChangeLogDetailDto> detail(@PathVariable Long id) {
        try {
            DataChangeLogDetailDto detail = dataChangeLogService.detail(id);
            if (detail == null) {
                return error("数据变更日志不存在");
            }
            return success(detail);
        } catch (Exception ex) {
            return exception(ex, "查询数据变更详情");
        }
    }

    // endregion

    // region 分页参数处理
    // ===================================
    // 分页参数处理
    // ===================================

    /**
     * 处理日志分页参数
     * <p>
     * 日志表数据量大，禁止 -1（全量）查询；缺省为第 0 页、每页 20 条，单页最多 200 条
     * </p>
     *
     * @param pageRequest 原始分页参数
     * @return 修正后的分页参数
     */
    private PageRequestDto processLogPageRequest(PageRequestDto pageRequest) {
        if (pageRequest == null) {
            pageRequest = new PageRequestDto();
        }
        if (pageRequest.getPageIndex() < 0) {
            pageRequest.setPageIndex(0);
        }
        if (pageRequest.getPageSize() <= 0 || pageRequest.getPageSize() > 200) {
            pageRequest.setPageSize(20);
        }
        return pageRequest;
    }

    // endregion
}
