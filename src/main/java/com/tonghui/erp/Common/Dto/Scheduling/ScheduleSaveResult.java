package com.tonghui.erp.Common.Dto.Scheduling;

import lombok.Data;

import java.util.List;

/**
 * 保存排产结果
 */
@Data
public class ScheduleSaveResult {

    /** 是否保存成功 */
    private boolean saved;

    /** 冲突列表；正式保存被拒时返回 */
    private List<ScheduleConflictDto> conflicts;

    /** 保存后的工序计划 */
    private List<ProcessScheduleDto> processes;
}
