package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 班组成员
 *
 * @TableName production_team_member
 */
@TableName(value = "production_team_member")
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductionTeamMember extends AuditEntity {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // endregion

    // region 业务字段
    // ===================================
    // 业务字段
    // ===================================

    /**
     * 班组ID
     */
    @TableField(value = "team_id")
    private Long teamId;

    /**
     * 人员档案ID
     */
    @TableField(value = "personnel_id")
    private Long personnelId;

    /**
     * 是否主操作：0否/1是
     */
    @TableField(value = "is_primary")
    private Integer isPrimary;

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;

    // endregion

    // region 状态与审计字段
    // ===================================
    // 状态与审计字段
    // ===================================

    /**
     * 是否已删除：0否/1是
     */
    @TableField(value = "is_deleted")
    private Integer isDeleted;

    /**
     * 乐观锁版本号
     */
    @TableField(value = "version")
    private Integer version;

    // endregion

    // region 关联显示字段（不映射数据库）
    // ===================================
    // 关联显示字段
    // ===================================

    /**
     * 人员姓名
     */
    @TableField(exist = false)
    private String personnelName;

    /**
     * 工号
     */
    @TableField(exist = false)
    private String employeeNo;

    // endregion
}
