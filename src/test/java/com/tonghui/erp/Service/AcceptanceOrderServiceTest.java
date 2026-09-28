package com.tonghui.erp.Service;

import com.tonghui.erp.Data.Entity.AcceptanceDetail;
import com.tonghui.erp.Data.Entity.AcceptanceOrder;
import com.tonghui.erp.Data.Entity.ProductionUnit;
import com.tonghui.erp.Data.Entity.Stock;
import com.tonghui.erp.Common.Dto.Stock.AcceptanceInboundRequest;
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
 * 覆盖验收单创建、行级状态流转（确认到货/逐行初验/逐行检验/部分验收拆行）、
 * 整单入库的库存联动、已入库禁止删除等核心业务逻辑。
 * 测试前通过幂等SQL脚本确保验收表存在，业务数据在事务结束后自动回滚，不污染数据库
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
     * 测试验收单完整状态流转 + 整单入库的库存联动
     * <p>
     * 运输中 → 确认到货 → 验收中 → 逐行初验合格（明细待检验）→ 逐行检验合格（明细待入库）
     * → 整单入库（明细已入库），断言库存批次增加且库存流水写入
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

        // 逐行初验合格（detailIds为空=全部待初验行）：主单维持验收中，明细进入待检验
        acceptanceOrderService.inspect(id, null, true, "数量外观核对无误");
        assertEquals("验收中", acceptanceOrderService.getAcceptanceById(id).getStatus());
        assertTrue(acceptanceOrderService.getDetailsByAcceptanceId(id).stream()
                        .allMatch(d -> "待检验".equals(d.getStatus())),
                "初验合格后明细应全部为待检验");

        // 逐行检验合格：明细进入待入库，主单按明细派生仍为验收中（等待整单入库）
        acceptanceOrderService.qualityCheck(id, null, true, "合格");
        assertEquals("验收中", acceptanceOrderService.getAcceptanceById(id).getStatus());
        assertTrue(acceptanceOrderService.getDetailsByAcceptanceId(id).stream()
                        .allMatch(d -> "待入库".equals(d.getStatus())),
                "检验合格后明细应全部为待入库");

        // 整单入库：为每条待入库明细指定仓库，生成入库单，明细已入库、主单已入库
        List<Long> detailIds = acceptanceOrderService.getDetailsByAcceptanceId(id).stream()
                .map(AcceptanceDetail::getDetailId).collect(java.util.stream.Collectors.toList());
        acceptanceOrderService.inbound(id, detailIds.stream().map(did -> {
            AcceptanceInboundRequest.Item item = new AcceptanceInboundRequest.Item();
            item.setDetailId(did);
            item.setProdUnitId(prodUnitId);
            return item;
        }).collect(java.util.stream.Collectors.toList()));

        AcceptanceOrder done = acceptanceOrderService.getAcceptanceById(id);
        assertEquals("已入库", done.getStatus());
        assertEquals(prodUnitId, done.getProdUnitId());
        assertTrue(acceptanceOrderService.getDetailsByAcceptanceId(id).stream()
                        .allMatch(d -> "已入库".equals(d.getStatus())),
                "整单入库后明细应全部为已入库");

        // 断言库存联动：按 物料编码+仓库+批号 能查到库存批次，且流水已写入
        List<AcceptanceDetail> details = acceptanceOrderService.getDetailsByAcceptanceId(id);
        AcceptanceDetail first = details.get(0);
        Stock stock = stockMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Stock>()
                .eq("item_code", first.getMaterialCode())
                .eq("prod_unit_id", prodUnitId)
                .eq("batch_number", first.getBatchNumber()));
        assertNotNull(stock, "整单入库后库存批次应存在");
        assertEquals(0, first.getQuantity().compareTo(stock.getQuantity()), "库存数量应与验收数量一致");

        // 断言库存流水写入（入库类型为验收来源类型）
        List<StockTransactionDto> transactions = stockService.getTransactionsByStockId(stock.getStockId());
        assertFalse(transactions.isEmpty(), "整单入库后应写入库存流水");
        assertTrue(transactions.stream().anyMatch(t -> "采购入库".equals(String.valueOf(t.getTransactionType()))),
                "流水类型应为采购入库");
    }

    /**
     * 测试逐行检验合格但存在未填写批号的物料时无法推进
     */
    @Test
    @Transactional
    public void testQualityCheckWithoutBatchThrows() {
        // 创建验收中的验收单（明细批号为空，状态待初验）
        AcceptanceOrder acceptance = createAcceptance("验收中", null);
        Long id = acceptance.getAcceptanceId();
        acceptanceOrderService.inspect(id, null, true, "初验合格");

        // 检验合格但批号缺失，应抛出异常
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> acceptanceOrderService.qualityCheck(id, null, true, "合格"));
        assertTrue(ex.getMessage().contains("批号"), "异常信息应提示批号必填");
    }

    /**
     * 测试部分验收（拆行）：一行拆为验收子行+退货子行，状态/数量/订单明细同步拆分
     * <p>初验环节拆分：验收子行→待检验、退货子行→待退货，记录原行序号，数量守恒</p>
     */
    @Test
    @Transactional
    public void testPartialAcceptance() {
        AcceptanceOrder acceptance = createAcceptance("验收中", null);
        Long id = acceptance.getAcceptanceId();
        Long detailId = acceptanceOrderService.getDetailsByAcceptanceId(id).get(0).getDetailId();

        // 数量不守恒应被拒绝
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> acceptanceOrderService.partialAcceptance(detailId, "初验",
                        new BigDecimal("0.800"), new BigDecimal("0.100"), "部分破损"));
        assertTrue(ex.getMessage().contains("等于采购数量"), "两段数量之和必须等于原行采购数量");

        // 合法拆分：验收0.700 + 退货0.300 = 采购1.000
        acceptanceOrderService.partialAcceptance(detailId, "初验",
                new BigDecimal("0.700"), new BigDecimal("0.300"), "部分破损");

        List<AcceptanceDetail> rows = acceptanceOrderService.getDetailsByAcceptanceId(id);
        assertEquals(2, rows.size(), "拆分后应为两条明细");

        AcceptanceDetail acceptChild = rows.stream()
                .filter(d -> d.getDetailId().equals(detailId)).findFirst().orElseThrow();
        AcceptanceDetail returnChild = rows.stream()
                .filter(d -> !d.getDetailId().equals(detailId)).findFirst().orElseThrow();

        // 验收子行：数量0.700、状态待检验、金额重算=0.700×10.00
        assertEquals(0, new BigDecimal("0.700").compareTo(acceptChild.getQuantity()), "验收子行数量应为验收数量");
        assertEquals("待检验", acceptChild.getStatus(), "初验环节验收子行应为待检验");
        assertEquals(0, new BigDecimal("7.00").compareTo(acceptChild.getAmount()), "验收子行金额应重算");

        // 退货子行：数量0.300、状态待退货、原因初验不合格、原行序号追溯、金额重算
        assertEquals(0, new BigDecimal("0.300").compareTo(returnChild.getQuantity()), "退货子行数量应为退货数量");
        assertEquals("待退货", returnChild.getStatus(), "退货子行应为待退货");
        assertEquals("初验不合格", returnChild.getReturnReason(), "退货子行原因为初验不合格");
        assertEquals(acceptChild.getSeq(), returnChild.getOriginalSeq(), "退货子行应记录原行序号");
        assertTrue(returnChild.getSeq() > acceptChild.getSeq(), "退货子行序号应大于原行");
        assertEquals(0, new BigDecimal("3.00").compareTo(returnChild.getAmount()), "退货子行金额应重算");

        // 主单维持验收中（存在待检验行）且备注记录拆分
        AcceptanceOrder afterSplit = acceptanceOrderService.getAcceptanceById(id);
        assertEquals("验收中", afterSplit.getStatus(), "拆分后主单应为验收中");
        assertTrue(afterSplit.getRemark().contains("部分验收"), "备注应记录部分验收");
    }

    /**
     * 测试整单入库前置条件：明细未全部终结或无待入库时拒绝入库
     */
    @Test
    @Transactional
    public void testInboundPreconditions() {
        // 存在待检验行（未终结）时不可入库
        AcceptanceOrder acceptance = createAcceptance("验收中", null);
        Long id = acceptance.getAcceptanceId();
        acceptanceOrderService.inspect(id, null, true, "初验合格");
        RuntimeException ex1 = assertThrows(RuntimeException.class,
                () -> acceptanceOrderService.inbound(id, null));
        assertTrue(ex1.getMessage().contains("终结"), "未全部终结时应提示先推进明细");

        // 全部为待退货：主单派生为已结束（终态），拒绝入库
        AcceptanceOrder allFail = createAcceptance("验收中", null);
        acceptanceOrderService.inspect(allFail.getAcceptanceId(), null, false, "外观破损");
        assertEquals("已结束", acceptanceOrderService.getAcceptanceById(allFail.getAcceptanceId()).getStatus(),
                "全部明细待退货时主单应派生为已结束");
        RuntimeException ex2 = assertThrows(RuntimeException.class,
                () -> acceptanceOrderService.inbound(allFail.getAcceptanceId(), null));
        assertTrue(ex2.getMessage().contains("入库"), "已结束的验收单应拒绝入库");
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

        // 创建并流转到已入库（明细带批号）：初验→检验→整单入库
        AcceptanceOrder acceptance = createAcceptance("运输中", "TEST-BATCH-001");
        Long id = acceptance.getAcceptanceId();
        acceptanceOrderService.confirmArrival(id);
        acceptanceOrderService.inspect(id, null, true, "初验合格");
        acceptanceOrderService.qualityCheck(id, null, true, "合格");
        List<Long> inboundDetailIds = acceptanceOrderService.getDetailsByAcceptanceId(id).stream()
                .map(AcceptanceDetail::getDetailId).collect(java.util.stream.Collectors.toList());
        acceptanceOrderService.inbound(id, inboundDetailIds.stream().map(did -> {
            AcceptanceInboundRequest.Item item = new AcceptanceInboundRequest.Item();
            item.setDetailId(did);
            item.setProdUnitId(prodUnitId);
            return item;
        }).collect(java.util.stream.Collectors.toList()));

        // 已入库不可删除
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> acceptanceOrderService.deleteAcceptance(id));
        assertTrue(ex.getMessage().contains("已入库"), "异常信息应提示已入库不可删除");
    }

    /**
     * 测试已入库验收明细锁定：已入库明细不可再修改（批号/单价/仓库锁定）
     */
    @Test
    @Transactional
    public void testUpdateInboundDetailRejected() {
        // 创建验收单（明细状态待初验，批号为空）
        AcceptanceOrder acceptance = createAcceptance("验收中", null);
        Long id = acceptance.getAcceptanceId();
        List<AcceptanceDetail> details = acceptanceOrderService.getDetailsByAcceptanceId(id);
        AcceptanceDetail detail = details.get(0);
        Long detailId = detail.getDetailId();

        // 非已入库行仍可更新（含状态流转：待初验→已入库）
        AcceptanceDetail promote = new AcceptanceDetail();
        promote.setDetailId(detailId);
        promote.setBatchNumber("LOCK-BATCH-001");
        promote.setStatus("已入库");
        acceptanceOrderService.updateAcceptanceDetails(List.of(promote));
        assertEquals("已入库", acceptanceOrderService.getDetailsByAcceptanceId(id).get(0).getStatus(),
                "非已入库行应允许更新");

        // 已入库行再次更新任意字段应被整体拒绝
        AcceptanceDetail attempt = new AcceptanceDetail();
        attempt.setDetailId(detailId);
        attempt.setUnitPrice(new BigDecimal("99.00"));
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> acceptanceOrderService.updateAcceptanceDetails(List.of(attempt)));
        assertTrue(ex.getMessage().contains("已入库"), "异常信息应提示已入库锁定");
        assertTrue(ex.getMessage().contains("锁定"), "异常信息应提示锁定");
    }

    /**
     * 测试初验/检验必须明确指定合格结果（pass 不允许为空，空请求防护）
     */
    @Test
    @Transactional
    public void testInspectRequiresExplicitPass() {
        AcceptanceOrder acceptance = createAcceptance("验收中", null);
        Long id = acceptance.getAcceptanceId();

        RuntimeException exInspect = assertThrows(RuntimeException.class,
                () -> acceptanceOrderService.inspect(id, null, null, "无结果"));
        assertTrue(exInspect.getMessage().contains("必须明确指定检验结果"),
                "初验未指定pass应被拒绝，实际: " + exInspect.getMessage());

        RuntimeException exQuality = assertThrows(RuntimeException.class,
                () -> acceptanceOrderService.qualityCheck(id, null, null, "无结果"));
        assertTrue(exQuality.getMessage().contains("必须明确指定检验结果"),
                "检验未指定pass应被拒绝，实际: " + exQuality.getMessage());
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
