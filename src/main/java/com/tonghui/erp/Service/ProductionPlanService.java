package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.ProductionPlanWithRecordsDto;
import com.tonghui.erp.Data.Entity.ProductionPlan;

import java.time.LocalDateTime;

/**
 * 生产计划服务接口
 * <p>
 * 提供生产计划相关的业务逻辑接口，包括计划的高级查询、
 * 带子表关联查询、状态变更、暂停恢复、状态验证等功能
 * </p>
 */
public interface ProductionPlanService extends IService<ProductionPlan> {

    /**
     * 高级查询生产计划（支持分页）
     *
     * @param productionPlan 查询条件
     * @param keyword 关键字（对计划编号、计划名称进行模糊匹配，可选）
     * @param createdTimeStart 创建时间起始
     * @param createdTimeEnd 创建时间结束
     * @param updatedTimeStart 更新时间起始
     * @param updatedTimeEnd 更新时间结束
     * @param productionStartTimeStart 生产开始时间起始
     * @param productionStartTimeEnd 生产开始时间结束
     * @param productionEndTimeStart 生产结束时间起始
     * @param productionEndTimeEnd 生产结束时间结束
     * @param inspectionStartTimeStart 检验开始时间起始
     * @param inspectionStartTimeEnd 检验开始时间结束
     * @param inspectionEndTimeStart 检验结束时间起始
     * @param inspectionEndTimeEnd 检验结束时间结束
     * @param outboundTimeStart 出库时间起始
     * @param outboundTimeEnd 出库时间结束
     * @param archiveTimeStart 归档时间起始
     * @param archiveTimeEnd 归档时间结束
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    Page<ProductionPlan> queryProductionPlans(ProductionPlan productionPlan,
                                             String keyword,
                                             LocalDateTime createdTimeStart, LocalDateTime createdTimeEnd,
                                             LocalDateTime updatedTimeStart, LocalDateTime updatedTimeEnd,
                                             LocalDateTime productionStartTimeStart, LocalDateTime productionStartTimeEnd,
                                             LocalDateTime productionEndTimeStart, LocalDateTime productionEndTimeEnd,
                                             LocalDateTime inspectionStartTimeStart, LocalDateTime inspectionStartTimeEnd,
                                             LocalDateTime inspectionEndTimeStart, LocalDateTime inspectionEndTimeEnd,
                                             LocalDateTime outboundTimeStart, LocalDateTime outboundTimeEnd,
                                             LocalDateTime archiveTimeStart, LocalDateTime archiveTimeEnd,
                                             String timeFieldType, LocalDateTime timeStart, LocalDateTime timeEnd,
                                             int pageNum, int pageSize);

    /**
     * 刷新生产计划状态
     * <p>
     * 根据计划关联的工单状态动态计算计划状态并落库：
     * <ul>
     *   <li>无关联工单 → 待生产</li>
     *   <li>有关联工单但存在未出库工单 → 生产中</li>
     *   <li>所有关联工单均已出库或已归档 → 已完成</li>
     * </ul>
     * 在计划创建及工单新增/修改/删除时调用，保证状态实时准确
     * </p>
     *
     * @param planId 生产计划ID
     */
    void refreshPlanStatus(Integer planId);

    /**
     * 高级查询生产计划（包含工序记录子表）
     *
     * @param productionPlan 查询条件
     * @param keyword 关键字（对计划编号、计划名称进行模糊匹配，可选）
     * @param createdTimeStart 创建时间起始
     * @param createdTimeEnd 创建时间结束
     * @param updatedTimeStart 更新时间起始
     * @param updatedTimeEnd 更新时间结束
     * @param productionStartTimeStart 生产开始时间起始
     * @param productionStartTimeEnd 生产开始时间结束
     * @param productionEndTimeStart 生产结束时间起始
     * @param productionEndTimeEnd 生产结束时间结束
     * @param inspectionStartTimeStart 检验开始时间起始
     * @param inspectionStartTimeEnd 检验开始时间结束
     * @param inspectionEndTimeStart 检验结束时间起始
     * @param inspectionEndTimeEnd 检验结束时间结束
     * @param outboundTimeStart 出库时间起始
     * @param outboundTimeEnd 出库时间结束
     * @param archiveTimeStart 归档时间起始
     * @param archiveTimeEnd 归档时间结束
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页结果（包含工序记录）
     */
    PagedResult<ProductionPlanWithRecordsDto> searchWithDetails(ProductionPlan productionPlan,
                                                                String keyword,
                                                                LocalDateTime createdTimeStart, LocalDateTime createdTimeEnd,
                                                                LocalDateTime updatedTimeStart, LocalDateTime updatedTimeEnd,
                                                                LocalDateTime productionStartTimeStart, LocalDateTime productionStartTimeEnd,
                                                                LocalDateTime productionEndTimeStart, LocalDateTime productionEndTimeEnd,
                                                                LocalDateTime inspectionStartTimeStart, LocalDateTime inspectionStartTimeEnd,
                                                                LocalDateTime inspectionEndTimeStart, LocalDateTime inspectionEndTimeEnd,
                                                                LocalDateTime outboundTimeStart, LocalDateTime outboundTimeEnd,
                                                                LocalDateTime archiveTimeStart, LocalDateTime archiveTimeEnd,
                                                                String timeFieldType, LocalDateTime timeStart, LocalDateTime timeEnd,
                                                                int pageNum, int pageSize);

    /**
     * 根据生产任务（工单）查询关联的生产计划
     * <p>
     * 按工单ID查询工单，取其关联的计划ID（planId），再查询对应的生产计划并返回
     * </p>
     *
     * @param workOrderId 生产任务（工单）ID（必填）
     * @return 关联的生产计划，工单未关联计划或计划不存在时抛出异常
     */
    ProductionPlan getPlanByWorkOrder(Long workOrderId);
}
