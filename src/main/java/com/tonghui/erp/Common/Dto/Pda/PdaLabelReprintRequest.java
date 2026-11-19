package com.tonghui.erp.Common.Dto.Pda;

import lombok.Data;

/**
 * PDA 补打标签请求
 */
@Data
public class PdaLabelReprintRequest {

    /** 补打原因 */
    private String reason;

    /** PDA设备编号 */
    private String deviceNo;
}
