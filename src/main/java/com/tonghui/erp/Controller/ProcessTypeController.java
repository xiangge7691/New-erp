package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.ProcessTypeWithDetailsDto;
import com.tonghui.erp.Common.utils.EntityUtils;
import com.tonghui.erp.Data.Entity.ProcessType;
import com.tonghui.erp.Service.ProcessTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工序类型控制器
 * <p>
 * 提供工序类型的CRUD操作、名称搜索、启用列表查询及带子表查询功能，用于生产工序的基础信息管理
 * </p>
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────────┬────────┬─────────────────────────────────────┐
 * │ #  │ 接口                                 │ 方法   │ 说明                                │
 * ├────┼──────────────────────────────────────┼────────┼─────────────────────────────────────┤
 * │ 1  │ /api/process-type                    │ GET   │ 获取所有工序类型列表（分页）        │
 * │ 2  │ /api/process-type/{id}               │ GET   │ 根据ID获取工序类型详情              │
 * │ 3  │ /api/process-type                    │ POST  │ 新增工序类型                        │
 * │ 4  │ /api/process-type/{id}               │ PUT   │ 修改工序类型                        │
 * │ 5  │ /api/process-type/{id}               │ DELETE│ 删除工序类型                        │
 * │ 6  │ /api/process-type/search             │ GET   │ 搜索工序类型                        │
 * │ 7  │ /api/process-type/active             │ GET   │ 获取所有启用的工序类型列表          │
 * │ 8  │ /api/process-type/search-with-details│ GET   │ 带子表查询工序类型                  │
 * └────┴──────────────────────────────────────┴────────┴─────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/process-type")
public class ProcessTypeController extends BaseCrudController<ProcessType, ProcessType, Integer> {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 工序类型服务
     */
    private final ProcessTypeService processTypeService;

    @Autowired
    public ProcessTypeController(ProcessTypeService processTypeService) {
        this.processTypeService = processTypeService;
    }

    // endregion

    // region CRUD操作实现
    // ===================================
    // CRUD操作实现
    // ===================================

    /**
     * 获取所有工序类型列表（分页）
     *
     * 示例请求：
     * GET /api/process-type?pageIndex=0&pageSize=10
     *
     * @param pageIndex 页码，从0开始
     * @param pageSize 每页大小
     * @return PagedResult&lt;ProcessType&gt; 分页结果，包含工序类型列表
     */
    @Override
    protected PagedResult<ProcessType> getAllData(int pageIndex, int pageSize) {
        PageRequestDto pageRequest = new PageRequestDto();
        pageRequest.setPageIndex(pageIndex);
        pageRequest.setPageSize(pageSize);
        return processTypeService.searchByName(null, pageRequest);
    }

    /**
     * 根据ID获取工序类型详情
     *
     * 示例请求：
     * GET /api/process-type/1
     *
     * @param id 工序类型ID
     * @return ProcessType 工序类型详情
     */
    @Override
    protected ProcessType getDataById(Integer id) {
        return processTypeService.getById(id);
    }

    /**
     * 新增工序类型
     * <p>
     * 新增前会检查编码是否已存在，自动设置创建人和创建时间
     * </p>
     *
     * 示例请求：
     * POST /api/process-type
     * Content-Type: application/json
     * {
     *   "processCode": "PT001",
     *   "processName": "配制",
     *   "description": "药品配制工序",
     *   "status": 1
     * }
     *
     * @param processType 工序类型实体对象
     * @return ProcessType 新增的工序类型
     */
    @Override
    protected ProcessType doCreate(ProcessType processType) {
        // 检查编码是否已存在
        if (processTypeService.getByCode(processType.getProcessCode()) != null) {
            throw new RuntimeException("工序类型编码已存在");
        }

        // 清理已软删除的相同编码记录（避免唯一键冲突）
        if (processType.getProcessCode() != null && !processType.getProcessCode().isEmpty()) {
            processTypeService.cleanSoftDeletedByProcessCode(processType.getProcessCode());
        }
        // 清理已软删除的相同名称记录（避免唯一键冲突）
        if (processType.getProcessName() != null && !processType.getProcessName().isEmpty()) {
            processTypeService.cleanSoftDeletedByProcessName(processType.getProcessName());
        }

        // 设置创建人 ID 和创建时间
        Long currentUserId = EntityUtils.getCurrentUserId();
        if (currentUserId != null) {
            processType.setCreatedBy(currentUserId);
            processType.setUpdatedBy(currentUserId);
        }
        
        LocalDateTime now = LocalDateTime.now();
        processType.setCreatedTime(now);
        processType.setUpdatedTime(now);

        processTypeService.save(processType);
        return processType;
    }

    /**
     * 修改工序类型
     * <p>
     * 修改前会检查工序类型是否存在以及编码是否被其他记录使用
     * </p>
     *
     * 示例请求：
     * PUT /api/process-type/1
     * Content-Type: application/json
     * {
     *   "processCode": "PT001",
     *   "processName": "配制（更新）",
     *   "description": "药品配制工序（更新）"
     * }
     *
     * @param id 工序类型ID
     * @param processType 工序类型实体对象
     * @return ProcessType 修改后的工序类型
     */
    @Override
    protected ProcessType doUpdate(Integer id, ProcessType processType) {
        ProcessType existing = processTypeService.getById(id);
        if (existing == null) {
            throw new RuntimeException("工序类型不存在");
        }

        // 检查编码是否被其他记录使用
        ProcessType byCode = processTypeService.getByCode(processType.getProcessCode());
        if (byCode != null && !byCode.getProcessId().equals(id)) {
            throw new RuntimeException("工序类型编码已存在");
        }

        // 设置更新人 ID 和更新时间
        Long currentUserId = EntityUtils.getCurrentUserId();
        if (currentUserId != null) {
            processType.setUpdatedBy(currentUserId);
        }
        processType.setUpdatedTime(LocalDateTime.now());

        processType.setProcessId(id);
        processTypeService.updateById(processType);
        return processType;
    }

    /**
     * 删除工序类型
     *
     * 示例请求：
     * DELETE /api/process-type/1
     *
     * @param id 工序类型ID
     * @return boolean 删除结果
     */
    @Override
    protected boolean doDelete(Integer id) {
        return processTypeService.removeById(id);
    }

    // endregion

    // region 高级查询接口
    // ===================================
    // 高级查询接口
    // ===================================

    /**
     * 搜索工序类型
     *
     * 示例请求：
     * GET /api/process-type/search?processName=配制&pageIndex=0&pageSize=10
     *
     * @param processName 工序名称（可选，支持模糊搜索）
     * @param pageRequest 分页请求参数（页码、页面大小）
     * @return ApiResponse&lt;PagedResult&lt;ProcessType&gt;&gt; 工序类型列表（分页）
     */
    @GetMapping("/search")
    public ApiResponse<PagedResult<ProcessType>> searchProcessTypes(
            @RequestParam(required = false) String processName,
            @ModelAttribute PageRequestDto pageRequest) {
        try {
            pageRequest = processPageRequest(pageRequest);
            PagedResult<ProcessType> result = processTypeService.searchByName(processName, pageRequest);
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "搜索工序类型");
        }
    }

    /**
     * 获取所有启用的工序类型列表
     * <p>
     * 仅返回状态为启用的工序类型，用于下拉选择等场景
     * </p>
     *
     * 示例请求：
     * GET /api/process-type/active
     *
     * @return ApiResponse&lt;List&lt;ProcessType&gt;&gt; 启用的工序类型列表
     */
    @GetMapping("/active")
    public ApiResponse<List<ProcessType>> listActiveProcessTypes() {
        try {
            List<ProcessType> result = processTypeService.listActive();
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "获取启用工序类型");
        }
    }

    // endregion

    // region 带子表查询接口
    // ===================================
    // 带子表查询接口
    // ===================================

    /**
     * 带子表查询工序类型
     *
     * 示例请求：
     * GET /api/process-type/search-with-details?processName=配制&pageIndex=0&pageSize=10
     *
     * @param processType 工序类型查询条件对象
     * @param pageIndex 页码，从0开始
     * @param pageSize 每页大小
     * @return ApiResponse&lt;PagedResult&lt;ProcessTypeWithDetailsDto&gt;&gt; 工序类型列表（含子表信息）
     */
    @GetMapping("/search-with-details")
    public ApiResponse<PagedResult<ProcessTypeWithDetailsDto>> searchWithDetails(ProcessType processType,
                                                                                 @RequestParam int pageIndex,
                                                                                 @RequestParam int pageSize) {
        try {
            int safePageIndex = Math.max(0, pageIndex);
            int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);
            PagedResult<ProcessTypeWithDetailsDto> result = processTypeService.searchWithDetails(processType, safePageIndex, safePageSize);
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "查询失败");
        }
    }

    // endregion
}
