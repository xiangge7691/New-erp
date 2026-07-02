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
 * @TableName cleaning_record
 */
@TableName(value = "cleaning_record")
@Data
@EqualsAndHashCode(callSuper = true)
public class CleaningRecord extends AuditEntity {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "room_id")
    private Long roomId;

    @TableField(value = "cleaning_date")
    private LocalDate cleaningDate;

    @TableField(value = "cleaning_area")
    private String cleaningArea;

    @TableField(value = "cleaning_method")
    private String cleaningMethod;

    @TableField(value = "cleaning_person")
    private String cleaningPerson;

    @TableField(value = "cleaning_cycle")
    private Integer cleaningCycle;

    @TableField(value = "next_cleaning_date")
    private LocalDate nextCleaningDate;

    @TableField(value = "attachment")
    private String attachment;

    @TableField(value = "remark")
    private String remark;

    @TableField(value = "is_deleted")
    private Integer isDeleted;

    @Version
    @TableField(value = "version")
    private Integer version;

    @TableField(exist = false)
    private String roomName;
}
