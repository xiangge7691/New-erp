package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.WorkOrder;

import java.time.LocalDateTime;

/**
* @author 87954
* @description 针对表【work_order(工单表)】的数据库操作Service
* @createDate 2025-12-01 13:48:18
*/
public interface WorkOrderService extends IService<WorkOrder> {
    
    /**
     * 获取工单列表（分页）
     * @param pageRequestDto 分页请求参数
     * @return 分页结果
     */
    PagedResult<WorkOrder> getWorkOrderList(PageRequestDto pageRequestDto);

    /**
     * 添加工单
     * @param workOrder 工单信息
     * @return 是否添加成功
     */
    boolean addWorkOrder(WorkOrder workOrder);

    /**
     * 更新工单
     * @param workOrder 工单信息
     * @return 是否更新成功
     */
    boolean updateWorkOrder(WorkOrder workOrder);

    /**
     * 删除工单
     * @param workOrderId 工单ID
     * @return 是否删除成功
     */
    boolean deleteWorkOrder(Long workOrderId);

    /**
     * 根据ID获取工单
     * @param workOrderId 工单ID
     * @return 工单信息
     */
    WorkOrder getWorkOrderById(Long workOrderId);

    /**
     * 查询工单（支持多条件 + 分页 + 时间范围）
     * @param workOrder 查询条件
     * @param createdTimeStart 创建时间起始
     * @param createdTimeEnd 创建时间结束
     * @param updatedTimeStart 更新时间起始
     * @param updatedTimeEnd 更新时间结束
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    Page<WorkOrder> queryWorkOrders(WorkOrder workOrder,
                                    LocalDateTime createdTimeStart, LocalDateTime createdTimeEnd,
                                    LocalDateTime updatedTimeStart, LocalDateTime updatedTimeEnd,
                                    int pageNum, int pageSize);
    
    /**
     * 生成工单编号
     * @return 工单编号
     */
    String generateWorkOrderCode();
}
