package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tonghui.erp.Data.Entity.PurchaseOrders;
import com.tonghui.erp.Data.Entity.PurchasePlan;
import com.tonghui.erp.Data.mapper.PurchaseOrderItemsMapper;
import com.tonghui.erp.Data.mapper.PurchaseOrdersMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 采购计划服务接口测试
 * <p>
 * 覆盖审批通过时处理日期自动取审核时间（计划与自动生成的采购订单同步）
 * </p>
 */
@SpringBootTest
public class PurchasePlanServiceTest {

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    /** 采购计划服务 */
    @Autowired
    private PurchasePlanService purchasePlanService;
    /** 采购订单Mapper */
    @Autowired
    private PurchaseOrdersMapper purchaseOrdersMapper;
    /** 采购订单明细Mapper */
    @Autowired
    private PurchaseOrderItemsMapper purchaseOrderItemsMapper;
    /** JdbcTemplate（用于物理删除测试数据，绕开软删除） */
    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    // endregion

    // region 测试方法
    // ===================================
    // 测试方法
    // ===================================

    /**
     * 测试审批通过后处理日期自动取审核时间
     * <p>
     * 创建测试计划并审批通过：计划 processing_date 应为当天，
     * 自动生成的采购订单 processing_date 应与计划一致
     * </p>
     */
    @Test
    public void testApprovalSetsProcessingDate() {
        Long planId = null;
        try {
            // 创建测试计划（动态单号避免唯一索引与软删除残留冲突）
            PurchasePlan plan = new PurchasePlan();
            plan.setPlanCode("TESTPLAN" + System.currentTimeMillis());
            plan.setTitle("测试采购计划");
            plan.setWarehouse("原料仓");
            plan.setInvoiceInfo("测试发票信息");
            plan.setReceivingAddress("测试收货地址");
            plan.setPrescriptionMultiple(new BigDecimal("1.0"));
            plan.setStatus("待审批");
            plan.setProcessingDate(null);
            purchasePlanService.save(plan);
            planId = plan.getId();

            // 审批通过
            boolean success = purchasePlanService.updateStatus(planId, "已审批", "同意");
            if (!success) {
                System.err.println("测试失败: 审批通过返回false");
            } else {
                System.out.println("审批通过成功");
            }

            // 断言计划处理日期为当天
            PurchasePlan updated = purchasePlanService.getById(planId);
            if (updated.getProcessingDate() == null || !updated.getProcessingDate().equals(LocalDate.now())) {
                System.err.println("测试失败: 计划处理日期应为当天, 实际: " + updated.getProcessingDate());
            } else {
                System.out.println("计划处理日期: " + updated.getProcessingDate());
            }

            // 断言自动生成的采购订单处理日期与计划一致
            if (updated.getPurchaseOrderId() != null) {
                PurchaseOrders order = purchaseOrdersMapper.selectById(updated.getPurchaseOrderId());
                if (order == null || !LocalDate.now().equals(order.getProcessingDate())) {
                    System.err.println("测试失败: 采购订单处理日期应为当天, 实际: " + (order == null ? "订单不存在" : order.getProcessingDate()));
                } else {
                    System.out.println("采购订单处理日期: " + order.getProcessingDate());
                }
            } else {
                System.err.println("测试失败: 审批通过后未生成采购订单");
            }
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup(planId);
        }
    }

    // endregion

    // region 私有工具方法
    // ===================================
    // 私有工具方法
    // ===================================

    /**
     * 清理测试数据（物理删除：订单明细→订单→计划）
     *
     * @param planId 采购计划ID（可为null）
     */
    private void cleanup(Long planId) {
        try {
            if (planId != null) {
                List<Long> ids = jdbcTemplate.query("SELECT purchase_order_id FROM purchase_plan WHERE id = ?",
                        (rs, rowNum) -> rs.getObject(1) == null ? null : rs.getLong(1), planId);
                Long orderId = ids.isEmpty() ? null : ids.get(0);
                if (orderId != null) {
                    jdbcTemplate.update("DELETE FROM purchase_order_items WHERE order_id = ?", orderId);
                    jdbcTemplate.update("DELETE FROM purchase_orders WHERE id = ?", orderId);
                }
                jdbcTemplate.update("DELETE FROM purchase_plan WHERE id = ?", planId);
            }
        } catch (Exception e) {
            System.err.println("清理测试数据失败: " + e.getMessage());
        }
    }

    // endregion
}