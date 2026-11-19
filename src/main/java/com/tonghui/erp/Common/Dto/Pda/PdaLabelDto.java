package com.tonghui.erp.Common.Dto.Pda;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PDA 补打候选标签
 */
@Data
public class PdaLabelDto {

    /** 标签ID */
    private Long id;

    /** 入库单ID */
    private Long inId;

    /** 入库单号 */
    private String inCode;

    /** 入库明细ID */
    private Long lineId;

    /** 标签码 */
    private String labelCode;

    /** 二维码内容 */
    private String qrContent;

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

    /** 仓库名称 */
    private String warehouseName;

    /** 关联制剂名称（标签用途） */
    private String preparationName;

    /** 单据日期 */
    private LocalDateTime docDate;

    /** 打印状态 */
    private String printStatus;

    /** 扫码状态 */
    private String scanStatus;

    /** 补打次数 */
    private Integer reprintCount;

    /** 可补打状态：可补打/已作废 */
    private String status;
}
