package com.tonghui.erp.Common.Dto.Pda;

import lombok.Data;

import java.util.List;

/**
 * PDA 标签打印请求
 */
@Data
public class PdaLabelPrintRequest {

    /** 需要打印的明细ID；为空表示全部 */
    private List<Long> lineIds;

    /** PDA设备编号 */
    private String deviceNo;
}
