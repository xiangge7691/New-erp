package com.tonghui.erp.Common.Dto.Log;

import lombok.Data;

/**
 * 登录日志统计结果
 * <p>
 * 用于登录日志页顶部卡片展示指定时间范围内的登录情况
 * </p>
 */
@Data
public class LoginLogSummaryDto {

    // region 统计字段
    // ===================================
    // 统计字段
    // ===================================

    /**
     * 登录总次数
     */
    private Long totalCount;

    /**
     * 登录成功次数
     */
    private Long successCount;

    /**
     * 登录失败次数
     */
    private Long failCount;

    /**
     * 去重登录账号数
     */
    private Long uniqueAccountCount;

    // endregion
}
