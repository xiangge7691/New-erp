package com.tonghui.erp.Service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tonghui.erp.Common.Dto.Log.BusinessHistoryDto;
import com.tonghui.erp.Common.Dto.Log.BusinessHistoryEventDto;
import com.tonghui.erp.Common.Dto.Log.BusinessHistoryScope;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Data.Entity.SysDataChangeLog;
import com.tonghui.erp.Data.Entity.SysOperationLog;
import com.tonghui.erp.Data.mapper.SysDataChangeLogMapper;
import com.tonghui.erp.Data.mapper.SysOperationLogMapper;
import com.tonghui.erp.Service.businesshistory.BusinessHistoryProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 业务变更历史服务单元测试
 * <p>
 * 使用 Mockito 覆盖事件归组、字段统计、字段/来源筛选与分页，不依赖数据库
 * </p>
 */
public class BusinessHistoryServiceImplTest {

    // region 测试依赖
    // ===================================
    // 测试依赖
    // ===================================

    private SysDataChangeLogMapper changeLogMapper;
    private SysOperationLogMapper operationLogMapper;
    private BusinessHistoryProvider provider;
    private BusinessHistoryServiceImpl service;

    /**
     * 初始化 Mock 与默认查询范围
     */
    @BeforeEach
    public void setUp() {
        changeLogMapper = mock(SysDataChangeLogMapper.class);
        operationLogMapper = mock(SysOperationLogMapper.class);
        provider = mock(BusinessHistoryProvider.class);

        BusinessHistoryScope scope = new BusinessHistoryScope();
        scope.setTitle("WO-20261110-008 · 理伤续断丸生产任务");
        scope.addTable("work_order", List.of("880"));

        when(provider.bizType()).thenReturn("WORK_ORDER");
        when(provider.resolve("880")).thenReturn(scope);
        when(operationLogMapper.selectBriefByIds(anyCollection())).thenReturn(List.of());

        service = new BusinessHistoryServiceImpl(
                List.of(provider), changeLogMapper, operationLogMapper, new ObjectMapper());
    }

    // endregion

    // region 归组与统计
    // ===================================
    // 归组与统计
    // ===================================

    /**
     * 同一操作日志下的多个字段变更应合并为一个事件
     */
    @Test
    public void testGroupChangesByOperationLog() {
        SysDataChangeLog spec = change(1L, 100L, "2026-11-10 10:05:33",
                "[{\"field\":\"spec\",\"label\":\"规格\",\"before\":\"A\",\"after\":\"B\"}]");
        SysDataChangeLog batch = change(2L, 100L, "2026-11-10 10:05:33",
                "[{\"field\":\"batch_qty\",\"label\":\"批量\",\"before\":\"1200\",\"after\":\"1500\"}]");
        when(changeLogMapper.selectHistory(eq("work_order"), anyList(), isNull(), isNull(), anyInt()))
                .thenReturn(List.of(spec, batch));

        SysOperationLog operationLog = operationLog(100L, "修改工单");
        when(operationLogMapper.selectBriefByIds(anyCollection())).thenReturn(List.of(operationLog));

        BusinessHistoryDto result = service.query("WORK_ORDER", "880", null, null, null, null, page(0, 20));

        assertEquals(1, result.getEvents().size());
        BusinessHistoryEventDto event = result.getEvents().get(0);
        assertEquals(100L, event.getOperationLogId());
        assertEquals("修改工单", event.getOperationDesc());
        assertEquals("张伟", event.getUserName());
        assertEquals("00-1A-2B-3C-4D-5E", event.getMacAddress());
        assertEquals(2, event.getChanges().size());
        assertEquals(2, result.getFieldStats().size());
    }

    // endregion

    // region 筛选与分页
    // ===================================
    // 筛选与分页
    // ===================================

    /**
     * 字段筛选只保留匹配字段，但字段统计仍返回全部字段
     */
    @Test
    public void testFieldFilter() {
        SysDataChangeLog spec = change(1L, 100L, "2026-11-10 10:05:33",
                "[{\"field\":\"spec\",\"label\":\"规格\",\"before\":\"A\",\"after\":\"B\"}]");
        SysDataChangeLog batch = change(2L, 100L, "2026-11-10 10:05:33",
                "[{\"field\":\"batch_qty\",\"label\":\"批量\",\"before\":\"1200\",\"after\":\"1500\"}]");
        when(changeLogMapper.selectHistory(eq("work_order"), anyList(), isNull(), isNull(), anyInt()))
                .thenReturn(List.of(spec, batch));

        BusinessHistoryDto result = service.query("WORK_ORDER", "880", "spec", null, null, null, page(0, 20));

        assertEquals(1, result.getEvents().size());
        assertEquals(1, result.getEvents().get(0).getChanges().size());
        assertEquals("spec", result.getEvents().get(0).getChanges().get(0).getField());
        assertEquals(2, result.getFieldStats().size());
    }

    /**
     * 来源筛选只返回匹配的变更
     */
    @Test
    public void testSourceFilter() {
        SysDataChangeLog primary = change(1L, 100L, "2026-11-10 10:05:33",
                "[{\"field\":\"spec\",\"label\":\"规格\",\"before\":\"A\",\"after\":\"B\"}]");
        when(changeLogMapper.selectHistory(eq("work_order"), anyList(), isNull(), isNull(), anyInt()))
                .thenReturn(List.of(primary));

        BusinessHistoryDto result = service.query("WORK_ORDER", "880", null, "RELATED", null, null, page(0, 20));

        assertEquals(0, result.getTotalCount());
        assertTrue(result.getEvents().isEmpty());
    }

    /**
     * 按事件分页
     */
    @Test
    public void testPagination() {
        SysDataChangeLog first = change(1L, 101L, "2026-11-10 10:00:00",
                "[{\"field\":\"spec\",\"label\":\"规格\",\"before\":\"A\",\"after\":\"B\"}]");
        SysDataChangeLog second = change(2L, 102L, "2026-11-10 11:00:00",
                "[{\"field\":\"batch_qty\",\"label\":\"批量\",\"before\":\"1\",\"after\":\"2\"}]");
        SysDataChangeLog third = change(3L, 103L, "2026-11-10 12:00:00",
                "[{\"field\":\"sales_price\",\"label\":\"销售单价\",\"before\":\"1\",\"after\":\"2\"}]");
        when(changeLogMapper.selectHistory(eq("work_order"), anyList(), isNull(), isNull(), anyInt()))
                .thenReturn(List.of(first, second, third));

        BusinessHistoryDto result = service.query("WORK_ORDER", "880", null, null, null, null, page(0, 2));

        assertEquals(3, result.getTotalCount());
        assertEquals(2, result.getEvents().size());
        assertEquals(0, result.getPageIndex());
        assertEquals(2, result.getPageSize());
        assertEquals(103L, result.getEvents().get(0).getOperationLogId());
        assertEquals(102L, result.getEvents().get(1).getOperationLogId());
    }

    // endregion

    // region 制剂关联明细
    // ===================================
    // 制剂关联明细
    // ===================================

    /**
     * 制剂历史应同时返回主表与处方明细，并补全处方行名称
     */
    @Test
    public void testPreparationRelatedChanges() {
        BusinessHistoryProvider preparationProvider = mock(BusinessHistoryProvider.class);
        BusinessHistoryScope scope = new BusinessHistoryScope();
        scope.setTitle("Z04-W37-009 · 理伤续断丸");
        scope.addTable("preparation", List.of("42"));
        scope.addTable("preparation_formula", List.of("32046"));
        scope.addRecordLabel("preparation_formula", "32046", "当归（检）");
        when(preparationProvider.bizType()).thenReturn("PREPARATION");
        when(preparationProvider.resolve("42")).thenReturn(scope);

        BusinessHistoryServiceImpl preparationService = new BusinessHistoryServiceImpl(
                List.of(preparationProvider), changeLogMapper, operationLogMapper, new ObjectMapper());

        SysDataChangeLog main = change(1L, 200L, "2026-11-10 10:00:00",
                "[{\"field\":\"spec\",\"label\":\"规格\",\"before\":\"A\",\"after\":\"B\"}]");
        main.setTableName("preparation");
        main.setBusinessId("42");

        SysDataChangeLog detail = change(2L, 201L, "2026-11-10 11:00:00",
                "[{\"field\":\"dosage\",\"label\":\"处方量\",\"before\":\"0.070\",\"after\":\"0.074\"}]");
        detail.setTableName("preparation_formula");
        detail.setBusinessId("32046");
        detail.setChangeSource("RELATED");

        when(changeLogMapper.selectHistory(anyString(), anyList(), isNull(), isNull(), anyInt()))
                .thenAnswer(invocation -> "preparation".equals(invocation.getArgument(0))
                        ? List.of(main) : List.of(detail));

        BusinessHistoryDto result = preparationService.query(
                "PREPARATION", "42", null, null, null, null, page(0, 20));

        assertEquals(2, result.getEvents().size());
        assertEquals(2, result.getRecordStats().size());
        assertEquals(1, result.getRecordStats().stream()
                .filter(stat -> "32046".equals(stat.getRecordId()))
                .findFirst()
                .orElseThrow()
                .getCount());
        assertEquals("当归（检）", result.getRecordStats().stream()
                .filter(stat -> "32046".equals(stat.getRecordId()))
                .findFirst()
                .orElseThrow()
                .getLabel());
    }

    // endregion

    // region 异常
    // ===================================
    // 异常
    // ===================================

    /**
     * 不支持的业务类型应抛出参数异常
     */
    @Test
    public void testUnsupportedBizType() {
        assertThrows(IllegalArgumentException.class, () ->
                service.query("UNKNOWN", "1", null, null, null, null, page(0, 20)));
    }

    // endregion

    // region 测试数据
    // ===================================
    // 测试数据
    // ===================================

    /**
     * 构造变更日志
     *
     * @param id            变更ID
     * @param operationLogId 操作日志ID
     * @param time          变更时间
     * @param changedFields 字段差异JSON
     * @return 变更日志
     */
    private SysDataChangeLog change(Long id, Long operationLogId, String time, String changedFields) {
        SysDataChangeLog change = new SysDataChangeLog();
        change.setId(id);
        change.setOperationLogId(operationLogId);
        change.setTraceId("tr-" + id);
        change.setTableName("work_order");
        change.setBusinessId("880");
        change.setChangeType("UPDATE");
        change.setChangeSource("PRIMARY");
        change.setChangedFields(changedFields);
        change.setFieldCount(1);
        change.setUserId(12L);
        change.setUserAccount("zhangwei");
        change.setUserName("张伟");
        change.setCreatedTime(LocalDateTime.parse(time.replace(' ', 'T')));
        return change;
    }

    /**
     * 构造操作日志摘要
     *
     * @param id  日志ID
     * @param desc 操作描述
     * @return 操作日志
     */
    private SysOperationLog operationLog(Long id, String desc) {
        SysOperationLog log = new SysOperationLog();
        log.setId(id);
        log.setBusinessType("WORK_ORDER");
        log.setBusinessId("880");
        log.setModule("工单管理");
        log.setOperationType("UPDATE");
        log.setOperationDesc(desc);
        log.setUserName("张伟");
        log.setUserAccount("zhangwei");
        log.setClientIp("192.168.1.24");
        log.setMacAddress("00-1A-2B-3C-4D-5E");
        log.setCreatedTime(LocalDateTime.of(2026, 11, 10, 10, 5, 33));
        return log;
    }

    /**
     * 构造分页参数
     *
     * @param pageIndex 页码
     * @param pageSize  每页数量
     * @return 分页参数
     */
    private PageRequestDto page(int pageIndex, int pageSize) {
        PageRequestDto pageRequest = new PageRequestDto();
        pageRequest.setPageIndex(pageIndex);
        pageRequest.setPageSize(pageSize);
        return pageRequest;
    }

    // endregion
}
