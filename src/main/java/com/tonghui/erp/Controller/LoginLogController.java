package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.Log.LoginLogDetailDto;
import com.tonghui.erp.Common.Dto.Log.LoginLogDto;
import com.tonghui.erp.Common.Dto.Log.LoginLogSummaryDto;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.utils.LogQueryTimeUtils;
import com.tonghui.erp.Service.LoginLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 登录日志控制器
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────────┬────────┬──────────────────────────────┐
 * │ #  │ 接口                                 │ 方法   │ 说明                         │
 * ├────┼──────────────────────────────────────┼────────┼──────────────────────────────┤
 * │ 1  │ /api/system/login-log/search         │ GET    │ 分页查询登录日志             │
 * │ 2  │ /api/system/login-log/latest         │ GET    │ 查询用户最近一次成功登录     │
 * │ 3  │ /api/system/login-log/summary        │ GET    │ 登录统计（成功/失败/去重）   │
 * │ 4  │ /api/system/login-log/{id}           │ GET    │ 登录日志详情（角色/权限）     │
 * └────┴──────────────────────────────────────┴────────┴──────────────────────────────┘
 */
@RestController
@RequestMapping("/api/system/login-log")
public class LoginLogController extends BaseController {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 登录日志服务
     */
    @Autowired
    private LoginLogService loginLogService;

    // endregion

    // region 查询接口
    // ===================================
    // 查询接口
    // ===================================

    /**
     * 分页查询登录日志
     *
     * 示例请求：
     * GET /api/system/login-log/search?pageIndex=0&pageSize=20&userAccount=root&loginResult=0&macAddress=3C-52-82-1A-4F-09&startTime=2026-11-01&endTime=2026-11-09
     *
     * @param userAccount 登录账号（精确匹配，可选）
     * @param userId      用户ID（可选）
     * @param userName    用户姓名（模糊匹配，可选）
     * @param loginResult 登录结果：1成功/0失败（可选）
     * @param loginIp     登录IP（精确匹配，可选）
     * @param macAddress  客户端MAC地址（支持完整地址或后缀片段，可选）
     * @param roleName    角色名称（模糊匹配，可选）
     * @param permissionKey 接口权限键（模糊匹配，可选）
     * @param startTime   起始时间，支持 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss（可选）
     * @param endTime     截止时间，支持 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss（可选）
     * @param pageRequest 分页参数
     * @return ApiResponse&lt;PagedResult&lt;LoginLogDto&gt;&gt; 分页登录日志
     */
    @GetMapping("/search")
    public ApiResponse<PagedResult<LoginLogDto>> search(
            @RequestParam(required = false) String userAccount,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) Integer loginResult,
            @RequestParam(required = false) String loginIp,
            @RequestParam(required = false) String macAddress,
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) String permissionKey,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @ModelAttribute PageRequestDto pageRequest) {
        try {
            PagedResult<LoginLogDto> result = loginLogService.search(
                    userAccount, userId, userName, loginResult, loginIp, macAddress, roleName, permissionKey,
                    LogQueryTimeUtils.parseStart(startTime),
                    LogQueryTimeUtils.parseEnd(endTime),
                    processLogPageRequest(pageRequest));
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "查询登录日志");
        }
    }

    /**
     * 查询登录日志详情
     *
     * 返回登录当时的角色、可访问菜单和接口权限快照。
     *
     * 示例请求：
     * GET /api/system/login-log/1001
     *
     * @param id 日志ID
     * @return ApiResponse&lt;LoginLogDetailDto&gt; 登录日志详情
     */
    @GetMapping("/{id}")
    public ApiResponse<LoginLogDetailDto> detail(@PathVariable Long id) {
        try {
            LoginLogDetailDto detail = loginLogService.detail(id);
            if (detail == null) {
                return error("登录日志不存在");
            }
            return success(detail);
        } catch (Exception ex) {
            return exception(ex, "查询登录日志详情");
        }
    }

    /**
     * 查询指定用户最近一次成功登录记录
     *
     * 示例请求：
     * GET /api/system/login-log/latest?userId=1
     *
     * @param userId 用户ID
     * @return ApiResponse&lt;LoginLogDto&gt; 最近一次成功登录；无记录时 data 为空
     */
    @GetMapping("/latest")
    public ApiResponse<LoginLogDto> latest(@RequestParam Long userId) {
        try {
            return success(loginLogService.latestByUserId(userId));
        } catch (Exception ex) {
            return exception(ex, "查询最近登录记录");
        }
    }

    /**
     * 登录统计（成功次数、失败次数、去重账号数）
     *
     * 示例请求：
     * GET /api/system/login-log/summary?startTime=2026-11-01&endTime=2026-11-09
     * GET /api/system/login-log/summary（默认统计今天）
     *
     * @param startTime 起始时间（可选，默认今天 00:00:00）
     * @param endTime   截止时间（可选，默认当前时间）
     * @return ApiResponse&lt;LoginLogSummaryDto&gt; 登录统计
     */
    @GetMapping("/summary")
    public ApiResponse<LoginLogSummaryDto> summary(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        try {
            LocalDateTime start = LogQueryTimeUtils.parseStart(startTime);
            LocalDateTime end = LogQueryTimeUtils.parseEnd(endTime);
            if (start == null) {
                start = LocalDate.now().atStartOfDay();
            }
            if (end == null) {
                end = LocalDateTime.now();
            }
            return success(loginLogService.summary(start, end));
        } catch (Exception ex) {
            return exception(ex, "查询登录统计");
        }
    }

    // endregion

    // region 分页参数处理
    // ===================================
    // 分页参数处理
    // ===================================

    /**
     * 处理日志分页参数
     * <p>
     * 日志表数据量大，禁止 -1（全量）查询；缺省为第 0 页、每页 20 条，单页最多 200 条
     * </p>
     *
     * @param pageRequest 原始分页参数
     * @return 修正后的分页参数
     */
    private PageRequestDto processLogPageRequest(PageRequestDto pageRequest) {
        if (pageRequest == null) {
            pageRequest = new PageRequestDto();
        }
        if (pageRequest.getPageIndex() < 0) {
            pageRequest.setPageIndex(0);
        }
        if (pageRequest.getPageSize() <= 0 || pageRequest.getPageSize() > 200) {
            pageRequest.setPageSize(20);
        }
        return pageRequest;
    }

    // endregion
}
