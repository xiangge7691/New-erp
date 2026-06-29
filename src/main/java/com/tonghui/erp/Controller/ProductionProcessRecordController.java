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
 * 提供生产工序记录的增删改查及搜索功能
 */
@RestController
@RequestMapping("/api/process-record")
public class ProductionProcessRecordController extends BaseCrudController<ProductionProcessRecord, ProductionProcessRecord, Long> {

    private final ProductionProcessRecordService productionProcessRecordService;

    @Autowired
    public ProductionProcessRecordController(ProductionProcessRecordService productionProcessRecordService) {
        this.productionProcessRecordService = productionProcessRecordService;
    }

    // CRUD 实现
    @Override
    protected PagedResult<ProductionProcessRecord> getAllData(int pageIndex, int pageSize) {
        PageRequestDto pageRequest = new PageRequestDto();
        pageRequest.setPageIndex(pageIndex);
        pageRequest.setPageSize(pageSize);
        return productionProcessRecordService.listByStatus(null, pageRequest);
    }

    @Override
    protected ProductionProcessRecord getDataById(Long id) {
        return productionProcessRecordService.getById(id);
    }

    @Override
    protected ProductionProcessRecord doCreate(ProductionProcessRecord record) {
        // 验证生产计划是否存在（可选）
        // 检查工序顺序等逻辑

        // 设置创建人 ID 和更新人 ID
        Long currentUserId = EntityUtils.getCurrentUserId();
        if (currentUserId != null) {
            record.setCreatorId(currentUserId);
            record.setUpdaterId(currentUserId);
        }
        
        // 设置创建时间和更新时间
        LocalDateTime now = LocalDateTime.now();
        record.setCreatedTime(now);
        record.setUpdatedTime(now);

        productionProcessRecordService.save(record);
        return record;
    }

    @Override
    protected ProductionProcessRecord doUpdate(Long id, ProductionProcessRecord record) {
        ProductionProcessRecord existing = productionProcessRecordService.getById(id);
        if (existing == null) {
            throw new RuntimeException("工序记录不存在");
        }

        // 设置更新人 ID 和更新时间
        Long currentUserId = EntityUtils.getCurrentUserId();
        if (currentUserId != null) {
            record.setUpdaterId(currentUserId);
        }
        record.setUpdatedTime(LocalDateTime.now());

        record.setRecordId(id);
        productionProcessRecordService.updateById(record);
        return record;
    }

    @Override
    protected boolean doDelete(Long id) {
        return productionProcessRecordService.removeById(id);
    }

    /**
     * 根据生产计划 ID 获取工序记录列表
     * @param planId 生产计划 ID
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
     * 根据生产计划 ID 分页获取工序记录
     * @param planId 生产计划 ID
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

    /**
     * 按工序名称搜索工序记录
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

    /**
     * 作废工序记录
     * @param id 工序记录 ID
     * @param updaterId 更新人 ID（可选，不传则使用当前登录用户）
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
}
