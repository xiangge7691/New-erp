package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.DosageForm.DosageFormWithDetailsDto;
import com.tonghui.erp.Service.DosageFormService;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.DosageForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 药品剂型管理控制器
 * <p>
 * 提供药品剂型的增删改查和模糊查询功能
 * </p>
 */
@RestController
@RequestMapping("/api/DosageForm")
public class DosageFormController extends BaseCrudController<DosageForm, DosageForm, Long> {
    
    //#region 字段和构造方法
    // ===================================
    // 字段和构造方法
    // ===================================
    
    private final DosageFormService dosageFormService;

    @Autowired
    public DosageFormController(DosageFormService dosageFormService) {
        this.dosageFormService = dosageFormService;
    }
    
    //#endregion

    //#region CRUD操作实现方法
    // ===================================
    // CRUD操作实现方法
    // ===================================
    
    @Override
    protected PagedResult<DosageForm> getAllData(int pageIndex, int pageSize) {
        PageRequestDto pageRequest = new PageRequestDto();
        pageRequest.setPageIndex(pageIndex);
        pageRequest.setPageSize(pageSize);
        return dosageFormService.searchByName(null, pageRequest);
    }

    @Override
    protected DosageForm getDataById(Long id) {
        return dosageFormService.getById(id);
    }

    @Override
    protected DosageForm doCreate(DosageForm dosageForm) {
        // 检查药品剂型名称是否已存在
        LambdaQueryWrapper<DosageForm> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DosageForm::getDosageName, dosageForm.getDosageName());
        if (dosageFormService.getOne(queryWrapper) != null) {
            throw new RuntimeException("药品剂型名称已存在");
        }

        // 添加药品剂型到数据库
        boolean result = dosageFormService.save(dosageForm);

        if (!result) {
            throw new RuntimeException("创建药品剂型失败");
        }

        return dosageForm;
    }

    @Override
    protected DosageForm doUpdate(Long id, DosageForm dosageForm) {
        // 检查药品剂型名称是否被其他药品剂型使用
        LambdaQueryWrapper<DosageForm> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DosageForm::getDosageName, dosageForm.getDosageName());
        DosageForm dosageFormWithSameName = dosageFormService.getOne(queryWrapper);
        if (dosageFormWithSameName != null && !dosageFormWithSameName.getDosageId().equals(id)) {
            throw new RuntimeException("药品剂型名称已存在");
        }

        // 更新药品剂型信息
        dosageForm.setDosageId(id);

        // 更新药品剂型
        boolean result = dosageFormService.updateById(dosageForm);

        if (!result) {
            throw new RuntimeException("更新药品剂型失败");
        }

        return dosageForm;
    }

    @Override
    protected boolean doDelete(Long id) {
        // 删除药品剂型
        return dosageFormService.removeById(id);
    }
    
    //#endregion

    //#region 剂型查询接口方法
    // ===================================
    // 剂型查询接口方法
    // ===================================
    
    /**
     * 根据药品剂型名称模糊查询药品剂型
     * 支持按药品剂型名称进行模糊查询和分页
     *
     * @param dosageFormName 药品剂型名称关键词
     * @param pageRequest 分页请求参数
     * @return 药品剂型列表
     */
    @GetMapping("/search")
    public ApiResponse<PagedResult<DosageForm>> searchDosageForms(
            @RequestParam(required = false) String dosageFormName,
            @ModelAttribute PageRequestDto pageRequest) {
        try {
            pageRequest = processPageRequest(pageRequest);
            PagedResult<DosageForm> result = dosageFormService.searchByName(dosageFormName, pageRequest);
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "搜索药品剂型");
        }
    }
    
    //#endregion

    //#region 带子表查询

    @GetMapping("/search-with-details")
    public ApiResponse<PagedResult<DosageFormWithDetailsDto>> searchWithDetails(DosageForm dosageForm,
                                                                                @RequestParam int pageIndex,
                                                                                @RequestParam int pageSize) {
        try {
            int safePageIndex = Math.max(0, pageIndex);
            int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);
            PagedResult<DosageFormWithDetailsDto> result = dosageFormService.searchWithDetails(dosageForm, safePageIndex, safePageSize);
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "查询失败");
        }
    }

    //#endregion
}
