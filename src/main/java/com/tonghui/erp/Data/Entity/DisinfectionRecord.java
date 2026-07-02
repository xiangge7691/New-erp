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
 * @TableName disinfection_record
 */
@TableName(value = "disinfection_record")
@Data
@EqualsAndHashCode(callSuper = true)
public class DisinfectionRecord extends AuditEntity {
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

    @TableField(value = "attachment")
    private String attachment;

    @TableField(value = "is_deleted")
    private Integer isDeleted;

    @TableField(exist = false)
    private String roomName;
}
