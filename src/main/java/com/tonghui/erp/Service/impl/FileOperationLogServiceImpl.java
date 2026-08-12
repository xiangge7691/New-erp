package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.utils.EntityUtils;
import com.tonghui.erp.Data.Entity.FileOperationLog;
import com.tonghui.erp.Data.mapper.FileOperationLogMapper;
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
 */
@Service
public class FileOperationLogServiceImpl implements FileOperationLogService {

    @Autowired
    private FileOperationLogMapper fileOperationLogMapper;

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

    /**
     * 从请求上下文中获取当前用户名
     */
    private String getCurrentUserName() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                Object username = request.getAttribute("username");
                if (username != null) {
                    return username.toString();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    public Page<FileOperationLog> queryLogs(String operationType, Long userId, String userName, LocalDateTime startTime, LocalDateTime endTime, int pageIndex, int pageSize) {
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
}
