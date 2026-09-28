package com.tonghui.erp.Service;

import com.tonghui.erp.Data.Entity.AcceptanceDetail;
import com.tonghui.erp.Data.Entity.AcceptanceOrder;
import com.tonghui.erp.Data.Entity.ProductionUnit;
import com.tonghui.erp.Data.Entity.Stock;
import com.tonghui.erp.Common.Dto.Stock.StockTransactionDto;
import com.tonghui.erp.Data.mapper.ProductionUnitMapper;
import com.tonghui.erp.Data.mapper.StockMapper;
import com.tonghui.erp.Service.impl.SequenceServiceImpl;
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
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 货物验收单服务集成测试
 * <p>
 * 覆盖验收单创建、状态流转（确认到货/初验/检验/重新收货）、检验合格入库的库存联动、
 * 已入库禁止删除等核心业务逻辑。测试前通过幂等SQL脚本确保验收表存在，
 * 业务数据在事务结束后自动回滚，不污染数据库
 * </p>
 */
@SpringBootTest
public class AcceptanceOrderServiceTest {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 验收单服务
     */
    @Autowired
    private AcceptanceOrderService acceptanceOrderService;

    /**
     * 库存服务（验证检验合格入库的库存联动）
     */
    @Autowired
    private StockService stockService;

    /**
     * 库存数据访问层（直接查询库存批次）
     */
    @Autowired
    private StockMapper stockMapper;

    /**
     * 生产单位数据访问层（查询仓库ID）
     */
    @Autowired
    private ProductionUnitMapper productionUnitMapper;

    /**
     * 数据源（用于执行幂等建表脚本）
     */
    @Autowired
    private DataSource dataSource;

    /**
     * 序列号生成服务（生成验收单号）
     */
    @Autowired
    private SequenceServiceImpl sequenceService;

    // endregion

    // region 测试初始化
    // ===================================
    // 测试初始化
    // ===================================

    /**
     * 建表标志位（只执行一次幂等建表脚本）
     */
    private static boolean tablesInitialized = false;

    /**
     * 每个测试前确保验收表存在（CREATE TABLE IF NOT EXISTS 幂等）
     */
    @BeforeEach
    public void ensureTables() {
        if (tablesInitialized) {
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            // 建表脚本位于 test resources，随测试打包
            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("acceptance_create_tables.sql"));
            tablesInitialized = true;
        } catch (Exception e) {
            throw new RuntimeException("初始化验收表失败: " + e.getMessage(), e);
        }
    }

    // endregion

    // region 测试用例
    // ===================================
    // 测试用例
    // ===================================

    /**
     * 测试验收单完整状态流转 + 检验合格入库的库存联动
     * <p>
     * 运输中 → 确认到货 → 验收中 → 初验合格（明细待检验）→ 检验合格入库（明细已入库），
     * 断言库存批次增加且库存流水写入
     * </p>
     */
    @Test
    @Transactional
    public void testAcceptanceFullFlow() {
        Long prodUnitId = findAnyProdUnitId();
        if (prodUnitId == null) {
            // 无生产单位数据时跳过库存联动断言
            System.out.println("无生产单位数据，跳过库存联动断言");
            return;
        }

        // 创建运输中的验收单（明细带批号，供后续检验合格入库）
        AcceptanceOrder acceptance = createAcceptance("运输中", "TEST-BATCH-001");
        Long id = acceptance.getAcceptanceId();
        assertEquals("运输中", acceptanceOrderService.getAcceptanceById(id).getStatus());

        // 确认到货：运输中 → 验收中，明细进入待初验
        acceptanceOrderService.confirmArrival(id);
        assertEquals("验收中", acceptanceOrderService.getAcceptanceById(id).getStatus());

        // 初验合格：主单维持验收中，明细进入待检验
        acceptanceOrderService.inspect(id, true, "数量外观核对无误");
        assertEquals("验收中", acceptanceOrderService.getAcceptanceById(id).getStatus());
        assertTrue(acceptanceOrderService.getDetailsByAcceptanceId(id).stream()
                        .allMatch(d -> "待检验".equals(d.getStatus())),
                "初验合格后明细应全部为待检验");

        // 检验合格入库（批号齐全 + 选择仓库）
        acceptanceOrderService.qualityCheck(id, true, prodUnitId, "合格");
        AcceptanceOrder done = acceptanceOrderService.getAcceptanceById(id);
        assertEquals("已入库", done.getStatus());
        assertEquals(prodUnitId, done.getProdUnitId());
        assertTrue(acceptanceOrderService.getDetailsByAcceptanceId(id).stream()
                        .allMatch(d -> "已入库".equals(d.getStatus())),
                "检验合格入库后明细应全部为已入库");

        // 断言库存联动：按 物料编码+仓库+批号 能查到库存批次，且流水已写入
        List<AcceptanceDetail> details = acceptanceOrderService.getDetailsByAcceptanceId(id);
        AcceptanceDetail first = details.get(0);
        Stock stock = stockMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Stock>()
                .eq("item_code", first.getMaterialCode())
                .eq("prod_unit_id", prodUnitId)
                .eq("batch_number", first.getBatchNumber()));
        assertNotNull(stock, "检验合格入库后库存批次应存在");
        assertEquals(0, first.getQuantity().compareTo(stock.getQuantity()), "库存数量应与验收数量一致");

        // 断言库存流水写入（入库类型为验收来源类型）
        List<StockTransactionDto> transactions = stockService.getTransactionsByStockId(stock.getStockId());
        assertFalse(transactions.isEmpty(), "检验合格入库后应写入库存流水");
        assertTrue(transactions.stream().anyMatch(t -> "采购入库".equals(String.valueOf(t.getTransactionType()))),
                "流水类型应为采购入库");
    }

    /**
     * 测试检验合格但存在未填写批号的物料时无法入库
     */
    @Test
    @Transactional
    public void testQualityCheckWithoutBatchThrows() {
        Long prodUnitId = findAnyProdUnitId();
        if (prodUnitId == null) {
            System.out.println("无生产单位数据，跳过该测试");
            return;
        }

        // 创建验收中的验收单（明细批号为空，状态待初验）
        AcceptanceOrder acceptance = createAcceptance("验收中", null);
        Long id = acceptance.getAcceptanceId();
        acceptanceOrderService.inspect(id, true, "初验合格");

        // 检验合格但批号缺失，应抛出异常
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> acceptanceOrderService.qualityCheck(id, true, prodUnitId, "合格"));
        assertTrue(ex.getMessage().contains("批号"), "异常信息应提示批号必填");
    }

    /**
     * 测试重新收货：生成新验收单，原单明细已重发、主单已结束
     */
    @Test
    @Transactional
    public void testReReceiveFlow() {
        // 创建验收中的验收单，初验不合格使明细进入待退货（主单派生为已结束）
        AcceptanceOrder acceptance = createAcceptance("验收中", null);
        Long id = acceptance.getAcceptanceId();
        acceptanceOrderService.inspect(id, false, "外观破损");

        AcceptanceOrder newAcceptance = acceptanceOrderService.reReceive(id);

        // 原单明细已重发、主单已结束
        assertEquals("已结束", acceptanceOrderService.getAcceptanceById(id).getStatus());
        assertTrue(acceptanceOrderService.getDetailsByAcceptanceId(id).stream()
                        .allMatch(d -> "已重发".equals(d.getStatus())),
                "原单明细应全部为已重发");
        // 新单状态为验收中，明细回到待初验且沿用原单、批号清空
        assertEquals("验收中", newAcceptance.getStatus());
        List<AcceptanceDetail> newDetails = acceptanceOrderService.getDetailsByAcceptanceId(newAcceptance.getAcceptanceId());
        assertEquals(acceptanceOrderService.getDetailsByAcceptanceId(id).size(), newDetails.size(), "新单明细数应与原单一致");
        assertTrue(newDetails.stream().allMatch(d -> d.getBatchNumber() == null || d.getBatchNumber().isEmpty()),
                "新单明细批号应清空");
        assertTrue(newDetails.stream().allMatch(d -> "待初验".equals(d.getStatus())),
                "新单明细状态应回到待初验");
    }

    /**
     * 测试已入库的验收单禁止删除
     */
    @Test
    @Transactional
    public void testDeleteInboundRejected() {
        Long prodUnitId = findAnyProdUnitId();
        if (prodUnitId == null) {
            System.out.println("无生产单位数据，跳过该测试");
            return;
        }

        // 创建并流转到已入库（明细带批号）
        AcceptanceOrder acceptance = createAcceptance("运输中", "TEST-BATCH-001");
        Long id = acceptance.getAcceptanceId();
        acceptanceOrderService.confirmArrival(id);
        acceptanceOrderService.inspect(id, true, "初验合格");
        acceptanceOrderService.qualityCheck(id, true, prodUnitId, "合格");

        // 已入库不可删除
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> acceptanceOrderService.deleteAcceptance(id));
        assertTrue(ex.getMessage().contains("已入库"), "异常信息应提示已入库不可删除");
    }

    /**
     * 测试验收单号自动生成格式（YS-YYYYMMDD-NNN）
     */
    @Test
    public void testGenerateAcceptanceCode() {
        String code = sequenceService.generateAcceptanceCode();
        assertTrue(code.matches("YS-\\d{8}-\\d{3}"), "验收单号格式应为 YS-YYYYMMDD-NNN，实际: " + code);
    }

    // endregion

    // region 私有工具方法
    // ===================================
    // 私有工具方法
    // ===================================

    /**
     * 创建验收单（含一条明细）
     *
     * @param status   初始状态
     * @param batchNo  批号（可为null）
     * @return 创建的验收单
     */
    private AcceptanceOrder createAcceptance(String status, String batchNo) {
        AcceptanceOrder acceptance = new AcceptanceOrder();
        acceptance.setAcceptanceCode(sequenceService.generateAcceptanceCode());
        acceptance.setSourceType("采购入库");
        acceptance.setStatus(status);
        acceptance.setRelatedOrder("TEST-ORDER-001");
        acceptance.setDeliveryDate(LocalDate.now().plusDays(7));

        AcceptanceDetail detail = new AcceptanceDetail();
        detail.setItemType("material");
        detail.setMaterialCode("TEST001");
        detail.setMaterialName("测试物料");
        detail.setMaterialCategory("原料");
        detail.setUnitName("kg");
        detail.setStandardDosage(new BigDecimal("0.1000"));
        detail.setQuantity(new BigDecimal("1.000"));
        detail.setUnitPrice(new BigDecimal("10.00"));
        detail.setBatchNumber(batchNo);
        detail.setExpiryDate(LocalDate.now().plusYears(1));

        acceptanceOrderService.addAcceptance(acceptance, List.of(detail));
        return acceptance;
    }

    /**
     * 查询任意一个生产单位ID（仓库），无数据时返回null
     *
     * @return 生产单位ID
     */
    private Long findAnyProdUnitId() {
        try {
            List<ProductionUnit> units = productionUnitMapper.selectList(null);
            return units.isEmpty() ? null : units.get(0).getProdUnitId();
        } catch (Exception e) {
            return null;
        }
    }

    // endregion
}
