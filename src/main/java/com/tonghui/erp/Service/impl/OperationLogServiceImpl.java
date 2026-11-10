package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Config.LogProperties;
import com.tonghui.erp.Common.Dto.Log.OperationLogContext;
import com.tonghui.erp.Common.Dto.Log.OperationLogDetailDto;
import com.tonghui.erp.Common.Dto.Log.OperationLogDto;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.SysOperationLog;
import com.tonghui.erp.Data.Entity.User;
import com.tonghui.erp.Data.mapper.SysOperationLogMapper;
import com.tonghui.erp.Data.mapper.UserMapper;
import com.tonghui.erp.Service.OperationLogService;
import com.tonghui.erp.Service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 操作日志服务实现类
 * <p>
 * 负责操作日志的异步写入、分页查询、详情查询与过期清理
 * </p>
 */
@Service
public class OperationLogServiceImpl implements OperationLogService {

    // region 常量与依赖
    // ===================================
    // 常量与依赖
    // ===================================

    private static final Logger log = LoggerFactory.getLogger(OperationLogServiceImpl.class);

    /** 操作日志开关配置键 */
    private static final String KEY_OPERATION_ENABLED = "log.operation.enabled";

    /** UTF-8 字符串截断上限 */
    private static final int MAX_ACCOUNT_LENGTH = 64;
    private static final int MAX_USER_NAME_LENGTH = 64;
    private static final int MAX_MODULE_LENGTH = 64;
    private static final int MAX_TYPE_LENGTH = 32;
    private static final int MAX_DESC_LENGTH = 255;
    private static final int MAX_BUSINESS_TYPE_LENGTH = 64;
    private static final int MAX_BUSINESS_ID_LENGTH = 64;
    private static final int MAX_REQUEST_METHOD_LENGTH = 10;
    private static final int MAX_REQUEST_URL_LENGTH = 500;
    private static final int MAX_CLASS_METHOD_LENGTH = 255;
    private static final int MAX_ERROR_MSG_LENGTH = 1000;
    private static final int MAX_IP_LENGTH = 60;
    private static final int MAX_LOCATION_LENGTH = 120;
    private static final int MAX_USER_AGENT_LENGTH = 500;

    /** 单次清理最大循环次数，避免异常情况下无限删除 */
    private static final int MAX_CLEANUP_ROUNDS = 1000;

    private final SysOperationLogMapper sysOperationLogMapper;
    private final UserMapper userMapper;
    private final LogProperties logProperties;
    private final SystemConfigService systemConfigService;

    /**
     * 构造操作日志服务
     *
     * @param sysOperationLogMapper 操作日志Mapper
     * @param userMapper            用户Mapper
     * @param logProperties         日志配置
     * @param systemConfigService   系统配置服务
     */
    public OperationLogServiceImpl(SysOperationLogMapper sysOperationLogMapper,
                                   UserMapper userMapper,
                                   LogProperties logProperties,
                                   SystemConfigService systemConfigService) {
        this.sysOperationLogMapper = sysOperationLogMapper;
        this.userMapper = userMapper;
        this.logProperties = logProperties;
        this.systemConfigService = systemConfigService;
    }

    // endregion

    // region 日志记录
    // ===================================
    // 日志记录
    // ===================================

    /**
     * 异步写入操作日志
     *
     * @param context 操作日志上下文
     */
    @Async("logExecutor")
    @Override
    public void recordAsync(OperationLogContext context) {
        if (context == null) {
            return;
        }
        try {
            if (!isEnabled()) {
                return;
            }
            // 补全操作人账号与姓名（用户信息在异步线程中按ID查询）
            fillUserInfo(context);

            SysOperationLog entity = new SysOperationLog();
            entity.setUserId(context.getUserId());
            entity.setUserAccount(truncate(context.getUserAccount(), MAX_ACCOUNT_LENGTH));
            entity.setUserName(truncate(context.getUserName(), MAX_USER_NAME_LENGTH));
            entity.setModule(truncate(context.getModule(), MAX_MODULE_LENGTH));
            entity.setOperationType(truncate(context.getOperationType(), MAX_TYPE_LENGTH));
            entity.setOperationDesc(truncate(context.getOperationDesc(), MAX_DESC_LENGTH));
            entity.setBusinessType(truncate(context.getBusinessType(), MAX_BUSINESS_TYPE_LENGTH));
            entity.setBusinessId(truncate(context.getBusinessId(), MAX_BUSINESS_ID_LENGTH));
            entity.setRequestMethod(truncate(context.getRequestMethod(), MAX_REQUEST_METHOD_LENGTH));
            entity.setRequestUrl(truncate(context.getRequestUrl(), MAX_REQUEST_URL_LENGTH));
            entity.setClassMethod(truncate(context.getClassMethod(), MAX_CLASS_METHOD_LENGTH));
            entity.setRequestParams(context.getRequestParams());
            entity.setResponseCode(context.getResponseCode());
            entity.setResultStatus(context.getResultStatus() == null ? 1 : context.getResultStatus());
            entity.setErrorMsg(truncate(context.getErrorMsg(), MAX_ERROR_MSG_LENGTH));
            entity.setCostTime(context.getCostTime());
            entity.setClientIp(truncate(context.getClientIp(), MAX_IP_LENGTH));
            entity.setIpLocation(truncate(context.getIpLocation(), MAX_LOCATION_LENGTH));
            entity.setUserAgent(truncate(context.getUserAgent(), MAX_USER_AGENT_LENGTH));
            entity.setCreatedTime(context.getCreatedTime() == null ? LocalDateTime.now() : context.getCreatedTime());
            sysOperationLogMapper.insert(entity);
        } catch (Exception e) {
            // 日志失败绝不影响业务，仅记录应用日志
            log.error("写入操作日志失败：模块={}，类型={}，原因={}",
                    context.getModule(), context.getOperationType(), e.getMessage(), e);
        }
    }

    /**
     * 补全操作人账号与姓名
     *
     * @param context 操作日志上下文
     */
    private void fillUserInfo(OperationLogContext context) {
        if (context.getUserId() == null) {
            return;
        }
        if (hasText(context.getUserAccount()) && hasText(context.getUserName())) {
            return;
        }
        User user = userMapper.selectById(context.getUserId());
        if (user == null) {
            return;
        }
        if (!hasText(context.getUserAccount())) {
            context.setUserAccount(user.getUserAccount());
        }
        if (!hasText(context.getUserName())) {
            context.setUserName(user.getUserName());
        }
    }

    /**
     * 判断操作日志是否启用
     * <p>数据库配置优先于 application.yml</p>
     *
     * @return 是否启用
     */
    private boolean isEnabled() {
        if (!logProperties.getOperation().isEnabled()) {
            return false;
        }
        String value = systemConfigService.getString(KEY_OPERATION_ENABLED, "true");
        return !"false".equalsIgnoreCase(value == null ? "true" : value.trim());
    }

    // endregion

    // region 日志查询
    // ===================================
    // 日志查询
    // ===================================

    /**
     * 分页查询操作日志
     *
     * @param userAccount   操作人账号
     * @param userId        操作人ID
     * @param userName      操作人姓名
     * @param module        业务模块
     * @param operationType 操作类型
     * @param resultStatus  结果
     * @param businessType  业务对象类型
     * @param businessId    业务对象ID
     * @param keyword       关键字
     * @param startTime     起始时间
     * @param endTime       截止时间
     * @param pageRequest   分页参数
     * @return 分页操作日志
     */
    @Override
    public PagedResult<OperationLogDto> search(String userAccount, Long userId, String userName, String module,
                                               String operationType, Integer resultStatus, String businessType,
                                               String businessId, String keyword, LocalDateTime startTime,
                                               LocalDateTime endTime, PageRequestDto pageRequest) {
        QueryWrapper<SysOperationLog> wrapper = new QueryWrapper<>();
        wrapper.eq(hasText(userAccount), "user_account", userAccount);
        wrapper.eq(userId != null, "user_id", userId);
        wrapper.like(hasText(userName), "user_name", userName);
        wrapper.like(hasText(module), "module", module);
        wrapper.eq(hasText(operationType), "operation_type", operationType);
        wrapper.eq(resultStatus != null, "result_status", resultStatus);
        wrapper.eq(hasText(businessType), "business_type", businessType);
        wrapper.eq(hasText(businessId), "business_id", businessId);
        wrapper.ge(startTime != null, "created_time", startTime);
        wrapper.le(endTime != null, "created_time", endTime);
        if (hasText(keyword)) {
            String value = keyword.trim();
            wrapper.and(w -> w.like("operation_desc", value)
                    .or().like("request_url", value)
                    .or().like("user_account", value)
                    .or().like("user_name", value)
                    .or().like("business_id", value));
        }
        wrapper.orderByDesc("created_time").orderByDesc("id");

        Page<SysOperationLog> page = PagedResult.toMybatisPage(pageRequest);
        page = sysOperationLogMapper.selectPage(page, wrapper);
        PagedResult<SysOperationLog> entityResult = PagedResult.fromPage(page, pageRequest);

        PagedResult<OperationLogDto> result = new PagedResult<>();
        result.setItems(entityResult.getItems().stream().map(this::toDto).toList());
        result.setTotalCount(entityResult.getTotalCount());
        result.setPageIndex(entityResult.getPageIndex());
        result.setPageSize(entityResult.getPageSize());
        return result;
    }

    /**
     * 查询操作日志详情
     *
     * @param id 日志ID
     * @return 日志详情；不存在返回 null
     */
    @Override
    public OperationLogDetailDto detail(Long id) {
        if (id == null) {
            return null;
        }
        SysOperationLog entity = sysOperationLogMapper.selectById(id);
        if (entity == null) {
            return null;
        }
        OperationLogDetailDto dto = new OperationLogDetailDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    // endregion

    // region 日志清理
    // ===================================
    // 日志清理
    // ===================================

    /**
     * 物理清理指定天数之前的操作日志
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
            int deleted = sysOperationLogMapper.deleteBefore(before, batchSize);
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
     * 实体转换为列表项
     *
     * @param entity 操作日志实体
     * @return 列表项
     */
    private OperationLogDto toDto(SysOperationLog entity) {
        OperationLogDto dto = new OperationLogDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
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
