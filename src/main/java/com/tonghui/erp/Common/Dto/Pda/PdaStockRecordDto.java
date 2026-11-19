package com.tonghui.erp.Common.Dto.Pda;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * PDA 作业记录
 */
@Data
public class PdaStockRecordDto {

    /** 类型：入库/出库/退库/手工录入/补打 */
    private String type;

    /** 单据号或标签码 */
    private String docCode;

    /** 仓库名称 */
    private String warehouseName;

    /** 业务日期 */
    private LocalDateTime docDate;

    /** 摘要 */
    private String summary;

    /** 操作人 */
    private String operatorName;

    /** 设备编号 */
    private String deviceNo;

    /** 记录时间 */
    private LocalDateTime recordTime;

    /** 状态 */
    private String status;
}
