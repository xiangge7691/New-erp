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
 * @TableName temperature_humidity_record
 */
@TableName(value = "temperature_humidity_record")
@Data
@EqualsAndHashCode(callSuper = true)
public class TemperatureHumidityRecord extends AuditEntity {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "room_id")
    private Integer roomId;

    @TableField(value = "record_date")
    private LocalDate recordDate;

    @TableField(value = "inspection_area")
    private String inspectionArea;

    @TableField(value = "temperature")
    private BigDecimal temperature;

    @TableField(value = "humidity")
    private BigDecimal humidity;

    @TableField(value = "recorder")
    private String recorder;

    @TableField(value = "is_deleted")
    private Integer isDeleted;

    @TableField(exist = false)
    private String roomName;
}
