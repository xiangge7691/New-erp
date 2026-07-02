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
 */
@RestController
@RequestMapping("/api/work-orders")
public class WorkOrderController extends BaseCrudController<WorkOrder, WorkOrder, Long> {

    @Autowired
    private WorkOrderService workOrderService;

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

    // #region 高级查询

    /**
     * 高级查询工单（支持多条件 + 分页）
     *
     * 可选查询条件：
     * - workOrderCode：模糊匹配
     * - workOrderName：模糊匹配
     * - preparationId：制剂ID过滤
     * - preparationCode：制剂编码模糊匹配
     * - preparationName：制剂名称模糊匹配
     *
     * 示例请求：
     * GET /work-orders/search?pageIndex=1&pageSize=20&workOrderCode=WO&workOrderName=测试工单
     *
     * @param workOrder 查询条件（自动从query参数映射）
     * @param pageIndex      页码
     * @param pageSize       每页大小
     * @return 分页结果
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

    // #endregion
    
    // #region 工单编号生成
    
    /**
     * 自动生成工单编号
     * 
     * @return 工单编号
     */
    @GetMapping("/generate-code")
    public ApiResponse<String> generateWorkOrderCode() {
        String code = workOrderService.generateWorkOrderCode();
        return success(code, "工单编号生成成功");
    }
    
    // #endregion
}
