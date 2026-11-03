package com.tonghui.erp.Common.Dto;

import com.tonghui.erp.Data.Entity.AcceptanceOrder;
import com.tonghui.erp.Data.Entity.PurchaseOrders;
import com.tonghui.erp.Data.Entity.PurchasePlan;
import com.tonghui.erp.Data.Entity.StockIn;
import lombok.Data;

import java.util.List;

/**
 * 采购流程链路传输对象
 * <p>
 * 一条链路对应一个采购订单（无订单时对应一个采购计划），包含采购计划、采购订单、
 * 验收单、入库单以及六个流程节点。生产计划关联多个采购订单时返回多条链路。
 * </p>
 */
@Data
public class PurchaseChainDto {

    // region 链路标识
    // ===================================
    // 链路标识
    // ===================================

    /** 链路ID，用于前端 key */
    private String chainId;

    // endregion

    // region 关联单据
    // ===================================
    // 关联单据
    // ===================================

    /** 采购计划 */
    private PurchasePlan purchasePlan;

    /** 采购订单 */
    private PurchaseOrders purchaseOrder;

    /** 验收单列表（退货重发可能多张） */
    private List<AcceptanceOrder> acceptances;

    /** 入库单列表 */
    private List<StockIn> stockIns;

    // endregion

    // region 进度与异常
    // ===================================
    // 进度与异常
    // ===================================

    /** 当前阶段编码 */
    private String currentStageCode;

    /** 当前阶段名称 */
    private String currentStageName;

    /** 当前阶段状态：CURRENT/DONE/ABNORMAL */
    private String currentStageState;

    /** 链路进度百分比 */
    private Integer progressPercent;

    /** 是否异常 */
    private Boolean abnormal;

    /** 异常原因 */
    private String abnormalReason;

    /** 从采购申请到最后一个已完成节点的总耗时（分钟） */
    private Long totalDurationMinutes;

    /** 从采购申请到当前时间的已耗时（分钟） */
    private Long elapsedMinutes;

    // endregion

    // region 节点
    // ===================================
    // 节点
    // ===================================

    /** 六个流程节点 */
    private List<PurchaseNodeDto> nodes;

    // endregion
}
