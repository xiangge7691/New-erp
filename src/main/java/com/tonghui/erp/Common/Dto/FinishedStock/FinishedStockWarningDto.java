package com.tonghui.erp.Common.Dto.FinishedStock;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 成品库存预警行 DTO
 * <p>
 * 用于成品库存预警页面展示单个品种的月平均出库量、可用库存、
 * 可支撑时间及预警状态，同时附带统计窗口内各月出库明细
 * </p>
 */
@Data
public class FinishedStockWarningDto {

    // region 品种信息
    // ===================================
    // 品种信息
    // ===================================

    /**
     * 制剂ID（主数据缺失时可能为空）
     */
    private Long preparationId;

    /**
     * 制剂编码
     */
    private String preparationCode;

    /**
     * 制剂名称
     */
    private String preparationName;

    /**
     * 规格
     */
    private String spec;

    /**
     * 计量单位
     */
    private String unitName;

    // endregion

    // region 库存与出库数据
    // ===================================
    // 库存与出库数据
    // ===================================

    /**
     * 成品总库存数量（含待检、不合格等全部状态）
     */
    private BigDecimal totalStock;

    /**
     * 合格成品库存数量（可用库存的计算基数）
     */
    private BigDecimal qualifiedStock;

    /**
     * 可用库存数量（勾选剔除近效期时 = 合格库存 - 近效期库存）
     */
    private BigDecimal availableStock;

    /**
     * 近效期库存数量（含已过期）
     */
    private BigDecimal nearExpiryStock;

    /**
     * 已过期库存数量
     */
    private BigDecimal expiredStock;

    /**
     * 统计窗口内已出库数量合计
     */
    private BigDecimal totalOutbound;

    /**
     * 自动计算的月平均出库量（总出库量 ÷ 统计月数）
     */
    private BigDecimal autoMonthlyAvg;

    /**
     * 可支撑时间（月）= 可用库存 ÷ 月平均出库量；月均出库量为 0 时为空
     */
    private BigDecimal supportMonths;

    // endregion

    // region 预警信息
    // ===================================
    // 预警信息
    // ===================================

    /**
     * 当前预警阈值（月）
     */
    private Integer warningMonths;

    /**
     * 是否触发成品库存预警
     */
    private boolean warning;

    /**
     * 预警级别：danger-不足1个月 / warning-低于阈值 / normal-正常 / noDemand-无出库
     */
    private String warningLevel;

    /**
     * 统计月数
     */
    private Integer avgMonths;

    /**
     * 统计窗口起始月份（格式：yyyy-MM）
     */
    private String avgStartMonth;

    /**
     * 统计窗口结束月份（格式：yyyy-MM）
     */
    private String avgEndMonth;

    /**
     * 统计窗口内各月出库明细（无出库月份补 0）
     */
    private List<MonthlyOutboundDto> outboundMonths;

    // endregion
}
