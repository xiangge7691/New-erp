package com.tonghui.erp.Common.Dto.Pda;

import lombok.Data;

import java.util.List;

/**
 * PDA 仓储任务详情
 */
@Data
public class PdaStockTaskDetailDto {

    /** 任务 */
    private PdaStockTaskDto task;

    /** 明细行 */
    private List<PdaStockLineDto> lines;
}
