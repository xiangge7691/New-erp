package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Dto.Log.LoginLogContext;
import com.tonghui.erp.Common.Dto.Log.LoginLogDto;
import com.tonghui.erp.Common.Dto.Log.LoginLogSummaryDto;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;

/**
 * 登录日志服务接口
 */
public interface LoginLogService {

    // region 日志记录
    // ===================================
    // 日志记录
    // ===================================

    /**
     * 构建登录日志上下文
     * <p>
     * 必须在请求线程中调用，提前采集 IP、归属地、User-Agent 与登录时间，
     * 避免异步线程丢失请求上下文
     * </p>
     *
     * @param request     当前 HTTP 请求
     * @param userAccount 登录账号（失败时为尝试登录的账号）
     * @param loginTime   登录时间
     * @return 登录日志上下文
     */
    LoginLogContext buildContext(HttpServletRequest request, String userAccount, LocalDateTime loginTime);

    /**
     * 异步写入登录日志
     *
     * @param context 登录日志上下文
     */
    void recordAsync(LoginLogContext context);

    // endregion

    // region 日志查询
    // ===================================
    // 日志查询
    // ===================================

    /**
     * 分页查询登录日志
     *
     * @param userAccount 登录账号（精确匹配，可为空）
     * @param userId      用户ID（精确匹配，可为空）
     * @param userName    用户姓名（模糊匹配，可为空）
     * @param loginResult 登录结果：1成功/0失败（可为空）
     * @param loginIp     登录IP（精确匹配，可为空）
     * @param startTime   起始时间（含，可为空）
     * @param endTime     截止时间（含，可为空）
     * @param pageRequest 分页参数
     * @return 分页登录日志
     */
    PagedResult<LoginLogDto> search(String userAccount, Long userId, String userName, Integer loginResult,
                                    String loginIp, LocalDateTime startTime, LocalDateTime endTime,
                                    PageRequestDto pageRequest);

    /**
     * 查询指定用户最近一次成功登录记录
     *
     * @param userId 用户ID
     * @return 最近一次成功登录；无记录返回 null
     */
    LoginLogDto latestByUserId(Long userId);

    /**
     * 统计指定时间范围内的登录情况
     *
     * @param startTime 起始时间（含）
     * @param endTime   截止时间（含）
     * @return 登录统计
     */
    LoginLogSummaryDto summary(LocalDateTime startTime, LocalDateTime endTime);

    // endregion

    // region 日志清理
    // ===================================
    // 日志清理
    // ===================================

    /**
     * 物理清理指定天数之前的登录日志
     *
     * @param retentionDays 保留天数
     * @param batchSize     单批删除条数
     * @return 实际删除条数
     */
    int cleanup(int retentionDays, int batchSize);

    // endregion
}
