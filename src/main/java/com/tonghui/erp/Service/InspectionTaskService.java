package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.QualityPages.InspectionTaskDtos;
import com.tonghui.erp.Data.Entity.InspectionRequest;
import com.tonghui.erp.Data.Entity.InspectionTask;

/**
 * 检验任务服务
 * <p>
 * 负责检验任务列表/详情、六步办理、跳过、作废与归档。
 * 每个步骤成功后在同一事务内推进任务状态并写步骤记录。
 * </p>
 */
public interface InspectionTaskService extends IService<InspectionTask> {

    /**
     * 分页查询检验任务
     *
     * @param varietyCode 品种编码
     * @param batchNo     批号
     * @param itemCategory 被检品属性
     * @param step        当前环节
     * @param status      任务状态；为空时默认不包含已归档任务
     * @param keyword     关键字
     * @param pageIndex   页码，从0开始
     * @param pageSize    每页数量
     * @return 分页结果
     */
    PagedResult<InspectionTaskDtos.TaskSummary> queryTasks(String varietyCode, String batchNo, String itemCategory,
                                                           String step, String status, String keyword,
                                                           int pageIndex, int pageSize);

    /**
     * 分页查询检验批次（按工单/批次聚合任务）
     *
     * @param varietyCode 品种编码
     * @param batchNo     批号
     * @param itemCategory 被检品属性
     * @param step        当前环节
     * @param status      任务状态
     * @param keyword     关键字
     * @param pageIndex   页码，从0开始
     * @param pageSize    每页数量
     * @return 分页批次汇总
     */
    PagedResult<InspectionTaskDtos.BatchSummary> queryBatches(String varietyCode, String batchNo, String itemCategory,
                                                              String step, String status, String keyword,
                                                              int pageIndex, int pageSize);

    /**
     * 查询指定工单/批次的检验汇总
     *
     * @param workOrderId 工单ID
     * @return 批次汇总（含任务明细）
     */
    InspectionTaskDtos.BatchSummary queryBatch(Long workOrderId);

    /**
     * 查询检验看板统计
     * <p>
     * 三态口径：待办 = 请检/检验计划，检验中 = 取样至销毁（尚未办结），已完成 = 全部办结（DONE）。
     * 任务维度包含无工单的生产原料任务，已作废、已归档任务不计入；批次维度只统计挂工单的任务。
     * </p>
     *
     * @param varietyCode  品种编码
     * @param batchNo      批号
     * @param itemCategory 被检品属性
     * @param status       任务状态
     * @param keyword      关键字
     * @return 批次/任务计数与环节计数
     */
    InspectionTaskDtos.BoardStats queryBoardStats(String varietyCode, String batchNo, String itemCategory,
                                                  String status, String keyword);

    /**
     * 查询任务详情（含六步）
     *
     * @param id 任务ID
     * @return 任务详情
     */
    InspectionTaskDtos.TaskDetail getTaskDetail(Long id);

    /**
     * 创建检验任务
     *
     * @param request 创建请求
     * @return 任务详情
     */
    InspectionTaskDtos.TaskDetail createTask(InspectionTaskDtos.CreateTaskRequest request);

    /**
     * 根据已保存的请检单自动生成检验任务（一张请检单对应一张任务）
     * <p>
     * 任务生成后直接完成“请检”环节并推进到“检验计划”；若工单下已有未关联请检单的任务，
     * 则直接关联复用；生产原料请检单不关联工单，直接用请检单自身信息生成任务；
     * 请检单填写的检验计划编号若已存在，则自动关联并跳过计划环节。
     * </p>
     *
     * @param request 已保存的请检单
     * @return 任务详情；既无工单又被检品属性不合法时返回 null
     */
    InspectionTaskDtos.TaskDetail createTaskFromRequest(InspectionRequest request);

    /**
     * 办理检验计划
     *
     * @param id      任务ID
     * @param request 计划请求
     * @return 任务详情
     */
    InspectionTaskDtos.TaskDetail completePlan(Long id, InspectionTaskDtos.PlanStepRequest request);

    /**
     * 办理请检
     *
     * @param id      任务ID
     * @param request 请检请求
     * @return 任务详情
     */
    InspectionTaskDtos.TaskDetail completeRequest(Long id, InspectionTaskDtos.RequestStepRequest request);

    /**
     * 办理取样
     *
     * @param id      任务ID
     * @param request 取样请求
     * @return 任务详情
     */
    InspectionTaskDtos.TaskDetail completeSampling(Long id, InspectionTaskDtos.SamplingStepRequest request);

    /**
     * 办理留样
     *
     * @param id      任务ID
     * @param request 留样请求
     * @return 任务详情
     */
    InspectionTaskDtos.TaskDetail completeRetention(Long id, InspectionTaskDtos.RetentionStepRequest request);

    /**
     * 办理检验
     *
     * @param id      任务ID
     * @param request 检验请求
     * @return 任务详情
     */
    InspectionTaskDtos.TaskDetail completeInspection(Long id, InspectionTaskDtos.InspectionStepRequest request);

    /**
     * 办理销毁
     *
     * @param id      任务ID
     * @param request 销毁请求
     * @return 任务详情
     */
    InspectionTaskDtos.TaskDetail completeDestruction(Long id, InspectionTaskDtos.DestructionStepRequest request);

    /**
     * 跳过留样或销毁
     *
     * @param id      任务ID
     * @param request 跳过请求
     * @return 任务详情
     */
    InspectionTaskDtos.TaskDetail skipStep(Long id, InspectionTaskDtos.SkipStepRequest request);

    /**
     * 作废任务
     *
     * @param id      任务ID
     * @param request 作废请求
     * @return 任务详情
     */
    InspectionTaskDtos.TaskDetail cancelTask(Long id, InspectionTaskDtos.CancelRequest request);

    /**
     * 归档检验任务
     * <p>
     * 仅“待开始/进行中”的任务可归档；归档后任务只读，默认不在任务列表、批次视图和看板中展示，
     * 且不可解除归档。
     * </p>
     *
     * @param id 任务ID
     * @return 任务详情
     */
    InspectionTaskDtos.TaskDetail archiveTask(Long id);
}
