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
 * 压差记录表（硬件预留）
 * @TableName pressure_difference_record
 */
@TableName(value = "pressure_difference_record")
@Data
@EqualsAndHashCode(callSuper = true)
public class PressureDifferenceRecord extends AuditEntity {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "room_id")
    private Integer roomId;

    @TableField(value = "record_date")
    private LocalDate recordDate;

    @TableField(value = "inspection_area")
    private String inspectionArea;

    @TableField(value = "pressure_value")
    private BigDecimal pressureValue;

    @TableField(value = "recorder")
    private String recorder;

    @TableField(value = "remark")
    private String remark;

    @TableField(value = "is_deleted")
    private Integer isDeleted;

    @TableField(exist = false)
    private String roomName;
}
