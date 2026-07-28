package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 物料信息表（存储单位与分类名称，不用外键）
 * @TableName material
 */
@TableName(value ="material")
@Data
@EqualsAndHashCode(callSuper = true)
public class Material extends AuditEntity {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 物料唯一标识符
     */
    @TableId(value = "material_id", type = IdType.AUTO)
    private Long materialId;

    /**
     * 物料编码（唯一性约束）
     */
    @TableField(value = "material_code")
    private String materialCode;

    /**
     * 物料名称
     */
    @TableField(value = "material_name")
    private String materialName;

    // endregion

    // region 业务字段
    // ===================================
    // 业务字段
    // ===================================

    /**
     * 分类（如原料/辅料/包材等）
     */
    @TableField(value = "category_name")
    private String categoryName;

    /**
     * 物料属性（根据分类选择）
     * 原料：中药材、中药饮片、原料药
     * 辅料：防腐剂、崩解剂、矫味剂、粘合剂、赋形剂
     * 包材：内包材、外包材
     */
    @TableField(value = "material_attribute")
    private String materialAttribute;

    /**
     * 计量单位（直接存文本，如kg/张/瓶）
     */
    @TableField(value = "unit_name")
    private String unitName;

    /**
     * 规格描述
     */
    @TableField(value = "spec")
    private String spec;

    /**
     * 存储要求
     */
    @TableField(value = "storage_requirement")
    private String storageRequirement;

    /**
     * 备注信息
     */
    @TableField(value = "remark")
    private String remark;

    // endregion

    // region 状态与审计字段
    // ===================================
    // 状态与审计字段
    // ===================================

    /**
     * 状态：1启用/0禁用
     */
    @TableField(value = "material_status")
    private Integer materialStatus;

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

    // endregion

    // region 查询辅助字段（不映射数据库）
    // ===================================
    // 查询辅助字段（不映射数据库）
    // ===================================

    /**
     * 创建时间开始（用于范围查询，不映射数据库）
     */
    @TableField(exist = false)
    private LocalDateTime createdTimeStart;

    /**
     * 创建时间结束（用于范围查询，不映射数据库）
     */
    @TableField(exist = false)
    private LocalDateTime createdTimeEnd;

    /**
     * 更新时间开始（用于范围查询，不映射数据库）
     */
    @TableField(exist = false)
    private LocalDateTime updatedTimeStart;

    /**
     * 更新时间结束（用于范围查询，不映射数据库）
     */
    @TableField(exist = false)
    private LocalDateTime updatedTimeEnd;

    // endregion
}
