package com.tonghui.erp.Common.Dto.Purchase;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购数据导出行数据传输对象
 * <p>
 * 统一承载三类导出行，由单据类型区分：
 * <ul>
 *   <li>采购：每行对应一条采购订单明细（采购订单主表信息 + 明细物料/金额信息）</li>
 *   <li>退货：采购退货来源的验收明细行（退货数量/原因/状态/备注）</li>
 *   <li>重发：重新发货留痕行（重发数量/新验收单号/重发时间，按留痕逐条）</li>
 * </ul>
 * 供采购数据 Excel 导出使用
 * </p>
 */
@Data
public class PurchaseExportRowDto {

    // region 单据类型
    // ===================================
    // 单据类型
    // ===================================

    /**
     * 单据类型：采购/退货/重发
     */
    private String documentType;

    // endregion

    // region 采购订单主表信息
    // ===================================
    // 采购订单主表信息
    // ===================================

    /**
     * 采购订单编号
     */
    private String purchaseNumber;

    /**
     * 生产计划编号
     */
    private String productionPlanCode;

    /**
     * 工单标题
     */
    private String title;

    /**
     * 制剂名称
     */
    private String preparationName;

    /**
     * 委托单位（制剂所属单位）
     */
    private String entrustUnit;

    /**
     * 批量
     */
    private BigDecimal batchQty;

    /**
     * 处方倍数
     */
    private BigDecimal prescriptionMultiple;

    /**
     * 制剂规格
     */
    private String spec;

    /**
     * 采购订单状态
     */
    private String orderStatus;

    /**
     * 创建时间（采购时间）
     */
    private LocalDateTime createdTime;

    // endregion

    // region 采购明细信息
    // ===================================
    // 采购明细信息
    // ===================================

    /**
     * 物料编码
     */
    private String materialCode;

    /**
     * 物料名称
     */
    private String materialName;

    /**
     * 物料分类（原料/辅料/包材）
     */
    private String materialCategory;

    /**
     * 计量单位
     */
    private String unit;

    /**
     * 批号（退货/重发行取验收明细批号，采购行留空）
     */
    private String batchNumber;

    /**
     * 标准处方（标准处方量）
     */
    private BigDecimal standardDosage;

    /**
     * 采购数量
     */
    private BigDecimal purchaseQuantity;

    /**
     * 单价
     */
    private BigDecimal unitPrice;

    /**
     * 总价（金额）
     */
    private BigDecimal amount;

    /**
     * 标准量差值
     */
    private BigDecimal difference;

    /**
     * 发票号
     */
    private String invoiceNo;

    /**
     * 供应商
     */
    private String supplier;

    // endregion

    // region 退货与重发信息
    // ===================================
    // 退货与重发信息
    // ===================================

    /**
     * 验收单号（退货/重发行：来源验收单号；采购行留空）
     */
    private String acceptanceCode;

    /**
     * 退货数量（退货行：实际到货数量，缺失时取采购数量）
     */
    private BigDecimal returnQty;

    /**
     * 退货原因（初验不合格/检验不合格）
     */
    private String returnReason;

    /**
     * 退货状态（待退货/已退货/已重发/已取消；重发行固定为已重发）
     */
    private String returnStatus;

    /**
     * 退货备注（不合格原因说明）
     */
    private String returnRemark;

    /**
     * 重发数量（重发行：本次重新发货数量）
     */
    private BigDecimal resendQty;

    /**
     * 重发验收单号（重发行：重新发货生成的新验收单号）
     */
    private String newAcceptanceCode;

    /**
     * 重发时间（重发行：重新发货操作时间）
     */
    private LocalDateTime resendTime;

    // endregion
}
