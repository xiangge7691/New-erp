package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Config.LogProperties;
import com.tonghui.erp.Common.Dto.Log.LoginLogContext;
import com.tonghui.erp.Common.Dto.Log.LoginLogDetailDto;
import com.tonghui.erp.Common.Dto.Log.LoginLogDto;
import com.tonghui.erp.Common.Dto.Log.LoginLogSummaryDto;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.utils.ClientMacUtils;
import com.tonghui.erp.Common.utils.IpLocationService;
import com.tonghui.erp.Common.utils.IpUtils;
import com.tonghui.erp.Common.utils.LoginPermissionSnapshotUtils;
import com.tonghui.erp.Data.Entity.SysLoginLog;
import com.tonghui.erp.Data.mapper.SysLoginLogMapper;
import com.tonghui.erp.Service.LoginLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 登录日志服务实现类
 * <p>
 * 负责登录日志的采集、异步写入与分页查询
 * </p>
 */
@Service
public class LoginLogServiceImpl implements LoginLogService {

    // region 常量与依赖
    // ===================================
    // 常量与依赖
    // ===================================

    private static final Logger log = LoggerFactory.getLogger(LoginLogServiceImpl.class);

    /** User-Agent 最大长度 */
    private static final int MAX_USER_AGENT_LENGTH = 500;

    /** MAC 地址最大长度 */
    private static final int MAX_MAC_LENGTH = ClientMacUtils.MAX_MAC_LENGTH;

    /** 失败原因最大长度 */
    private static final int MAX_FAIL_REASON_LENGTH = 255;

    /** 角色快照最大长度 */
    private static final int MAX_ROLE_LENGTH = 500;

    /** 权限快照最大长度（TEXT 字段，预留足够空间） */
    private static final int MAX_PERMISSION_TEXT_LENGTH = 60000;

    /** 单次清理最大循环次数，避免异常情况下无限删除 */
    private static final int MAX_CLEANUP_ROUNDS = 1000;

    private final SysLoginLogMapper sysLoginLogMapper;
    private final LogProperties logProperties;
    private final IpLocationService ipLocationService;

    /**
     * 构造登录日志服务
     *
     * @param sysLoginLogMapper  登录日志Mapper
     * @param logProperties      日志配置
     * @param ipLocationService  IP归属地服务
     */
    public LoginLogServiceImpl(SysLoginLogMapper sysLoginLogMapper,
                               LogProperties logProperties,
                               IpLocationService ipLocationService) {
        this.sysLoginLogMapper = sysLoginLogMapper;
        this.logProperties = logProperties;
        this.ipLocationService = ipLocationService;
    }

    // endregion

    // region 日志记录
    // ===================================
    // 日志记录
    // ===================================

    /**
     * 构建登录日志上下文
     *
     * @param request     当前 HTTP 请求
     * @param userAccount 登录账号
     * @param loginTime   登录时间
     * @return 登录日志上下文
     */
    @Override
    public LoginLogContext buildContext(HttpServletRequest request, String userAccount, LocalDateTime loginTime) {
        LoginLogContext context = new LoginLogContext();
        context.setUserAccount(userAccount);
        context.setLoginTime(loginTime == null ? LocalDateTime.now() : loginTime);
        if (request != null) {
            try {
                String clientIp = IpUtils.getClientIp(request, logProperties.getIp().isTrustProxyHeaders());
                context.setLoginIp(clientIp);
                context.setMacAddress(ClientMacUtils.resolve(request));
                context.setIpLocation(ipLocationService.resolve(clientIp));
                context.setUserAgent(truncate(request.getHeader("User-Agent"), MAX_USER_AGENT_LENGTH));
            } catch (Exception e) {
                // 采集客户端信息失败不能影响登录本身
                log.warn("采集登录客户端信息失败：{}", e.getMessage());
            }
        }
        return context;
    }

    /**
     * 异步写入登录日志
     *
     * @param context 登录日志上下文
     */
    @Async("logExecutor")
    @Override
    public void recordAsync(LoginLogContext context) {
        if (context == null) {
            return;
        }
        boolean success = context.getLoginResult() != null && context.getLoginResult() == 1;
        if (success && !logProperties.getLogin().isRecordSuccess()) {
            return;
        }
        if (!success && !logProperties.getLogin().isRecordFailures()) {
            return;
        }
        try {
            SysLoginLog entity = new SysLoginLog();
            entity.setUserId(context.getUserId());
            // user_account 为 NOT NULL，登录参数为空时落空串，避免丢失失败记录
            entity.setUserAccount(context.getUserAccount() == null ? "" : truncate(context.getUserAccount(), 64));
            entity.setUserName(truncate(context.getUserName(), 64));
            // 登录当时的角色与权限快照，便于回溯账号实际拥有的操作范围
            entity.setRoleIds(truncate(LoginPermissionSnapshotUtils.join(
                    context.getRoleIds(), LoginPermissionSnapshotUtils.KEY_SEPARATOR), MAX_ROLE_LENGTH));
            entity.setRoleNames(truncate(LoginPermissionSnapshotUtils.joinNames(
                    context.getRoleNames()), MAX_ROLE_LENGTH));
            entity.setMenuCount(sizeOf(context.getMenuKeys()));
            entity.setMenuKeys(truncate(LoginPermissionSnapshotUtils.joinKeys(
                    context.getMenuKeys()), MAX_PERMISSION_TEXT_LENGTH));
            entity.setMenuNames(truncate(LoginPermissionSnapshotUtils.joinNames(
                    context.getMenuNames()), MAX_PERMISSION_TEXT_LENGTH));
            entity.setPermissionCount(sizeOf(context.getPermissionKeys()));
            entity.setPermissionKeys(truncate(LoginPermissionSnapshotUtils.joinKeys(
                    context.getPermissionKeys()), MAX_PERMISSION_TEXT_LENGTH));
            entity.setPermissionNames(truncate(LoginPermissionSnapshotUtils.joinNames(
                    context.getPermissionNames()), MAX_PERMISSION_TEXT_LENGTH));
            entity.setLoginType(context.getLoginType() == null ? "LOGIN" : context.getLoginType());
            entity.setLoginResult(success ? 1 : 0);
            entity.setFailReason(truncate(context.getFailReason(), MAX_FAIL_REASON_LENGTH));
            entity.setLoginIp(truncate(context.getLoginIp(), 60));
            entity.setMacAddress(truncate(context.getMacAddress(), MAX_MAC_LENGTH));
            entity.setIpLocation(truncate(context.getIpLocation(), 120));
            entity.setUserAgent(truncate(context.getUserAgent(), MAX_USER_AGENT_LENGTH));
            entity.setLoginTime(context.getLoginTime() == null ? LocalDateTime.now() : context.getLoginTime());
            sysLoginLogMapper.insert(entity);
        } catch (Exception e) {
            // 日志失败绝不影响业务，仅记录应用日志
            log.error("写入登录日志失败：账号={}，原因={}", context.getUserAccount(), e.getMessage(), e);
        }
    }

    // endregion

    // region 日志查询
    // ===================================
    // 日志查询
    // ===================================

    /**
     * 分页查询登录日志
     *
     * @param userAccount 登录账号
     * @param userId      用户ID
     * @param userName    用户姓名
     * @param loginResult 登录结果
     * @param loginIp     登录IP
     * @param macAddress  客户端MAC地址（支持完整地址或后缀片段）
     * @param startTime   起始时间
     * @param endTime     截止时间
     * @param pageRequest 分页参数
     * @return 分页登录日志
     */
    @Override
    public PagedResult<LoginLogDto> search(String userAccount, Long userId, String userName, Integer loginResult,
                                           String loginIp, String macAddress, String roleName, String permissionKey,
                                           LocalDateTime startTime, LocalDateTime endTime,
                                           PageRequestDto pageRequest) {
        QueryWrapper<SysLoginLog> wrapper = new QueryWrapper<>();
        wrapper.eq(hasText(userAccount), "user_account", userAccount);
        wrapper.eq(userId != null, "user_id", userId);
        wrapper.like(hasText(userName), "user_name", userName);
        wrapper.eq(loginResult != null, "login_result", loginResult);
        wrapper.eq(hasText(loginIp), "login_ip", loginIp);
        applyMacFilter(wrapper, macAddress);
        wrapper.like(hasText(roleName), "role_names", roleName);
        wrapper.like(hasText(permissionKey), "permission_keys", permissionKey);
        wrapper.ge(startTime != null, "login_time", startTime);
        wrapper.le(endTime != null, "login_time", endTime);
        wrapper.orderByDesc("login_time").orderByDesc("id");

        Page<SysLoginLog> page = PagedResult.toMybatisPage(pageRequest);
        page = sysLoginLogMapper.selectPage(page, wrapper);
        PagedResult<SysLoginLog> entityResult = PagedResult.fromPage(page, pageRequest);

        PagedResult<LoginLogDto> result = new PagedResult<>();
        result.setItems(entityResult.getItems().stream().map(this::toDto).toList());
        result.setTotalCount(entityResult.getTotalCount());
        result.setPageIndex(entityResult.getPageIndex());
        result.setPageSize(entityResult.getPageSize());
        return result;
    }

    /**
     * 查询登录日志详情
     *
     * @param id 日志ID
     * @return 登录日志详情；不存在返回 null
     */
    @Override
    public LoginLogDetailDto detail(Long id) {
        if (id == null) {
            return null;
        }
        SysLoginLog entity = sysLoginLogMapper.selectById(id);
        if (entity == null) {
            return null;
        }
        LoginLogDetailDto dto = new LoginLogDetailDto();
        BeanUtils.copyProperties(entity, dto);
        fillSnapshot(dto, entity);
        dto.setMenuKeys(LoginPermissionSnapshotUtils.splitKeys(entity.getMenuKeys()));
        dto.setMenuNames(LoginPermissionSnapshotUtils.splitNames(entity.getMenuNames()));
        dto.setPermissionKeys(LoginPermissionSnapshotUtils.splitKeys(entity.getPermissionKeys()));
        dto.setPermissionNames(LoginPermissionSnapshotUtils.splitNames(entity.getPermissionNames()));
        return dto;
    }

    /**
     * 查询指定用户最近一次成功登录记录
     *
     * @param userId 用户ID
     * @return 最近一次成功登录；无记录返回 null
     */
    @Override
    public LoginLogDto latestByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        QueryWrapper<SysLoginLog> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("login_result", 1)
                .orderByDesc("login_time")
                .orderByDesc("id")
                .last("LIMIT 1");
        SysLoginLog entity = sysLoginLogMapper.selectOne(wrapper);
        return entity == null ? null : toDto(entity);
    }

    /**
     * 统计指定时间范围内的登录情况
     *
     * @param startTime 起始时间
     * @param endTime   截止时间
     * @return 登录统计
     */
    @Override
    public LoginLogSummaryDto summary(LocalDateTime startTime, LocalDateTime endTime) {
        LoginLogSummaryDto summary = new LoginLogSummaryDto();
        summary.setTotalCount(defaultZero(sysLoginLogMapper.countByTimeRange(startTime, endTime)));
        summary.setSuccessCount(defaultZero(sysLoginLogMapper.countSuccessByTimeRange(startTime, endTime)));
        summary.setFailCount(defaultZero(sysLoginLogMapper.countFailByTimeRange(startTime, endTime)));
        summary.setUniqueAccountCount(defaultZero(sysLoginLogMapper.countDistinctAccountByTimeRange(startTime, endTime)));
        return summary;
    }

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
    @Override
    public int cleanup(int retentionDays, int batchSize) {
        if (retentionDays <= 0 || batchSize <= 0) {
            return 0;
        }
        LocalDateTime before = LocalDateTime.now().minusDays(retentionDays);
        int total = 0;
        for (int round = 0; round < MAX_CLEANUP_ROUNDS; round++) {
            int deleted = sysLoginLogMapper.deleteBefore(before, batchSize);
            total += deleted;
            if (deleted < batchSize) {
                break;
            }
        }
        return total;
    }

    // endregion

    // region 辅助方法
    // ===================================
    // 辅助方法
    // ===================================

    /**
     * 实体转换为查询结果
     *
     * @param entity 登录日志实体
     * @return 查询结果
     */
    private LoginLogDto toDto(SysLoginLog entity) {
        LoginLogDto dto = new LoginLogDto();
        BeanUtils.copyProperties(entity, dto);
        fillSnapshot(dto, entity);
        return dto;
    }

    /**
     * 填充角色与权限数量快照
     *
     * @param dto    查询结果
     * @param entity 登录日志实体
     */
    private void fillSnapshot(LoginLogDto dto, SysLoginLog entity) {
        dto.setRoleIds(LoginPermissionSnapshotUtils.splitIds(entity.getRoleIds()));
        dto.setRoleNames(LoginPermissionSnapshotUtils.splitNames(entity.getRoleNames()));
        dto.setMenuCount(entity.getMenuCount() == null ? 0 : entity.getMenuCount());
        dto.setPermissionCount(entity.getPermissionCount() == null ? 0 : entity.getPermissionCount());
    }

    /**
     * 追加 MAC 地址筛选条件
     * <p>
     * 支持完整地址或后缀片段，忽略短横线/冒号分隔符；查询值参数化，
     * 列上使用 REPLACE 仅为兼容历史格式，数据量大时优先配合时间范围查询
     * </p>
     *
     * @param wrapper    查询条件
     * @param macAddress 原始 MAC 查询关键字
     */
    private void applyMacFilter(QueryWrapper<SysLoginLog> wrapper, String macAddress) {
        String mac = ClientMacUtils.normalizeQuery(macAddress);
        if (mac == null) {
            return;
        }
        String column = "UPPER(REPLACE(REPLACE(mac_address, '-', ''), ':', ''))";
        if (mac.length() == 12) {
            wrapper.apply(column + " = {0}", mac);
        } else {
            wrapper.apply(column + " LIKE CONCAT('%', {0})", mac);
        }
    }

    /**
     * 判断字符串是否有内容
     *
     * @param text 字符串
     * @return 是否有内容
     */
    private boolean hasText(String text) {
        return text != null && !text.trim().isEmpty();
    }

    /**
     * 空计数转为 0
     *
     * @param value 计数值
     * @return 非空计数值
     */
    private Long defaultZero(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * 计算列表大小
     *
     * @param values 列表
     * @return 元素数量
     */
    private int sizeOf(List<?> values) {
        return values == null ? 0 : values.size();
    }

    /**
     * 按长度截断字符串
     *
     * @param text      字符串
     * @param maxLength 最大长度
     * @return 截断后的字符串
     */
    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    // endregion
}
