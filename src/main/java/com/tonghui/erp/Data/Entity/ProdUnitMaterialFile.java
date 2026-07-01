package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 生产单位材料文件表（Base64存储）
 * @TableName prod_unit_material_file
 */
@TableName(value ="prod_unit_material_file")
@Data
public class ProdUnitMaterialFile {
    /**
     * 材料文件唯一标识
     */
    @TableId(value = "prod_material_id", type = IdType.AUTO)
    private Long prodMaterialId;

    /**
     * 关联的生产单位ID
     */
    @TableField(value = "prod_unit_id")
    private Long prodUnitId;

    /**
     * 材料文件类型
     */
    @TableField(value = "material_type")
    private Object materialType;

    /**
     * 文件名称
     */
    @TableField(value = "file_name")
    private String fileName;

    /**
     * 文件内容MD5哈希值
     */
    @TableField(value = "file_md5")
    private String fileMd5;

    /**
     * 文件大小（字节）
     */
    @TableField(value = "file_size")
    private Integer fileSize;

    /**
     * 文件存储路径
     */
    @TableField(value = "file_path")
    private String filePath;

    /**
     * 文件描述
     */
    @TableField(value = "description")
    private String description;

    /**
     * 文件内容（Base64编码）
     */
    @TableField(value = "file_content")
    private String fileContent;

    /**
     * 创建人ID
     */
    @TableField(value = "created_by", fill = FieldFill.INSERT)
    private Long createdBy;

    /**
     * 创建时间
     */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

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