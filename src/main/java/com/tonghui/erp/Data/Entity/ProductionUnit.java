package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 生产单位信息表
 * @TableName production_unit
 */
@TableName(value ="production_unit")
@Data
public class ProductionUnit {
    /**
     * 生产单位唯一标识
     */
    @TableId(value = "prod_unit_id", type = IdType.AUTO)
    private Long prodUnitId;

    /**
     * 生产单位编号（唯一性约束）
     */
    @TableField(value = "prod_unit_code")
    private String prodUnitCode;

    /**
     * 生产单位名称
     */
    @TableField(value = "prod_unit_name")
    private String prodUnitName;

    /**
     * 生产单位地址
     */
    @TableField(value = "prod_unit_address")
    private String prodUnitAddress;

    /**
     * 负责人姓名
     */
    @TableField(value = "prod_unit_manager")
    private String prodUnitManager;

    /**
     * 联系电话
     */
    @TableField(value = "prod_unit_phone")
    private String prodUnitPhone;

    /**
     * 状态：0停用/1启用
     */
    @TableField(value = "prod_unit_status")
    private Integer prodUnitStatus;

    /**
     * 备注信息
     */
    @TableField(value = "prod_unit_remark")
    private String prodUnitRemark;

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
     * 更新人ID
     */
    @TableField(value = "updated_by")
    private Long updatedBy;

    /**
     * 更新时间
     */
    @TableField(value = "update_time")
    private LocalDateTime updateTime;

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