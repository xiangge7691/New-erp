package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.FinishedStock.FinishedStockWarningDto;
import com.tonghui.erp.Common.Dto.FinishedStock.FinishedStockWarningSummaryDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Service.FinishedStockWarningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 成品库存预警控制器
 * <p>
 * 基于成品出库台账前 N 个完整自然月的已出库数量计算月平均出库量，
 * 结合成品可用库存计算可支撑时间，为生产安排提供预警依据
 * </p>
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────────────────┬────────┬──────────────────────────────────┐
 * │ #  │ 接口                                         │ 方法   │ 说明                             │
 * ├────┼──────────────────────────────────────────────┼────────┼──────────────────────────────────┤
 * │ 1  │ /api/finished-stock-warning/list             │ GET    │ 分页查询成品库存预警列表         │
 * │ 2  │ /api/finished-stock-warning/summary          │ GET    │ 查询预警汇总统计与当前统计口径   │
 * └────┴──────────────────────────────────────────────┴────────┴──────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/finished-stock-warning")
public class FinishedStockWarningController extends BaseController {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 成品库存预警服务
     */
    private final FinishedStockWarningService finishedStockWarningService;

    /**
     * 构造方法注入成品库存预警服务
     *
     * @param finishedStockWarningService 成品库存预警服务
     */
    @Autowired
    public FinishedStockWarningController(FinishedStockWarningService finishedStockWarningService) {
        this.finishedStockWarningService = finishedStockWarningService;
    }

    // endregion

    // region 查询接口
    // ===================================
    // 查询接口
    // ===================================

    /**
     * 分页查询成品库存预警列表
     * <p>
     * 月平均出库量按系统配置的统计月数（默认前3个完整自然月）自动计算；
     * 可用库存默认剔除近效期批次，可通过 excludeNearExpiry=false 查看未剔除口径。
     * </p>
     *
     * 示例请求：
     * GET /api/finished-stock-warning/list?pageIndex=0&pageSize=20
     * GET /api/finished-stock-warning/list?pageIndex=0&pageSize=20&keyword=壮骨
     * GET /api/finished-stock-warning/list?pageIndex=0&pageSize=20&onlyWarning=true&excludeNearExpiry=true
     *
     * @param keyword           品种编码或名称关键字（可选，模糊匹配）
     * @param onlyWarning       是否只返回触发预警的品种，默认 false
     * @param excludeNearExpiry 是否剔除近效期库存，默认 true
     * @param pageIndex         页码（从0开始），默认 0
     * @param pageSize          每页大小，默认 20
     * @return 分页后的成品库存预警列表
     */
    @GetMapping("/list")
    public ApiResponse<PagedResult<FinishedStockWarningDto>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "false") boolean onlyWarning,
            @RequestParam(defaultValue = "true") boolean excludeNearExpiry,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "20") int pageSize) {
        try {
            int safePageIndex = Math.max(0, pageIndex);
            int safePageSize = pageSize <= 0 ? 20 : pageSize;
            return success(finishedStockWarningService.queryWarnings(
                    keyword, onlyWarning, excludeNearExpiry, safePageIndex, safePageSize));
        } catch (Exception ex) {
            return exception(ex, "查询成品库存预警列表");
        }
    }

    /**
     * 查询成品库存预警汇总统计
     * <p>
     * 返回品种总数、预警数量、各预警级别数量，以及当前统计月数、
     * 预警月数、近效期天数、统计窗口等口径信息，供页面顶部展示。
     * </p>
     *
     * 示例请求：
     * GET /api/finished-stock-warning/summary
     * GET /api/finished-stock-warning/summary?excludeNearExpiry=false
     *
     * @param excludeNearExpiry 是否剔除近效期库存，默认 true
     * @return 预警汇总统计与当前统计口径
     */
    @GetMapping("/summary")
    public ApiResponse<FinishedStockWarningSummaryDto> summary(
            @RequestParam(defaultValue = "true") boolean excludeNearExpiry) {
        try {
            return success(finishedStockWarningService.getSummary(excludeNearExpiry));
        } catch (Exception ex) {
            return exception(ex, "查询成品库存预警汇总");
        }
    }

    // endregion
}
