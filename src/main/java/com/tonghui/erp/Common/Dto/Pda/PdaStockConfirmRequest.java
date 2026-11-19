package com.tonghui.erp.Common.Dto.Pda;

import lombok.Data;

/**
 * PDA 整单确认请求（作业签名）
 */
@Data
public class PdaStockConfirmRequest {

    /** 登录密码 */
    private String password;

    /** PDA设备编号 */
    private String deviceNo;

    /** 客户端幂等ID */
    private String requestId;
}
