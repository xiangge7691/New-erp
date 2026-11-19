package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 入库标签打印/补打记录
 */
@TableName("stock_label_print_log")
@Data
@EqualsAndHashCode(callSuper = true)
public class StockLabelPrintLog extends AuditEntity {

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 入库单ID */
    @TableField("in_id")
    private Long inId;

    /** 入库明细ID */
    @TableField("in_detail_id")
    private Long inDetailId;

    /** 标签码 */
    @TableField("label_code")
    private String labelCode;

    /** 动作：PRINT/REPRINT */
    @TableField("action")
    private String action;

    /** 补打原因 */
    @TableField("reason")
    private String reason;

    /** 操作人ID */
    @TableField("operator_id")
    private Long operatorId;

    /** 操作人姓名 */
    @TableField("operator_name")
    private String operatorName;

    /** PDA设备编号 */
    @TableField("device_no")
    private String deviceNo;

    /** 是否删除 */
    @TableField("is_deleted")
    private Integer isDeleted;

    /** 乐观锁版本号 */
    @TableField("version")
    private Integer version;
}
