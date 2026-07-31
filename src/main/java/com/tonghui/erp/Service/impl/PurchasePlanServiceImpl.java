package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.utils.EntityUtils;
import com.tonghui.erp.Data.Entity.PurchaseOrderItems;
import com.tonghui.erp.Data.Entity.PurchaseOrders;
import com.tonghui.erp.Data.Entity.PurchasePlan;
import com.tonghui.erp.Data.Entity.PurchasePlanDetail;
import com.tonghui.erp.Data.mapper.PurchaseOrderItemsMapper;
import com.tonghui.erp.Data.mapper.PurchaseOrdersMapper;
import com.tonghui.erp.Data.mapper.PurchasePlanDetailMapper;
import com.tonghui.erp.Data.mapper.PurchasePlanMapper;
import com.tonghui.erp.Service.PurchasePlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
            PurchaseOrders order = createOrderFromPlan(plan);
            purchaseOrdersMapper.insert(order);
            plan.setPurchaseOrderId(order.getId());
            this.updateById(plan);
            copyPlanDetailsToOrder(planId, order.getId());
            return true;
        }

        return this.updateById(plan);
    }

    @Override
    public Page<PurchasePlan> queryPurchasePlans(String status, String keyword, int pageIndex, int pageSize) {
        Page<PurchasePlan> page = new Page<>(pageIndex + 1, pageSize);
        QueryWrapper<PurchasePlan> wrapper = new QueryWrapper<>();

        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like("plan_code", keyword)
                    .or().like("work_order_code", keyword)
                    .or().like("title", keyword)
                    .or().like("preparation_name", keyword)
            );
        }

        wrapper.orderByDesc("created_time");
        return this.page(page, wrapper);
    }

    /**
     * 从采购计划生成采购订单
     */
    private PurchaseOrders createOrderFromPlan(PurchasePlan plan) {
        PurchaseOrders order = new PurchaseOrders();
        order.setPurchaseNumber(generateOrderNumber());
        order.setPlanId(plan.getId());
        order.setPlanCode(plan.getPlanCode());
        order.setWorkOrderId(plan.getWorkOrderId());
        order.setWorkOrderCode(plan.getWorkOrderCode());
        order.setTitle(plan.getTitle());
        order.setPreparationCode(plan.getPreparationCode());
        order.setPreparationName(plan.getPreparationName());
        order.setSpec(plan.getSpec());
        order.setBatchQty(plan.getBatchQty());
        order.setPrescriptionMultiple(plan.getPrescriptionMultiple());
        order.setMaterialType(plan.getMaterialType());
        order.setWarehouse(plan.getWarehouse());
        order.setProcessingDate(plan.getProcessingDate());
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
     * 复制采购计划明细到采购订单明细
     */
    private void copyPlanDetailsToOrder(Long planId, Long orderId) {
        // 查询计划明细
        QueryWrapper<PurchasePlanDetail> detailWrapper = new QueryWrapper<>();
        detailWrapper.eq("plan_id", planId);
        detailWrapper.eq("is_deleted", 0);
        List<PurchasePlanDetail> planDetails = purchasePlanDetailMapper.selectList(detailWrapper);

        // 复制到订单明细
        for (PurchasePlanDetail planDetail : planDetails) {
            PurchaseOrderItems orderItem = new PurchaseOrderItems();
            orderItem.setOrderId(orderId);
            orderItem.setSequenceNumber(planDetail.getSequenceNumber());
            orderItem.setMaterialId(planDetail.getMaterialId());
            orderItem.setRawMaterialName(planDetail.getMaterialName());
            orderItem.setProductName(planDetail.getMaterialCategory());
            orderItem.setUnit(planDetail.getUnit());
            orderItem.setDose(planDetail.getStandardQty());
            orderItem.setPurchaseQuantity(planDetail.getPurchaseQty());
            orderItem.setStock(planDetail.getStockQty());
            orderItem.setDifference(planDetail.getDifference());

            purchaseOrderItemsMapper.insert(orderItem);
        }
    }

    /**
     * 生成采购计划编号
     */
    private String generatePlanCode() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "CG" + dateStr;

        QueryWrapper<PurchasePlan> queryWrapper = new QueryWrapper<>();
        queryWrapper.likeRight("plan_code", prefix);
        queryWrapper.orderByDesc("plan_code");
        queryWrapper.last("LIMIT 1");

        PurchasePlan latestPlan = this.getOne(queryWrapper);

        int sequence = 1;
        if (latestPlan != null && StringUtils.hasText(latestPlan.getPlanCode())) {
            try {
                String latestCode = latestPlan.getPlanCode();
                String seqStr = latestCode.substring(prefix.length());
                sequence = Integer.parseInt(seqStr) + 1;
            } catch (Exception e) {
                sequence = 1;
            }
        }

        return prefix + String.format("%04d", sequence);
    }

    /**
     * 生成采购订单编号
     */
    private String generateOrderNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "DD" + dateStr;

        QueryWrapper<PurchaseOrders> queryWrapper = new QueryWrapper<>();
        queryWrapper.likeRight("purchase_number", prefix);
        queryWrapper.orderByDesc("purchase_number");
        queryWrapper.last("LIMIT 1");

        PurchaseOrders latestOrder = purchaseOrdersMapper.selectOne(queryWrapper);

        int sequence = 1;
        if (latestOrder != null && StringUtils.hasText(latestOrder.getPurchaseNumber())) {
            try {
                String latestCode = latestOrder.getPurchaseNumber();
                String seqStr = latestCode.substring(prefix.length());
                sequence = Integer.parseInt(seqStr) + 1;
            } catch (Exception e) {
                sequence = 1;
            }
        }

        return prefix + String.format("%04d", sequence);
    }
}
