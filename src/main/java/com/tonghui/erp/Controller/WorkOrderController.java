package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.WorkOrder;
import com.tonghui.erp.Service.WorkOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 工单控制器
 * <p>
 * 提供工单的增删改查、高级查询以及工单编号自动生成等功能
 * </p>
 *
 * 接口清单：
 * ┌────┬────────────────────────────────────┬────────┬──────────────────────────────┐
 * │ #  │ 接口                               │ 方法   │ 说明                         │
 * ├────┼────────────────────────────────────┼────────┼──────────────────────────────┤
 * │ 1  │ /api/work-orders                   │ GET    │ 分页查询所有工单             │
 * │ 2  │ /api/work-orders/{id}              │ GET    │ 获取工单详情                 │
 * │ 3  │ /api/work-orders                   │ POST   │ 新增工单                     │
 * │ 4  │ /api/work-orders/{id}              │ PUT    │ 修改工单                     │
 * │ 5  │ /api/work-orders/{id}              │ DELETE │ 删除工单                     │
 * │ 6  │ /api/work-orders/search            │ GET    │ 高级查询工单（支持多条件）   │
 * │ 7  │ /api/work-orders/generate-code     │ GET    │ 自动生成工单编号             │
 * └────┴────────────────────────────────────┴────────┴──────────────────────────────┘
 */
@RestController
@RequestMapping("/api/work-orders")
public class WorkOrderController extends BaseCrudController<WorkOrder, WorkOrder, Long> {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 工单服务
     */
    @Autowired
    private WorkOrderService workOrderService;

    // endregion

    // region 基础CRUD实现
    // ===================================
    // 基础CRUD实现
    // ===================================

    @Override
    protected PagedResult<WorkOrder> getAllData(int pageIndex, int pageSize) {
        // 页码从0开始的处理，确保不为负数
        int safePageIndex = Math.max(0, pageIndex);
        // 当pageSize<=0时，设置一个合理的默认值
        int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);

        // 使用WorkOrderService的queryWorkOrders方法进行查询
        WorkOrder workOrder = new WorkOrder();
        Page<WorkOrder> pageResult = workOrderService.queryWorkOrders(workOrder,
                null, null, null, null, safePageIndex, safePageSize);

        // 转换为PagedResult
        PagedResult<WorkOrder> pagedResult = new PagedResult<>();
        pagedResult.setItems(pageResult.getRecords());
        pagedResult.setTotalCount(pageResult.getTotal());
        pagedResult.setPageIndex(safePageIndex);
        pagedResult.setPageSize((int) pageResult.getSize());

        return pagedResult;
    }

    @Override
    protected WorkOrder getDataById(Long id) {
        return workOrderService.getWorkOrderById(id);
    }

    @Override
    protected WorkOrder doCreate(WorkOrder workOrder) {
        workOrderService.addWorkOrder(workOrder);
        return workOrder;
    }

    @Override
    protected WorkOrder doUpdate(Long id, WorkOrder workOrder) {
        workOrder.setWorkOrderId(id);
        workOrderService.updateWorkOrder(workOrder);
        return workOrder;
    }

    @Override
    protected boolean doDelete(Long id) {
        try {
            workOrderService.deleteWorkOrder(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // endregion

    // region 高级查询
    // ===================================
    // 高级查询
    // ===================================

    /**
     * 高级查询工单（支持多条件 + 分页）
     * <p>
     * 可选查询条件：workOrderCode（模糊匹配）、workOrderName（模糊匹配）、
     * preparationId（制剂ID精确匹配）、preparationCode（模糊匹配）、
     * preparationName（模糊匹配）、planId（关联计划ID精确匹配）、currentStatus（当前状态精确匹配），
     * 以及 createdTimeStart/End、updatedTimeStart/End 时间范围条件
     * </p>
     *
     * 示例请求：
     * GET /api/work-orders/search?pageIndex=1&pageSize=20&workOrderCode=WO&workOrderName=测试工单
     * GET /api/work-orders/search?pageIndex=1&pageSize=20&planId=1&currentStatus=生产中
     *
     * @param workOrder 查询条件（自动从query参数映射）
     * @param createdTimeStart 创建时间起始（可选）
     * @param createdTimeEnd 创建时间结束（可选）
     * @param updatedTimeStart 更新时间起始（可选）
     * @param updatedTimeEnd 更新时间结束（可选）
     * @param pageIndex 页码
     * @param pageSize  每页大小
     * @return ApiResponse&lt;PagedResult&lt;WorkOrder&gt;&gt; 分页结果
     */
    @GetMapping("/search")
    public ApiResponse<PagedResult<WorkOrder>> queryWorkOrders(WorkOrder workOrder,
                                                               @RequestParam(required = false) java.time.LocalDateTime createdTimeStart,
                                                               @RequestParam(required = false) java.time.LocalDateTime createdTimeEnd,
                                                               @RequestParam(required = false) java.time.LocalDateTime updatedTimeStart,
                                                               @RequestParam(required = false) java.time.LocalDateTime updatedTimeEnd,
                                                               @RequestParam int pageIndex,
                                                               @RequestParam int pageSize) {
        // 页码从0开始的处理，确保不为负数
        int safePageIndex = Math.max(0, pageIndex);
        // 当pageSize<=0时，设置一个合理的默认值
        int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);

        // 获取分页结果
        Page<WorkOrder> pageResult = workOrderService.queryWorkOrders(workOrder,
                createdTimeStart, createdTimeEnd, updatedTimeStart, updatedTimeEnd,
                safePageIndex, safePageSize);

        // 转换为统一的PagedResult格式
        PagedResult<WorkOrder> pagedResult = new PagedResult<>();
        pagedResult.setItems(pageResult.getRecords());
        pagedResult.setTotalCount(pageResult.getTotal());
        pagedResult.setPageIndex(safePageIndex);
        pagedResult.setPageSize((int) pageResult.getSize());

        return success(pagedResult);
    }

    // endregion
    
    // region 工单编号生成
    // ===================================
    // 工单编号生成
    // ===================================
    
    /**
     * 自动生成工单编号
     * <p>
     * 根据系统规则自动生成唯一的工单编号
     * </p>
     *
     * 示例请求：
     * GET /api/work-orders/generate-code
     *
     * @return ApiResponse&lt;String&gt; 工单编号
     */
    @GetMapping("/generate-code")
    public ApiResponse<String> generateWorkOrderCode() {
        String code = workOrderService.generateWorkOrderCode();
        return success(code, "工单编号生成成功");
    }
    
    // endregion
}
