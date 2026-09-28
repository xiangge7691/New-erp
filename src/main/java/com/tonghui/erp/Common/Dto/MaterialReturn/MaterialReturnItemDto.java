package com.tonghui.erp.Common.Dto.MaterialReturn;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 物料退货管理列表行DTO
 * <p>
 * 明细行级展示：以验收明细为行，关联验收单号、采购订单编号、制剂名称、
 * 供应商、退货原因/备注（验收环节只读带出）与最近一次重新发货时间
 * </p>
 */
@Data
public class MaterialReturnItemDto {

    // region 定位字段
    // ===================================
    // 定位字段
    // ===================================

    /**
     * 验收明细ID（退货操作定位键）
     */
    private Long detailId;

    /**
     * 来源验收单ID
     */
    private Long acceptanceId;

    /**
     * 明细序号
     */
    private Integer seq;

    /**
     * 验收单号（退货来源，不新增退货单号）
     */
    private String acceptanceCode;

    /**
     * 采购订单编号（关联采购订单）
     */
    private String purchaseNumber;

    // endregion

    // region 物料与制剂字段
    // ===================================
    // 物料与制剂字段
    // ===================================

    /**
     * 关联制剂名称（取验收单制剂，缺失时回退采购订单制剂）
     */
    private String preparationName;

    /**
     * 物料编码
     */
    private String materialCode;

    /**
     * 物料名称
     */
    private String materialName;

    /**
     * 批号（该退货物料的验收批号）
     */
    private String batchNumber;

    // endregion

    // region 退货业务字段
    // ===================================
    // 退货业务字段
    // ===================================

    /**
     * 退货数量（整行退货 = 采购数量）
     */
    private BigDecimal returnQty;

    /**
     * 供应商（根据验收明细的供应商自动获取）
     */
    private String supplier;

    /**
     * 退货原因（初验不合格/检验不合格，验收环节带出，只读）
     */
    private String returnReason;

    /**
     * 备注（不合格原因说明，验收环节带出，只读；无说明时为null前端显示"-"）
     */
    private String returnRemark;

    /**
     * 明细状态（待退货/已退货/已重发/已取消）
     */
    private String status;

    /**
     * 重发时间（该物料最近一次重新发货的留痕时间，无记录时为null前端显示"-"）
     */
    private LocalDateTime resendTime;

    // endregion
}
