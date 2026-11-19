package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 入库明细逐条标签
 */
@TableName("stock_in_label")
@Data
@EqualsAndHashCode(callSuper = true)
public class StockInLabel extends AuditEntity {

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 入库单ID */
    @TableField("in_id")
    private Long inId;

    /** 入库明细ID */
    @TableField("in_detail_id")
    private Long inDetailId;

    /** 逐条入库码（唯一） */
    @TableField("label_code")
    private String labelCode;

    /** 二维码内容 */
    @TableField("qr_content")
    private String qrContent;

    /** 打印状态：待打印/已打印 */
    @TableField("print_status")
    private String printStatus;

    /** 打印人ID */
    @TableField("printed_by")
    private Long printedBy;

    /** 打印时间 */
    @TableField("printed_time")
    private LocalDateTime printedTime;

    /** 补打次数 */
    @TableField("reprint_count")
    private Integer reprintCount;

    /** 扫码状态：待扫/已扫/作废 */
    @TableField("scan_status")
    private String scanStatus;

    /** 扫码人ID */
    @TableField("scanned_by")
    private Long scannedBy;

    /** 扫码时间 */
    @TableField("scanned_time")
    private LocalDateTime scannedTime;

    /** 是否删除 */
    @TableField("is_deleted")
    private Integer isDeleted;

    /** 乐观锁版本号 */
    @TableField("version")
    private Integer version;

    // region 关联显示字段（不映射数据库）
    // ===================================
    // 关联显示字段
    // ===================================

    /** 入库单号 */
    @TableField(exist = false)
    private String inCode;

    /** 入库单状态 */
    @TableField(exist = false)
    private String inStatus;

    /** 物料编码 */
    @TableField(exist = false)
    private String materialCode;

    /** 物料名称 */
    @TableField(exist = false)
    private String materialName;

    /** 批号 */
    @TableField(exist = false)
    private String batchNumber;

    /** 数量 */
    @TableField(exist = false)
    private BigDecimal quantity;

    /** 单位 */
    @TableField(exist = false)
    private String unitName;

    /** 仓库ID */
    @TableField(exist = false)
    private Long prodUnitId;

    /** 仓库名称 */
    @TableField(exist = false)
    private String warehouseName;

    // endregion
}
