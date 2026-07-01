package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.utils.EntityUtils;
import com.tonghui.erp.Data.Entity.ProcessType;
import com.tonghui.erp.Service.ProcessTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工序类型控制器
 * 提供工序类型的增删改查及搜索功能
 */
@RestController
@RequestMapping("/api/process-type")
public class ProcessTypeController extends BaseCrudController<ProcessType, ProcessType, Integer> {

    private final ProcessTypeService processTypeService;

    @Autowired
    public ProcessTypeController(ProcessTypeService processTypeService) {
        this.processTypeService = processTypeService;
    }

    // CRUD 实现
    @Override
    protected PagedResult<ProcessType> getAllData(int pageIndex, int pageSize) {
        PageRequestDto pageRequest = new PageRequestDto();
        pageRequest.setPageIndex(pageIndex);
        pageRequest.setPageSize(pageSize);
        return processTypeService.searchByName(null, pageRequest);
    }

    @Override
    protected ProcessType getDataById(Integer id) {
        return processTypeService.getById(id);
    }

    @Override
    protected ProcessType doCreate(ProcessType processType) {
        // 检查编码是否已存在
        if (processTypeService.getByCode(processType.getProcessCode()) != null) {
            throw new RuntimeException("工序类型编码已存在");
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

    @Override
    protected boolean doDelete(Integer id) {
        return processTypeService.removeById(id);
    }

    /**
     * 搜索工序类型
     * @param processName 工序名称（可选，支持模糊搜索）
     * @param pageRequest 分页请求参数（页码、页面大小）
     * @return 工序类型列表（分页）
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
     * @return 启用的工序类型列表
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
}
