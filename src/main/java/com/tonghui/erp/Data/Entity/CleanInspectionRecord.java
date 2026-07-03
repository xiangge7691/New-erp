package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 洁净检测记录表
 * <p>
 * 记录洁净车间的洁净度检测信息，包括检测日期、区域、项目、结果及下次检测日期等，
 * 用于GMP合规管理中的洁净度跟踪和周期提醒
 * </p>
 *
 * @TableName clean_inspection_record
 */
@TableName(value = "clean_inspection_record")
@Data
@EqualsAndHashCode(callSuper = true)
public class CleanInspectionRecord extends AuditEntity {

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
     * 检测日期
     */
    @TableField(value = "inspection_date")
    private LocalDate inspectionDate;

    /**
     * 检测区域描述
     */
    @TableField(value = "inspection_area")
    private String inspectionArea;

    // endregion

    // region 检测结果字段
    // ===================================
    // 检测结果字段
    // ===================================

    /**
     * 检测项目名称（如：悬浮粒子、沉降菌等）
     */
    @TableField(value = "inspection_item")
    private String inspectionItem;

    /**
     * 检测结果描述（如：合格/不合格，或具体数值）
     */
    @TableField(value = "inspection_result")
    private String inspectionResult;

    /**
     * 检测人员姓名
     */
    @TableField(value = "inspector")
    private String inspector;

    /**
     * 检测报告文件ID
     */
    @TableField(value = "report_file_id")
    private Long reportFileId;

    // endregion

    // region 周期与备注字段
    // ===================================
    // 周期与备注字段
    // ===================================

    /**
     * 下次计划检测日期
     */
    @TableField(value = "next_inspection_date")
    private LocalDate nextInspectionDate;

    /**
     * 备注说明
     */
    @TableField(value = "remark")
    private String remark;

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
