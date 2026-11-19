package com.tonghui.erp.Common.Dto.Pda;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * PDA 仓储单据明细行
 */
@Data
public class PdaStockLineDto {

    /** 明细ID */
    private Long lineId;

    /** 物料编码 */
    private String materialCode;

    /** 物料名称 */
    private String materialName;

    /** 批号 */
    private String batchNumber;

    /** 数量 */
    private BigDecimal quantity;

    /** 单位 */
    private String unitName;

    /** 有效期 */
    private LocalDate expiryDate;

    /** 库位 */
    private String location;

    /** 标签码（入库）/应扫码（出库、退库） */
    private String labelCode;

    /** 二维码内容 */
    private String qrContent;

    /** 打印状态（入库） */
    private String printStatus;

    /** 扫码状态：待扫/已扫 */
    private String scanStatus;

    /** 扫码人ID */
    private Long scannedBy;

    /** 扫码时间 */
    private LocalDateTime scannedTime;

    /** 应扫标签码（出库、退库） */
    private String expectedLabelCode;

    /** 补打次数 */
    private Integer reprintCount;
}
