package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Data.Entity.WorkOrderProcessExecution;

import java.time.LocalDate;
import java.util.List;

/**
 * 工单工序执行记录服务接口
 */
public interface WorkOrderProcessExecutionService extends IService<WorkOrderProcessExecution> {

    /**
     * 根据工单ID查询工序执行记录列表（含实际参与人）
     *
     * @param workOrderId 工单ID
     * @return 工序执行记录列表
     */
    List<WorkOrderProcessExecution> getByWorkOrderId(Long workOrderId);

    /**
     * 查询已完工（锁定）的工序执行记录
     *
     * @param from        完工日期起（可选）
     * @param to          完工日期止（可选）
     * @param workOrderId 工单ID（可选）
     * @return 已完工记录列表（含参与人、计划信息与生成绩效数）
     */
    List<WorkOrderProcessExecution> listCompleted(LocalDate from, LocalDate to, Long workOrderId);

    /**
     * 批量保存工序执行记录与实际参与人
     * <p>
     * 保存的是「实际生产登记」而不是「完工」：只填实际开始时间也可保存，
     * 此时状态由后端推导为「执行中」；实际结束时间由
     * {@link #complete(Long, java.time.LocalDateTime)} 单独登记。
     * </p>
     *
     * @param workOrderId 工单ID
     * @param executions  工序执行记录列表
     * @return 非阻断的设备占用提醒（进行中记录的冲突）
     */
    List<String> batchSave(Long workOrderId, List<WorkOrderProcessExecution> executions);

    /**
     * 工序/批次完工：锁定执行记录并生成个人绩效
     *
     * @param executionId 工序执行记录ID
     * @return 完工后的执行记录
     */
    WorkOrderProcessExecution complete(Long executionId);

    /**
     * 工序/批次完工：按指定结束时间锁定执行记录并生成个人绩效
     * <p>
     * 结束时间为空时取当前时间；指定结束时间时必须晚于实际开始时间，
     * 且必须落在工单配置时间窗内。
     * </p>
     *
     * @param executionId 工序执行记录ID
     * @param endTime     实际结束时间（可空，空则取当前时间）
     * @return 完工后的执行记录
     */
    WorkOrderProcessExecution complete(Long executionId, java.time.LocalDateTime endTime);

    /**
     * 删除误拆的工序执行行
     * <p>已完工锁定、PDA 已提交或已生成绩效的记录不允许删除</p>
     *
     * @param executionId 工序执行记录ID
     * @return 所属工单ID（供前端刷新列表）
     */
    Long deleteExecution(Long executionId);

    /**
     * 按关联的实际执行记录刷新排产批次状态
     * <p>无执行记录=计划中，已登记实际生产但未全部完成=执行中，全部完成=已完工。</p>
     *
     * @param planBatchId 排产批次ID（为空时忽略）
     */
    void refreshPlanBatchStatus(Long planBatchId);

    /**
     * 执行记录未关联排产计划时，按「工单+工序类型+工序顺序」回填计划ID并落库
     *
     * @param execution 执行记录（需包含工单、工序类型与工序顺序）
     */
    void fillPlanReference(WorkOrderProcessExecution execution);
}
