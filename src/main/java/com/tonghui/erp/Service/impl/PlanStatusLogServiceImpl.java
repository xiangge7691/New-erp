package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tonghui.erp.Data.Entity.PlanStatusLog;
import com.tonghui.erp.Service.PlanStatusLogService;
import com.tonghui.erp.Data.mapper.PlanStatusLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
* @author 87954
* @description 针对表【plan_status_log(计划状态流水表)】的数据库操作Service实现
* @createDate 2025-12-08 14:02:29
*/
@Service
public class PlanStatusLogServiceImpl extends ServiceImpl<PlanStatusLogMapper, PlanStatusLog>
    implements PlanStatusLogService{
    
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
    @Override
    public Page<PlanStatusLog> queryPlanStatusLogs(PlanStatusLog planStatusLog, LocalDateTime changeTimeStart, LocalDateTime changeTimeEnd, int pageNum, int pageSize) {
        // 将页码从0开始转换为1开始
        int actualPageNum = pageNum + 1;

        Page<PlanStatusLog> page = new Page<>(actualPageNum, pageSize);
        QueryWrapper<PlanStatusLog> wrapper = new QueryWrapper<>();

        if (planStatusLog.getId() != null) {
            wrapper.eq("id", planStatusLog.getId());
        }
        if (planStatusLog.getPlanId() != null) {
            wrapper.eq("plan_id", planStatusLog.getPlanId());
        }
        if (StringUtils.hasText(planStatusLog.getFromStatus())) {
            wrapper.eq("from_status", planStatusLog.getFromStatus());
        }
        if (StringUtils.hasText(planStatusLog.getToStatus())) {
            wrapper.eq("to_status", planStatusLog.getToStatus());
        }
        if (planStatusLog.getOperator() != null) {
            wrapper.eq("operator", planStatusLog.getOperator());
        }
        
        // Handle change time range query
        if (changeTimeStart != null) {
            wrapper.ge("change_time", changeTimeStart);
        }
        if (changeTimeEnd != null) {
            wrapper.le("change_time", changeTimeEnd);
        }

        return this.page(page, wrapper);
    }
}




