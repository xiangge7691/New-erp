package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Data.Entity.FileOperationLog;

/**
 * 文件操作日志服务接口
 */
public interface FileOperationLogService {

    /**
     * 记录文件操作日志（自动获取当前用户）
     *
     * @param fileId        文件ID（可为null，如文件夹操作）
     * @param fileName      文件/文件夹名称
     * @param filePath      文件路径
     * @param operationType 操作类型
     * @param rootType      根目录类型
     * @param detail        操作详情（可为null）
     */
    void log(Long fileId, String fileName, String filePath, String operationType, String rootType, String detail);

    /**
     * 分页查询操作日志
     *
     * @param operationType 操作类型（可选）
     * @param userId        操作人ID（可选）
     * @param pageIndex     页码
     * @param pageSize      每页大小
     * @return 分页结果
     */
    Page<FileOperationLog> queryLogs(String operationType, Long userId, int pageIndex, int pageSize);
}
