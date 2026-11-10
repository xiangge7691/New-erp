package com.tonghui.erp.Common.aspect;

import com.tonghui.erp.Common.Config.LogProperties;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.Log.DataChangeRecord;
import com.tonghui.erp.Common.Dto.Log.DataChangeTraceContext;
import com.tonghui.erp.Common.Dto.Log.OperationLogContext;
import com.tonghui.erp.Common.annotation.OperationLog;
import com.tonghui.erp.Common.utils.IpLocationService;
import com.tonghui.erp.Common.utils.IpUtils;
import com.tonghui.erp.Common.utils.LogParamUtils;
import com.tonghui.erp.Service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 操作日志切面
 * <p>
 * 拦截标注了 {@link OperationLog} 的 Controller 方法，在请求线程中采集
 * 操作人、模块、参数摘要、IP 归属地与耗时，再交给异步服务落库。
 * 切面只记录，不改变业务返回，也不吞业务异常。
 * </p>
 */
@Aspect
@Component
public class OperationLogAspect {

    // region 常量与依赖
    // ===================================
    // 常量与依赖
    // ===================================

    private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);

    /** 请求地址最大长度 */
    private static final int MAX_REQUEST_URL_LENGTH = 500;

    /** 方法全名最大长度 */
    private static final int MAX_CLASS_METHOD_LENGTH = 255;

    /** User-Agent 最大长度 */
    private static final int MAX_USER_AGENT_LENGTH = 500;

    /** 异常信息最大长度 */
    private static final int MAX_ERROR_MSG_LENGTH = 1000;

    /** 业务对象ID最大长度 */
    private static final int MAX_BUSINESS_ID_LENGTH = 64;

    /** SpEL 表达式解析器 */
    private static final ExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();

    /** 参数名发现器 */
    private static final DefaultParameterNameDiscoverer PARAMETER_NAME_DISCOVERER =
            new DefaultParameterNameDiscoverer();

    private final OperationLogService operationLogService;
    private final LogParamUtils logParamUtils;
    private final LogProperties logProperties;
    private final IpLocationService ipLocationService;

    /**
     * 构造操作日志切面
     *
     * @param operationLogService 操作日志服务
     * @param logParamUtils       参数脱敏工具
     * @param logProperties       日志配置
     * @param ipLocationService   IP归属地服务
     */
    public OperationLogAspect(OperationLogService operationLogService,
                              LogParamUtils logParamUtils,
                              LogProperties logProperties,
                              IpLocationService ipLocationService) {
        this.operationLogService = operationLogService;
        this.logParamUtils = logParamUtils;
        this.logProperties = logProperties;
        this.ipLocationService = ipLocationService;
    }

    // endregion

    // region 环绕通知
    // ===================================
    // 环绕通知
    // ===================================

    /**
     * 环绕记录操作日志
     *
     * @param joinPoint    连接点
     * @param operationLog 操作日志注解
     * @return 原方法返回值
     * @throws Throwable 原方法异常
     */
    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long startMillis = System.currentTimeMillis();
        OperationLogContext context = null;
        try {
            context = buildContext(joinPoint, operationLog, startMillis);
        } catch (Exception e) {
            // 上下文构建失败不能影响业务
            log.warn("构建操作日志上下文失败：{}", e.getMessage());
        }

        Object result = null;
        DataChangeTraceContext.Trace changeTrace = null;
        if (context != null && logProperties.getChange().isEnabled()) {
            changeTrace = DataChangeTraceContext.begin();
        }
        try {
            result = joinPoint.proceed();
            if (context != null) {
                fillResult(context, result);
            }
            return result;
        } catch (Throwable ex) {
            if (context != null) {
                context.setResultStatus(0);
                context.setErrorMsg(truncate(ex.getClass().getSimpleName() + ": " + ex.getMessage(),
                        MAX_ERROR_MSG_LENGTH));
            }
            throw ex;
        } finally {
            if (changeTrace != null) {
                DataChangeTraceContext.Trace ended = DataChangeTraceContext.end();
                if (context != null && ended != null) {
                    context.setTraceId(ended.getTraceId());
                    context.setChanges(ended.getChanges());
                    context.setChangeTruncated(ended.isTruncated());
                }
            }
            if (context != null) {
                context.setCostTime((int) Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - startMillis));
                if (!hasText(context.getBusinessId())) {
                    context.setBusinessId(resolveBusinessId(operationLog.businessId(), joinPoint, result));
                }
                // 失败/回滚的操作不记录数据变更，避免日志与实际提交数据不一致
                if (context.getResultStatus() == null || context.getResultStatus() != 1) {
                    context.setChanges(new ArrayList<>());
                }
                markPrimaryChange(context, operationLog);
                try {
                    operationLogService.recordAsync(context);
                } catch (Exception e) {
                    log.warn("提交操作日志失败：{}", e.getMessage());
                }
            }
        }
    }

    // endregion

    // region 上下文构建
    // ===================================
    // 上下文构建
    // ===================================

    /**
     * 构建操作日志上下文
     *
     * @param joinPoint    连接点
     * @param operationLog 操作日志注解
     * @param startMillis  开始时间戳
     * @return 操作日志上下文
     */
    private OperationLogContext buildContext(ProceedingJoinPoint joinPoint,
                                             OperationLog operationLog,
                                             long startMillis) {
        OperationLogContext context = new OperationLogContext();
        context.setModule(operationLog.module());
        context.setOperationType(operationLog.type().name());
        context.setOperationDesc(operationLog.desc());
        context.setBusinessType(trimToNull(operationLog.businessType()));
        context.setBusinessId(resolveBusinessId(operationLog.businessId(), joinPoint, null));
        context.setCreatedTime(LocalDateTime.now());
        context.setResultStatus(1);

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        context.setClassMethod(truncate(signature.getDeclaringTypeName() + "." + signature.getName(),
                MAX_CLASS_METHOD_LENGTH));

        HttpServletRequest request = currentRequest();
        if (request != null) {
            context.setRequestMethod(truncate(request.getMethod(), 10));
            String query = request.getQueryString();
            String url = request.getRequestURI() + (query == null ? "" : "?" + query);
            context.setRequestUrl(truncate(url, MAX_REQUEST_URL_LENGTH));
            String clientIp = IpUtils.getClientIp(request, logProperties.getIp().isTrustProxyHeaders());
            context.setClientIp(clientIp);
            context.setIpLocation(ipLocationService.resolve(clientIp));
            context.setUserAgent(truncate(request.getHeader("User-Agent"), MAX_USER_AGENT_LENGTH));
        }

        // 用户信息必须在请求线程中取好，异步线程拿不到 SecurityContext
        context.setUserId(currentUserId());
        context.setUserName(currentUserName());

        if (operationLog.recordParams()) {
            context.setRequestParams(logParamUtils.toJsonSafely(
                    joinPoint.getArgs(), logProperties.getOperation().getMaxParamLength()));
        }
        return context;
    }

    /**
     * 根据返回值判定操作结果
     *
     * @param context 操作日志上下文
     * @param result  方法返回值
     */
    private void fillResult(OperationLogContext context, Object result) {
        if (result instanceof ApiResponse<?> response) {
            context.setResponseCode(response.getCode());
            if (response.getCode() == ApiResponse.SUCCESS) {
                context.setResultStatus(1);
            } else {
                context.setResultStatus(0);
                context.setErrorMsg(truncate(response.getMessage(), MAX_ERROR_MSG_LENGTH));
            }
        }
    }

    // endregion

    // region 用户与业务ID解析
    // ===================================
    // 用户与业务ID解析
    // ===================================

    /**
     * 获取当前请求中的用户ID
     *
     * @return 用户ID；无法获取返回 null
     */
    private Long currentUserId() {
        HttpServletRequest request = currentRequest();
        if (request != null) {
            Object attribute = request.getAttribute("userId");
            Long userId = parseLong(attribute);
            if (userId != null) {
                return userId;
            }
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            return parseLong(authentication.getPrincipal());
        }
        return null;
    }

    /**
     * 获取当前请求中的用户姓名
     *
     * @return 用户姓名；无法获取返回 null
     */
    private String currentUserName() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        Object username = request.getAttribute("username");
        return username == null ? null : trimToNull(String.valueOf(username));
    }

    /**
     * 解析业务对象ID
     *
     * @param expression SpEL 表达式
     * @param joinPoint  连接点
     * @param result     方法返回值（可为空）
     * @return 业务对象ID；解析失败返回 null
     */
    private String resolveBusinessId(String expression, ProceedingJoinPoint joinPoint, Object result) {
        if (!hasText(expression)) {
            return null;
        }
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            EvaluationContext evaluationContext = new MethodBasedEvaluationContext(
                    null, method, joinPoint.getArgs(), PARAMETER_NAME_DISCOVERER);
            evaluationContext.setVariable("result", result);
            Expression parsed = EXPRESSION_PARSER.parseExpression(expression);
            Object value = parsed.getValue(evaluationContext);
            return value == null ? null : truncate(String.valueOf(value), MAX_BUSINESS_ID_LENGTH);
        } catch (Exception e) {
            log.debug("解析操作日志业务ID失败：表达式={}，原因={}", expression, e.getMessage());
            return null;
        }
    }

    // endregion

    // region 数据变更主记录标记
    // ===================================
    // 数据变更主记录标记
    // ===================================

    /**
     * 标记主记录与关联变更
     * <p>
     * 优先按注解的 businessId + businessType 匹配；匹配不到时按 businessId，
     * 仍匹配不到则把第一条变更作为主记录
     * </p>
     *
     * @param context      操作日志上下文
     * @param operationLog 操作日志注解
     */
    private void markPrimaryChange(OperationLogContext context, OperationLog operationLog) {
        List<DataChangeRecord> changes = context.getChanges();
        if (changes == null || changes.isEmpty()) {
            return;
        }
        String businessType = normalizeType(operationLog.businessType());
        String businessId = context.getBusinessId();

        DataChangeRecord primary = null;
        if (hasText(businessId)) {
            for (DataChangeRecord change : changes) {
                if (businessId.equals(change.getBusinessId()) && matchesType(change, businessType)) {
                    primary = change;
                    break;
                }
            }
            if (primary == null) {
                for (DataChangeRecord change : changes) {
                    if (businessId.equals(change.getBusinessId())) {
                        primary = change;
                        break;
                    }
                }
            }
        }
        if (primary == null && hasText(businessType)) {
            for (DataChangeRecord change : changes) {
                if (matchesType(change, businessType)) {
                    primary = change;
                    break;
                }
            }
        }
        if (primary == null) {
            primary = changes.get(0);
        }
        for (DataChangeRecord change : changes) {
            change.setChangeSource(change == primary ? "PRIMARY" : "RELATED");
        }
    }

    /**
     * 判断变更记录是否匹配业务类型
     *
     * @param change       变更记录
     * @param businessType 归一化后的业务类型
     * @return 是否匹配
     */
    private boolean matchesType(DataChangeRecord change, String businessType) {
        if (!hasText(businessType) || change.getEntityClass() == null) {
            return false;
        }
        String entityClass = change.getEntityClass();
        String simpleName = entityClass.substring(entityClass.lastIndexOf('.') + 1);
        String entityType = normalizeType(simpleName);
        return entityType.startsWith(businessType) || businessType.startsWith(entityType);
    }

    /**
     * 归一化业务类型/实体名，便于跨命名风格比较
     *
     * @param value 原始值
     * @return 仅保留字母数字并转小写
     */
    private String normalizeType(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
    }

    // endregion

    // region 辅助方法
    // ===================================
    // 辅助方法
    // ===================================

    /**
     * 获取当前 HTTP 请求
     *
     * @return 请求对象；非 Web 线程返回 null
     */
    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    /**
     * 将对象解析为 Long
     *
     * @param value 对象
     * @return Long 值；无法解析返回 null
     */
    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
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
     * 去除首尾空白，空串转 null
     *
     * @param text 字符串
     * @return 处理后的字符串
     */
    private String trimToNull(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
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
