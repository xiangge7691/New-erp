package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 制剂文档表
 * @TableName preparation_document
 */
@TableName(value = "preparation_document")
@Data
@EqualsAndHashCode(callSuper = true)
public class PreparationDocument extends AuditEntity {
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
     * 是否已删除
     */
    @TableField(value = "is_deleted")
    private Integer isDeleted;

    /**
     * 乐观锁版本号
     */
    @TableField(value = "version")
    private Integer version;

    // ========== 关联表显示字段（非数据库字段）==========

    /**
     * 制剂名称（关联preparation表）
     */
    @TableField(exist = false)
    private String preparationName;

    /**
     * 文件名称（关联file_info表）
     */
    @TableField(exist = false)
    private String fileName;
}