package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 温湿度记录表（硬件预留）
 * <p>
 * 记录车间环境的温湿度检测数据，用于GMP合规管理中的环境监控，
 * 后续可对接硬件传感器自动采集
 * </p>
 *
 * @TableName temperature_humidity_record
 */
@TableName(value = "temperature_humidity_record")
@Data
@EqualsAndHashCode(callSuper = true)
public class TemperatureHumidityRecord extends AuditEntity {

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
    @TableField(value = "record_date")
    private LocalDate recordDate;

    /**
     * 检测区域描述
     */
    @TableField(value = "inspection_area")
    private String inspectionArea;

    // endregion

    // region 检测数据字段
    // ===================================
    // 检测数据字段
    // ===================================

    /**
     * 温度值（摄氏度）
     */
    @TableField(value = "temperature")
    private BigDecimal temperature;

    /**
     * 湿度值（百分比）
     */
    @TableField(value = "humidity")
    private BigDecimal humidity;

    // endregion

    // region 人员与备注字段
    // ===================================
    // 人员与备注字段
    // ===================================

    /**
     * 记录人姓名
     */
    @TableField(value = "recorder")
    private String recorder;

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
