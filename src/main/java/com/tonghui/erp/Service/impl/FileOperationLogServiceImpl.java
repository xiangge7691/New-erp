package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.utils.EntityUtils;
import com.tonghui.erp.Data.Entity.FileOperationLog;
import com.tonghui.erp.Data.Entity.User;
import com.tonghui.erp.Data.mapper.FileOperationLogMapper;
import com.tonghui.erp.Data.mapper.UserMapper;
import com.tonghui.erp.Service.FileOperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 文件操作日志服务实现类
 * <p>
 * 记录文件的上传/下载/删除等操作日志，并支持按操作类型、用户、业务类型等条件分页查询
 * </p>
 */
@Service
public class FileOperationLogServiceImpl implements FileOperationLogService {

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    /**
     * 文件操作日志Mapper
     */
    @Autowired
    private FileOperationLogMapper fileOperationLogMapper;

    /**
     * 用户信息Mapper
     */
    @Autowired
    private UserMapper userMapper;

    // endregion

    // region 日志记录方法
    // ===================================
    // 日志记录方法
    // ===================================

    /**
     * 记录一条文件操作日志
     * <p>
     * 自动从当前请求上下文获取操作用户ID与用户名
     * </p>
     *
     * @param fileId        文件ID
     * @param fileName      文件原始名称
     * @param filePath      文件存储路径
     * @param operationType 操作类型（UPLOAD/DOWNLOAD/DELETE）
     * @param rootType      根目录类型（business=系统文件/custom=用户文件）
     * @param detail        操作明细描述（可为null）
     */
    @Override
    public void log(Long fileId, String fileName, String filePath, String operationType, String rootType, String detail) {
        FileOperationLog log = new FileOperationLog();
        log.setFileId(fileId);
        log.setFileName(fileName);
        log.setFilePath(filePath);
        log.setOperationType(operationType);
        log.setRootType(rootType);
        log.setUserId(EntityUtils.getCurrentUserId());
        log.setUserName(getCurrentUserName());
        log.setDetail(detail);
        log.setCreatedTime(LocalDateTime.now());
        fileOperationLogMapper.insert(log);
    }

    // endregion

    // region 用户名解析辅助方法
    // ===================================
    // 用户名解析辅助方法
    // ===================================

    /**
     * 从请求上下文中获取当前用户名
     * <p>
     * 优先从 request attribute 获取，如果获取不到则从数据库查询用户信息
     * </p>
     *
     * @return 用户名（优先真实姓名，回退登录账号），获取不到时返回null
     */
    private String getCurrentUserName() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                // 1. 先尝试从 request attribute 获取
                Object username = request.getAttribute("username");
                if (username != null) {
                    return username.toString();
                }
                // 2. 从数据库查询用户信息
                Long userId = EntityUtils.getCurrentUserId();
                if (userId != null && userId > 0) {
                    User user = userMapper.selectById(userId);
                    if (user != null) {
                        // 优先返回真实姓名，如果没有则返回登录账号
                        return StringUtils.hasText(user.getUserName()) ? user.getUserName() : user.getUserAccount();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    // endregion

    // region 日志查询方法
    // ===================================
    // 日志查询方法
    // ===================================

    /**
     * 分页查询文件操作日志
     *
     * @param operationType 操作类型（UPLOAD/DOWNLOAD/DELETE，可为null）
     * @param userId        操作用户ID（可为null）
     * @param userName      操作人姓名（模糊匹配，可为null）
     * @param businessType  业务类型（可为null）
     * @param rootType      根目录类型（business/custom，可为null）
     * @param startTime     操作开始时间（可为null）
     * @param endTime       操作结束时间（可为null）
     * @param pageIndex     页码，从0开始
     * @param pageSize      每页数量
     * @return 分页后的日志记录
     */
    @Override
    public Page<FileOperationLog> queryLogs(String operationType, Long userId, String userName, String businessType, String rootType, LocalDateTime startTime, LocalDateTime endTime, int pageIndex, int pageSize) {
        Page<FileOperationLog> page = new Page<>(pageIndex + 1, pageSize);
        QueryWrapper<FileOperationLog> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(operationType)) {
            wrapper.eq("operation_type", operationType);
        }
        if (userId != null) {
            wrapper.eq("user_id", userId);
        }
        // 操作人姓名模糊匹配（user_name 存真实姓名，兼容登录账号）
        if (StringUtils.hasText(userName)) {
            wrapper.like("user_name", userName);
        }
        // 业务类型筛选
        if (StringUtils.hasText(businessType)) {
            wrapper.eq("business_type", businessType);
        }
        // 根目录类型筛选（business=系统文件/custom=用户文件）
        if (StringUtils.hasText(rootType)) {
            wrapper.eq("root_type", rootType);
        }
        // 操作时间范围筛选（含边界）
        if (startTime != null) {
            wrapper.ge("created_time", startTime);
        }
        if (endTime != null) {
            wrapper.le("created_time", endTime);
        }
        wrapper.orderByDesc("created_time");
        return fileOperationLogMapper.selectPage(page, wrapper);
    }

    // endregion
}
