package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 消毒管理记录表
 * <p>
 * 记录车间环境的消毒操作信息，包括消毒日期、方法、人员、周期及下次消毒日期等，
 * 用于GMP合规管理中的消毒计划跟踪和周期提醒
 * </p>
 *
 * @TableName disinfection_record
 */
@TableName(value = "disinfection_record")
@Data
@EqualsAndHashCode(callSuper = true)
public class DisinfectionRecord extends AuditEntity {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 记录唯一标识（自增主键）
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联车间房间ID
     */
    @TableField(value = "room_id")
    private Integer roomId;

    /**
     * 消毒执行日期
     */
    @TableField(value = "disinfection_date")
    private LocalDate disinfectionDate;

    /**
     * 消毒方法说明（如：紫外线、臭氧、化学消毒等）
     */
    @TableField(value = "disinfection_method")
    private String disinfectionMethod;

    /**
     * 执行消毒的人员姓名
     */
    @TableField(value = "disinfection_person")
    private String disinfectionPerson;

    // endregion

    // region 周期与提醒字段
    // ===================================
    // 周期与提醒字段
    // ===================================

    /**
     * 消毒周期（天），用于计算下次消毒日期
     */
    @TableField(value = "disinfection_cycle")
    private Integer disinfectionCycle;

    /**
     * 下次计划消毒日期，由消毒日期+消毒周期自动计算
     */
    @TableField(value = "next_disinfection_date")
    private LocalDate nextDisinfectionDate;

    // endregion

    // region 附件与备注字段
    // ===================================
    // 附件与备注字段
    // ===================================

    /**
     * 备注说明
     */
    @TableField(value = "remark")
    private String remark;

    /**
     * 消毒附件文件ID，多个以逗号分隔
     */
    @TableField(value = "attachment")
    private String attachment;

    // endregion

    // region 状态字段
    // ===================================
    // 状态字段
    // ===================================

    /**
     * 是否已删除：0否/1是
     */
    @TableField(value = "is_deleted")
    private Integer isDeleted;

    // endregion

    // region 关联表显示字段
    // ===================================
    // 关联表显示字段
    // ===================================

    /**
     * 房间名称（关联查询时填充，不存储到数据库）
     */
    @TableField(exist = false)
    private String roomName;

    // endregion
}
