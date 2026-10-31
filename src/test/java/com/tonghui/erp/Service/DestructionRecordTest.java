package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Quality.DestructionRecordDetailSaveDto;
import com.tonghui.erp.Common.Dto.Quality.DestructionRecordSaveDto;
import com.tonghui.erp.Common.Dto.Quality.DestructionRecordWithDetailsDto;
import com.tonghui.erp.Common.Dto.Quality.RetainedSampleOptionDto;
import com.tonghui.erp.Data.Entity.DestructionRecord;
import com.tonghui.erp.Data.Entity.DestructionRecordDetail;
import com.tonghui.erp.Data.Entity.RetainedSample;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 销毁记录模块专项测试
 * <p>
 * 覆盖业务规则：
 * 1. 单号按记录类型选择前缀（BPXH/LYXH）自动生成
 * 2. 合计数量由服务端按明细求和计算
 * 3. 更新时明细整体重建
 * 4. 作废后禁止修改、禁止重复作废
 * 5. 留样销毁类型反写联动：登记置留样已销毁，移除引用/类型变更/作废回滚留样中
 * 6. 条件分页查询（批号经明细命中主表）
 * 7. 详情/列表明细行回填关联留样编号（retainedCode）
 * 8. 列表返回创建人姓名（createdByName）及明细列表（details）
 * 全部用例使用事务回滚，不污染数据库
 * </p>
 */
@SpringBootTest
@Transactional
public class DestructionRecordTest {

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    /**
     * 销毁记录服务
     */
    @Autowired
    private DestructionRecordService destructionRecordService;

    /**
     * 留样记录服务（创建测试关联数据）
     */
    @Autowired
    private RetainedSampleService retainedSampleService;

    /**
     * 用户Mapper（查询root用户ID用于显式回填创建人）
     */
    @Autowired
    private com.tonghui.erp.Data.mapper.UserMapper userMapper;

    /**
     * 销毁记录明细Mapper（构造"其他正常销毁单引用同一留样"的测试场景）
     */
    @Autowired
    private com.tonghui.erp.Data.mapper.DestructionRecordDetailMapper destructionRecordDetailMapper;

    // endregion

    // region 测试用例
    // ===================================
    // 测试用例
    // ===================================

    /**
     * 测试销毁单号生成：不合格品销毁BPXH前缀，留样销毁LYXH前缀
     */
    @Test
    public void testGenerateCodeByRecordType() {
        String unqualifiedCode = destructionRecordService.generateCode(DestructionRecord.TYPE_UNQUALIFIED);
        String retainedCode = destructionRecordService.generateCode(DestructionRecord.TYPE_RETAINED);
        assertTrue(unqualifiedCode.startsWith("BPXH-"), "不合格品销毁单号应以BPXH-开头");
        assertTrue(unqualifiedCode.matches("^BPXH-\\d{8}-\\d{3}$"),
                "不合格品销毁单号格式应为BPXH-YYYYMMDD-NNN：" + unqualifiedCode);
        assertTrue(retainedCode.startsWith("LYXH-"), "留样销毁单号应以LYXH-开头");
        assertTrue(retainedCode.matches("^LYXH-\\d{8}-\\d{3}$"),
                "留样销毁单号格式应为LYXH-YYYYMMDD-NNN：" + retainedCode);
        // 非法记录类型应抛异常
        assertThrows(RuntimeException.class, () -> destructionRecordService.generateCode("非法类型"),
                "非法记录类型应抛异常");
    }

    /**
     * 测试新增销毁记录：主表+明细一次保存，合计数量按明细求和
     */
    @Test
    public void testCreateWithDetailsAndTotalQuantity() {
        DestructionRecordSaveDto dto = buildSaveDto();
        DestructionRecord created = destructionRecordService.create(dto);
        assertNotNull(created.getId(), "新增后应有ID");
        assertNotNull(created.getDestructionCode(), "新增后应有自动生成的单号");
        assertTrue(created.getDestructionCode().startsWith("BPXH-"), "单号应以BPXH-开头");
        assertEquals(0, new BigDecimal("80.5").compareTo(created.getTotalQuantity()),
                "合计数量应为明细求和（50.5+30.0=80.5）");
        assertEquals(DestructionRecord.STATUS_NORMAL, created.getStatus(), "新建状态应为正常");

        // 明细已落库
        DestructionRecordWithDetailsDto detail = destructionRecordService.getDetail(created.getId());
        assertNotNull(detail, "详情应可查询");
        assertEquals(2, detail.getDetails().size(), "明细应有2行");
        assertEquals("维生素C", detail.getDetails().get(0).getMaterialName(), "明细品名应一致");
    }

    /**
     * 测试修改销毁记录：明细整体重建（旧行删除后按新列表重插），合计重算
     */
    @Test
    public void testUpdateRebuildsDetails() {
        DestructionRecord created = destructionRecordService.create(buildSaveDto());
        Long detailId1 = destructionRecordService.getDetail(created.getId()).getDetails().get(0).getId();

        // 修改为1行明细，数量变化
        DestructionRecordSaveDto dto = buildSaveDto();
        dto.getDetails().remove(1);
        dto.getDetails().get(0).setQuantity(new BigDecimal("66.666"));
        DestructionRecord updated = destructionRecordService.update(created.getId(), dto);
        assertEquals(0, new BigDecimal("66.666").compareTo(updated.getTotalQuantity()),
                "合计数量应按新明细重算");
        assertEquals(created.getDestructionCode(), updated.getDestructionCode(), "单号应保留原值");

        // 明细整体重建：旧明细ID应已物理删除，新明细为新ID
        DestructionRecordWithDetailsDto detail = destructionRecordService.getDetail(created.getId());
        assertEquals(1, detail.getDetails().size(), "明细应重建为1行");
        assertNotEquals(detailId1, detail.getDetails().get(0).getId(), "重建后明细ID应为新ID");
        assertEquals(0, new BigDecimal("66.666").compareTo(detail.getDetails().get(0).getQuantity()),
                "新明细数量应一致");
    }

    /**
     * 测试作废：作废后禁止修改、禁止重复作废
     */
    @Test
    public void testVoidBlocksEditAndDoubleVoid() {
        DestructionRecord created = destructionRecordService.create(buildSaveDto());
        destructionRecordService.voidRecord(created.getId(), "单号填写错误");

        DestructionRecord voided = destructionRecordService.getById(created.getId());
        assertEquals(DestructionRecord.STATUS_VOID, voided.getStatus(), "作废后状态应为作废");
        assertNotNull(voided.getVoidTime(), "作废时间应已记录");
        assertEquals("单号填写错误", voided.getVoidRemark(), "作废原因应一致");

        // 作废后禁止修改
        DestructionRecordSaveDto dto = buildSaveDto();
        assertThrows(RuntimeException.class, () -> destructionRecordService.update(created.getId(), dto),
                "已作废记录修改应抛异常");
        // 禁止重复作废
        assertThrows(RuntimeException.class, () -> destructionRecordService.voidRecord(created.getId(), "再次作废"),
                "重复作废应抛异常");
    }

    /**
     * 测试留样反写联动：留样销毁类型登记后，留样置为已销毁并写入销毁日期
     */
    @Test
    public void testRetainedSampleLinkedOnCreate() {
        // 创建测试留样（状态留样中）
        RetainedSample sample = createTestRetainedSample();
        Long sampleId = sample.getId();

        DestructionRecordSaveDto dto = buildSaveDto();
        dto.setRecordType(DestructionRecord.TYPE_RETAINED);
        LocalDate destroyDate = LocalDate.of(2026, 10, 11);
        dto.setDestroyDate(destroyDate);
        dto.getDetails().get(0).setRetainedSampleId(sampleId);
        DestructionRecord created = destructionRecordService.create(dto);
        assertTrue(created.getDestructionCode().startsWith("LYXH-"), "留样销毁单号应以LYXH-开头");

        // 留样应被反写为已销毁，销毁日期取销毁单上的销毁日期
        RetainedSample after = retainedSampleService.getById(sampleId);
        assertEquals(RetainedSample.STATUS_DESTROYED, after.getStatus(),
                "留样销毁类型登记后应反写留样为已销毁");
        assertEquals(destroyDate, after.getDestroyDate(), "留样销毁日期应取销毁单销毁日期");

        // 已销毁留样不应再出现在下拉中
        assertFalse(destructionRecordService.searchRetainedSamples(sample.getRetainedCode()).stream()
                        .anyMatch(o -> o.getId().equals(sampleId)),
                "已销毁留样不应出现在下拉中");

        // 详情明细行应回填关联留样编号retainedCode
        DestructionRecordWithDetailsDto detail = destructionRecordService.getDetail(created.getId());
        DestructionRecordDetail linked = detail.getDetails().stream()
                .filter(d -> sampleId.equals(d.getRetainedSampleId()))
                .findFirst()
                .orElse(null);
        assertNotNull(linked, "明细应包含引用留样的行");
        assertNotNull(linked.getRetainedCode(), "明细行应回填留样编号");
        assertEquals(sample.getRetainedCode(), linked.getRetainedCode(), "留样编号应与留样记录一致");

        // 列表项明细行同样应回填留样编号
        PageRequestDto pageRequest = new PageRequestDto();
        pageRequest.setPageIndex(-1);
        pageRequest.setPageSize(-1);
        PagedResult<DestructionRecordWithDetailsDto> result = destructionRecordService.search(
                null, created.getDestructionCode(), null, null, null, null, pageRequest);
        DestructionRecordWithDetailsDto listItem = result.getItems().stream()
                .filter(r -> r.getId().equals(created.getId()))
                .findFirst()
                .orElse(null);
        assertNotNull(listItem, "列表应命中新建记录");
        DestructionRecordDetail listLinked = listItem.getDetails().stream()
                .filter(d -> sampleId.equals(d.getRetainedSampleId()))
                .findFirst()
                .orElse(null);
        assertNotNull(listLinked, "列表项明细应包含引用留样的行");
        assertEquals(sample.getRetainedCode(), listLinked.getRetainedCode(),
                "列表项明细行应一致回填留样编号");
    }

    /**
     * 测试不合格品销毁类型不反写留样：即使明细传了留样ID，留样仍保持留样中
     */
    @Test
    public void testUnqualifiedTypeDoesNotLinkRetainedSample() {
        RetainedSample sample = createTestRetainedSample();
        DestructionRecordSaveDto dto = buildSaveDto();
        dto.getDetails().get(0).setRetainedSampleId(sample.getId());
        destructionRecordService.create(dto);

        RetainedSample after = retainedSampleService.getById(sample.getId());
        assertEquals(RetainedSample.STATUS_RETAINING, after.getStatus(),
                "不合格品销毁类型不应反写留样状态");
        assertNull(after.getDestroyDate(), "不合格品销毁类型不应写入留样销毁日期");
    }

    /**
     * 测试销毁日期兜底：销毁单未填销毁日期时，留样销毁日期取登记当天
     */
    @Test
    public void testLinkageDestroyDateFallbackToToday() {
        RetainedSample sample = createTestRetainedSample();
        DestructionRecordSaveDto dto = buildSaveDto();
        dto.setRecordType(DestructionRecord.TYPE_RETAINED);
        dto.setDestroyDate(null);
        dto.getDetails().get(0).setRetainedSampleId(sample.getId());
        destructionRecordService.create(dto);

        assertEquals(LocalDate.now(), retainedSampleService.getById(sample.getId()).getDestroyDate(),
                "销毁单未填销毁日期时应取登记当天");
    }

    /**
     * 测试修改时移除留样引用：留样回滚为留样中并清空销毁日期
     */
    @Test
    public void testRollbackWhenReferenceRemovedOnUpdate() {
        RetainedSample sample = createTestRetainedSample();
        DestructionRecordSaveDto createDto = buildSaveDto();
        createDto.setRecordType(DestructionRecord.TYPE_RETAINED);
        createDto.setDestroyDate(LocalDate.now());
        createDto.getDetails().get(0).setRetainedSampleId(sample.getId());
        DestructionRecord created = destructionRecordService.create(createDto);
        assertEquals(RetainedSample.STATUS_DESTROYED,
                retainedSampleService.getById(sample.getId()).getStatus(), "登记后留样应为已销毁");

        // 修改时不再引用该留样
        DestructionRecordSaveDto updateDto = buildSaveDto();
        updateDto.setRecordType(DestructionRecord.TYPE_RETAINED);
        updateDto.getDetails().remove(1);
        destructionRecordService.update(created.getId(), updateDto);

        RetainedSample after = retainedSampleService.getById(sample.getId());
        assertEquals(RetainedSample.STATUS_RETAINING, after.getStatus(), "移除留样引用后应回滚为留样中");
        assertNull(after.getDestroyDate(), "回滚后应清空留样销毁日期");
    }

    /**
     * 测试修改记录类型为不合格品销毁：原有留样引用全部回滚
     */
    @Test
    public void testRollbackWhenRecordTypeChangedToUnqualified() {
        RetainedSample sample = createTestRetainedSample();
        DestructionRecordSaveDto createDto = buildSaveDto();
        createDto.setRecordType(DestructionRecord.TYPE_RETAINED);
        createDto.getDetails().get(0).setRetainedSampleId(sample.getId());
        DestructionRecord created = destructionRecordService.create(createDto);

        // 类型改为不合格品销毁，明细仍保留留样ID引用但不联动
        DestructionRecordSaveDto updateDto = buildSaveDto();
        updateDto.getDetails().get(0).setRetainedSampleId(sample.getId());
        destructionRecordService.update(created.getId(), updateDto);

        RetainedSample after = retainedSampleService.getById(sample.getId());
        assertEquals(RetainedSample.STATUS_RETAINING, after.getStatus(),
                "类型改为不合格品销毁后应回滚留样状态");
        assertNull(after.getDestroyDate(), "类型改为不合格品销毁后应清空销毁日期");
    }

    /**
     * 测试销毁单作废：关联留样回滚为留样中并清空销毁日期
     */
    @Test
    public void testRollbackWhenVoided() {
        RetainedSample sample = createTestRetainedSample();
        DestructionRecordSaveDto dto = buildSaveDto();
        dto.setRecordType(DestructionRecord.TYPE_RETAINED);
        dto.getDetails().get(0).setRetainedSampleId(sample.getId());
        DestructionRecord created = destructionRecordService.create(dto);

        destructionRecordService.voidRecord(created.getId(), "登记错误");

        RetainedSample after = retainedSampleService.getById(sample.getId());
        assertEquals(RetainedSample.STATUS_RETAINING, after.getStatus(), "作废后应回滚留样为留样中");
        assertNull(after.getDestroyDate(), "作废后应清空留样销毁日期");
    }

    /**
     * 测试重复销毁校验：同一留样被第二张正常销毁单引用时应拒绝
     */
    @Test
    public void testDuplicateActiveReferenceRejected() {
        RetainedSample sample = createTestRetainedSample();
        DestructionRecordSaveDto first = buildSaveDto();
        first.setRecordType(DestructionRecord.TYPE_RETAINED);
        first.getDetails().get(0).setRetainedSampleId(sample.getId());
        destructionRecordService.create(first);

        DestructionRecordSaveDto second = buildSaveDto();
        second.setRecordType(DestructionRecord.TYPE_RETAINED);
        second.getDetails().get(0).setRetainedSampleId(sample.getId());
        assertThrows(RuntimeException.class, () -> destructionRecordService.create(second),
                "同一留样被多张正常销毁单引用应被拒绝");
    }

    /**
     * 测试仍被其他正常销毁单引用时不回滚：作废A后，留样因B仍正常引用而保持已销毁
     */
    @Test
    public void testNoRollbackWhenOtherActiveRecordReferences() {
        RetainedSample sample = createTestRetainedSample();
        DestructionRecordSaveDto dtoA = buildSaveDto();
        dtoA.setRecordType(DestructionRecord.TYPE_RETAINED);
        dtoA.getDetails().get(0).setRetainedSampleId(sample.getId());
        DestructionRecord recordA = destructionRecordService.create(dtoA);

        // 另建一张正常销毁单B，直接写入同留样引用（模拟历史数据/并发场景）
        DestructionRecordSaveDto dtoB = buildSaveDto();
        dtoB.setRecordType(DestructionRecord.TYPE_RETAINED);
        DestructionRecord recordB = destructionRecordService.create(dtoB);
        DestructionRecordDetail extraDetail = new DestructionRecordDetail();
        extraDetail.setDestructionId(recordB.getId());
        extraDetail.setMaterialName("测试留样品");
        extraDetail.setRetainedSampleId(sample.getId());
        destructionRecordDetailMapper.insert(extraDetail);

        // 作废A时B仍正常引用该留样，不应回滚
        destructionRecordService.voidRecord(recordA.getId(), "重复登记");
        assertEquals(RetainedSample.STATUS_DESTROYED,
                retainedSampleService.getById(sample.getId()).getStatus(),
                "仍被其他正常销毁单引用时不应回滚留样状态");
    }

    /**
     * 测试列表返回创建人姓名映射：显式设置创建人ID后，列表查询应回填创建人姓名
     */
    @Test
    public void testSearchReturnsCreatedByName() {
        DestructionRecord created = destructionRecordService.create(buildSaveDto());
        Long rootUserId = findRootUserId();
        assertNotNull(rootUserId, "应存在root默认账户");
        // 显式设置创建人ID（测试环境无登录上下文，MetaObjectHandler不填充）
        boolean updated = destructionRecordService.update(
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<DestructionRecord>()
                        .eq("id", created.getId())
                        .set("created_by", rootUserId));
        assertTrue(updated, "显式设置创建人ID应成功");

        PageRequestDto pageRequest = new PageRequestDto();
        pageRequest.setPageIndex(-1);
        pageRequest.setPageSize(-1);
        PagedResult<DestructionRecordWithDetailsDto> result = destructionRecordService.search(
                null, created.getDestructionCode(), null, null, null, null, pageRequest);
        DestructionRecordWithDetailsDto item = result.getItems().stream()
                .filter(r -> r.getId().equals(created.getId()))
                .findFirst()
                .orElse(null);
        assertNotNull(item, "列表应命中新建记录");
        assertNotNull(item.getCreatedByName(), "列表项应回填创建人姓名");
        assertFalse(item.getCreatedByName().isBlank(), "创建人姓名不应为空");
    }

    /**
     * 测试条件分页查询：批号经明细命中主表，状态过滤生效
     */
    @Test
    public void testSearchByBatchNoAndStatus() {
        DestructionRecordSaveDto dto = buildSaveDto();
        dto.getDetails().get(0).setBatchNo("BATCHSEARCH20261012");
        DestructionRecord created = destructionRecordService.create(dto);

        PageRequestDto pageRequest = new PageRequestDto();
        pageRequest.setPageIndex(0);
        pageRequest.setPageSize(10);
        // 按明细批号查询
        PagedResult<DestructionRecordWithDetailsDto> byBatchNo = destructionRecordService.search(
                null, null, "BATCHSEARCH20261012", null, null, null, pageRequest);
        assertTrue(byBatchNo.getItems().stream().anyMatch(r -> r.getId().equals(created.getId())),
                "按明细批号应命中主表");
        // 按状态查询（新建记录状态为正常）
        PagedResult<DestructionRecordWithDetailsDto> byStatus = destructionRecordService.search(
                null, null, null, DestructionRecord.STATUS_NORMAL, null, null, pageRequest);
        assertTrue(byStatus.getItems().stream().anyMatch(r -> r.getId().equals(created.getId())),
                "按状态正常应命中新建记录");
        // 按单号关键字查询
        PagedResult<DestructionRecordWithDetailsDto> byCode = destructionRecordService.search(
                null, created.getDestructionCode(), null, null, null, null, pageRequest);
        assertTrue(byCode.getItems().stream().anyMatch(r -> r.getId().equals(created.getId())),
                "按单号关键字应命中新建记录");
        // 列表项应携带明细列表
        DestructionRecordWithDetailsDto matched = byCode.getItems().stream()
                .filter(r -> r.getId().equals(created.getId()))
                .findFirst()
                .orElse(null);
        assertNotNull(matched, "列表应命中新建记录");
        assertNotNull(matched.getDetails(), "列表项应携带明细列表");
        assertEquals(2, matched.getDetails().size(), "列表项明细应有2行");
    }

    /**
     * 测试留样下拉查询：仅返回状态为留样中的记录
     */
    @Test
    public void testSearchRetainedSamples() {
        RetainedSample sample = createTestRetainedSample();
        List<RetainedSampleOptionDto> options =
                destructionRecordService.searchRetainedSamples(sample.getRetainedCode());
        assertTrue(options.stream().anyMatch(o -> o.getId().equals(sample.getId())),
                "按留样编号应命中下拉选项");
        // 已销毁的留样不应出现在下拉中
        sample.setStatus(RetainedSample.STATUS_DESTROYED);
        assertTrue(retainedSampleService.updateById(sample), "更新留样状态应成功");
        List<RetainedSampleOptionDto> afterDestroy =
                destructionRecordService.searchRetainedSamples(sample.getRetainedCode());
        assertFalse(afterDestroy.stream().anyMatch(o -> o.getId().equals(sample.getId())),
                "已销毁留样不应出现在下拉中");
    }

    /**
     * 测试必填校验：明细为空/明细品名缺失时应抛异常
     */
    @Test
    public void testRequiredValidation() {
        // 明细为空
        DestructionRecordSaveDto noDetails = buildSaveDto();
        noDetails.setDetails(null);
        assertThrows(RuntimeException.class, () -> destructionRecordService.create(noDetails),
                "明细为空应抛异常");
        // 明细品名缺失
        DestructionRecordSaveDto noMaterialName = buildSaveDto();
        noMaterialName.getDetails().get(0).setMaterialName(null);
        assertThrows(RuntimeException.class, () -> destructionRecordService.create(noMaterialName),
                "明细品名缺失应抛异常");
        // 非法记录类型
        DestructionRecordSaveDto badType = buildSaveDto();
        badType.setRecordType("不存在的类型");
        assertThrows(RuntimeException.class, () -> destructionRecordService.create(badType),
                "非法记录类型应抛异常");
    }

    // endregion

    // region 私有工具方法
    // ===================================
    // 私有工具方法
    // ===================================

    /**
     * 构建销毁记录保存请求体（必填字段齐全，2行明细，明细列表可变便于测试修改）
     *
     * @return 保存请求体
     */
    private DestructionRecordSaveDto buildSaveDto() {
        DestructionRecordSaveDto dto = new DestructionRecordSaveDto();
        dto.setRecordType(DestructionRecord.TYPE_UNQUALIFIED);
        dto.setDestroyReason("中间体含量不合格");
        dto.setDestroyMethod("交有资质单位高温焚烧");
        dto.setDestroyLocation("厂区危废暂存间");
        dto.setApplicantName("李四");
        dto.setApplyDate(LocalDate.now());

        DestructionRecordDetailSaveDto detail1 = new DestructionRecordDetailSaveDto();
        detail1.setMaterialName("维生素C");
        detail1.setSpec("食品级");
        detail1.setBatchNo("20260701");
        detail1.setQuantity(new BigDecimal("50.5"));
        DestructionRecordDetailSaveDto detail2 = new DestructionRecordDetailSaveDto();
        detail2.setMaterialName("淀粉");
        detail2.setSpec("药用");
        detail2.setBatchNo("20260615");
        detail2.setQuantity(new BigDecimal("30.0"));
        List<DestructionRecordDetailSaveDto> details = new java.util.ArrayList<>();
        details.add(detail1);
        details.add(detail2);
        dto.setDetails(details);
        return dto;
    }

    /**
     * 创建测试用留样记录（状态留样中）
     *
     * @return 新建的留样记录
     */
    private RetainedSample createTestRetainedSample() {
        RetainedSample sample = new RetainedSample();
        sample.setRetainedCode("LYT" + System.currentTimeMillis());
        sample.setMaterialName("测试留样品");
        sample.setBatchNo("20261012");
        sample.setRetainedQuantity("100g");
        sample.setRetainedDate(LocalDate.now());
        sample.setExpiryDate(LocalDate.now().plusYears(1));
        sample.setStorageLocation("留样室A架1");
        sample.setStatus(RetainedSample.STATUS_RETAINING);
        assertTrue(retainedSampleService.save(sample), "新增留样应成功");
        return sample;
    }

    /**
     * 查询root用户ID（系统初始化默认账户，账号root）
     *
     * @return root用户ID，找不到时返回null
     */
    private Long findRootUserId() {
        com.tonghui.erp.Data.Entity.User root = userMapper.selectOne(
                new LambdaQueryWrapper<com.tonghui.erp.Data.Entity.User>()
                        .eq(com.tonghui.erp.Data.Entity.User::getUserAccount, "root").last("LIMIT 1"));
        return root == null ? null : root.getUserId();
    }

    // endregion
}
