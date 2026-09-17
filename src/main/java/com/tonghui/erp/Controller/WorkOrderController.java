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
 * │ 8  │ /api/work-orders/by-out-type       │ GET    │ 按出库类型筛选工单           │
 * │ 9  │ /api/work-orders/{id}/void         │ PUT    │ 作废工单                     │
 * │ 10 │ /api/work-orders/{id}/archive      │ PUT    │ 归档工单                     │
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
                null, null, null, null, null, null, null, safePageIndex, safePageSize);

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
     * preparationName（模糊匹配）、planId（关联计划ID精确匹配）、
     * planName（关联计划名称模糊匹配）、planNumber（关联生产计划编号模糊匹配）、
     * currentStatus（当前状态精确匹配），
     * 以及 createdTimeStart/End、updatedTimeStart/End 时间范围条件
     * </p>
     * <p>
     * keyword 关键字对工单编号、工单名称、制剂编码、制剂名称、
     * 关联计划名称、关联计划编号进行模糊匹配
     * </p>
     *
     * 示例请求：
     * GET /api/work-orders/search?pageIndex=1&pageSize=20&keyword=WO
     * GET /api/work-orders/search?pageIndex=1&pageSize=20&planId=1&currentStatus=生产中
     * GET /api/work-orders/search?pageIndex=1&pageSize=20&planName=益智健脑
     * GET /api/work-orders/search?pageIndex=1&pageSize=20&planNumber=Plan20260730
     * GET /api/work-orders/search?pageIndex=1&pageSize=20&keyword=PP2026
     *
     * @param workOrder 查询条件（自动从query参数映射）
     * @param keyword 关键字（对工单编号、工单名称、制剂编码、制剂名称、关联计划名称、关联计划编号模糊匹配，可选）
     * @param createdTimeStart 创建时间起始（可选）
     * @param createdTimeEnd 创建时间结束（可选）
     * @param updatedTimeStart 更新时间起始（可选）
     * @param updatedTimeEnd 更新时间结束（可选）
     * @param configDateStart 配置日期起始（可选）
     * @param configDateEnd 配置日期结束（可选）
     * @param pageIndex 页码
     * @param pageSize  每页大小
     * @return ApiResponse&lt;PagedResult&lt;WorkOrder&gt;&gt; 分页结果
     */
    @GetMapping("/search")
    public ApiResponse<PagedResult<WorkOrder>> queryWorkOrders(WorkOrder workOrder,
                                                               @RequestParam(required = false) String keyword,
                                                               @RequestParam(required = false) java.time.LocalDateTime createdTimeStart,
                                                               @RequestParam(required = false) java.time.LocalDateTime createdTimeEnd,
                                                               @RequestParam(required = false) java.time.LocalDateTime updatedTimeStart,
                                                               @RequestParam(required = false) java.time.LocalDateTime updatedTimeEnd,
                                                               @RequestParam(required = false) java.time.LocalDateTime configDateStart,
                                                               @RequestParam(required = false) java.time.LocalDateTime configDateEnd,
                                                               @RequestParam int pageIndex,
                                                               @RequestParam int pageSize) {
        // 页码从0开始的处理，确保不为负数
        int safePageIndex = Math.max(0, pageIndex);
        // 当pageSize<=0时，设置一个合理的默认值
        int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);

        // 获取分页结果
        Page<WorkOrder> pageResult = workOrderService.queryWorkOrders(workOrder,
                keyword, createdTimeStart, createdTimeEnd, updatedTimeStart, updatedTimeEnd,
                configDateStart, configDateEnd,
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

    // region 按出库类型筛选工单
    // ===================================
    // 按出库类型筛选工单
    // ===================================

    /**
     * 按出库类型筛选工单列表（供出库单关联生产任务下拉选择）
     * <p>
     * 销售出库：仅展示"已生产"及之后状态的工单（已生产、检验中、已检验、已放行、已入库、已归档），
     * 因为销售出库的成品必须已经生产完成。
     * 生产领料出库：仅展示"已生产"之前状态的工单（待生产、生产中），
     * 因为领料出库发生在生产过程中。
     * </p>
     *
     * 示例请求：
     * GET /api/work-orders/by-out-type?outType=销售出库&pageIndex=0&pageSize=20
     * GET /api/work-orders/by-out-type?outType=生产领料出库&keyword=GD&pageIndex=0&pageSize=20
     *
     * @param outType   出库类型（销售出库/生产领料出库，必填）
     * @param keyword   关键字（可选，模糊匹配工单编号/制剂编码/制剂名称）
     * @param pageIndex 页码（从0开始，默认0）
     * @param pageSize  每页大小（默认20）
     * @return ApiResponse&lt;PagedResult&lt;WorkOrder&gt;&gt; 符合条件的工单分页结果
     */
    @GetMapping("/by-out-type")
    public ApiResponse<PagedResult<WorkOrder>> getWorkOrdersByOutType(
            @RequestParam String outType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "20") int pageSize) {
        try {
            int safePageIndex = Math.max(0, pageIndex);
            int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);

            Page<WorkOrder> pageResult = workOrderService.getWorkOrdersByOutType(outType, keyword, safePageIndex, safePageSize);

            PagedResult<WorkOrder> pagedResult = new PagedResult<>();
            pagedResult.setItems(pageResult.getRecords());
            pagedResult.setTotalCount(pageResult.getTotal());
            pagedResult.setPageIndex(safePageIndex);
            pagedResult.setPageSize((int) pageResult.getSize());

            return success(pagedResult);
        } catch (Exception ex) {
            return exception(ex, "按出库类型查询工单列表");
        }
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

    // region 工单作废
    // ===================================
    // 工单作废
    // ===================================

    /**
     * 作废工单
     * <p>
     * 将工单状态设置为"作废"，作废后不可再进行其他操作
     * 已归档或已作废的工单不能再次作废
     * </p>
     *
     * 示例请求：
     * PUT /api/work-orders/1/void
     *
     * @param id 工单ID
     * @return ApiResponse&lt;Boolean&gt; 作废结果
     */
    @PutMapping("/{id}/void")
    public ApiResponse<Boolean> voidWorkOrder(@PathVariable Long id) {
        try {
            boolean result = workOrderService.voidWorkOrder(id);
            if (result) {
                return success(true, "作废成功");
            } else {
                return error("作废失败，工单不存在或已归档/已作废");
            }
        } catch (Exception ex) {
            return exception(ex, "作废工单");
        }
    }

    // endregion

    // region 工单归档
    // ===================================
    // 工单归档
    // ===================================

    /**
     * 归档工单
     * <p>
     * 将已入库的工单进行归档，设置归档时间为当前时间，
     * 工单状态自动流转为"已归档"，归档后不可再进行其他操作。
     * 仅"已入库"状态的工单可以归档
     * </p>
     *
     * 示例请求：
     * PUT /api/work-orders/1/archive
     *
     * @param id 工单ID
     * @return ApiResponse&lt;Boolean&gt; 归档结果
     */
    @PutMapping("/{id}/archive")
    public ApiResponse<Boolean> archiveWorkOrder(@PathVariable Long id) {
        try {
            boolean result = workOrderService.archiveWorkOrder(id);
            if (result) {
                return success(true, "归档成功");
            } else {
                return error("归档失败，工单不存在、未入库或已归档/已作废");
            }
        } catch (Exception ex) {
            return exception(ex, "归档工单");
        }
    }

    // endregion
    // endregion
}
