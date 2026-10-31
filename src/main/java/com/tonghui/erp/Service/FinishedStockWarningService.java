package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Dto.FinishedStock.FinishedStockWarningDto;
import com.tonghui.erp.Common.Dto.FinishedStock.FinishedStockWarningSummaryDto;
import com.tonghui.erp.Common.Dto.PagedResult;

import java.util.List;

/**
 * 成品库存预警服务接口
 * <p>
 * 基于成品出库台账前 N 个完整自然月的已出库数量计算月平均出库量，
 * 结合当前成品可用库存计算可支撑时间，并在可支撑时间低于配置阈值时提示生产。
 * 统计月数、预警月数、近效期天数均读取系统配置项。
 * </p>
 */
public interface FinishedStockWarningService {

    // region 查询操作
    // ===================================
    // 查询操作
    // ===================================

    /**
     * 分页查询成品库存预警列表
     *
     * @param keyword           品种编码或名称关键字（可选，模糊匹配）
     * @param onlyWarning       是否只返回触发预警的品种
     * @param excludeNearExpiry 是否剔除近效期库存
     * @param pageIndex         页码（从0开始）
     * @param pageSize          每页大小
     * @return 分页后的预警列表
     */
    PagedResult<FinishedStockWarningDto> queryWarnings(String keyword,
                                                       boolean onlyWarning,
                                                       boolean excludeNearExpiry,
                                                       int pageIndex,
                                                       int pageSize);

    /**
     * 获取成品库存预警汇总数据
     *
     * @param excludeNearExpiry 是否剔除近效期库存
     * @return 预警数量统计与当前统计口径
     */
    FinishedStockWarningSummaryDto getSummary(boolean excludeNearExpiry);

    /**
     * 获取全部触发预警的品种（用于首页待办）
     *
     * @param excludeNearExpiry 是否剔除近效期库存
     * @return 触发预警的品种列表，按紧急程度排序
     */
    List<FinishedStockWarningDto> getWarnings(boolean excludeNearExpiry);

    // endregion
}
