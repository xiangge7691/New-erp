package com.tonghui.erp.Common.Dto.Pda;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * PDA 工序暂存/提交请求
 */
@Data
public class PdaExecutionSaveRequest {

    /** 使用设备ID */
    private Integer equipmentId;

    /** 配置室ID */
    private Integer roomId;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 是否手填时间 */
    private Boolean manualTime;

    /** 手填原因 */
    private String manualReason;

    /** 加工数量 */
    private BigDecimal processQty;

    /** 备注 */
    private String remark;

    /** PDA设备编号 */
    private String deviceNo;

    /** 客户端幂等ID */
    private String requestId;

    /** 实际参与人 */
    private List<PdaParticipantRequest> participants;
}
