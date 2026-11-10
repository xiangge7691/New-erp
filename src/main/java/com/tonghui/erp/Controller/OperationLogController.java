package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.Log.DataChangeLogDto;
import com.tonghui.erp.Common.Dto.Log.OperationLogDetailDto;
import com.tonghui.erp.Common.Dto.Log.OperationLogDto;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.utils.LogQueryTimeUtils;
import com.tonghui.erp.Service.DataChangeLogService;
import com.tonghui.erp.Service.OperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 操作日志控制器
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────────┬────────┬──────────────────────────────┐
 * │ #  │ 接口                                 │ 方法   │ 说明                         │
 * ├────┼──────────────────────────────────────┼────────┼──────────────────────────────┤
 * │ 1  │ /api/system/operation-log/search     │ GET    │ 分页查询操作日志             │
 * │ 2  │ /api/system/operation-log/{id}       │ GET    │ 查询操作日志详情             │
 * └────┴──────────────────────────────────────┴────────┴──────────────────────────────┘
 */
@RestController
@RequestMapping("/api/system/operation-log")
public class OperationLogController extends BaseController {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 操作日志服务
     */
    @Autowired
    private OperationLogService operationLogService;

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
     * 分页查询操作日志
     *
     * 示例请求：
     * GET /api/system/operation-log/search?pageIndex=0&pageSize=20&userAccount=root&module=客户管理&operationType=CREATE&resultStatus=0&startTime=2026-11-01&endTime=2026-11-09
     *
     * @param userAccount   操作人账号（精确匹配，可选）
     * @param userId        操作人ID（可选）
     * @param userName      操作人姓名（模糊匹配，可选）
     * @param module        业务模块（模糊匹配，可选）
     * @param operationType 操作类型（精确匹配，可选）
     * @param resultStatus  结果：1成功/0失败（可选）
     * @param businessType  业务对象类型（可选）
     * @param businessId    业务对象ID（可选）
     * @param keyword       关键字，模糊匹配操作描述、请求地址、账号、姓名、业务ID（可选）
     * @param startTime     起始时间，支持 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss（可选）
     * @param endTime       截止时间，支持 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss（可选）
     * @param pageRequest   分页参数
     * @return ApiResponse&lt;PagedResult&lt;OperationLogDto&gt;&gt; 分页操作日志
     */
    @GetMapping("/search")
    public ApiResponse<PagedResult<OperationLogDto>> search(
            @RequestParam(required = false) String userAccount,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) Integer resultStatus,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String businessId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @ModelAttribute PageRequestDto pageRequest) {
        try {
            PagedResult<OperationLogDto> result = operationLogService.search(
                    userAccount, userId, userName, module, operationType, resultStatus,
                    businessType, businessId, keyword,
                    LogQueryTimeUtils.parseStart(startTime),
                    LogQueryTimeUtils.parseEnd(endTime),
                    processLogPageRequest(pageRequest));
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "查询操作日志");
        }
    }

    /**
     * 查询操作日志详情
     *
     * 示例请求：
     * GET /api/system/operation-log/1001
     *
     * @param id 日志ID
     * @return ApiResponse&lt;OperationLogDetailDto&gt; 日志详情
     */
    @GetMapping("/{id}")
    public ApiResponse<OperationLogDetailDto> detail(@PathVariable Long id) {
        try {
            OperationLogDetailDto detail = operationLogService.detail(id);
            if (detail == null) {
                return error("操作日志不存在");
            }
            return success(detail);
        } catch (Exception ex) {
            return exception(ex, "查询操作日志详情");
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

    /**
     * 查询操作日志对应的数据变更
     *
     * 返回本次操作产生的全部数据变更（主记录与级联记录），按执行顺序排列。
     *
     * 示例请求：
     * GET /api/system/operation-log/1001/changes
     *
     * @param id 操作日志ID
     * @return ApiResponse&lt;List&lt;DataChangeLogDto&gt;&gt; 数据变更列表
     */
    @GetMapping("/{id}/changes")
    public ApiResponse<List<DataChangeLogDto>> changes(@PathVariable Long id) {
        try {
            return success(dataChangeLogService.listByOperationLogId(id));
        } catch (Exception ex) {
            return exception(ex, "查询数据变更日志");
        }
    }

    // endregion
}
