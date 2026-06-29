package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 制剂文档表
 * @TableName preparation_document
 */
@TableName(value = "preparation_document")
@Data
public class PreparationDocument {
    /**
     * 文档唯一标识
     */
    @TableId(value = "doc_id", type = IdType.AUTO)
    private Long docId;

    /**
     * 制剂ID
     */
    @TableField(value = "preparation_id")
    private Long preparationId;

    /**
     * 文档类型：工艺规程/验证方案/验证报告模板/批记录模板
     */
    @TableField(value = "doc_type")
    private String docType;

    /**
     * 文档名称
     */
    @TableField(value = "doc_name")
    private String docName;

    /**
     * 关联文件ID
     */
    @TableField(value = "file_id")
    private Long fileId;

    /**
     * 版本号
     */
    @TableField(value = "version_no")
    private String versionNo;

    /**
     * 生效日期
     */
    @TableField(value = "effective_date")
    private LocalDate effectiveDate;

    /**
     * 失效日期
     */
    @TableField(value = "expire_date")
    private LocalDate expireDate;

    /**
     * 状态：0作废/1有效
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 创建人ID
     */
    @TableField(value = "created_by")
    private Long createdBy;

    /**
     * 更新人ID
     */
    @TableField(value = "updated_by")
    private Long updatedBy;

    /**
     * 创建时间
     */
    @TableField(value = "created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at")
    private LocalDateTime updatedAt;

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
