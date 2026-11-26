package com.tonghui.erp.Common.utils;

import com.tonghui.erp.Data.Entity.WorkOrderProcessExecution;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 工序执行记录定位工具
 * <p>
 * 「实际生产」按工序/批次保存时，需要把提交行合并到既有执行记录上而不是每次新增。
 * 定位规则：
 * <ol>
 *   <li>带 id 时优先按主键定位，但 id 的批次必须与本次提交的批次一致；
 *       批次不一致说明客户端带了同工序其它批次的 executionId，丢弃该 id 重新定位</li>
 *   <li>无 id 或 id 不可用时，按「计划批次 → 工序计划」业务键定位</li>
 *   <li>按工序计划兜底时只匹配未写批次的记录，并要求批次序号一致，避免跨批次串数据</li>
 * </ol>
 * 定位过程会清除提交行上不可用的 id，保证调用方后续按「新增」处理。
 * </p>
 */
public final class ExecutionRecordMatcher {

    // region 构造方法
    // ===================================
    // 构造方法
    // ===================================

    /**
     * 私有构造方法，禁止实例化
     */
    private ExecutionRecordMatcher() {
    }

    // endregion

    // region 定位入口
    // ===================================
    // 定位入口
    // ===================================

    /**
     * 定位本次提交行要更新的既有记录
     *
     * @param row          待保存行（可带 id、planId、planBatchId、batchNo）
     * @param existingList 同一工单下的既有执行记录
     * @param existingMap  既有执行记录（按ID索引）
     * @param matchedIds   本次已匹配的记录ID（避免同一行被重复复用）
     * @return 匹配到的既有记录，未匹配返回 null
     */
    public static WorkOrderProcessExecution resolve(WorkOrderProcessExecution row,
                                                    List<WorkOrderProcessExecution> existingList,
                                                    Map<Long, WorkOrderProcessExecution> existingMap,
                                                    Set<Long> matchedIds) {
        WorkOrderProcessExecution byId = row.getId() == null ? null : existingMap.get(row.getId());
        if (byId != null) {
            boolean batchConflict = row.getPlanBatchId() != null && byId.getPlanBatchId() != null
                    && !Objects.equals(row.getPlanBatchId(), byId.getPlanBatchId());
            if (!batchConflict) {
                return byId;
            }
        }
        if (row.getId() != null) {
            // id 与批次冲突或不属于本工单：按业务键重新定位，避免跨批次/跨工单覆盖
            row.setId(null);
        }
        return findByPlanOrBatch(row, existingList, matchedIds);
    }

    /**
     * 按「计划批次 → 工序计划」定位既有执行记录
     *
     * @param row          待保存行（可带 planId、planBatchId、batchNo）
     * @param existingList 同一工单下的既有执行记录
     * @param matchedIds   本次已匹配的记录ID（避免同一行被重复复用）
     * @return 匹配到的既有记录，未匹配返回 null
     */
    public static WorkOrderProcessExecution findByPlanOrBatch(WorkOrderProcessExecution row,
                                                              List<WorkOrderProcessExecution> existingList,
                                                              Set<Long> matchedIds) {
        if (row.getPlanBatchId() != null) {
            WorkOrderProcessExecution batchRow = existingList.stream()
                    .filter(e -> !matchedIds.contains(e.getId()))
                    .filter(e -> Objects.equals(row.getPlanBatchId(), e.getPlanBatchId()))
                    .findFirst().orElse(null);
            if (batchRow != null) {
                return batchRow;
            }
            // 同一批次已有执行记录时到此为止，不能退回到工序级记录，否则会跨批次串数据
            if (existingList.stream().anyMatch(e -> Objects.equals(row.getPlanBatchId(), e.getPlanBatchId()))) {
                return null;
            }
        }
        if (row.getPlanId() != null) {
            return existingList.stream()
                    .filter(e -> !matchedIds.contains(e.getId()))
                    .filter(e -> Objects.equals(row.getPlanId(), e.getPlanId()))
                    .filter(e -> e.getPlanBatchId() == null)
                    // 历史记录可能只写了批次序号：未限定批次保存时按序号区分，避免合并到别的批次
                    .filter(e -> row.getBatchNo() == null || e.getBatchNo() == null
                            || Objects.equals(row.getBatchNo(), e.getBatchNo()))
                    .findFirst().orElse(null);
        }
        return null;
    }

    /**
     * 判断既有记录是否被本次提交的工序/批次替换（即应当删除）
     * <p>
     * 只按同一批次或同一工序判断，避免像全量覆盖那样删除其他工序的执行记录。
     * 历史记录未写批次ID、只写了批次序号时，本次提交未涉及该序号即视为其它批次的数据，
     * 不做删除，防止保存批次 2 时把批次 1 的旧记录一并清掉。
     * </p>
     *
     * @param old                 既有执行记录
     * @param incomingBatchIds    本次提交涉及的批次ID
     * @param incomingBatchNos    本次提交涉及的批次序号
     * @param incomingPlanIds     本次提交涉及的工序计划ID
     * @param incomingProcessKeys 本次提交涉及的「工序类型#顺序」键
     * @return 是否应删除
     */
    public static boolean isReplacedByIncoming(WorkOrderProcessExecution old,
                                               Set<Long> incomingBatchIds,
                                               Set<Integer> incomingBatchNos,
                                               Set<Long> incomingPlanIds,
                                               Set<String> incomingProcessKeys) {
        if (old.getPlanBatchId() != null) {
            return incomingBatchIds.contains(old.getPlanBatchId());
        }
        if (old.getBatchNo() != null
                && (incomingBatchNos.isEmpty() || !incomingBatchNos.contains(old.getBatchNo()))) {
            return false;
        }
        if (old.getPlanId() != null) {
            return incomingPlanIds.contains(old.getPlanId());
        }
        return old.getProcessTypeId() != null && old.getStepOrder() != null
                && incomingProcessKeys.contains(processKey(old.getProcessTypeId(), old.getStepOrder()));
    }

    /**
     * 工序定位键：工序类型 + 工序顺序
     *
     * @param processTypeId 工序类型ID
     * @param stepOrder     工序顺序
     * @return 定位键
     */
    public static String processKey(Long processTypeId, Integer stepOrder) {
        return processTypeId + "#" + stepOrder;
    }

    // endregion
}
