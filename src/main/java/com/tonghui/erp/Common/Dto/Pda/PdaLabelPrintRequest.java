package com.tonghui.erp.Common.Dto.Pda;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * PDA 标签打印请求
 */
@Data
public class PdaLabelPrintRequest {

    /** 需要打印的明细ID；为空表示全部 */
    private List<Long> lineIds;

    /** 明细ID → 库位，打印前可补充/修改标签库位 */
    private Map<Long, String> locations;

    /** PDA设备编号 */
    private String deviceNo;
}
