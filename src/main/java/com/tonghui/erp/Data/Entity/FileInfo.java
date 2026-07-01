package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 文件信息表
 * @TableName file_info
 */
@TableName(value ="file_info")
@Data
public class FileInfo {
    /**
     * 文件唯一标识
     */
    @TableId(value = "file_id", type = IdType.AUTO)
    private Long fileId;

    /**
     * 原始文件名
     */
    @TableField(value = "original_name")
    private String originalName;

    /**
     * 存储文件名
     */
    @TableField(value = "stored_name")
    private String storedName;

    /**
     * 文件路径
     */
    @TableField(value = "file_path")
    private String filePath;

    /**
     * 文件大小（字节）
     */
    @TableField(value = "file_size")
    private Long fileSize;

    /**
     * 文件类型/内容类型
     */
    @TableField(value = "content_type")
    private String contentType;

    /**
     * 文件扩展名
     */
    @TableField(value = "file_extension")
    private String fileExtension;

    /**
     * 文件MD5哈希值
     */
    @TableField(value = "file_md5")
    private String fileMd5;

    /**
     * 文件分类
     */
    @TableField(value = "category")
    private String category;

    /**
     * 文件描述
     */
    @TableField(value = "description")
    private String description;

    /**
     * 访问URL
     */
    @TableField(value = "file_url")
    private String fileUrl;

    /**
     * 存储类型：LOCAL-本地存储，CLOUD-云存储
     */
    @TableField(value = "storage_type")
    private String storageType;

    /**
     * 关联业务ID
     */
    @TableField(value = "business_id")
    private Long businessId;

    /**
     * 关联业务类型
     */
    @TableField(value = "business_type")
    private String businessType;

    /**
     * 创建人ID
     */
    @TableField(value = "created_by")
    private Long createdBy;

    /**
     * 创建时间
     */
    @TableField(value = "created_time")
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    @TableField(value = "updated_time")
    private LocalDateTime updatedTime;

    /**
     * 是否已删除
     */
    @TableField(value = "is_deleted")
    private Integer isDeleted;

    /**
     * 乐观锁版本号
     */
    @TableField(value = "version")
    private Integer version;
}