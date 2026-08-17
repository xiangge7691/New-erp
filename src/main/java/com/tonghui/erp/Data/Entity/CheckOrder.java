package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 盘点单主表
 * <p>
 * 仓库库存盘点单据主表，记录盘点仓库、物料种数及盘盈/盘亏/盘平条目数，
 * 支持全盘与抽盘，按仓库逐个盘点
 * </p>
 * @TableName check_order
 */
@TableName(value = "check_order")
@Data
@EqualsAndHashCode(callSuper = true)
public class CheckOrder extends AuditEntity {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 盘点单主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 盘点单号（格式：PD-YYYYMMDD-NNN，唯一）
     */
    @TableField(value = "check_no")
    private String checkNo;

    /**
     * 盘点仓库名称
     */
    @TableField(value = "warehouse")
    private String warehouse;

    /**
     * 物料种数（盘点条目数）
     */
    @TableField(value = "material_count")
    private Integer materialCount;

    /**
     * 盘盈条目数
     */
    @TableField(value = "profit_count")
    private Integer profitCount;

    /**
     * 盘亏条目数
     */
    @TableField(value = "loss_count")
    private Integer lossCount;

    /**
     * 盘平条目数
     */
    @TableField(value = "match_count")
    private Integer matchCount;

    // endregion

    // region 操作人与状态字段
    // ===================================
    // 操作人与状态字段
    // ===================================

    /**
     * 操作人ID
     */
    @TableField(value = "operator_id")
    private Long operatorId;

    /**
     * 操作人姓名
     */
    @TableField(value = "operator_name")
    private String operatorName;

    /**
     * 是否已删除（软删除标记）
     */
    @TableField(value = "is_deleted")
    private Integer isDeleted;

    /**
     * 乐观锁版本号
     */
    @TableField(value = "version")
    private Integer version;

    // endregion
}