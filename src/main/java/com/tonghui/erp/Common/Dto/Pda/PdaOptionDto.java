package com.tonghui.erp.Common.Dto.Pda;

import lombok.Data;

/**
 * PDA 下拉选项
 */
@Data
public class PdaOptionDto {

    /** ID */
    private Long id;

    /** 名称 */
    private String name;

    /** 编号/编码 */
    private String code;

    /** 附加信息（房间、型号等） */
    private String extra;

    /** 设备是否正在占用 */
    private Boolean occupied;

    /** 占用提示：任务、制剂、工序、开始时间 */
    private String occupiedInfo;
}
