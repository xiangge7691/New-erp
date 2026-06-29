package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.PlanStatusLog;
import com.tonghui.erp.Service.PlanStatusLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 计划状态日志控制器
 */
@RestController
@RequestMapping("/api/plan-status-logs")
public class PlanStatusLogController extends BaseCrudController<PlanStatusLog, PlanStatusLog, Integer> {

    @Autowired
    private PlanStatusLogService planStatusLogService;

    @Override
    protected PagedResult<PlanStatusLog> getAllData(int pageIndex, int pageSize) {
        // 页码从0开始的处理，确保不为负数
        int safePageIndex = Math.max(0, pageIndex);
        // 当pageSize<=0时，设置一个合理的默认值
        int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);

        // 使用PlanStatusLogService的queryPlanStatusLogs方法进行查询
        PlanStatusLog planStatusLog = new PlanStatusLog();
        Page<PlanStatusLog> pageResult = planStatusLogService.queryPlanStatusLogs(planStatusLog, null, null, safePageIndex, safePageSize);

        // 转换为PagedResult
        PagedResult<PlanStatusLog> pagedResult = new PagedResult<>();
        pagedResult.setItems(pageResult.getRecords());
        pagedResult.setTotalCount((int) pageResult.getTotal());
        pagedResult.setPageIndex(safePageIndex);
        pagedResult.setPageSize((int) pageResult.getSize());

        return pagedResult;
    }

    @Override
    protected PlanStatusLog getDataById(Integer id) {
        return planStatusLogService.getById(id);
    }

    @Override
    protected PlanStatusLog doCreate(PlanStatusLog entity) {
        planStatusLogService.save(entity);
        return entity;
    }

    @Override
    protected PlanStatusLog doUpdate(Integer id, PlanStatusLog entity) {
        entity.setId(id);
        planStatusLogService.updateById(entity);
        return entity;
    }

    @Override
    protected boolean doDelete(Integer id) {
        return planStatusLogService.removeById(id);
    }

    // #region 高级查询

    /**
     * 高级查询计划状态日志（支持多条件 + 分页）
     *
     * 可选查询条件：
     * - planId：精确匹配
     * - fromStatus：精确匹配
     * - toStatus：精确匹配
     * - operator：精确匹配
     * - changeTimeStart：变更时间起始（大于等于）
     * - changeTimeEnd：变更时间结束（小于等于）
     *
     * 示例请求：
     * GET /api/plan-status-logs/search?pageIndex=1&pageSize=20&planId=1001&toStatus=completed&changeTimeStart=2025-01-01T00:00:00&changeTimeEnd=2025-12-31T23:59:59
     *
     * @param planStatusLog 查询条件（自动从query参数映射）
     * @param changeTimeStart 变更时间起始
     * @param changeTimeEnd 变更时间结束
     * @param pageIndex 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    @GetMapping("/search")
    public ApiResponse<PagedResult<PlanStatusLog>> queryPlanStatusLogs(PlanStatusLog planStatusLog,
                                                                       @RequestParam(required = false) LocalDateTime changeTimeStart,
                                                                       @RequestParam(required = false) LocalDateTime changeTimeEnd,
                                                                       @RequestParam int pageIndex,
                                                                       @RequestParam int pageSize) {
        try {
            // 页码从0开始的处理，确保不为负数
            int safePageIndex = Math.max(0, pageIndex);
            // 当pageSize<=0时，设置一个合理的默认值
            int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);

            // 获取分页结果
            Page<PlanStatusLog> pageResult = planStatusLogService.queryPlanStatusLogs(planStatusLog, changeTimeStart, changeTimeEnd, safePageIndex, safePageSize);

            // 转换为统一的PagedResult格式
            PagedResult<PlanStatusLog> pagedResult = new PagedResult<>();
            pagedResult.setItems(pageResult.getRecords());
            pagedResult.setTotalCount((int) pageResult.getTotal());
            pagedResult.setPageIndex(safePageIndex);
            pagedResult.setPageSize((int) pageResult.getSize());

            return success(pagedResult);
        } catch (Exception ex) {
            return error("查询失败：" + ex.getMessage());
        }
    }

    // #endregion
}
