package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 房间表，用于记录设备所在房间的信息。
 * @TableName room_info
 */
@TableName(value ="room_info")
@Data
public class RoomInfo {
    /**
     * 房间 ID，主键。唯一标识，自增长。
     */
    @TableId(value = "room_id", type = IdType.AUTO)
    private Integer roomId;

    /**
     * 房间名。唯一标识房间的名称，如"提取一室"、"动力机房"。
     */
    @TableField(value = "room_name")
    private String roomName;

    /**
     * 房间位置。描述房间的具体位置，如"A 栋 3 楼 301"。
     */
    @TableField(value = "room_location")
    private String roomLocation;

    /**
     * 面积。房间的面积，单位为平方米（㎡）。
     */
    @TableField(value = "area")
    private BigDecimal area;

    /**
     * 生产制剂类型。多选：内服/外用/眼用。
     */
    @TableField(value = "production_type")
    private String productionType;

    /**
     * 是否为产尘操作间。
     */
    @TableField(value = "dust_room")
    private Boolean dustRoom;

    /**
     * 是否为洁净区。
     */
    @TableField(value = "clean_area")
    private Boolean cleanArea;

    /**
     * 洁净等级。当洁净区=是时填写：A级/B级/C级/D级。
     */
    @TableField(value = "clean_grade")
    private String cleanGrade;

    /**
     * 洁净规程文件ID，关联file_info表。
     */
    @TableField(value = "clean_procedure_file_id")
    private Long cleanProcedureFileId;

    /**
     * 备注。房间的附加说明信息，可为空。
     */
    @TableField(value = "remark")
    private String remark;

    /**
     * 房间状态。1-启用/可用，0-停用/不可用。默认启用。
     */
    @TableField(value = "room_status")
    private Integer roomStatus;

    /**
     * 创建人 ID。记录创建者 ID，关联用户表 (user.user_id)。
     */
    @TableField(value = "creator_id", fill = FieldFill.INSERT)
    private Long creatorId;

    /**
     * 创建时间。记录插入时间，自动填充当前时间。
     */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /**
     * 修改人 ID。记录最后修改者 ID，关联用户表 (user.user_id)。
     */
    @TableField(value = "updater_id", fill = FieldFill.INSERT_UPDATE)
    private Long updaterId;

    /**
     * 最后修改时间。记录更新时自动更新。
     */
    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    /**
     * 是否已删除
     */
    @TableField(value = "is_deleted")
    private Integer isDeleted;

    /**
     * 乐观锁版本号
     */
    @TableField(value = "version")
    private Integer version;
}