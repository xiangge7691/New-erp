package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 工序类型表
 * @TableName process_type
 */
@TableName(value ="process_type")
@Data
public class ProcessType {
    /**
     * 工序类型 ID，主键。唯一标识，自增长。
     */
    @TableId(value = "process_id", type = IdType.AUTO)
    private Integer processId;

    /**
     * 工序类型编码。唯一业务编码，如"ASSEMBLY"。
     */
    @TableField(value = "process_code")
    private String processCode;

    /**
     * 工序类型名称。中文描述，如"提取"。
     */
    @TableField(value = "process_name")
    private String processName;

    /**
     * 工序类型说明。详细描述，可为空。
     */
    @TableField(value = "process_description")
    private String processDescription;

    /**
     * 状态。`1`-启用，`0`-未启用。默认启用。
     */
    @TableField(value = "process_status")
    private Integer processStatus;

    /**
     * 创建人 ID。记录创建者 ID，关联用户表。
     */
    @TableField(value = "creator_id")
    private Long creatorId;

    /**
     * 创建时间。记录插入时间，自动填充。
     */
    @TableField(value = "created_time")
    private LocalDateTime createdTime;

    /**
     * 修改人 ID。记录最后修改者 ID，关联用户表。
     */
    @TableField(value = "updater_id")
    private Long updaterId;

    /**
     * 最后修改时间。记录更新时自动更新。
     */
    @TableField(value = "updated_time")
    private LocalDateTime updatedTime;
}