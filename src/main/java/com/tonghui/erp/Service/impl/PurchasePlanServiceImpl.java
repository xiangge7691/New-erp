package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.utils.EntityUtils;
import com.tonghui.erp.Data.Entity.Preparation;
import com.tonghui.erp.Data.Entity.PurchaseOrderItems;
import com.tonghui.erp.Data.Entity.PurchaseOrders;
import com.tonghui.erp.Data.Entity.PurchasePlan;
import com.tonghui.erp.Data.Entity.PurchasePlanDetail;
import com.tonghui.erp.Data.mapper.PurchaseOrderItemsMapper;
import com.tonghui.erp.Data.mapper.PurchaseOrdersMapper;
import com.tonghui.erp.Data.mapper.PurchasePlanDetailMapper;
import com.tonghui.erp.Data.mapper.PurchasePlanMapper;
import com.tonghui.erp.Service.PreparationService;
import com.tonghui.erp.Service.PurchaseOrdersService;
import com.tonghui.erp.Service.PurchasePlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 采购计划服务实现类
 */
@Service
public class PurchasePlanServiceImpl extends ServiceImpl<PurchasePlanMapper, PurchasePlan>
        implements PurchasePlanService {

    @Autowired
    private PurchasePlanDetailMapper purchasePlanDetailMapper;

    @Autowired
    private PurchaseOrdersMapper purchaseOrdersMapper;

    @Autowired
    private PurchaseOrderItemsMapper purchaseOrderItemsMapper;

    @Autowired
    private PurchaseOrdersService purchaseOrdersService;

    @Autowired
    private PreparationService preparationService;

    /** 采购订单编号生成最大重试次数（处理并发编号冲突） */
    private static final int MAX_RETRY = 3;

    @Override
    @Transactional
    public boolean addPurchasePlan(PurchasePlan purchasePlan) {
        // 自动生成采购计划编号
        if (!StringUtils.hasText(purchasePlan.getPlanCode())) {
            purchasePlan.setPlanCode(generatePlanCode());
        }

        // 清理已软删除的相同编码记录
        baseMapper.physicalDeleteByPlanCode(purchasePlan.getPlanCode());

        // 设置默认状态
        if (!StringUtils.hasText(purchasePlan.getStatus())) {
            purchasePlan.setStatus("草稿");
        }

        // 设置创建时间和更新时间
        LocalDateTime now = LocalDateTime.now();
        purchasePlan.setCreatedTime(now);
        purchasePlan.setUpdatedTime(now);

        // 获取当前用户ID
        Long currentUserId = EntityUtils.getCurrentUserId();
        if (currentUserId != null) {
            purchasePlan.setCreatedBy(currentUserId);
            purchasePlan.setUpdatedBy(currentUserId);
        }

        return this.save(purchasePlan);
    }

    @Override
    @Transactional
    public boolean updateStatus(Long planId, String targetStatus, String approvalOpinion) {
        PurchasePlan plan = this.getById(planId);
        if (plan == null) {
            throw new RuntimeException("采购计划不存在");
        }

        // 更新状态和审批意见
        plan.setStatus(targetStatus);
        if (StringUtils.hasText(approvalOpinion)) {
            plan.setApprovalOpinion(approvalOpinion);
        }
        plan.setUpdatedTime(LocalDateTime.now());

        Long currentUserId = EntityUtils.getCurrentUserId();
        if (currentUserId != null) {
            plan.setUpdatedBy(currentUserId);
        }

        // 审批通过时，自动生成采购订单
        if ("已审批".equals(targetStatus)) {
            PurchaseOrders order = null;
            // 重试机制：处理采购订单编号并发冲突
            for (int i = 0; i < MAX_RETRY; i++) {
                order = createOrderFromPlan(plan);
                try {
                    purchaseOrdersMapper.insert(order);
                    break;
                } catch (DuplicateKeyException e) {
                    if (i == MAX_RETRY - 1) {
                        throw new RuntimeException("创建失败: 采购订单编号生成冲突，请稍后重试", e);
                    }
                }
            }
            plan.setPurchaseOrderId(order.getId());
            this.updateById(plan);
            copyPlanDetailsToOrder(plan, order.getId());
            return true;
        }

        return this.updateById(plan);
    }

    @Override
    public Page<PurchasePlan> queryPurchasePlans(PurchasePlan purchasePlan, String keyword,
                                                 LocalDate processingDateStart, LocalDate processingDateEnd,
                                                 LocalDate desiredDeliveryDateStart, LocalDate desiredDeliveryDateEnd,
                                                 LocalDate expectedDeliveryDateStart, LocalDate expectedDeliveryDateEnd,
                                                 int pageIndex, int pageSize) {
        Page<PurchasePlan> page = new Page<>(pageIndex + 1, pageSize);
        QueryWrapper<PurchasePlan> wrapper = new QueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like("plan_code", keyword)
                    .or().like("production_plan_code", keyword)
                    .or().like("title", keyword)
                    .or().like("preparation_name", keyword)
            );
        }

        if (purchasePlan != null) {
            if (purchasePlan.getId() != null) {
                wrapper.eq("id", purchasePlan.getId());
            }
            if (StringUtils.hasText(purchasePlan.getPlanCode())) {
                wrapper.like("plan_code", purchasePlan.getPlanCode());
            }
            if (purchasePlan.getProductionPlanId() != null) {
                wrapper.eq("production_plan_id", purchasePlan.getProductionPlanId());
            }
            if (StringUtils.hasText(purchasePlan.getProductionPlanCode())) {
                wrapper.like("production_plan_code", purchasePlan.getProductionPlanCode());
            }
            if (StringUtils.hasText(purchasePlan.getTitle())) {
                wrapper.like("title", purchasePlan.getTitle());
            }
            if (StringUtils.hasText(purchasePlan.getPreparationCode())) {
                wrapper.like("preparation_code", purchasePlan.getPreparationCode());
            }
            if (StringUtils.hasText(purchasePlan.getPreparationName())) {
                wrapper.like("preparation_name", purchasePlan.getPreparationName());
            }
            if (StringUtils.hasText(purchasePlan.getSpec())) {
                wrapper.like("spec", purchasePlan.getSpec());
            }
            if (StringUtils.hasText(purchasePlan.getMaterialType())) {
                wrapper.like("material_type", purchasePlan.getMaterialType());
            }
            if (StringUtils.hasText(purchasePlan.getWarehouse())) {
                wrapper.like("warehouse", purchasePlan.getWarehouse());
            }
            if (StringUtils.hasText(purchasePlan.getReceivingUnit())) {
                wrapper.like("receiving_unit", purchasePlan.getReceivingUnit());
            }
            if (StringUtils.hasText(purchasePlan.getReceivingAddress())) {
                wrapper.like("receiving_address", purchasePlan.getReceivingAddress());
            }
            if (StringUtils.hasText(purchasePlan.getStatus())) {
                wrapper.eq("status", purchasePlan.getStatus());
            }
            if (StringUtils.hasText(purchasePlan.getRemark())) {
                wrapper.like("remark", purchasePlan.getRemark());
            }
        }

        // 时间范围条件
        if (processingDateStart != null) {
            wrapper.ge("processing_date", processingDateStart);
        }
        if (processingDateEnd != null) {
            wrapper.le("processing_date", processingDateEnd);
        }
        if (desiredDeliveryDateStart != null) {
            wrapper.ge("desired_delivery_date", desiredDeliveryDateStart);
        }
        if (desiredDeliveryDateEnd != null) {
            wrapper.le("desired_delivery_date", desiredDeliveryDateEnd);
        }
        if (expectedDeliveryDateStart != null) {
            wrapper.ge("expected_delivery_date", expectedDeliveryDateStart);
        }
        if (expectedDeliveryDateEnd != null) {
            wrapper.le("expected_delivery_date", expectedDeliveryDateEnd);
        }

        wrapper.orderByDesc("created_time");
        return this.page(page, wrapper);
    }

    /**
     * 从采购计划生成采购订单
     */
    private PurchaseOrders createOrderFromPlan(PurchasePlan plan) {
        PurchaseOrders order = new PurchaseOrders();
        order.setPurchaseNumber(purchaseOrdersService.generateOrderNumber());
        order.setPlanId(plan.getId());
        order.setPlanCode(plan.getPlanCode());
        // 生产计划信息：采购计划直接关联生产计划，直接复制到采购订单
        order.setProductionPlanId(plan.getProductionPlanId());
        order.setProductionPlanCode(plan.getProductionPlanCode());
        order.setTitle(plan.getTitle());
        order.setPreparationCode(plan.getPreparationCode());
        order.setPreparationName(plan.getPreparationName());
        order.setUnit(resolvePreparationUnit(plan.getPreparationCode()));
        order.setSpec(plan.getSpec());
        order.setBatchQty(plan.getBatchQty());
        order.setPrescriptionMultiple(plan.getPrescriptionMultiple());
        order.setMaterialType(plan.getMaterialType());
        order.setWarehouse(plan.getWarehouse());
        order.setProcessingDate(plan.getProcessingDate());
        order.setDesiredDeliveryDate(plan.getDesiredDeliveryDate());
        order.setExpectedDeliveryDate(plan.getExpectedDeliveryDate());
        order.setReceivingInfo(plan.getReceivingAddress());
        order.setInvoiceInfo(plan.getInvoiceInfo());
        order.setRemark("由" + plan.getPlanCode() + "审批通过自动生成");
        order.setStatus("待采购");
        order.setApprovalOpinion(plan.getApprovalOpinion());

        LocalDateTime now = LocalDateTime.now();
        order.setCreatedTime(now);
        order.setUpdatedTime(now);

        Long currentUserId = EntityUtils.getCurrentUserId();
        if (currentUserId != null) {
            order.setCreatedBy(currentUserId);
            order.setUpdatedBy(currentUserId);
        }

        return order;
    }

    /**
     * 根据制剂编码解析制剂所属单位（purchase_orders.unit 取值来源）
     * <p>制剂不存在或未配置单位名称时返回空字符串，避免 NOT NULL 列插入失败</p>
     *
     * @param preparationCode 制剂编码
     * @return 制剂所属单位，无匹配时返回空字符串
     */
    private String resolvePreparationUnit(String preparationCode) {
        if (!StringUtils.hasText(preparationCode)) {
            return "";
        }
        Preparation preparation = preparationService.getPreparationByCode(preparationCode);
        return preparation != null && StringUtils.hasText(preparation.getUnitName()) ? preparation.getUnitName() : "";
    }

    /**
     * 复制采购计划明细到采购订单明细
     * <p>明细表 NOT NULL 字段均做兜底处理，避免计划明细缺值导致插入失败</p>
     *
     * @param plan    采购计划（用于取物料类型等计划级字段）
     * @param orderId 生成的采购订单ID
     */
    private void copyPlanDetailsToOrder(PurchasePlan plan, Long orderId) {
        // 查询计划明细
        QueryWrapper<PurchasePlanDetail> detailWrapper = new QueryWrapper<>();
        detailWrapper.eq("plan_id", plan.getId());
        detailWrapper.eq("is_deleted", 0);
        List<PurchasePlanDetail> planDetails = purchasePlanDetailMapper.selectList(detailWrapper);

        // 加工性质（原料/辅料/包材）取自计划的物料类型
        String processingProperty = StringUtils.hasText(plan.getMaterialType()) ? plan.getMaterialType() : "";

        // 复制到订单明细
        for (PurchasePlanDetail planDetail : planDetails) {
            PurchaseOrderItems orderItem = new PurchaseOrderItems();
            orderItem.setOrderId(orderId);
            orderItem.setSequenceNumber(planDetail.getSequenceNumber());
            orderItem.setMaterialId(planDetail.getMaterialId());
            orderItem.setMaterialCode(StringUtils.hasText(planDetail.getMaterialCode()) ? planDetail.getMaterialCode() : "");
            orderItem.setRawMaterialName(StringUtils.hasText(planDetail.getMaterialName()) ? planDetail.getMaterialName() : "");
            orderItem.setProductName(StringUtils.hasText(planDetail.getMaterialCategory()) ? planDetail.getMaterialCategory() : "");
            orderItem.setUnit(StringUtils.hasText(planDetail.getUnit()) ? planDetail.getUnit() : "");
            orderItem.setDose(planDetail.getStandardQty() != null ? planDetail.getStandardQty() : BigDecimal.ZERO);
            orderItem.setStandardDosage(planDetail.getStandardQty());
            orderItem.setPurchaseQuantity(planDetail.getPurchaseQty() != null ? planDetail.getPurchaseQty() : BigDecimal.ZERO);
            orderItem.setStock(planDetail.getStockQty() != null ? planDetail.getStockQty() : BigDecimal.ZERO);
            orderItem.setDifference(planDetail.getDifference());
            orderItem.setProcessingProperty(processingProperty);

            purchaseOrderItemsMapper.insert(orderItem);
        }
    }

    /**
     * 生成采购计划编号（CGJH + yyyyMMdd + 4位流水号）
     * <p>查询最大编号时绕过全局软删除过滤，避免与已软删除计划编号冲突</p>
     *
     * @return 采购计划编号
     */
    private String generatePlanCode() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "CGJH" + dateStr;

        // 使用原生SQL查询当天最大编号，绕过软删除过滤
        String lastCode = baseMapper.selectMaxPlanCodeByPrefix(prefix);

        int sequence = 1;
        if (StringUtils.hasText(lastCode) && lastCode.length() > prefix.length()) {
            try {
                String seqStr = lastCode.substring(prefix.length());
                sequence = Integer.parseInt(seqStr) + 1;
            } catch (Exception e) {
                sequence = 1;
            }
        }

        return prefix + String.format("%04d", sequence);
    }
}
