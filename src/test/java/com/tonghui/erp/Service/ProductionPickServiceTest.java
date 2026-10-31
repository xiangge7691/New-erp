package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tonghui.erp.Common.Dto.ProductionPick.ProductionPickIssueDetailRequest;
import com.tonghui.erp.Common.Dto.ProductionPick.ProductionPickIssueRequest;
import com.tonghui.erp.Common.Dto.ProductionPick.ProductionPickWithDetailsDto;
import com.tonghui.erp.Data.Entity.PreparationFormula;
import com.tonghui.erp.Data.Entity.ProductionPick;
import com.tonghui.erp.Data.Entity.ProductionPickDetail;
import com.tonghui.erp.Data.Entity.ProductionUnit;
import com.tonghui.erp.Data.Entity.Stock;
import com.tonghui.erp.Data.Entity.StockOut;
import com.tonghui.erp.Data.Entity.WorkOrder;
import com.tonghui.erp.Data.mapper.PreparationFormulaMapper;
import com.tonghui.erp.Data.mapper.ProductionPickMapper;
import com.tonghui.erp.Data.mapper.ProductionUnitMapper;
import com.tonghui.erp.Data.mapper.StockMapper;
import com.tonghui.erp.Data.mapper.WorkOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 生产领料服务集成测试
 * <p>
 * 覆盖：按生产任务预填、新增领料单、发料生成草稿出库单（不扣库存）、
 * 仓库确认出库扣库存、作废已发料领料单回滚库存。
 * 全部用例使用事务回滚，不污染数据库。
 * </p>
 */
@SpringBootTest
public class ProductionPickServiceTest {

    // region 服务与数据访问层注入
    // ===================================
    // 服务与数据访问层注入
    // ===================================

    /** 生产领料服务 */
    @Autowired
    private ProductionPickService productionPickService;

    /** 出库单服务 */
    @Autowired
    private StockOutService stockOutService;

    /** 生产领料单主表数据访问层 */
    @Autowired
    private ProductionPickMapper productionPickMapper;

    /** 生产任务数据访问层 */
    @Autowired
    private WorkOrderMapper workOrderMapper;

    /** 制剂处方数据访问层 */
    @Autowired
    private PreparationFormulaMapper preparationFormulaMapper;

    /** 库存数据访问层 */
    @Autowired
    private StockMapper stockMapper;

    /** 生产单位数据访问层 */
    @Autowired
    private ProductionUnitMapper productionUnitMapper;

    /** 数据源（用于执行幂等建表脚本） */
    @Autowired
    private DataSource dataSource;

    /** 建表标志位（只执行一次） */
    private static boolean tablesInitialized = false;

    // endregion

    // region 测试初始化
    // ===================================
    // 测试初始化
    // ===================================

    /**
     * 每个测试前确保生产领料表结构就绪（幂等）
     */
    @BeforeEach
    public void ensureTables() {
        if (tablesInitialized) {
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("Data/migration/create_production_pick_table.sql"));
            tablesInitialized = true;
        } catch (Exception e) {
            throw new RuntimeException("初始化生产领料表结构失败: " + e.getMessage(), e);
        }
    }

    // endregion

    // region 测试用例
    // ===================================
    // 测试用例
    // ===================================

    /**
     * 测试生产领料全流程：
     * 预填 → 新增 → 发料生成草稿出库单（库存不变）→ 仓库确认（扣库存）→ 作废（回滚库存）
     */
    @Test
    @Transactional
    public void testProductionPickIssueAndCancelFlow() {
        WorkOrder workOrder = findWorkOrderWithFormula();
        if (workOrder == null) {
            System.out.println("无可用生产任务/制剂处方数据，跳过该测试");
            return;
        }
        ProductionUnit productionUnit = productionUnitMapper.selectList(
                new QueryWrapper<ProductionUnit>().last("LIMIT 1")).stream().findFirst().orElse(null);
        if (productionUnit == null) {
            System.out.println("无生产单位数据，跳过该测试");
            return;
        }

        // 1. 按生产任务预填
        ProductionPickWithDetailsDto prefill = productionPickService.buildPrefillFromWorkOrder(
                workOrder.getWorkOrderId(), BigDecimal.ONE);
        assertNotNull(prefill, "预填数据不应为空");
        assertEquals(workOrder.getWorkOrderCode(), prefill.getWorkOrderCode(), "任务号应与生产任务一致");
        assertEquals(workOrder.getWorkOrderName(), prefill.getWorkOrderName(), "任务名应与生产任务一致");
        assertTrue(prefill.getDetails() != null && !prefill.getDetails().isEmpty(), "预填明细不应为空");

        // 2. 为第一条明细物料建立库存批次
        ProductionPickDetail firstDetail = prefill.getDetails().get(0);
        BigDecimal stockQty = firstDetail.getApplyQty().add(new BigDecimal("10"));
        Stock stock = buildStock(productionUnit.getProdUnitId(), firstDetail, stockQty);
        Long stockId = stock.getStockId();

        // 3. 新增领料单（只取第一条明细，便于验证单物料发料）
        ProductionPick pick = new ProductionPick();
        pick.setWorkOrderId(prefill.getWorkOrderId());
        pick.setApplicant("测试领料人");
        pick.setPickTime(LocalDateTime.now());
        pick.setPurpose(prefill.getPurpose());
        ProductionPick saved = productionPickService.createPick(pick, List.of(firstDetail));
        assertNotNull(saved.getId(), "领料单应保存成功");
        assertTrue(saved.getPickCode().startsWith("SCLL-"), "领料单号应以 SCLL- 开头");
        assertEquals("待发料", saved.getStatus(), "新增后状态应为待发料");

        ProductionPickWithDetailsDto savedDetail = productionPickService.getPickWithDetails(saved.getId());
        assertNotNull(savedDetail, "领料单详情不应为空");
        assertEquals(1, savedDetail.getDetails().size(), "领料单应有一条明细");
        Long detailId = savedDetail.getDetails().get(0).getId();

        // 4. 发料：生成草稿出库单，库存不变
        ProductionPickIssueRequest issueRequest = new ProductionPickIssueRequest();
        issueRequest.setReviewer("测试审核人");
        issueRequest.setIssuer("测试发料人");
        ProductionPickIssueDetailRequest issueDetail = new ProductionPickIssueDetailRequest();
        issueDetail.setDetailId(detailId);
        issueDetail.setActualQty(firstDetail.getApplyQty());
        issueDetail.setStockId(stockId);
        issueRequest.setDetails(List.of(issueDetail));

        ProductionPick issued = productionPickService.issuePick(saved.getId(), issueRequest);
        assertEquals("已发料", issued.getStatus(), "发料后状态应为已发料");
        assertNotNull(issued.getStockOutId(), "发料后应关联出库单");
        assertTrue(issued.getStockOutCode().startsWith("OUT"), "出库单号应以 OUT 开头");

        StockOut draft = stockOutService.getStockOutById(issued.getStockOutId());
        assertNotNull(draft, "草稿出库单应存在");
        assertEquals("草稿", draft.getOutStatus(), "发料后出库单应为草稿状态");
        Stock afterIssue = stockMapper.selectById(stockId);
        assertEquals(0, stockQty.compareTo(afterIssue.getQuantity()), "发料生成草稿单不应扣减库存");

        // 5. 仓库确认出库：扣减库存
        StockOut confirm = new StockOut();
        confirm.setOutId(issued.getStockOutId());
        confirm.setOutStatus("已出库");
        stockOutService.partialUpdateStockOut(confirm);
        Stock afterConfirm = stockMapper.selectById(stockId);
        assertEquals(0, stockQty.subtract(firstDetail.getApplyQty()).compareTo(afterConfirm.getQuantity()),
                "仓库确认后库存应扣减实发数量");

        // 6. 作废已发料领料单：取消出库并回滚库存
        productionPickService.cancelPick(saved.getId());
        ProductionPick cancelled = productionPickMapper.selectById(saved.getId());
        assertEquals("已作废", cancelled.getStatus(), "作废后状态应为已作废");
        StockOut cancelledOut = stockOutService.getStockOutById(issued.getStockOutId());
        assertEquals("已取消", cancelledOut.getOutStatus(), "作废后关联出库单应为已取消");
        Stock afterCancel = stockMapper.selectById(stockId);
        assertEquals(0, stockQty.compareTo(afterCancel.getQuantity()), "作废后库存应恢复");
    }

    /**
     * 测试库存不足时发料失败：领料单保持待发料，不生成草稿出库单
     */
    @Test
    @Transactional
    public void testIssueInsufficientStockRollback() {
        WorkOrder workOrder = findWorkOrderWithFormula();
        if (workOrder == null) {
            System.out.println("无可用生产任务数据，跳过该测试");
            return;
        }

        // 使用一个必然没有库存的物料编码
        ProductionPickDetail detail = new ProductionPickDetail();
        detail.setMaterialCode("NO-STOCK-" + System.currentTimeMillis());
        detail.setMaterialName("无库存测试物料");
        detail.setCategoryName("原料");
        detail.setUnitName("kg");
        detail.setApplyQty(BigDecimal.ONE);

        ProductionPick pick = new ProductionPick();
        pick.setWorkOrderId(workOrder.getWorkOrderId());
        pick.setApplicant("测试领料人");
        pick.setPickTime(LocalDateTime.now());
        ProductionPick saved = productionPickService.createPick(pick, List.of(detail));

        ProductionPickWithDetailsDto savedDetail = productionPickService.getPickWithDetails(saved.getId());
        ProductionPickIssueDetailRequest issueDetail = new ProductionPickIssueDetailRequest();
        issueDetail.setDetailId(savedDetail.getDetails().get(0).getId());
        issueDetail.setActualQty(BigDecimal.ONE);
        ProductionPickIssueRequest issueRequest = new ProductionPickIssueRequest();
        issueRequest.setReviewer("测试审核人");
        issueRequest.setIssuer("测试发料人");
        issueRequest.setDetails(List.of(issueDetail));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> productionPickService.issuePick(saved.getId(), issueRequest));
        assertTrue(ex.getMessage().contains("库存不足"), "异常信息应提示库存不足");

        ProductionPick afterFail = productionPickMapper.selectById(saved.getId());
        assertEquals("待发料", afterFail.getStatus(), "发料失败后领料单应保持待发料");
        assertNull(afterFail.getStockOutId(), "发料失败后不应生成出库单");
    }

    // endregion

    // region 私有辅助方法
    // ===================================
    // 私有辅助方法
    // ===================================

    /**
     * 查找一个已配置处方且未作废/未归档的生产任务
     *
     * @return 生产任务，找不到返回 null
     */
    private WorkOrder findWorkOrderWithFormula() {
        List<WorkOrder> workOrders = workOrderMapper.selectList(
                new QueryWrapper<WorkOrder>().orderByDesc("work_order_id").last("LIMIT 200"));
        for (WorkOrder workOrder : workOrders) {
            if ("已作废".equals(workOrder.getCurrentStatus()) || "已归档".equals(workOrder.getCurrentStatus())) {
                continue;
            }
            if (workOrder.getPreparationId() == null && workOrder.getPreparationCode() == null) {
                continue;
            }
            QueryWrapper<PreparationFormula> wrapper = new QueryWrapper<>();
            if (workOrder.getPreparationId() != null) {
                wrapper.eq("preparation_id", workOrder.getPreparationId());
            } else {
                wrapper.eq("preparation_code", workOrder.getPreparationCode());
            }
            if (!preparationFormulaMapper.selectList(wrapper).isEmpty()) {
                return workOrder;
            }
        }
        return null;
    }

    /**
     * 为测试明细建立库存批次
     *
     * @param prodUnitId 生产单位ID
     * @param detail     领料明细
     * @param quantity   库存数量
     * @return 新建的库存批次
     */
    private Stock buildStock(Long prodUnitId, ProductionPickDetail detail, BigDecimal quantity) {
        Stock stock = new Stock();
        stock.setProdUnitId(prodUnitId);
        stock.setItemType("material");
        stock.setItemId(detail.getMaterialId());
        stock.setItemCode(detail.getMaterialCode());
        stock.setItemName(detail.getMaterialName());
        stock.setCategoryName(detail.getCategoryName() != null ? detail.getCategoryName() : "原料");
        stock.setUnitName(detail.getUnitName() != null ? detail.getUnitName() : "kg");
        stock.setBatchNumber("PICK-TEST-" + System.currentTimeMillis());
        stock.setQuantity(quantity);
        stock.setUnitPrice(new BigDecimal("12.50"));
        stock.setStockStatus("合格");
        stock.setRemark("生产领料测试库存");
        stock.setIsDeleted(0);
        stock.setVersion(1);
        stockMapper.insert(stock);
        return stock;
    }

    // endregion
}
