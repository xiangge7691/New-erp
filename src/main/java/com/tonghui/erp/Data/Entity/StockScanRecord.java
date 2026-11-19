package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 仓储扫码记录
 */
@TableName("stock_scan_record")
@Data
@EqualsAndHashCode(callSuper = true)
public class StockScanRecord extends AuditEntity {

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 业务类型：INBOUND/OUTBOUND/RETURN */
    @TableField("biz_type")
    private String bizType;

    /** 业务单据ID */
    @TableField("biz_id")
    private Long bizId;

    /** 明细ID */
    @TableField("detail_id")
    private Long detailId;

    /** 扫描标签码 */
    @TableField("label_code")
    private String labelCode;

    /** 动作：SCAN/CLEAR/MANUAL */
    @TableField("action")
    private String action;

    /** 结果：SUCCESS/FAIL */
    @TableField("result")
    private String result;

    /** 失败原因 */
    @TableField("fail_reason")
    private String failReason;

    /** 操作人ID */
    @TableField("operator_id")
    private Long operatorId;

    /** 操作人姓名 */
    @TableField("operator_name")
    private String operatorName;

    /** PDA设备编号 */
    @TableField("device_no")
    private String deviceNo;

    /** 客户端幂等ID */
    @TableField("request_id")
    private String requestId;

    /** 备注：手工录入授权/原因 */
    @TableField("remark")
    private String remark;

    /** 扫码时间 */
    @TableField("scan_time")
    private LocalDateTime scanTime;

    /** 是否删除 */
    @TableField("is_deleted")
    private Integer isDeleted;

    /** 乐观锁版本号 */
    @TableField("version")
    private Integer version;
}
