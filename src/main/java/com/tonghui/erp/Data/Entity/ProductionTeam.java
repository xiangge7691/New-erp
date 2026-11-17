package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 生产班组
 *
 * @TableName production_team
 */
@TableName(value = "production_team")
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductionTeam extends AuditEntity {

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
     * 班组编码
     */
    @TableField(value = "team_code")
    private String teamCode;

    /**
     * 班组名称
     */
    @TableField(value = "team_name")
    private String teamName;

    /**
     * 所属车间
     */
    @TableField(value = "workshop")
    private String workshop;

    /**
     * 状态：0停用/1启用
     */
    @TableField(value = "status")
    private Integer status;

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
     * 成员数量
     */
    @TableField(exist = false)
    private Integer memberCount;

    // endregion

    // region 请求辅助字段（不映射数据库）
    // ===================================
    // 请求辅助字段
    // ===================================

    /**
     * 成员人员档案ID列表
     */
    @TableField(exist = false)
    private java.util.List<Long> memberIds;

    // endregion
}
