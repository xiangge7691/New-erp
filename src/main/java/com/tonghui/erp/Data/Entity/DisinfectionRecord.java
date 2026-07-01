package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 消毒管理记录表
 * @TableName disinfection_record
 */
@TableName(value = "disinfection_record")
@Data
public class DisinfectionRecord {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "room_id")
    private Integer roomId;

    @TableField(value = "disinfection_date")
    private LocalDate disinfectionDate;

    @TableField(value = "disinfection_method")
    private String disinfectionMethod;

    @TableField(value = "disinfection_person")
    private String disinfectionPerson;

    @TableField(value = "disinfection_cycle")
    private Integer disinfectionCycle;

    @TableField(value = "next_disinfection_date")
    private LocalDate nextDisinfectionDate;

    @TableField(value = "remark")
    private String remark;

    @TableField(value = "created_at")
    private LocalDateTime createdAt;

    @TableField(value = "updated_at")
    private LocalDateTime updatedAt;

    @TableField(value = "is_deleted")
    private Integer isDeleted;

    @TableField(exist = false)
    private String roomName;
}
