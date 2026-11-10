package com.tonghui.erp.Common.Scheduled;

import com.tonghui.erp.Service.LoginLogService;
import com.tonghui.erp.Service.OperationLogService;
import com.tonghui.erp.Service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 日志清理定时任务
 * <p>
 * 每天凌晨按保留天数物理删除过期的登录日志与操作日志，
 * 分批删除避免长事务和锁表
 * </p>
 */
@Component
public class LogCleanupTask {

    // region 常量与依赖
    // ===================================
    // 常量与依赖
    // ===================================

    private static final Logger log = LoggerFactory.getLogger(LogCleanupTask.class);

    /** 登录日志保留天数配置键 */
    private static final String KEY_LOGIN_RETENTION = "log.login.retention.days";

    /** 操作日志保留天数配置键 */
    private static final String KEY_OPERATION_RETENTION = "log.operation.retention.days";

    /** 登录日志默认保留天数 */
    private static final int DEFAULT_LOGIN_RETENTION_DAYS = 180;

    /** 操作日志默认保留天数 */
    private static final int DEFAULT_OPERATION_RETENTION_DAYS = 365;

    /** 单批删除条数 */
    private static final int BATCH_SIZE = 1000;

    @Autowired
    private LoginLogService loginLogService;

    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private SystemConfigService systemConfigService;

    // endregion

    // region 定时清理
    // ===================================
    // 定时清理
    // ===================================

    /**
     * 清理过期日志（每天 03:10 执行）
     */
    @Scheduled(cron = "0 10 3 * * ?")
    public void cleanupExpiredLogs() {
        cleanupLoginLogs();
        cleanupOperationLogs();
    }

    /**
     * 清理过期登录日志
     */
    private void cleanupLoginLogs() {
        try {
            int days = systemConfigService.getInt(KEY_LOGIN_RETENTION, DEFAULT_LOGIN_RETENTION_DAYS);
            int deleted = loginLogService.cleanup(days, BATCH_SIZE);
            if (deleted > 0) {
                log.info("登录日志清理完成：保留{}天，删除{}条", days, deleted);
            }
        } catch (Exception e) {
            log.error("登录日志清理失败：{}", e.getMessage(), e);
        }
    }

    /**
     * 清理过期操作日志
     */
    private void cleanupOperationLogs() {
        try {
            int days = systemConfigService.getInt(KEY_OPERATION_RETENTION, DEFAULT_OPERATION_RETENTION_DAYS);
            int deleted = operationLogService.cleanup(days, BATCH_SIZE);
            if (deleted > 0) {
                log.info("操作日志清理完成：保留{}天，删除{}条", days, deleted);
            }
        } catch (Exception e) {
            log.error("操作日志清理失败：{}", e.getMessage(), e);
        }
    }

    // endregion
}
