package com.tonghui.erp.Common.Dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 采购流程节点传输对象
 * <p>
 * 描述采购申请、审批、下单、到货、验收、入库六个节点中的一个节点，
 * 包含完成时间、距上一节点耗时、节点状态及可点击跳转的单据信息。
 * </p>
 */
@Data
public class PurchaseNodeDto {

    // region 节点信息
    // ===================================
    // 节点信息
    // ===================================

    /** 节点编码 */
    private String code;

    /** 节点名称 */
    private String name;

    /** 节点状态：DONE/CURRENT/PENDING/SKIPPED/ABNORMAL */
    private String state;

    /** 节点完成时间，未到达时为 null */
    private LocalDateTime time;

    /** 时间是否为历史数据近似值 */
    private Boolean estimated;

    // endregion

    // region 耗时信息
    // ===================================
    // 耗时信息
    // ===================================

    /** 距上一节点耗时（分钟） */
    private Long durationMinutes;

    /** 距上一节点耗时展示文案 */
    private String durationText;

    /** 耗时是否包含近似时间 */
    private Boolean durationEstimated;

    // endregion

    // region 验收进度
    // ===================================
    // 验收进度
    // ===================================

    /** 已完成验收明细数（验收节点使用） */
    private Integer completedDetails;

    /** 验收明细总数（验收节点使用） */
    private Integer totalDetails;

    // endregion

    // region 关联单据
    // ===================================
    // 关联单据
    // ===================================

    /** 节点对应单据，未到达时为 null */
    private RelatedDocumentDto doc;

    /** 节点备注，如审批意见 */
    private String remark;

    // endregion
}
