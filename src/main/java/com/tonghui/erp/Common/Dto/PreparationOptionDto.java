package com.tonghui.erp.Common.Dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 制剂采购下拉选项数据传输对象
 * <p>
 * 供采购模块选择制剂使用，除基础展示字段外，附带批件过期状态标识：
 * 已过期（EXPIRED）/ 未过期（VALID）/ 未添加批件过期时间（UNSET）
 * </p>
 */
@Data
public class PreparationOptionDto {

    // region 过期状态常量
    // ===================================
    // 过期状态常量
    // ===================================

    /**
     * 已过期
     */
    public static final String STATUS_EXPIRED = "EXPIRED";

    /**
     * 未过期
     */
    public static final String STATUS_VALID = "VALID";

    /**
     * 未添加批件过期时间
     */
    public static final String STATUS_UNSET = "UNSET";

    // endregion

    // region 制剂基本信息
    // ===================================
    // 制剂基本信息
    // ===================================

    /**
     * 制剂唯一标识
     */
    private Long preparationId;

    /**
     * 制剂编码
     */
    private String preparationCode;

    /**
     * 制剂品名
     */
    private String preparationName;

    /**
     * 规格描述
     */
    private String spec;

    /**
     * 单位名称
     */
    private String unitName;

    /**
     * 生产单位
     */
    private String producer;

    /**
     * 状态：1启用/0禁用
     */
    private Integer status;

    // endregion

    // region 批件过期信息
    // ===================================
    // 批件过期信息
    // ===================================

    /**
     * 批件过期时间（批件有效期截止日期，可为空）
     */
    private LocalDate approvalExpiryDate;

    /**
     * 过期状态标识：EXPIRED 已过期 / VALID 未过期 / UNSET 未添加
     */
    private String expiryStatus;

    /**
     * 过期状态中文描述：已过期 / 未过期 / 未添加
     */
    private String expiryStatusText;

    /**
     * 距过期剩余天数（已过期为负数；未添加批件过期时间时为空）
     */
    private Integer remainingDays;

    // endregion
}
