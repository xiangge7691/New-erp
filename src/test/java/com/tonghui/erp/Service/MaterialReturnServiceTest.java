package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Dto.MaterialReturn.MaterialResendRequestDto;
import com.tonghui.erp.Common.Dto.MaterialReturn.MaterialReturnItemDto;
import com.tonghui.erp.Common.Dto.MaterialReturn.MaterialReturnQueryDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.AcceptanceDetail;
import com.tonghui.erp.Data.Entity.AcceptanceOrder;
import com.tonghui.erp.Data.Entity.AcceptanceResendLog;
import com.tonghui.erp.Data.Entity.ProductionPlan;
import com.tonghui.erp.Data.mapper.AcceptanceResendLogMapper;
import com.tonghui.erp.Service.impl.SequenceServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
 * 物料退货管理服务集成测试
 * <p>
 * 覆盖退货明细行查询、退货（待退货→已退货）、取消（已退货→已取消）、
 * 重新发货（合并生成新验收单+原明细已重发+操作留痕）与非法状态流转校验。
 * 测试前通过幂等SQL脚本确保验收表存在，业务数据在事务结束后自动回滚
 * </p>
 */
@SpringBootTest
public class MaterialReturnServiceTest {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 物料退货管理服务
     */
    @Autowired
    private MaterialReturnService materialReturnService;

    /**
     * 验收单服务（创建测试验收单与流转）
     */
    @Autowired
    private AcceptanceOrderService acceptanceOrderService;

    /**
     * 重新发货留痕数据访问层
     */
    @Autowired
    private AcceptanceResendLogMapper acceptanceResendLogMapper;

    /**
     * 验收单数据访问层（测试内回填关联字段）
     */
    @Autowired
    private com.tonghui.erp.Data.mapper.AcceptanceOrderMapper acceptanceOrderMapper;

    /**
     * 生产计划数据访问层（造制剂名称回填测试数据）
     */
    @Autowired
    private com.tonghui.erp.Data.mapper.ProductionPlanMapper productionPlanMapper;

    /**
     * 序列号生成服务
     */
    @Autowired
    private SequenceServiceImpl sequenceService;

    /**
     * 数据源（用于执行幂等建表脚本）
     */
    @Autowired
    private DataSource dataSource;

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
     * 测试退货明细行查询：初验不合格后出现在退货列表，字段只读带出
     * <p>验收中 → 初验不合格（明细待退货、原因初验不合格、备注带出）→ 列表可查</p>
     */
    @Test
    @Transactional
    public void testSearchReturnItems() {
        AcceptanceOrder acceptance = createFailAcceptance("TEST-MR-001", "包装破损");

        MaterialReturnQueryDto query = new MaterialReturnQueryDto();
        query.setStatus("待退货");
        PagedResult<MaterialReturnItemDto> result = materialReturnService.searchItems(query);

        MaterialReturnItemDto item = findByAcceptanceCode(result, acceptance.getAcceptanceCode());
        assertNotNull(item, "初验不合格后应出现在退货列表");
        assertEquals("待退货", item.getStatus());
        assertEquals("初验不合格", item.getReturnReason(), "退货原因应由验收环节带出");
        assertEquals("包装破损", item.getReturnRemark(), "备注应由验收环节带出");
        assertEquals("TEST-MR-SUPPLIER", item.getSupplier(), "供应商应取验收明细供应商");
        assertEquals(0, new BigDecimal("1.500").compareTo(item.getReturnQty()), "退货数量应为采购数量");
        assertNull(item.getResendTime(), "未重新发货时重发时间为空");
    }

    /**
     * 测试退货：明细 待退货 → 已退货，重复退货与错误状态被拒绝
     */
    @Test
    @Transactional
    public void testReturnDetail() {
        AcceptanceOrder acceptance = createFailAcceptance("TEST-MR-002", "外观不良");
        Long detailId = acceptanceOrderService.getDetailsByAcceptanceId(acceptance.getAcceptanceId())
                .get(0).getDetailId();

        materialReturnService.returnDetail(detailId);
        assertEquals("已退货", acceptanceOrderService.getDetailsByAcceptanceId(acceptance.getAcceptanceId())
                .get(0).getStatus(), "退货后明细应为已退货");

        // 重复退货应被拒绝
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> materialReturnService.returnDetail(detailId));
        assertTrue(ex.getMessage().contains("待退货"), "重复退货应提示仅待退货可执行");
    }

    /**
     * 测试取消：已退货 → 已取消，待退货明细不可直接取消
     */
    @Test
    @Transactional
    public void testCancelDetail() {
        AcceptanceOrder acceptance = createFailAcceptance("TEST-MR-003", "数量短少");
        Long detailId = acceptanceOrderService.getDetailsByAcceptanceId(acceptance.getAcceptanceId())
                .get(0).getDetailId();

        // 待退货状态不可直接取消
        RuntimeException before = assertThrows(RuntimeException.class,
                () -> materialReturnService.cancelDetail(detailId));
        assertTrue(before.getMessage().contains("已退货"), "取消前应先退货");

        materialReturnService.returnDetail(detailId);
        materialReturnService.cancelDetail(detailId);
        assertEquals("已取消", acceptanceOrderService.getDetailsByAcceptanceId(acceptance.getAcceptanceId())
                .get(0).getStatus(), "取消后明细应为已取消");
    }

    /**
     * 测试重新发货：已退货明细生成新验收单（退货重发/运输中/数量重算），
     * 原明细已重发、写入操作留痕、原单备注记录去向
     */
    @Test
    @Transactional
    public void testResend() {
        AcceptanceOrder acceptance = createFailAcceptance("TEST-MR-004", "检验不合格");
        List<AcceptanceDetail> originalDetails =
                acceptanceOrderService.getDetailsByAcceptanceId(acceptance.getAcceptanceId());
        Long detailId = originalDetails.get(0).getDetailId();
        materialReturnService.returnDetail(detailId);

        MaterialResendRequestDto request = new MaterialResendRequestDto();
        request.setAcceptanceId(acceptance.getAcceptanceId());
        MaterialResendRequestDto.ResendItem item = new MaterialResendRequestDto.ResendItem();
        item.setDetailId(detailId);
        item.setResendQty(new BigDecimal("2.000"));
        request.setItems(List.of(item));

        AcceptanceOrder newAcceptance = materialReturnService.resend(request);

        // 新单：来源类型=退货重发、状态=运输中、记录原验收单号、关联采购订单号沿用
        assertNotNull(newAcceptance.getAcceptanceId(), "应生成新验收单");
        assertEquals("退货重发", newAcceptance.getSourceType(), "来源类型应为退货重发");
        assertEquals("运输中", newAcceptance.getStatus(), "新单初始状态应为运输中");
        assertEquals(acceptance.getAcceptanceCode(), newAcceptance.getOriginalAcceptanceCode(), "应记录原验收单号");
        assertTrue(newAcceptance.getRemark().contains("发货数量 2.000"), "备注应追加发货数量");

        // 新单明细：采购数量=输入的重新发货数量、状态待初验、金额重算=2.000×10.00
        List<AcceptanceDetail> newDetails =
                acceptanceOrderService.getDetailsByAcceptanceId(newAcceptance.getAcceptanceId());
        assertEquals(1, newDetails.size(), "新单明细数应与所选明细一致");
        AcceptanceDetail newDetail = newDetails.get(0);
        assertEquals(0, new BigDecimal("2.000").compareTo(newDetail.getQuantity()), "新单采购数量应为重新发货数量");
        assertEquals("待初验", newDetail.getStatus(), "新单明细应为待初验");
        assertEquals(0, new BigDecimal("20.00").compareTo(newDetail.getAmount()), "金额应按新数量重算");

        // 原明细已重发
        assertEquals("已重发", acceptanceOrderService.getDetailsByAcceptanceId(acceptance.getAcceptanceId())
                .get(0).getStatus(), "原明细应为已重发");

        // 操作留痕
        List<AcceptanceResendLog> logs = acceptanceResendLogMapper.selectList(
                new QueryWrapper<AcceptanceResendLog>().eq("detail_id", detailId));
        assertEquals(1, logs.size(), "应写入一条重新发货留痕");
        assertEquals(0, new BigDecimal("2.000").compareTo(logs.get(0).getResendQty()), "留痕数量应为重新发货数量");
        assertEquals(newAcceptance.getAcceptanceCode(), logs.get(0).getNewAcceptanceCode(), "留痕应记录新验收单号");
        assertNotNull(logs.get(0).getOperationTime(), "留痕应记录操作时间");

        // 已重发的明细不可再次重新发货
        MaterialResendRequestDto again = new MaterialResendRequestDto();
        again.setAcceptanceId(acceptance.getAcceptanceId());
        again.setItems(List.of(item));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> materialReturnService.resend(again));
        assertTrue(ex.getMessage().contains("已退货"), "已重发明细不可再次重新发货");
    }

    /**
     * 测试重新发货校验：数量必须大于0、待退货明细不可重新发货
     */
    @Test
    @Transactional
    public void testResendValidation() {
        AcceptanceOrder acceptance = createFailAcceptance("TEST-MR-006", "含量不合格");
        Long detailId = acceptanceOrderService.getDetailsByAcceptanceId(acceptance.getAcceptanceId())
                .get(0).getDetailId();

        // 待退货（未先退货）不可重新发货
        MaterialResendRequestDto pending = new MaterialResendRequestDto();
        pending.setAcceptanceId(acceptance.getAcceptanceId());
        MaterialResendRequestDto.ResendItem item = new MaterialResendRequestDto.ResendItem();
        item.setDetailId(detailId);
        item.setResendQty(BigDecimal.ONE);
        pending.setItems(List.of(item));
        RuntimeException ex1 = assertThrows(RuntimeException.class,
                () -> materialReturnService.resend(pending));
        assertTrue(ex1.getMessage().contains("已退货"), "待退货明细不可重新发货");

        // 数量必须大于0
        materialReturnService.returnDetail(detailId);
        item.setResendQty(BigDecimal.ZERO);
        RuntimeException ex2 = assertThrows(RuntimeException.class,
                () -> materialReturnService.resend(pending));
        assertTrue(ex2.getMessage().contains("大于0"), "重新发货数量必须大于0");
    }

/**
     * 测试退货来源区分：采购入库与领料入库来源的退货行均返回，来源值正确
     */
    @Test
    @Transactional
    public void testSearchDistinguishSources() {
        createFailAcceptance("TEST-MR-SRC-001", "采购来源退货", "采购入库");
        createFailAcceptance("TEST-MR-SRC-002", "领料来源退货", "领料入库");

        MaterialReturnQueryDto query = new MaterialReturnQueryDto();
        PagedResult<MaterialReturnItemDto> result = materialReturnService.searchItems(query);

        MaterialReturnItemDto purchase = findByAcceptanceCode(result, "TEST-MR-SRC-001");
        MaterialReturnItemDto requisition = findByAcceptanceCode(result, "TEST-MR-SRC-002");
        assertNotNull(purchase, "采购来源退货应出现在列表");
        assertNotNull(requisition, "领料来源退货应出现在列表");
        assertEquals("采购退货", purchase.getReturnSource(), "采购入库来源应为采购退货");
        assertEquals("领料退货", requisition.getReturnSource(), "领料入库来源应为领料退货");
    }

    /**
     * 测试按退货来源筛选：采购退货仅返回采购来源行
     */
    @Test
    @Transactional
    public void testSearchFilterByPurchaseSource() {
        createFailAcceptance("TEST-MR-PUR-001", "采购来源退货", "采购入库");
        createFailAcceptance("TEST-MR-PUR-002", "领料来源退货", "领料入库");

        MaterialReturnQueryDto query = new MaterialReturnQueryDto();
        query.setReturnSource("采购退货");
        PagedResult<MaterialReturnItemDto> result = materialReturnService.searchItems(query);

        assertNotNull(findByAcceptanceCode(result, "TEST-MR-PUR-001"), "采购退货筛选应命中采购来源");
        assertNull(findByAcceptanceCode(result, "TEST-MR-PUR-002"), "采购退货筛选应排除领料来源");
    }

    /**
     * 测试按退货来源筛选：领料退货仅返回领料来源行
     */
    @Test
    @Transactional
    public void testSearchFilterByRequisitionSource() {
        createFailAcceptance("TEST-MR-REQ-001", "采购来源退货", "采购入库");
        createFailAcceptance("TEST-MR-REQ-002", "领料来源退货", "领料入库");

        MaterialReturnQueryDto query = new MaterialReturnQueryDto();
        query.setReturnSource("领料退货");
        PagedResult<MaterialReturnItemDto> result = materialReturnService.searchItems(query);

        assertNull(findByAcceptanceCode(result, "TEST-MR-REQ-001"), "领料退货筛选应排除采购来源");
        assertNotNull(findByAcceptanceCode(result, "TEST-MR-REQ-002"), "领料退货筛选应命中领料来源");
    }

    /**
     * 测试制剂名称回填：验收单未存制剂名称时按 plan_code 关联 production_plan 推导
     * <p>模拟领料/未回填场景：验收单仅带 planCode，列表 preparationName 应取生产计划制剂名称</p>
     */
    @Test
    @Transactional
    public void testSearchFillsPreparationNameFromPlan() {
        // 造生产计划主数据（先删后插防历史残留，测试事务结束自动回滚）
        String planNumber = "TEST-MR-PLAN-001";
        productionPlanMapper.delete(new QueryWrapper<ProductionPlan>().eq("plan_number", planNumber));
        ProductionPlan plan = new ProductionPlan();
        plan.setPlanNumber(planNumber);
        plan.setPlanName("物料退货制剂回填测试计划");
        plan.setPreparationCode("TESTP1");
        plan.setPreparationName("回填测试制剂");
        plan.setPlanQuantity(new BigDecimal("1"));
        plan.setPlanType("生产计划");
        productionPlanMapper.insert(plan);

        // 验收单：带 planCode、不存制剂名称
        AcceptanceOrder acceptance = createFailAcceptance("TEST-MR-PREP-001", "制剂回填测试");
        acceptance.setPlanCode(planNumber);
        acceptance.setPreparationName(null);
        acceptanceOrderMapper.updateById(acceptance);

        MaterialReturnQueryDto query = new MaterialReturnQueryDto();
        PagedResult<MaterialReturnItemDto> result = materialReturnService.searchItems(query);
        MaterialReturnItemDto row = findByAcceptanceCode(result, "TEST-MR-PREP-001");

        assertNotNull(row, "退货行应存在");
        assertEquals("回填测试制剂", row.getPreparationName(), "制剂名称应按 plan_code 关联生产计划回填");
    }

    /**
     * 测试领料来源退货重新发货：生成退货重发验收单，不抛异常且关联领料单号
     */
    @Test
    @Transactional
    public void testRequisitionResend() {
        AcceptanceOrder acceptance = createFailAcceptance("TEST-MR-RESEND-001", "领料来源退货", "领料入库");
        // 领料来源验收单无采购订单编号，resend 应回退关联领料单号
        acceptance.setPurchaseNumber(null);
        acceptanceOrderService.updateById(acceptance);
        Long detailId = acceptanceOrderService.getDetailsByAcceptanceId(acceptance.getAcceptanceId())
                .get(0).getDetailId();
        materialReturnService.returnDetail(detailId);

        MaterialResendRequestDto request = new MaterialResendRequestDto();
        request.setAcceptanceId(acceptance.getAcceptanceId());
        MaterialResendRequestDto.ResendItem item = new MaterialResendRequestDto.ResendItem();
        item.setDetailId(detailId);
        item.setResendQty(new BigDecimal("1.000"));
        request.setItems(List.of(item));

        AcceptanceOrder newAcceptance = materialReturnService.resend(request);
        assertEquals("退货重发", newAcceptance.getSourceType(), "领料退货重新发货来源类型应为退货重发");
        assertEquals("运输中", newAcceptance.getStatus(), "重新发货新验收单初始状态应为运输中");
        assertEquals("TEST-MR-ORDER-TEST-MR-RESEND-001", newAcceptance.getRelatedOrder(),
                "关联单号应沿用领料单号（回退relatedOrder）");
        assertEquals("已重发", acceptanceOrderService.getDetailsByAcceptanceId(acceptance.getAcceptanceId())
                .get(0).getStatus(), "原领料明细应已重发");
    }

    // endregion

    // region 私有工具方法
    // ===================================
    // 私有工具方法
    // ===================================

    /**
     * 创建"验收中"的验收单（明细待退货），随后初验不合格使明细进入待退货
     *
     * @param codePrefix 验收单号前缀（区分测试数据）
     * @param remark     初验备注（作为退货备注带出）
     * @return 明细已处于待退货的验收单
     */
    private AcceptanceOrder createFailAcceptance(String codePrefix, String remark) {
        return createFailAcceptance(codePrefix, remark, "采购入库");
    }

    /**
     * 创建"验收中"的验收单（明细待退货），支持指定来源类型
     *
     * @param codePrefix 验收单号前缀（区分测试数据）
     * @param remark     初验备注（作为退货备注带出）
     * @param sourceType 验收单来源类型（采购入库/领料入库等）
     * @return 明细已处于待退货的验收单
     */
    private AcceptanceOrder createFailAcceptance(String codePrefix, String remark, String sourceType) {
        AcceptanceOrder acceptance = new AcceptanceOrder();
        acceptance.setAcceptanceCode(codePrefix);
        acceptance.setSourceType(sourceType);
        acceptance.setStatus("验收中");
        acceptance.setRelatedOrder("TEST-MR-ORDER-" + codePrefix);
        acceptance.setPurchaseNumber("TEST-MR-ORDER-" + codePrefix);
        acceptance.setDeliveryDate(LocalDate.now().plusDays(7));

        AcceptanceDetail detail = new AcceptanceDetail();
        detail.setItemType("material");
        detail.setMaterialCode("TEST-MR-001");
        detail.setMaterialName("测试物料");
        detail.setMaterialCategory("原料");
        detail.setUnitName("kg");
        detail.setQuantity(new BigDecimal("1.500"));
        detail.setUnitPrice(new BigDecimal("10.00"));
        detail.setSupplier("TEST-MR-SUPPLIER");
        detail.setStatus("待退货");
        detail.setReturnReason("初验不合格");
        detail.setReturnRemark(remark);
        acceptanceOrderService.addAcceptance(acceptance, List.of(detail));
        return acceptance;
    }

    /**
     * 在分页结果中按验收单号查找行
     *
     * @param result         分页结果
     * @param acceptanceCode 验收单号
     * @return 匹配的行，未找到返回null
     */
    private MaterialReturnItemDto findByAcceptanceCode(PagedResult<MaterialReturnItemDto> result,
                                                       String acceptanceCode) {
        return result.getItems().stream()
                .filter(i -> acceptanceCode.equals(i.getAcceptanceCode()))
                .findFirst().orElse(null);
    }

    // endregion
}
