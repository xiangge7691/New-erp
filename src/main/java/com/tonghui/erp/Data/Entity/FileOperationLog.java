package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件操作日志表
 * @TableName file_operation_log
 */
@TableName(value = "file_operation_log")
@Data
public class FileOperationLog {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 日志唯一标识
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联file_info表的文件ID
     */
    @TableField(value = "file_id")
    private Long fileId;

    /**
     * 文件/文件夹名称
     */
    @TableField(value = "file_name")
    private String fileName;

    /**
     * 文件路径
     */
    @TableField(value = "file_path")
    private String filePath;

    // endregion

    // region 操作信息字段
    // ===================================
    // 操作信息字段
    // ===================================

    /**
     * 操作类型：UPLOAD/DOWNLOAD/PREVIEW/CREATE_FOLDER/DELETE/RESTORE/RENAME/MOVE/COPY
     */
    @TableField(value = "operation_type")
    private String operationType;

    /**
     * 根目录类型：business/custom
     */
    @TableField(value = "root_type")
    private String rootType;

    /**
     * 操作人ID
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 操作人姓名
     */
    @TableField(value = "user_name")
    private String userName;

    /**
     * 操作详情（如重命名前后的名称、移动的目标路径等）
     */
    @TableField(value = "detail")
    private String detail;

    /**
     * 操作时间
     */
    @TableField(value = "created_time")
    private LocalDateTime createdTime;

    // endregion
}
