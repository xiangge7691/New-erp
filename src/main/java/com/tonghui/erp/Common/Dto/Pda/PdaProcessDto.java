package com.tonghui.erp.Common.Dto.Pda;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * PDA 工序执行行
 */
@Data
public class PdaProcessDto {

    /** 执行记录ID，未生成执行行时为空 */
    private Long executionId;

    /** 工序顺序 */
    private Integer stepOrder;

    /** 工序类型ID */
    private Long processTypeId;

    /** 工序名称 */
    private String processName;

    /** 批次序号 */
    private Integer batchNo;

    /** 排产批次名称 */
    private String batchName;

    /** 计划开始 */
    private LocalDateTime planStartTime;

    /** 计划结束 */
    private LocalDateTime planEndTime;

    /** 计划数量 */
    private BigDecimal planQty;

    /** 设备ID */
    private Integer equipmentId;

    /** 设备名称 */
    private String equipmentName;

    /** 房间ID */
    private Integer roomId;

    /** 房间名称 */
    private String roomName;

    /** 实际开始 */
    private LocalDateTime actualStartTime;

    /** 实际结束 */
    private LocalDateTime actualEndTime;

    /** 实际设备名称 */
    private String actualEquipmentName;

    /** 实际数量 */
    private BigDecimal actualQty;

    /** 工序状态：待执行/执行中/已完成 */
    private String status;

    /** 是否已在PDA提交 */
    private Boolean submitted;

    /** 是否已完工锁定 */
    private Boolean locked;

    /** 是否手填时间 */
    private Boolean manualTime;

    /** 手填原因 */
    private String manualReason;

    /** 备注 */
    private String remark;

    /** 实际参与人 */
    private List<PdaParticipantDto> participants;
}
