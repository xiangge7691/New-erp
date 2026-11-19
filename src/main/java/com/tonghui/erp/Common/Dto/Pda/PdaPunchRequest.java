package com.tonghui.erp.Common.Dto.Pda;

import lombok.Data;

/**
 * PDA 开始/结束打点请求
 */
@Data
public class PdaPunchRequest {

    /** 使用设备ID */
    private Integer equipmentId;

    /** 配置室ID */
    private Integer roomId;

    /** PDA设备编号 */
    private String deviceNo;

    /** 客户端幂等ID */
    private String requestId;
}
