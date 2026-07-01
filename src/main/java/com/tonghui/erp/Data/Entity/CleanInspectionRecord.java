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
 * @TableName clean_inspection_record
 */
@TableName(value = "clean_inspection_record")
@Data
@EqualsAndHashCode(callSuper = true)
public class CleanInspectionRecord extends AuditEntity {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "room_id")
    private Integer roomId;

    @TableField(value = "inspection_date")
    private LocalDate inspectionDate;

    @TableField(value = "inspection_area")
    private String inspectionArea;

    @TableField(value = "inspection_item")
    private String inspectionItem;

    @TableField(value = "inspection_result")
    private String inspectionResult;

    @TableField(value = "inspector")
    private String inspector;

    @TableField(value = "report_file_id")
    private Long reportFileId;

    @TableField(value = "is_deleted")
    private Integer isDeleted;

    @TableField(exist = false)
    private String roomName;
}
