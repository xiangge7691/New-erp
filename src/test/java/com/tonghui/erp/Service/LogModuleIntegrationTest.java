package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Constants.OperationType;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.Log.LoginLogContext;
import com.tonghui.erp.Common.Dto.Log.LoginLogDto;
import com.tonghui.erp.Common.Dto.Log.OperationLogContext;
import com.tonghui.erp.Common.Dto.Log.OperationLogDetailDto;
import com.tonghui.erp.Common.Dto.Log.OperationLogDto;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.annotation.OperationLog;
import com.tonghui.erp.Data.mapper.SysLoginLogMapper;
import com.tonghui.erp.Data.mapper.SysOperationLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 日志模块集成测试
 * <p>
 * 使用测试库验证登录日志、操作日志的写入、查询、详情与 AOP 切面，
 * 测试数据使用独立账号标记并在用例结束后清理
 * </p>
 */
@SpringBootTest
public class LogModuleIntegrationTest {

    // region 依赖与初始化
    // ===================================
    // 依赖与初始化
    // ===================================

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private LoginLogService loginLogService;

    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private LogTestService logTestService;

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Autowired
    private SysLoginLogMapper sysLoginLogMapper;

    @Autowired
    private SysOperationLogMapper sysOperationLogMapper;

    /**
     * 每个用例前确保日志表与默认配置存在（幂等）
     */
    @BeforeEach
    public void initTables() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ClassPathResource("Data/migration/create_login_log_table.sql"),
                new ClassPathResource("Data/migration/create_operation_log_table.sql"),
                new ClassPathResource("Data/migration/init_log_config_data.sql"));
        populator.execute(dataSource);
    }

    // endregion

    // region 登录日志用例
    // ===================================
    // 登录日志用例
    // ===================================

    /**
     * 登录日志写入后可按账号查询，并记录 IPv6 地址与归属地
     */
    @Test
    public void testLoginLogRecordAndQuery() {
        String account = "logtest_login_" + System.nanoTime();
        try {
            LoginLogContext context = new LoginLogContext();
            context.setUserAccount(account);
            context.setUserName("日志测试");
            context.setLoginIp("240e:3b7:3272:d8d0:db09:c067:8d59:539e");
            context.setIpLocation("中国广东省深圳市");
            context.setUserAgent("JUnit-Test");
            context.setLoginTime(LocalDateTime.now());
            context.markSuccess(999999L, account, "日志测试");
            loginLogService.recordAsync(context);

            await(() -> countLoginLog(account) > 0, "登录日志写入超时");

            PageRequestDto pageRequest = new PageRequestDto();
            pageRequest.setPageIndex(0);
            pageRequest.setPageSize(10);
            PagedResult<LoginLogDto> result = loginLogService.search(
                    account, null, null, 1, null, null, null, pageRequest);

            assertEquals(1, result.getTotalCount());
            LoginLogDto dto = result.getItems().get(0);
            assertEquals("240e:3b7:3272:d8d0:db09:c067:8d59:539e", dto.getLoginIp());
            assertEquals("中国广东省深圳市", dto.getIpLocation());
            assertEquals(1, dto.getLoginResult());

            LoginLogDto latest = loginLogService.latestByUserId(999999L);
            assertNotNull(latest);
        } finally {
            jdbcTemplate.update("DELETE FROM sys_login_log WHERE user_account = ?", account);
        }
    }

    // endregion

    // region 操作日志用例
    // ===================================
    // 操作日志用例
    // ===================================

    /**
     * 操作日志写入后可按模块查询详情
     */
    @Test
    public void testOperationLogRecordAndQuery() {
        String account = "logtest_op_" + System.nanoTime();
        String module = "日志测试模块";
        try {
            OperationLogContext context = new OperationLogContext();
            context.setUserId(999999L);
            context.setUserAccount(account);
            context.setUserName("日志测试");
            context.setModule(module);
            context.setOperationType(OperationType.CREATE.name());
            context.setOperationDesc("测试新增");
            context.setBusinessType("LOG_TEST");
            context.setBusinessId("1001");
            context.setRequestMethod("POST");
            context.setRequestUrl("/api/log-test");
            context.setRequestParams("[{\"name\":\"test\"}]");
            context.setResultStatus(1);
            context.setResponseCode(200);
            context.setCostTime(12);
            context.setClientIp("240e:3b7:3272:d8d0:db09:c067:8d59:539e");
            context.setIpLocation("中国广东省深圳市");
            context.setCreatedTime(LocalDateTime.now());
            operationLogService.recordAsync(context);

            await(() -> countOperationLog(account) > 0, "操作日志写入超时");

            PageRequestDto pageRequest = new PageRequestDto();
            pageRequest.setPageIndex(0);
            pageRequest.setPageSize(10);
            PagedResult<OperationLogDto> result = operationLogService.search(
                    account, null, null, module, null, null, null, null, null, null, null, pageRequest);

            assertEquals(1, result.getTotalCount());
            OperationLogDto dto = result.getItems().get(0);
            assertEquals(module, dto.getModule());
            assertEquals("CREATE", dto.getOperationType());
            assertEquals(1, dto.getResultStatus());

            OperationLogDetailDto detail = operationLogService.detail(dto.getId());
            assertNotNull(detail);
            assertEquals(account, detail.getUserAccount());
            assertEquals("[{\"name\":\"test\"}]", detail.getRequestParams());
        } finally {
            jdbcTemplate.update("DELETE FROM sys_operation_log WHERE user_account = ?", account);
        }
    }

    /**
     * 标注 {@link OperationLog} 的方法会经过切面写入操作日志
     */
    @Test
    public void testOperationLogAspect() {
        String businessId = "aspect_" + System.nanoTime();
        try {
            ApiResponse<String> response = logTestService.run(businessId, Map.of("password", "secret-value"));
            assertEquals(ApiResponse.SUCCESS, response.getCode());

            await(() -> countOperationLogByBusinessId(businessId) > 0, "操作日志切面写入超时");

            String params = jdbcTemplate.queryForObject(
                    "SELECT request_params FROM sys_operation_log WHERE business_id = ? LIMIT 1",
                    String.class, businessId);
            assertNotNull(params);
            assertTrue(params.contains("******"));
            assertFalse(params.contains("secret-value"));
        } finally {
            jdbcTemplate.update("DELETE FROM sys_operation_log WHERE business_id = ?", businessId);
        }
    }

    // endregion

    // region 路由与注解校验
    // ===================================
    // 路由与注解校验
    // ===================================

    /**
     * 验证用户新增接口仍能正确注册，且处理方法带有操作日志注解
     *
     * @throws Exception 获取路由失败
     */
    @Test
    public void testUserCreateMappingHasOperationLog() throws Exception {
        assertMappingHasOperationLog("POST", "/api/User");
    }

    /**
     * 验证登录日志查询接口已注册
     *
     * @throws Exception 获取路由失败
     */
    @Test
    public void testLoginLogMappingExists() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/system/login-log/search");
        HandlerExecutionChain chain = requestMappingHandlerMapping.getHandler(request);
        assertNotNull(chain);
        assertTrue(chain.getHandler() instanceof HandlerMethod);
    }

    /**
     * 验证业务模块的写接口已注册且带有操作日志注解
     *
     * @throws Exception 获取路由失败
     */
    @Test
    public void testBusinessMappingsHaveOperationLog() throws Exception {
        assertMappingHasOperationLog("POST", "/api/customer");
        assertMappingHasOperationLog("POST", "/api/stockin/withDetails");
        assertMappingHasOperationLog("POST", "/api/inspection/tasks");
        assertMappingHasOperationLog("POST", "/api/acceptance/withDetails");
        assertMappingHasOperationLog("POST", "/api/product-summary/export");
    }

    /**
     * 断言指定路由的处理方法带有操作日志注解
     *
     * @param method  HTTP方法
     * @param url     请求地址
     * @throws Exception 获取路由失败
     */
    private void assertMappingHasOperationLog(String method, String url) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, url);
        HandlerExecutionChain chain = requestMappingHandlerMapping.getHandler(request);
        assertNotNull(chain, "未找到路由：" + method + " " + url);
        assertTrue(chain.getHandler() instanceof HandlerMethod);
        HandlerMethod handlerMethod = (HandlerMethod) chain.getHandler();
        assertTrue(handlerMethod.getMethod().isAnnotationPresent(OperationLog.class),
                "路由缺少操作日志注解：" + method + " " + url);
    }

    // endregion

    // region 清理SQL校验
    // ===================================
    // 清理SQL校验
    // ===================================

    /**
     * 验证日志表的分批物理删除 SQL（DELETE ... LIMIT）可正常执行
     * <p>
     * 使用 2000 年的历史时间作为数据，截止时间设为 2001 年，避免影响现有测试数据
     */
    @Test
    public void testPhysicalDeleteBefore() {
        String account = "logtest_cleanup_" + System.nanoTime();
        LocalDateTime ancient = LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime cutoff = LocalDateTime.of(2001, 1, 1, 0, 0);
        jdbcTemplate.update("INSERT INTO sys_login_log "
                        + "(user_account, login_type, login_result, login_ip, login_time) VALUES (?,?,?,?,?)",
                account, "LOGIN", 1, "10.0.0.1", ancient);
        jdbcTemplate.update("INSERT INTO sys_operation_log "
                        + "(user_account, module, operation_type, result_status, created_time) VALUES (?,?,?,?,?)",
                account, "日志测试模块", "OTHER", 1, ancient);
        try {
            int loginDeleted = sysLoginLogMapper.deleteBefore(cutoff, 1000);
            int operationDeleted = sysOperationLogMapper.deleteBefore(cutoff, 1000);

            assertTrue(loginDeleted >= 1);
            assertTrue(operationDeleted >= 1);
            assertEquals(0, countLoginLog(account));
            assertEquals(0, countOperationLog(account));
        } finally {
            jdbcTemplate.update("DELETE FROM sys_login_log WHERE user_account = ?", account);
            jdbcTemplate.update("DELETE FROM sys_operation_log WHERE user_account = ?", account);
        }
    }

    // endregion

    // region 测试辅助
    // ===================================
    // 测试辅助
    // ===================================

    /**
     * 轮询等待条件成立
     *
     * @param condition 条件
     * @param message   超时提示
     */
    private void await(Supplier<Boolean> condition, String message) {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            if (Boolean.TRUE.equals(condition.get())) {
                return;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw new AssertionError(message);
    }

    /**
     * 统计指定账号的登录日志数量
     *
     * @param account 账号
     * @return 数量
     */
    private Integer countLoginLog(String account) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_login_log WHERE user_account = ?", Integer.class, account);
    }

    /**
     * 统计指定账号的操作日志数量
     *
     * @param account 账号
     * @return 数量
     */
    private Integer countOperationLog(String account) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_operation_log WHERE user_account = ?", Integer.class, account);
    }

    /**
     * 统计指定业务ID的操作日志数量
     *
     * @param businessId 业务ID
     * @return 数量
     */
    private Integer countOperationLogByBusinessId(String businessId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_operation_log WHERE business_id = ?", Integer.class, businessId);
    }

    // endregion

    // region 测试用切面服务
    // ===================================
    // 测试用切面服务
    // ===================================

    /**
     * 测试配置：注册带 {@link OperationLog} 注解的 Spring Bean
     */
    @TestConfiguration
    static class LogTestConfig {

        /**
         * 注册测试服务
         *
         * @return 测试服务
         */
        @Bean
        public LogTestService logTestService() {
            return new LogTestService();
        }
    }

    /**
     * 用于验证操作日志切面的测试服务
     */
    public static class LogTestService {

        /**
         * 执行一次被切面拦截的操作
         *
         * @param businessId 业务ID
         * @param params     请求参数
         * @return 成功响应
         */
        @OperationLog(module = "日志测试模块", type = OperationType.CREATE, desc = "切面测试",
                businessType = "LOG_TEST", businessId = "#businessId")
        public ApiResponse<String> run(String businessId, Map<String, Object> params) {
            return ApiResponse.successResponse("ok");
        }
    }

    // endregion
}
