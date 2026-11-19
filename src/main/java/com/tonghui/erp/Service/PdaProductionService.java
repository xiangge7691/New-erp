package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Dto.Pda.PdaExecutionSaveRequest;
import com.tonghui.erp.Common.Dto.Pda.PdaOptionDto;
import com.tonghui.erp.Common.Dto.Pda.PdaProcessDto;
import com.tonghui.erp.Common.Dto.Pda.PdaPunchRequest;
import com.tonghui.erp.Common.Dto.Pda.PdaRecordDto;
import com.tonghui.erp.Common.Dto.Pda.PdaTaskDto;

import java.util.List;

/**
 * PDA 生产工序执行服务
 */
public interface PdaProductionService {

    /**
     * 查询 PDA 任务列表（任务—工序两级）
     *
     * @param status        任务状态：未开始/进行中/已完成
     * @param personId      操作人档案ID
     * @param processTypeId 工序类型ID
     * @param workshop      车间（当前工单无车间字段，预留）
     * @param keyword       制剂名称/批号/工单号关键字
     * @param todoOnly      true 时隐藏已全部完成的工单（查看“已完成”状态时除外）
     * @return 任务列表
     */
    List<PdaTaskDto> listTasks(String status, Long personId, Long processTypeId,
                               String workshop, String keyword, boolean todoOnly);

    /**
     * 查询单个 PDA 任务
     *
     * @param workOrderId 工单ID
     * @return 任务详情
     */
    PdaTaskDto getTask(Long workOrderId);

    /**
     * 工序开始打点
     *
     * @param executionId 执行记录ID
     * @param request     请求
     * @return 更新后的工序
     */
    PdaProcessDto start(Long executionId, PdaPunchRequest request);

    /**
     * 工序结束打点
     *
     * @param executionId 执行记录ID
     * @param request     请求
     * @return 更新后的工序
     */
    PdaProcessDto end(Long executionId, PdaPunchRequest request);

    /**
     * 暂存工序执行信息
     *
     * @param executionId 执行记录ID
     * @param request     请求
     * @return 更新后的工序
     */
    PdaProcessDto saveDraft(Long executionId, PdaExecutionSaveRequest request);

    /**
     * 提交工序执行信息（PDA 端转只读）
     *
     * @param executionId 执行记录ID
     * @param request     请求
     * @return 更新后的工序
     */
    PdaProcessDto submit(Long executionId, PdaExecutionSaveRequest request);

    /**
     * 查询 PDA 提交记录
     *
     * @param keyword       关键字
     * @param processTypeId 工序类型ID
     * @param personId      操作人档案ID
     * @return 提交记录
     */
    List<PdaRecordDto> listRecords(String keyword, Long processTypeId, Long personId);

    /**
     * 启用设备选项
     *
     * @param keyword 名称/编号关键字
     * @return 设备选项
     */
    List<PdaOptionDto> equipmentOptions(String keyword);

    /**
     * 在职人员选项
     *
     * @param keyword 姓名/工号关键字
     * @return 人员选项
     */
    List<PdaOptionDto> personnelOptions(String keyword);
}
