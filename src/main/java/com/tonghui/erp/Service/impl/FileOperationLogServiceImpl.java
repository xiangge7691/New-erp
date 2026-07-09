package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.utils.EntityUtils;
import com.tonghui.erp.Data.Entity.FileOperationLog;
import com.tonghui.erp.Data.mapper.FileOperationLogMapper;
import com.tonghui.erp.Service.FileOperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
        log.setDetail(detail);
        log.setCreatedTime(LocalDateTime.now());
        fileOperationLogMapper.insert(log);
    }

    @Override
    public Page<FileOperationLog> queryLogs(String operationType, Long userId, int pageIndex, int pageSize) {
        Page<FileOperationLog> page = new Page<>(pageIndex + 1, pageSize);
        QueryWrapper<FileOperationLog> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(operationType)) {
            wrapper.eq("operation_type", operationType);
        }
        if (userId != null) {
            wrapper.eq("user_id", userId);
        }
        wrapper.orderByDesc("created_time");
        return fileOperationLogMapper.selectPage(page, wrapper);
    }
}
