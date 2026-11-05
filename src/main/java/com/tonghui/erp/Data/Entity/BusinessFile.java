package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业务附件关系表
 * <p>
 * 只表达“业务记录 -> 文件”的绑定关系；物理文件元数据仍在 file_info。
 * 关系行解绑时物理删除，is_deleted 固定为 0，供 MyBatis-Plus 查询过滤。
 * </p>
 *
 * @TableName business_file
 */
@TableName(value = "business_file")
@Data
@EqualsAndHashCode(callSuper = true)
public class BusinessFile extends AuditEntity {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 关联ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 业务类型编码（一表一码，由 BizFileType 注册表校验）
     */
    @TableField(value = "biz_type")
    private String bizType;

    /**
     * 业务表主键值
     */
    @TableField(value = "biz_id")
    private Long bizId;

    /**
     * 子对象ID（如审批节点ID），无子对象时为0
     */
    @TableField(value = "ref_id")
    private Long refId;

    /**
     * 文件ID（file_info.file_id）
     */
    @TableField(value = "file_id")
    private Long fileId;

    /**
     * 文件槽位/用途（如 WAYBILL、PROCESS_ATTACHMENT）
     */
    @TableField(value = "file_kind")
    private String fileKind;

    /**
     * 排序号
     */
    @TableField(value = "sort_order")
    private Integer sortOrder;

    // endregion

    // region 状态字段
    // ===================================
    // 状态字段
    // ===================================

    /**
     * 是否删除（关系行实际物理删除，该字段固定为0）
     */
    @TableField(value = "is_deleted")
    private Integer isDeleted;

    // endregion
}
