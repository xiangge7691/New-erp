package com.tonghui.erp.Common.Dto.Pda;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * PDA 提交记录
 */
@Data
public class PdaRecordDto {

    /** 工单ID */
    private Long workOrderId;

    /** 工单编号 */
    private String workOrderCode;

    /** 制剂名称 */
    private String preparationName;

    /** 批号 */
    private String batchNumber;

    /** 执行记录ID */
    private Long executionId;

    /** 工序顺序 */
    private Integer stepOrder;

    /** 工序类型ID */
    private Long processTypeId;

    /** 工序名称 */
    private String processName;

    /** 实际开始 */
    private LocalDateTime actualStartTime;

    /** 实际结束 */
    private LocalDateTime actualEndTime;

    /** 设备名称 */
    private String equipmentName;

    /** 加工数量 */
    private BigDecimal processQty;

    /** 参与人姓名 */
    private List<String> participantNames;

    /** 提交人姓名 */
    private String submitterName;

    /** 提交时间 */
    private LocalDateTime submitTime;
}
