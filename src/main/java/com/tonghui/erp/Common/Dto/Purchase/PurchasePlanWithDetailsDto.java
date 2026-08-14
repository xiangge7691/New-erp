package com.tonghui.erp.Common.Dto.Purchase;

import com.tonghui.erp.Data.Entity.PurchasePlan;
import com.tonghui.erp.Data.Entity.PurchasePlanDetail;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 采购计划包含明细项的组合传输对象
 * <p>
 * 在采购计划主表基础上扩展明细列表，用于主表和明细一起新增的场景，
 * 一次请求同时保存采购计划主表及其明细记录
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PurchasePlanWithDetailsDto extends PurchasePlan {

    /**
     * 采购计划明细列表（可选，为空时仅保存主表）
     */
    private List<PurchasePlanDetail> details;
}
