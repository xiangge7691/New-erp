package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Quality.DeviationRecordSaveDto;
import com.tonghui.erp.Common.Dto.Quality.DeviationRecordWithAttachmentsDto;
import com.tonghui.erp.Common.Dto.Quality.WorkOrderOptionDto;
import com.tonghui.erp.Data.Entity.DeviationRecord;
import com.tonghui.erp.Data.Entity.WorkOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 偏差记录模块专项测试
 * <p>
 * 覆盖业务规则：
 * 1. 偏差编号PC-YYYYMMDD-NNN自动生成且格式正确
 * 2. 保存时从生产任务表快照任务编号/制剂名称/批号
 * 3. 修改时编号保留原值
 * 4. 必填校验（生产任务/标题/发生时间/描述）
 * 5. 条件分页查询（关键字/生产任务编号）
 * 6. 生产任务下拉查询
 * 7. 列表返回创建人姓名（createdByName）
 * 全部用例使用事务回滚，不污染数据库
 * </p>
 */
@SpringBootTest
@Transactional
public class DeviationRecordTest {

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    /**
     * 偏差记录服务
     */
    @Autowired
    private DeviationRecordService deviationRecordService;

    /**
     * 生产任务服务（创建测试关联数据）
     */
    @Autowired
    private WorkOrderService workOrderService;

    /**
     * 制剂服务（创建测试用工单必依赖的制剂数据）
     */
    @Autowired
    private PreparationService preparationService;

    /**
     * 用户Mapper（查询root用户ID用于显式回填创建人）
     */
    @Autowired
    private com.tonghui.erp.Data.mapper.UserMapper userMapper;

    // endregion

    // region 测试用例
    // ===================================
    // 测试用例
    // ===================================

    /**
     * 测试偏差编号自动生成：格式PC-YYYYMMDD-NNN，连续生成返回相同序号（未落库不递增）
     */
    @Test
    public void testGenerateCode() {
        String code1 = deviationRecordService.generateCode();
        String code2 = deviationRecordService.generateCode();
        assertNotNull(code1, "生成的偏差编号不应为空");
        assertTrue(code1.startsWith("PC-"), "偏差编号应以PC-开头");
        assertTrue(code1.matches("^PC-\\d{8}-\\d{3}$"), "偏差编号格式应为PC-YYYYMMDD-NNN：" + code1);
        // 未落库时两次生成基于同一最大值，编号相同
        assertEquals(code1, code2, "未落库时连续生成的编号应相同");
    }

    /**
     * 测试新增偏差记录：自动生成编号并快照生产任务字段
     */
    @Test
    public void testCreateWithWorkOrderSnapshot() {
        WorkOrder workOrder = createTestWorkOrder();
        DeviationRecordSaveDto dto = buildSaveDto(workOrder.getWorkOrderId());
        dto.setDeviationTitle("混合工序装量偏差");
        dto.setProcessName("混合");

        DeviationRecord record = deviationRecordService.create(dto);
        assertNotNull(record.getId(), "新增后应有ID");
        assertNotNull(record.getDeviationCode(), "新增后应有自动生成的编号");
        assertTrue(record.getDeviationCode().startsWith("PC-"), "编号应以PC-开头");
        assertEquals("混合工序装量偏差", record.getDeviationTitle(), "标题应一致");
        // 生产任务快照字段应已带出
        assertEquals(workOrder.getWorkOrderCode(), record.getWorkOrderCode(), "任务编号快照应一致");
        assertEquals(workOrder.getPreparationName(), record.getPreparationName(), "制剂名称快照应一致");
        assertEquals(workOrder.getBatchNumber(), record.getBatchNumber(), "批号快照应一致");
    }

    /**
     * 测试修改偏差记录：编号保留原值，标题可更新
     */
    @Test
    public void testUpdateKeepsCode() {
        WorkOrder workOrder = createTestWorkOrder();
        DeviationRecordSaveDto dto = buildSaveDto(workOrder.getWorkOrderId());
        DeviationRecord created = deviationRecordService.create(dto);
        String originalCode = created.getDeviationCode();

        dto.setDeviationTitle("修订后的偏差标题");
        DeviationRecord updated = deviationRecordService.update(created.getId(), dto);
        assertEquals(originalCode, updated.getDeviationCode(), "修改后编号应保留原值");
        assertEquals("修订后的偏差标题", updated.getDeviationTitle(), "修改后标题应更新");
    }

    /**
     * 测试必填校验：生产任务/标题/发生时间/描述缺失时应抛异常
     */
    @Test
    public void testRequiredValidation() {
        WorkOrder workOrder = createTestWorkOrder();
        // 缺生产任务
        DeviationRecordSaveDto noWorkOrder = buildSaveDto(null);
        assertThrows(RuntimeException.class, () -> deviationRecordService.create(noWorkOrder),
                "缺生产任务应抛异常");
        // 缺标题
        DeviationRecordSaveDto noTitle = buildSaveDto(workOrder.getWorkOrderId());
        noTitle.setDeviationTitle(null);
        assertThrows(RuntimeException.class, () -> deviationRecordService.create(noTitle),
                "缺标题应抛异常");
        // 缺发生时间
        DeviationRecordSaveDto noOccurTime = buildSaveDto(workOrder.getWorkOrderId());
        noOccurTime.setOccurTime(null);
        assertThrows(RuntimeException.class, () -> deviationRecordService.create(noOccurTime),
                "缺发生时间应抛异常");
        // 缺描述
        DeviationRecordSaveDto noDescription = buildSaveDto(workOrder.getWorkOrderId());
        noDescription.setDescription(null);
        assertThrows(RuntimeException.class, () -> deviationRecordService.create(noDescription),
                "缺描述应抛异常");
    }

    /**
     * 测试条件分页查询：关键字模糊匹配编号/标题，生产任务编号模糊匹配，列表项含创建人姓名
     */
    @Test
    public void testSearch() {
        WorkOrder workOrder = createTestWorkOrder();
        DeviationRecordSaveDto dto = buildSaveDto(workOrder.getWorkOrderId());
        dto.setDeviationTitle("搜索专用偏差标题XYZ");
        DeviationRecord created = deviationRecordService.create(dto);

        PageRequestDto pageRequest = new PageRequestDto();
        pageRequest.setPageIndex(0);
        pageRequest.setPageSize(10);
        // 按标题关键字查询
        PagedResult<DeviationRecordWithAttachmentsDto> byKeyword = deviationRecordService.search(
                "搜索专用偏差标题XYZ", null, null, null, pageRequest);
        assertTrue(byKeyword.getItems().stream().anyMatch(r -> r.getId().equals(created.getId())),
                "按标题关键字应命中新建记录");
        // 按编号关键字查询
        PagedResult<DeviationRecordWithAttachmentsDto> byCode = deviationRecordService.search(
                created.getDeviationCode(), null, null, null, pageRequest);
        assertTrue(byCode.getItems().stream().anyMatch(r -> r.getId().equals(created.getId())),
                "按编号关键字应命中新建记录");
        // 按生产任务编号查询
        PagedResult<DeviationRecordWithAttachmentsDto> byWorkOrder = deviationRecordService.search(
                null, workOrder.getWorkOrderCode(), null, null, pageRequest);
        assertTrue(byWorkOrder.getItems().stream().anyMatch(r -> r.getId().equals(created.getId())),
                "按生产任务编号应命中新建记录");
    }

    /**
     * 测试列表返回创建人姓名映射：显式设置创建人ID后，列表查询应回填创建人姓名
     */
    @Test
    public void testSearchReturnsCreatedByName() {
        WorkOrder workOrder = createTestWorkOrder();
        DeviationRecord created = deviationRecordService.create(buildSaveDto(workOrder.getWorkOrderId()));
        Long rootUserId = findRootUserId();
        // 显式设置创建人ID（测试环境无登录上下文，MetaObjectHandler不填充）
        boolean updated = deviationRecordService.update(
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<DeviationRecord>()
                        .eq("id", created.getId())
                        .set("created_by", rootUserId));
        assertTrue(updated, "显式设置创建人ID应成功");

        PageRequestDto pageRequest = new PageRequestDto();
        pageRequest.setPageIndex(-1);
        pageRequest.setPageSize(-1);
        PagedResult<DeviationRecordWithAttachmentsDto> result = deviationRecordService.search(
                created.getDeviationCode(), null, null, null, pageRequest);
        DeviationRecordWithAttachmentsDto item = result.getItems().stream()
                .filter(r -> r.getId().equals(created.getId()))
                .findFirst()
                .orElse(null);
        assertNotNull(item, "列表应命中新建记录");
        assertNotNull(item.getCreatedByName(), "列表项应回填创建人姓名");
        assertFalse(item.getCreatedByName().isBlank(), "创建人姓名不应为空");
    }

    /**
     * 测试生产任务下拉查询：关键字模糊匹配任务编号/制剂名称
     */
    @Test
    public void testSearchWorkOrders() {
        WorkOrder workOrder = createTestWorkOrder();
        List<WorkOrderOptionDto> options = deviationRecordService.searchWorkOrders(workOrder.getWorkOrderCode());
        assertTrue(options.stream().anyMatch(o -> o.getWorkOrderId().equals(workOrder.getWorkOrderId())),
                "按任务编号应命中下拉选项");
        assertTrue(options.stream().anyMatch(o -> o.getWorkOrderId().equals(workOrder.getWorkOrderId())
                        && o.getPreparationName() != null),
                "下拉选项应包含制剂名称");
    }

    /**
     * 测试删除偏差记录：软删除后详情查询不可见
     */
    @Test
    public void testDelete() {
        WorkOrder workOrder = createTestWorkOrder();
        DeviationRecord created = deviationRecordService.create(buildSaveDto(workOrder.getWorkOrderId()));
        assertTrue(deviationRecordService.removeById(created.getId()), "软删除应成功");
        assertNull(deviationRecordService.getDetail(created.getId()), "删除后详情应不可见");
    }

    // endregion

    // region 私有工具方法
    // ===================================
    // 私有工具方法
    // ===================================

    /**
     * 创建测试用生产任务（先建制剂再建工单，避免依赖既有基础数据）
     *
     * @return 新建的生产任务
     */
    private WorkOrder createTestWorkOrder() {
        // 先创建测试制剂（工单的preparation_id非空必填）
        com.tonghui.erp.Data.Entity.Preparation preparation = new com.tonghui.erp.Data.Entity.Preparation();
        preparation.setPreparationCode("DEVPREP" + System.currentTimeMillis());
        preparation.setPreparationName("偏差测试制剂");
        preparation.setUnitName("kg");
        preparation.setStatus(1);
        preparationService.addPreparation(preparation);

        WorkOrder workOrder = new WorkOrder();
        workOrder.setWorkOrderCode("GDDEV" + System.currentTimeMillis());
        workOrder.setWorkOrderName("偏差测试工单");
        workOrder.setPreparationId(preparation.getPreparationId());
        workOrder.setBatchNumber("2026" + (System.currentTimeMillis() % 10000));
        workOrderService.addWorkOrder(workOrder);
        return workOrder;
    }

    /**
     * 构建偏差记录保存请求体（必填字段齐全）
     *
     * @param workOrderId 生产任务ID
     * @return 保存请求体
     */
    private DeviationRecordSaveDto buildSaveDto(Long workOrderId) {
        DeviationRecordSaveDto dto = new DeviationRecordSaveDto();
        dto.setWorkOrderId(workOrderId);
        dto.setDeviationTitle("测试偏差标题");
        dto.setOccurTime(LocalDateTime.now());
        dto.setDescription("测试偏差描述：生产过程中发现异常");
        return dto;
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
