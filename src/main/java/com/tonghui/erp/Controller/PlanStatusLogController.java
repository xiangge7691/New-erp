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
 * <p>
 * 处理计划状态变更日志相关的HTTP请求，提供RESTful API接口，包括日志的增删改查及高级查询操作
 * </p>
 *
 * 接口清单：
 * ┌────┬───────────────────────────────────────┬────────┬──────────────────────────────┐
 * │ #  │ 接口                                  │ 方法   │ 说明                         │
 * ├────┼───────────────────────────────────────┼────────┼──────────────────────────────┤
 * │ 1  │ /api/plan-status-logs                 │ GET    │ 分页查询日志列表             │
 * │ 2  │ /api/plan-status-logs/{id}            │ GET    │ 获取日志详情                 │
 * │ 3  │ /api/plan-status-logs                 │ POST   │ 新增日志                     │
 * │ 4  │ /api/plan-status-logs/{id}            │ PUT    │ 修改日志                     │
 * │ 5  │ /api/plan-status-logs/{id}            │ DELETE │ 删除日志                     │
 * │ 6  │ /api/plan-status-logs/search          │ GET    │ 高级查询日志（多条件+分页）  │
 * └────┴───────────────────────────────────────┴────────┴──────────────────────────────┘
 */
@RestController
@RequestMapping("/api/plan-status-logs")
public class PlanStatusLogController extends BaseCrudController<PlanStatusLog, PlanStatusLog, Integer> {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    @Autowired
    private PlanStatusLogService planStatusLogService;

    // endregion

    // region CRUD操作实现方法
    // ===================================
    // CRUD操作实现方法
    // ===================================

    @Override
    protected PagedResult<PlanStatusLog> getAllData(int pageIndex, int pageSize) {
        int safePageIndex = Math.max(0, pageIndex);
        int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);

        PlanStatusLog planStatusLog = new PlanStatusLog();
        Page<PlanStatusLog> pageResult = planStatusLogService.queryPlanStatusLogs(planStatusLog, null, null, safePageIndex, safePageSize);

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

    // endregion

    // region 搜索与查询
    // ===================================
    // 搜索与查询
    // ===================================

    /**
     * 高级查询计划状态日志（支持多条件 + 分页）
     * <p>
     * 可选查询条件：ID、计划ID、来源状态、目标状态、操作人、变更时间范围
     * </p>
     *
     * 示例请求：
     * GET /api/plan-status-logs/search?pageIndex=0&pageSize=20&planId=1001&toStatus=completed&changeTimeStart=2025-01-01T00:00:00&changeTimeEnd=2025-12-31T23:59:59
     *
     * @param planStatusLog 查询条件（自动从query参数映射）
     * @param changeTimeStart 变更时间起始（可选）
     * @param changeTimeEnd 变更时间结束（可选）
     * @param pageIndex 页码，从0开始
     * @param pageSize 每页大小
     * @return ApiResponse&lt;PagedResult&lt;PlanStatusLog&gt;&gt; 计划状态日志分页列表
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

    // endregion
}
