package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 清洁记录表
 * <p>
 * 记录车间环境的清洁操作信息，包括清洁日期、区域、方法、人员及下次清洁日期等，
 * 用于GMP合规管理中的清洁计划跟踪和提醒
 * </p>
 *
 * @TableName cleaning_record
 */
@TableName(value = "cleaning_record")
@Data
@EqualsAndHashCode(callSuper = true)
public class CleaningRecord extends AuditEntity {

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
    private Long roomId;

    /**
     * 清洁执行日期
     */
    @TableField(value = "cleaning_date")
    private LocalDate cleaningDate;

    /**
     * 清洁区域描述
     */
    @TableField(value = "cleaning_area")
    private String cleaningArea;

    /**
     * 清洁方法说明（如：擦拭、喷洒、紫外线等）
     */
    @TableField(value = "cleaning_method")
    private String cleaningMethod;

    /**
     * 执行清洁的人员姓名
     */
    @TableField(value = "cleaning_person")
    private String cleaningPerson;

    // endregion

    // region 周期与提醒字段
    // ===================================
    // 周期与提醒字段
    // ===================================

    /**
     * 清洁周期（天），用于计算下次清洁日期
     */
    @TableField(value = "cleaning_cycle")
    private Integer cleaningCycle;

    /**
     * 下次计划清洁日期，由清洁日期+清洁周期自动计算
     */
    @TableField(value = "next_cleaning_date")
    private LocalDate nextCleaningDate;

    // endregion

    // region 附件与备注字段
    // ===================================
    // 附件与备注字段
    // ===================================

    /**
     * 清洁附件文件ID，多个以逗号分隔
     */
    @TableField(value = "attachment")
    private String attachment;

    /**
     * 备注说明
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
    @Version
    @TableField(value = "version")
    private Integer version;

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
