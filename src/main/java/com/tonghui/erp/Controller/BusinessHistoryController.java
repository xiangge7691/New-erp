package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.Log.BusinessHistoryDto;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.utils.LogQueryTimeUtils;
import com.tonghui.erp.Service.BusinessHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 业务变更历史控制器
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────────────────┬────────┬──────────────────────────────┐
 * │ #  │ 接口                                         │ 方法   │ 说明                         │
 * ├────┼──────────────────────────────────────────────┼────────┼──────────────────────────────┤
 * │ 1  │ /api/business-history/{bizType}/{bizId}      │ GET    │ 聚合业务对象变更历史         │
 * └────┴──────────────────────────────────────────────┴────────┴──────────────────────────────┘
 */
@RestController
@RequestMapping("/api/business-history")
public class BusinessHistoryController extends BaseController {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 业务变更历史服务
     */
    @Autowired
    private BusinessHistoryService businessHistoryService;

    // endregion

    // region 查询接口
    // ===================================
    // 查询接口
    // ===================================

    /**
     * 查询业务对象的变更历史
     *
     * 返回按操作归组的字段级变更时间线，同时返回字段与关联记录变更次数统计，
     * 供业务详情页直接渲染字段角标和变更列表。
     *
     * 示例请求：
     * GET /api/business-history/WORK_ORDER/880?pageIndex=0&pageSize=20
     * GET /api/business-history/PREPARATION/42?pageIndex=0&pageSize=20&field=spec
     * GET /api/business-history/PREPARATION/42?pageIndex=0&pageSize=20&source=RELATED&startTime=2026-11-01&endTime=2026-11-13
     *
     * @param bizType    业务类型：WORK_ORDER（生产任务）/ PREPARATION（制剂）
     * @param bizId      业务对象主键
     * @param field      字段筛选，数据库列名（可选）
     * @param source     来源筛选：PRIMARY主表/RELATED关联明细（可选）
     * @param startTime  起始时间，支持 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss（可选）
     * @param endTime    截止时间，支持 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss（可选）
     * @param pageRequest 分页参数（按操作事件分页）
     * @return ApiResponse&lt;BusinessHistoryDto&gt; 业务变更历史
     */
    @GetMapping("/{bizType}/{bizId}")
    public ApiResponse<BusinessHistoryDto> history(
            @PathVariable String bizType,
            @PathVariable String bizId,
            @RequestParam(required = false) String field,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @ModelAttribute PageRequestDto pageRequest) {
        try {
            BusinessHistoryDto result = businessHistoryService.query(
                    bizType, bizId, field, source,
                    LogQueryTimeUtils.parseStart(startTime),
                    LogQueryTimeUtils.parseEnd(endTime),
                    processLogPageRequest(pageRequest));
            return success(result);
        } catch (IllegalArgumentException ex) {
            return error(ex.getMessage());
        } catch (Exception ex) {
            return exception(ex, "查询业务变更历史");
        }
    }

    // endregion

    // region 分页参数处理
    // ===================================
    // 分页参数处理
    // ===================================

    /**
     * 处理业务历史分页参数
     * <p>
     * 历史事件按页返回；缺省第 0 页、每页 20 条，单页最多 200 条
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
