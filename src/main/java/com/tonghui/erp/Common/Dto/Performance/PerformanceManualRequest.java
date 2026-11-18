package com.tonghui.erp.Common.Dto.Performance;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 手动添加绩效请求
 */
@Data
public class PerformanceManualRequest {

    /** 被考核人（人员档案ID） */
    private Long personnelId;

    /** 关联工单ID，只写时长时为空 */
    private Long workOrderId;

    /** 关联工序类型ID，只写时长时为空 */
    private Long processTypeId;

    /** 关联排产批次ID，可为空 */
    private Long planBatchId;

    /** 关联实际执行记录ID，可为空；未排产工单按完工执行记录补录时使用 */
    private Long executionId;

    /** 绩效归属日期 */
    private LocalDate performanceDate;

    /** 个人工作时间（小时），为空时取绩效时长 */
    private BigDecimal workHours;

    /** 绩效时长（小时） */
    private BigDecimal performanceHours;

    /** 完成数量（选填） */
    private BigDecimal completedQty;

    /** 备注，必填 */
    private String note;

    /** 计算方式：按系数计算/直接计入 */
    private String calcMode;

    /** 已存在记录时是否确认继续添加（前端二次确认后传 true） */
    private Boolean allowDuplicate;
}
