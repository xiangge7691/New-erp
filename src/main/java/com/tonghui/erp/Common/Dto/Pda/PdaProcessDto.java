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

    /** 排产计划ID */
    private Long planId;

    /** 排产批次ID（拆分批次时用于定位实际执行记录） */
    private Long planBatchId;

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

    /** 计量单位ID */
    private Long unitId;

    /** 计量单位名称 */
    private String unitName;

    /** 计量单位符号（如 kg） */
    private String unitSymbol;

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

    /** 工序状态：未开始/进行中/已完成 */
    private String status;

    /** 是否已完成（PDA提交、PC填写完整起止时间或已完工锁定） */
    private Boolean done;

    /** 是否已在PDA提交 */
    private Boolean submitted;

    /** 是否由PC端填写完整起止时间（未走PDA提交、未完工锁定） */
    private Boolean pcFilled;

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
