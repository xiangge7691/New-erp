package com.tonghui.erp.Common.utils;

import com.tonghui.erp.Data.Entity.WorkOrderProcessExecution;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 工序执行记录定位测试
 * <p>
 * 重点验证多批次场景：批次行即使带上了同工序其它批次的 executionId，
 * 也不能把那条记录改到自己名下（否则拆分后的批次只剩一条实际生产）。
 * </p>
 */
public class ExecutionRecordMatcherTest {

    // region 按主键定位
    // ===================================
    // 按主键定位
    // ===================================

    /**
     * id 与批次一致 → 直接按主键命中，id 保留
     */
    @Test
    public void testResolveByIdWhenBatchMatches() {
        WorkOrderProcessExecution existing = execution(475L, 27L, 121L, 1);
        WorkOrderProcessExecution row = execution(475L, 27L, 121L, 1);

        WorkOrderProcessExecution matched = ExecutionRecordMatcher.resolve(
                row, List.of(existing), mapOf(existing), Set.of());

        assertEquals(475L, matched.getId(), "批次一致时应按主键命中");
        assertEquals(475L, row.getId(), "批次一致时不应清除提交行的 id");
    }

    /**
     * id 属于同工序另一批次 → 丢弃 id，改按批次定位到本批次自己的记录
     */
    @Test
    public void testResolveOtherBatchRecordByBusinessKey() {
        WorkOrderProcessExecution batchOne = execution(475L, 27L, 121L, 1);
        WorkOrderProcessExecution batchTwo = execution(480L, 27L, 122L, 2);
        // 前端误把批次1的 executionId 带到了批次2 上
        WorkOrderProcessExecution row = execution(475L, 27L, 122L, 2);

        WorkOrderProcessExecution matched = ExecutionRecordMatcher.resolve(
                row, List.of(batchOne, batchTwo), mapOf(batchOne, batchTwo), Set.of());

        assertEquals(480L, matched.getId(), "应定位到本批次自己的执行记录");
        assertNull(row.getId(), "跨批次的 id 应被清除");
    }

    /**
     * id 属于同工序另一批次且本批次尚无记录 → 不命中，交由调用方新增
     */
    @Test
    public void testResolveOtherBatchWithoutOwnRecord() {
        WorkOrderProcessExecution batchOne = execution(475L, 27L, 121L, 1);
        WorkOrderProcessExecution row = execution(475L, 27L, 122L, 2);

        WorkOrderProcessExecution matched = ExecutionRecordMatcher.resolve(
                row, List.of(batchOne), mapOf(batchOne), Set.of());

        assertNull(matched, "不能复用到其它批次的记录");
        assertNull(row.getId(), "跨批次的 id 应被清除");
        assertEquals(122L, row.getPlanBatchId(), "提交行的批次定位信息应保留");
    }

    /**
     * id 不属于本工单 → 按业务键定位，避免误改其它工单记录
     */
    @Test
    public void testResolveUnknownIdFallsBackToBusinessKey() {
        WorkOrderProcessExecution existing = execution(475L, 27L, 122L, 2);
        WorkOrderProcessExecution row = execution(999L, 27L, 122L, 2);

        WorkOrderProcessExecution matched = ExecutionRecordMatcher.resolve(
                row, List.of(existing), mapOf(existing), Set.of());

        assertEquals(475L, matched.getId());
        assertNull(row.getId());
    }

    // endregion

    // region 按业务键定位
    // ===================================
    // 按业务键定位
    // ===================================

    /**
     * 同一批次已有记录 → 命中该记录
     */
    @Test
    public void testFindByPlanBatch() {
        WorkOrderProcessExecution existing = execution(475L, 27L, 122L, 2);
        WorkOrderProcessExecution row = execution(null, 27L, 122L, 2);

        WorkOrderProcessExecution matched = ExecutionRecordMatcher.findByPlanOrBatch(
                row, List.of(existing), Set.of());

        assertEquals(475L, matched.getId());
    }

    /**
     * 工序级兜底只匹配未写批次的记录，不会命中别的批次
     */
    @Test
    public void testPlanFallbackSkipsBatchedRecords() {
        WorkOrderProcessExecution batchOne = execution(475L, 27L, 121L, 1);
        WorkOrderProcessExecution row = execution(null, 27L, null, null);

        WorkOrderProcessExecution matched = ExecutionRecordMatcher.findByPlanOrBatch(
                row, List.of(batchOne), Set.of());

        assertNull(matched, "已有批次的记录不能被工序级保存复用");
    }

    /**
     * 工序级兜底按批次序号区分历史记录
     */
    @Test
    public void testPlanFallbackMatchesBatchNo() {
        WorkOrderProcessExecution legacy = execution(475L, 27L, null, 2);
        WorkOrderProcessExecution row = execution(null, 27L, null, 1);

        assertNull(ExecutionRecordMatcher.findByPlanOrBatch(row, List.of(legacy), Set.of()),
                "批次序号不一致时不应命中");
        assertEquals(475L, ExecutionRecordMatcher.findByPlanOrBatch(
                execution(null, 27L, null, 2), List.of(legacy), Set.of()).getId(),
                "批次序号一致时应命中");
    }

    /**
     * 已匹配过的记录不再复用，避免两行提交合并到同一条记录
     */
    @Test
    public void testMatchedRecordNotReused() {
        WorkOrderProcessExecution existing = execution(475L, 27L, 121L, 1);
        WorkOrderProcessExecution row = execution(null, 27L, 121L, 1);

        assertNull(ExecutionRecordMatcher.findByPlanOrBatch(row, List.of(existing), Set.of(475L)),
                "已被本次其它行匹配的记录不应重复复用");
    }

    // endregion

    // region 替换删除判断
    // ===================================
    // 替换删除判断
    // ===================================

    /**
     * 旧记录属于其它批次 → 不删除（保存批次 2 不影响批次 1）
     */
    @Test
    public void testOtherBatchNotReplaced() {
        WorkOrderProcessExecution old = execution(475L, 27L, 121L, 1);

        assertEquals(false, ExecutionRecordMatcher.isReplacedByIncoming(
                old, Set.of(122L), Set.of(2), Set.of(27L), Set.of()), "其它批次的记录不能删除");
        assertEquals(true, ExecutionRecordMatcher.isReplacedByIncoming(
                old, Set.of(121L), Set.of(1), Set.of(27L), Set.of()), "同批次记录才允许替换");
    }

    /**
     * 历史记录只写了批次序号 → 本次提交未涉及该序号时不删除
     */
    @Test
    public void testLegacyBatchNoNotReplacedByOtherBatch() {
        WorkOrderProcessExecution old = execution(475L, 27L, null, 1);

        assertEquals(false, ExecutionRecordMatcher.isReplacedByIncoming(
                old, Set.of(122L), Set.of(2), Set.of(27L), Set.of()), "批次序号不一致时不能删除");
        assertEquals(false, ExecutionRecordMatcher.isReplacedByIncoming(
                old, Set.of(), Set.of(), Set.of(27L), Set.of()), "工序级保存不能删除已记录批次的旧行");
        assertEquals(true, ExecutionRecordMatcher.isReplacedByIncoming(
                old, Set.of(121L), Set.of(1), Set.of(27L), Set.of()), "同一批次序号允许替换");
    }

    /**
     * 未写批次的工序级旧记录 → 按计划替换
     */
    @Test
    public void testPlanLevelRecordReplaced() {
        WorkOrderProcessExecution old = execution(470L, 27L, null, null);

        assertEquals(true, ExecutionRecordMatcher.isReplacedByIncoming(
                old, Set.of(122L), Set.of(2), Set.of(27L), Set.of()), "工序级旧记录按计划替换");
        assertEquals(false, ExecutionRecordMatcher.isReplacedByIncoming(
                old, Set.of(), Set.of(), Set.of(28L), Set.of()), "其它计划的记录不能删除");
    }

    // endregion

    // region 测试数据构造
    // ===================================
    // 测试数据构造
    // ===================================

    /**
     * 构造工序执行记录
     */
    private WorkOrderProcessExecution execution(Long id, Long planId, Long planBatchId, Integer batchNo) {
        WorkOrderProcessExecution execution = new WorkOrderProcessExecution();
        execution.setId(id);
        execution.setWorkOrderId(470L);
        execution.setPlanId(planId);
        execution.setPlanBatchId(planBatchId);
        execution.setBatchNo(batchNo);
        return execution;
    }

    /**
     * 按ID索引记录
     */
    private Map<Long, WorkOrderProcessExecution> mapOf(WorkOrderProcessExecution... executions) {
        Map<Long, WorkOrderProcessExecution> map = new HashMap<>();
        for (WorkOrderProcessExecution execution : executions) {
            map.put(execution.getId(), execution);
        }
        return map;
    }

    // endregion
}
