package com.tonghui.erp.Common.Dto.Pda;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * PDA 生产任务（任务—工序两级）
 */
@Data
public class PdaTaskDto {

    /** 工单ID */
    private Long workOrderId;

    /** 工单编号 */
    private String workOrderCode;

    /** 工单名称 */
    private String workOrderName;

    /** 制剂名称 */
    private String preparationName;

    /** 批号 */
    private String batchNumber;

    /** 计划数量 */
    private BigDecimal batchQty;

    /** 计划开始时间（为空表示未排期） */
    private LocalDateTime planStartTime;

    /** PDA任务状态：未开始/进行中/已完成 */
    private String status;

    /** 车间（当前工单无车间字段，暂为空） */
    private String workshop;

    /** 工序总数 */
    private Integer processCount;

    /** 已完成工序数（含PC填写完整起止时间、PDA提交、完工锁定） */
    private Integer doneCount;

    /** 已通过PDA提交的工序数 */
    private Integer submittedCount;

    /** 工序行 */
    private List<PdaProcessDto> processes;
}
