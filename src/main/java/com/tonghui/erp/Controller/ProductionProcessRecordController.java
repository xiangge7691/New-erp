package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.utils.EntityUtils;
import com.tonghui.erp.Data.Entity.ProductionProcessRecord;
import com.tonghui.erp.Service.ProductionProcessRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 生产工序记录控制器
 * <p>
 * 提供生产工序记录的增删改查及搜索功能，支持按生产计划查询工序、按工序名称和操作人搜索，
 * 以及工序记录作废和批量保存操作
 * </p>
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────────────┬────────┬──────────────────────────────────────┐
 * │ #  │ 接口                                     │ 方法   │ 说明                                 │
 * ├────┼──────────────────────────────────────────┼────────┼──────────────────────────────────────┤
 * │ 1  │ /api/process-record                      │ GET    │ 获取所有工序记录（分页）             │
 * │ 2  │ /api/process-record/{id}                 │ GET    │ 根据ID获取工序记录详情               │
 * │ 3  │ /api/process-record                      │ POST   │ 新增工序记录                         │
 * │ 4  │ /api/process-record/{id}                 │ PUT    │ 修改工序记录                         │
 * │ 5  │ /api/process-record/{id}                 │ DELETE │ 删除工序记录                         │
 * │ 6  │ /api/process-record/plan/{planId}        │ GET    │ 根据生产计划ID获取工序记录列表       │
 * │ 7  │ /api/process-record/plan/{planId}/paged  │ GET    │ 根据生产计划ID分页获取工序记录       │
 * │ 8  │ /api/process-record/search/process-name  │ GET    │ 按工序名称搜索工序记录               │
 * │ 9  │ /api/process-record/search/operator      │ GET    │ 按操作人姓名搜索工序记录             │
 * │ 10 │ /api/process-record/{id}/cancel          │ PUT    │ 作废工序记录                         │
 * │ 11 │ /api/process-record/batch/plan/{planId}  │ POST   │ 批量保存工序记录（先删后增）         │
 * └────┴──────────────────────────────────────────┴────────┴──────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/process-record")
public class ProductionProcessRecordController extends BaseCrudController<ProductionProcessRecord, ProductionProcessRecord, Long> {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 生产工序记录服务
     */
    private final ProductionProcessRecordService productionProcessRecordService;

    /**
     * 构造方法，注入生产工序记录服务
     *
     * @param productionProcessRecordService 生产工序记录服务
     */
    @Autowired
    public ProductionProcessRecordController(ProductionProcessRecordService productionProcessRecordService) {
        this.productionProcessRecordService = productionProcessRecordService;
    }

    // endregion

    // region CRUD操作实现方法
    // ===================================
    // CRUD操作实现方法
    // ===================================

    /**
     * 获取所有工序记录（分页）
     *
     * 示例请求：
     * GET /api/process-record?pageIndex=0&pageSize=10
     *
     * @param pageIndex 页码，从0开始
     * @param pageSize 每页大小
     * @return 分页结果，包含工序记录列表
     */
    @Override
    protected PagedResult<ProductionProcessRecord> getAllData(int pageIndex, int pageSize) {
        PageRequestDto pageRequest = new PageRequestDto();
        pageRequest.setPageIndex(pageIndex);
        pageRequest.setPageSize(pageSize);
        return productionProcessRecordService.listByStatus(null, pageRequest);
    }

    /**
     * 根据ID获取工序记录详情
     *
     * 示例请求：
     * GET /api/process-record/1
     *
     * @param id 工序记录ID
     * @return 工序记录详情
     */
    @Override
    protected ProductionProcessRecord getDataById(Long id) {
        return productionProcessRecordService.getById(id);
    }

    /**
     * 新增工序记录
     *
     * 示例请求：
     * POST /api/process-record
     * Content-Type: application/json
     * {
     *   "planId": 1,
     *   "processName": "配料",
     *   "operatorName": "张三",
     *   "processOrder": 1
     * }
     *
     * @param record 工序记录实体对象
     * @return 新增的工序记录
     */
    @Override
    protected ProductionProcessRecord doCreate(ProductionProcessRecord record) {
        // 设置创建人 ID 和更新人 ID
        Long currentUserId = EntityUtils.getCurrentUserId();
        if (currentUserId != null) {
            record.setCreatedBy(currentUserId);
            record.setUpdatedBy(currentUserId);
        }
        
        // 设置创建时间和更新时间
        LocalDateTime now = LocalDateTime.now();
        record.setCreatedTime(now);
        record.setUpdatedTime(now);

        productionProcessRecordService.save(record);
        return record;
    }

    /**
     * 修改工序记录
     *
     * 示例请求：
     * PUT /api/process-record/1
     * Content-Type: application/json
     * {
     *   "processName": "配料（已更新）",
     *   "operatorName": "李四"
     * }
     *
     * @param id 工序记录ID
     * @param record 工序记录实体对象
     * @return 修改后的工序记录
     */
    @Override
    protected ProductionProcessRecord doUpdate(Long id, ProductionProcessRecord record) {
        ProductionProcessRecord existing = productionProcessRecordService.getById(id);
        if (existing == null) {
            throw new RuntimeException("工序记录不存在");
        }

        // 设置更新人 ID 和更新时间
        Long currentUserId = EntityUtils.getCurrentUserId();
        if (currentUserId != null) {
            record.setUpdatedBy(currentUserId);
        }
        record.setUpdatedTime(LocalDateTime.now());

        record.setRecordId(id);
        productionProcessRecordService.updateById(record);
        return record;
    }

    /**
     * 删除工序记录
     *
     * 示例请求：
     * DELETE /api/process-record/1
     *
     * @param id 工序记录ID
     * @return 删除结果
     */
    @Override
    protected boolean doDelete(Long id) {
        return productionProcessRecordService.removeById(id);
    }

    // endregion

    // region 工序记录查询接口
    // ===================================
    // 工序记录查询接口
    // ===================================

    /**
     * 根据生产计划ID获取工序记录列表
     *
     * 示例请求：
     * GET /api/process-record/plan/1
     *
     * @param planId 生产计划ID
     * @return 该生产计划下的所有工序记录列表
     */
    @GetMapping("/plan/{planId}")
    public ApiResponse<List<ProductionProcessRecord>> getByPlanId(@PathVariable Integer planId) {
        try {
            List<ProductionProcessRecord> result = productionProcessRecordService.listByPlanId(planId);
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "获取生产计划工序记录");
        }
    }

    /**
     * 根据生产计划ID分页获取工序记录
     *
     * 示例请求：
     * GET /api/process-record/plan/1/paged?pageIndex=0&pageSize=10
     *
     * @param planId 生产计划ID
     * @param pageRequest 分页请求参数（页码、页面大小）
     * @return 工序记录列表（分页）
     */
    @GetMapping("/plan/{planId}/paged")
    public ApiResponse<PagedResult<ProductionProcessRecord>> getByPlanIdPaged(
            @PathVariable Integer planId,
            @ModelAttribute PageRequestDto pageRequest) {
        try {
            pageRequest = processPageRequest(pageRequest);
            PagedResult<ProductionProcessRecord> result =
                productionProcessRecordService.listByPlanIdPaged(planId, pageRequest);
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "分页获取生产计划工序记录");
        }
    }

    // endregion

    // region 工序记录搜索接口
    // ===================================
    // 工序记录搜索接口
    // ===================================

    /**
     * 按工序名称搜索工序记录
     *
     * 示例请求：
     * GET /api/process-record/search/process-name?processName=配料&pageIndex=0&pageSize=10
     *
     * @param processName 工序名称（可选，支持模糊搜索）
     * @param pageRequest 分页请求参数（页码、页面大小）
     * @return 工序记录列表（分页）
     */
    @GetMapping("/search/process-name")
    public ApiResponse<PagedResult<ProductionProcessRecord>> searchByProcessName(
            @RequestParam(required = false) String processName,
            @ModelAttribute PageRequestDto pageRequest) {
        try {
            pageRequest = processPageRequest(pageRequest);
            PagedResult<ProductionProcessRecord> result =
                productionProcessRecordService.searchByProcessName(processName, pageRequest);
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "搜索工序记录");
        }
    }

    /**
     * 按操作人姓名搜索工序记录
     *
     * 示例请求：
     * GET /api/process-record/search/operator?operatorName=张三&pageIndex=0&pageSize=10
     *
     * @param operatorName 操作人姓名（可选，支持模糊搜索）
     * @param pageRequest 分页请求参数（页码、页面大小）
     * @return 工序记录列表（分页）
     */
    @GetMapping("/search/operator")
    public ApiResponse<PagedResult<ProductionProcessRecord>> searchByOperatorName(
            @RequestParam(required = false) String operatorName,
            @ModelAttribute PageRequestDto pageRequest) {
        try {
            pageRequest = processPageRequest(pageRequest);
            PagedResult<ProductionProcessRecord> result =
                productionProcessRecordService.searchByOperatorName(operatorName, pageRequest);
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "搜索操作人记录");
        }
    }

    // endregion

    // region 工序记录操作接口
    // ===================================
    // 工序记录操作接口
    // ===================================

    /**
     * 作废工序记录
     *
     * 示例请求：
     * PUT /api/process-record/1/cancel
     * PUT /api/process-record/1/cancel?updaterId=1
     *
     * @param id 工序记录ID
     * @param updaterId 更新人ID（可选，不传则使用当前登录用户）
     * @return 作废结果
     */
    @PutMapping("/{id}/cancel")
    public ApiResponse<Boolean> cancelRecord(
            @PathVariable Long id,
            @RequestParam(required = false) Long updaterId) {
        try {
            // 如果没有提供更新人 ID，则使用当前登录用户 ID
            if (updaterId == null) {
                updaterId = EntityUtils.getCurrentUserId();
            }
            
            boolean result = productionProcessRecordService.cancelRecord(id, updaterId);
            if (result) {
                return success(true, "作废成功");
            } else {
                return error("作废失败");
            }
        } catch (Exception ex) {
            return exception(ex, "作废旧记录");
        }
    }

    /**
     * 批量保存工序记录（先删后增）
     *
     * 示例请求：
     * POST /api/process-record/batch/plan/1
     * Content-Type: application/json
     * [
     *   {
     *     "processName": "配料",
     *     "operatorName": "张三",
     *     "processOrder": 1
     *   },
     *   {
     *     "processName": "灌装",
     *     "operatorName": "李四",
     *     "processOrder": 2
     *   }
     * ]
     *
     * @param planId 生产计划ID
     * @param records 工序记录列表
     * @return 保存结果
     */
    @PostMapping("/batch/plan/{planId}")
    public ApiResponse<List<ProductionProcessRecord>> batchSaveByPlanId(
            @PathVariable Integer planId,
            @RequestBody List<ProductionProcessRecord> records) {
        try {
            List<ProductionProcessRecord> result = productionProcessRecordService.batchSaveByPlanId(planId, records);
            return success(result, "批量保存成功");
        } catch (Exception ex) {
            return exception(ex, "批量保存工序记录");
        }
    }

    // endregion
}
