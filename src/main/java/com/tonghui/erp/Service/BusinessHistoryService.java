package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Dto.Log.BusinessHistoryDto;
import com.tonghui.erp.Common.Dto.PageRequestDto;

import java.time.LocalDateTime;

/**
 * 业务变更历史服务接口
 * <p>
 * 按业务对象聚合操作日志与字段级数据变更，供生产任务、制剂等详情页展示变更时间线
 * </p>
 */
public interface BusinessHistoryService {

    /**
     * 查询业务对象的变更历史
     *
     * @param bizType    业务类型：WORK_ORDER / PREPARATION
     * @param bizId      业务对象主键
     * @param field      字段筛选（数据库列名，可为空）
     * @param source     来源筛选：PRIMARY / RELATED（可为空）
     * @param startTime  起始时间（可为空）
     * @param endTime    截止时间（可为空）
     * @param pageRequest 分页参数（按操作事件分页）
     * @return 业务变更历史
     */
    BusinessHistoryDto query(String bizType, String bizId, String field, String source,
                             LocalDateTime startTime, LocalDateTime endTime,
                             PageRequestDto pageRequest);
}
