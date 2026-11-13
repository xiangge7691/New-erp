package com.tonghui.erp.Common.Dto.Log;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 业务变更历史结果
 * <p>
 * 按业务对象聚合操作日志与字段级数据变更，供业务详情页直接渲染变更时间线
 * </p>
 */
@Data
public class BusinessHistoryDto {

    // region 业务对象字段
    // ===================================
    // 业务对象字段
    // ===================================

    /**
     * 业务类型：WORK_ORDER / PREPARATION
     */
    private String bizType;

    /**
     * 业务对象主键
     */
    private String bizId;

    /**
     * 业务对象展示名称
     */
    private String title;

    // endregion

    // region 汇总字段
    // ===================================
    // 汇总字段
    // ===================================

    /**
     * 字段级变更统计，用于详情页字段角标
     */
    private List<BusinessHistoryFieldStatDto> fieldStats = new ArrayList<>();

    /**
     * 记录级变更统计，用于关联明细行角标（如处方行）
     */
    private List<BusinessHistoryRecordStatDto> recordStats = new ArrayList<>();

    // endregion

    // region 时间线字段
    // ===================================
    // 时间线字段
    // ===================================

    /**
     * 变更事件列表（按时间倒序）
     */
    private List<BusinessHistoryEventDto> events = new ArrayList<>();

    /**
     * 满足筛选条件的事件总数
     */
    private long totalCount;

    /**
     * 页码，从0开始
     */
    private int pageIndex;

    /**
     * 每页事件数
     */
    private int pageSize;

    /**
     * 变更数据是否因单次查询上限被截断
     */
    private boolean truncated;

    // endregion
}
