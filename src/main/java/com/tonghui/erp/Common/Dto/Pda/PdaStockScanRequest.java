package com.tonghui.erp.Common.Dto.Pda;

import lombok.Data;

/**
 * PDA 单条扫码请求
 */
@Data
public class PdaStockScanRequest {

    /** 扫描码 */
    private String code;

    /** PDA设备编号 */
    private String deviceNo;

    /** 客户端幂等ID */
    private String requestId;

    /** 手工录入授权人（主管） */
    private String authorizer;

    /** 手工录入原因 */
    private String reason;
}
