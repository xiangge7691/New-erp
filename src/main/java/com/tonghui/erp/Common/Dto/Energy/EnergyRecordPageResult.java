package com.tonghui.erp.Common.Dto.Energy;

import com.tonghui.erp.Data.Entity.EnergyRecord;
import lombok.Data;

import java.util.List;

/**
 * 能耗记录分页查询结果DTO
 * <p>
 * 包含分页数据与筛选条件下的费用汇总（随列表一次返回，供前端汇总卡片展示）
 * </p>
 */
@Data
public class EnergyRecordPageResult {

    /**
     * 当前页数据列表
     */
    private List<EnergyRecord> items;

    /**
     * 满足筛选条件的总记录数
     */
    private long totalCount;

    /**
     * 页码（从0开始）
     */
    private int pageIndex;

    /**
     * 每页数量
     */
    private int pageSize;

    /**
     * 筛选条件下的费用汇总
     */
    private EnergySummaryDto summary;
}
