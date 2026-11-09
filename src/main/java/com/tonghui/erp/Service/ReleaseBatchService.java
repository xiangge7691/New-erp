package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.QualityPages.ReleaseBatchDtos;

/**
 * 批次审核放行服务
 * <p>
 * 提供批次合规列表、全批详情、工序检查保存、通过/拒绝放行。
 * 工序要求读取现有工序页配置（品种工序模板/工序类型描述），不新增配置页面。
 * </p>
 */
public interface ReleaseBatchService {

    /**
     * 分页查询批次审核列表
     *
     * @param varietyCode 品种编码
     * @param batchNo     批号/工单号
     * @param status      放行状态
     * @param keyword     关键字
     * @param pageIndex   页码，从0开始
     * @param pageSize    每页数量
     * @return 分页结果
     */
    PagedResult<ReleaseBatchDtos.BatchSummary> queryBatches(String varietyCode, String batchNo,
                                                            String status, String keyword,
                                                            int pageIndex, int pageSize);

    /**
     * 查询审核放行看板统计
     *
     * @param varietyCode 品种编码
     * @param batchNo     批号/工单号
     * @param status      当前列表筛选的放行状态
     * @param keyword     关键字
     * @return 看板计数
     */
    ReleaseBatchDtos.BoardStats queryBoardStats(String varietyCode, String batchNo,
                                                String status, String keyword);

    /**
     * 查询批次全批审核详情
     *
     * @param workOrderId 工单ID
     * @return 批次详情
     */
    ReleaseBatchDtos.BatchDetail getBatchDetail(Long workOrderId);

    /**
     * 保存工序符合性检查
     *
     * @param workOrderId 工单ID
     * @param request     检查项
     */
    void saveProcessChecks(Long workOrderId, ReleaseBatchDtos.ProcessCheckSaveRequest request);

    /**
     * 通过放行
     *
     * @param workOrderId 工单ID
     * @param request     放行请求
     * @return 批次详情
     */
    ReleaseBatchDtos.BatchDetail approve(Long workOrderId, ReleaseBatchDtos.ApproveRequest request);

    /**
     * 拒绝放行
     *
     * @param workOrderId 工单ID
     * @param request     拒绝请求
     * @return 批次详情
     */
    ReleaseBatchDtos.BatchDetail reject(Long workOrderId, ReleaseBatchDtos.RejectRequest request);
}
