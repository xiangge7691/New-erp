package com.tonghui.erp.Common.Dto.Scheduling;

import lombok.Data;

import java.util.List;

/**
 * 保存排产请求
 */
@Data
public class ScheduleSaveRequest {

    /** 工单ID */
    private Long workOrderId;

    /** 工序计划行 */
    private List<ProcessPlanSaveDto> processes;
}
