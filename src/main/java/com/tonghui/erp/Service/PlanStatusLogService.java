package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Data.Entity.PlanStatusLog;

import java.time.LocalDateTime;

/**
*/
public interface PlanStatusLogService extends IService<PlanStatusLog> {
    /**
     * 高级查询计划状态日志（支持分页）
     *
     * @param planStatusLog 查询条件
     * @param changeTimeStart 变更时间起始
     * @param changeTimeEnd 变更时间结束
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    Page<PlanStatusLog> queryPlanStatusLogs(PlanStatusLog planStatusLog, LocalDateTime changeTimeStart, LocalDateTime changeTimeEnd, int pageNum, int pageSize);
}
