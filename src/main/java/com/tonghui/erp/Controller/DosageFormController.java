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
 * 提供药品剂型的CRUD操作、名称模糊搜索及带子表查询功能，用于药品基础信息中的剂型维护
 * </p>
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────────┬────────┬─────────────────────────────────────┐
 * │ #  │ 接口                                 │ 方法   │ 说明                                │
 * ├────┼──────────────────────────────────────┼────────┼─────────────────────────────────────┤
 * │ 1  │ /api/DosageForm                      │ GET   │ 获取所有剂型列表（分页）            │
 * │ 2  │ /api/DosageForm/{id}                 │ GET   │ 根据ID获取剂型详情                  │
 * │ 3  │ /api/DosageForm                      │ POST  │ 新增剂型                            │
 * │ 4  │ /api/DosageForm/{id}                 │ PUT   │ 修改剂型                            │
 * │ 5  │ /api/DosageForm/{id}                 │ DELETE│ 删除剂型                            │
 * │ 6  │ /api/DosageForm/search               │ GET   │ 根据药品剂型名称模糊查询            │
 * │ 7  │ /api/DosageForm/search-with-details  │ GET   │ 带子表查询剂型                      │
 * └────┴──────────────────────────────────────┴────────┴─────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/DosageForm")
public class DosageFormController extends BaseCrudController<DosageForm, DosageForm, Long> {
    
    // region 字段和构造方法
    // ===================================
    // 字段和构造方法
    // ===================================

    /**
     * 药品剂型服务
     */
    private final DosageFormService dosageFormService;

    @Autowired
    public DosageFormController(DosageFormService dosageFormService) {
        this.dosageFormService = dosageFormService;
    }
    
    // endregion

    // region CRUD操作实现方法
    // ===================================
    // CRUD操作实现方法
    // ===================================
    
    /**
     * 获取所有剂型列表（分页）
     *
     * 示例请求：
     * GET /api/DosageForm?pageIndex=0&pageSize=10
     *
     * @param pageIndex 页码，从0开始
     * @param pageSize 每页大小
     * @return PagedResult&lt;DosageForm&gt; 分页结果，包含剂型列表
     */
    @Override
    protected PagedResult<DosageForm> getAllData(int pageIndex, int pageSize) {
        PageRequestDto pageRequest = new PageRequestDto();
        pageRequest.setPageIndex(pageIndex);
        pageRequest.setPageSize(pageSize);
        return dosageFormService.searchByName(null, pageRequest);
    }

    /**
     * 根据ID获取剂型详情
     *
     * 示例请求：
     * GET /api/DosageForm/1
     *
     * @param id 剂型ID
     * @return DosageForm 剂型详情
     */
    @Override
    protected DosageForm getDataById(Long id) {
        return dosageFormService.getById(id);
    }

    /**
     * 新增剂型
     * <p>
     * 新增前会检查剂型名称是否已存在，若已存在则抛出异常
     * </p>
     *
     * 示例请求：
     * POST /api/DosageForm
     * Content-Type: application/json
     * {
     *   "dosageName": "片剂",
     *   "description": "固体制剂，便于服用和储存"
     * }
     *
     * @param dosageForm 剂型实体对象
     * @return DosageForm 新增的剂型
     */
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

    /**
     * 修改剂型
     * <p>
     * 修改前会检查剂型名称是否被其他记录使用
     * </p>
     *
     * 示例请求：
     * PUT /api/DosageForm/1
     * Content-Type: application/json
     * {
     *   "dosageName": "片剂（更新）",
     *   "description": "固体制剂，便于服用和储存（更新）"
     * }
     *
     * @param id 剂型ID
     * @param dosageForm 剂型实体对象
     * @return DosageForm 修改后的剂型
     */
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

    /**
     * 删除剂型
     *
     * 示例请求：
     * DELETE /api/DosageForm/1
     *
     * @param id 剂型ID
     * @return boolean 删除结果
     */
    @Override
    protected boolean doDelete(Long id) {
        // 删除药品剂型
        return dosageFormService.removeById(id);
    }
    
    // endregion

    // region 剂型查询接口方法
    // ===================================
    // 剂型查询接口方法
    // ===================================
    
    /**
     * 根据药品剂型名称模糊查询药品剂型
     *
     * 示例请求：
     * GET /api/DosageForm/search?dosageFormName=片剂&pageIndex=0&pageSize=10
     *
     * @param dosageFormName 药品剂型名称关键词
     * @param pageRequest 分页请求参数
     * @return ApiResponse&lt;PagedResult&lt;DosageForm&gt;&gt; 药品剂型列表（分页）
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
    
    // endregion

    // region 带子表查询接口
    // ===================================
    // 带子表查询接口
    // ===================================

    /**
     * 带子表查询剂型
     *
     * 示例请求：
     * GET /api/DosageForm/search-with-details?dosageName=片剂&pageIndex=0&pageSize=10
     *
     * @param dosageForm 剂型查询条件对象
     * @param pageIndex 页码，从0开始
     * @param pageSize 每页大小
     * @return ApiResponse&lt;PagedResult&lt;DosageFormWithDetailsDto&gt;&gt; 剂型列表（含子表信息）
     */
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

    // endregion
}
