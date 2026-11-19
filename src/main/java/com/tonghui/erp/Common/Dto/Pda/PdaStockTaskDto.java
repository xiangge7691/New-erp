package com.tonghui.erp.Common.Dto.Pda;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * PDA 仓储任务
 */
@Data
public class PdaStockTaskDto {

    /** 单据ID */
    private Long id;

    /** 单据号 */
    private String docCode;

    /** 类型：INBOUND/OUTBOUND/RETURN */
    private String type;

    /** 单据状态 */
    private String status;

    /** PDA扫码状态 */
    private String scanStatus;

    /** 仓库ID */
    private Long warehouseId;

    /** 仓库名称 */
    private String warehouseName;

    /** 单据日期 */
    private LocalDateTime docDate;

    /** 明细数 */
    private Integer lineCount;

    /** 已打印标签数（入库） */
    private Integer printedCount;

    /** 已扫码行数 */
    private Integer scannedCount;

    /** 是否可确认 */
    private Boolean canConfirm;

    /** 备注 */
    private String remark;
}
