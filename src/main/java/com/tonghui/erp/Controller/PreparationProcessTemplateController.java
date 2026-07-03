package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.Preparation;
import com.tonghui.erp.Data.Entity.PreparationProcessTemplate;
import com.tonghui.erp.Data.Entity.ProcessType;
import com.tonghui.erp.Data.Entity.Unit;
import com.tonghui.erp.Service.PreparationProcessTemplateService;
import com.tonghui.erp.Service.PreparationService;
import com.tonghui.erp.Service.ProcessTypeService;
import com.tonghui.erp.Service.UnitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 制剂工序模版控制器
 * <p>
 * 提供制剂工序模版的CRUD操作、按制剂ID查询及批量保存功能，用于制剂生产中的工序流程模板管理
 * </p>
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────────────────┬────────┬─────────────────────────────────────┐
 * │ #  │ 接口                                         │ 方法   │ 说明                                │
 * ├────┼──────────────────────────────────────────────┼────────┼─────────────────────────────────────┤
 * │ 1  │ /api/preparationProcessTemplate              │ GET   │ 分页查询工序模版列表                │
 * │ 2  │ /api/preparationProcessTemplate/{id}         │ GET   │ 根据ID查询工序模版详情              │
 * │ 3  │ /api/preparationProcessTemplate              │ POST  │ 新增工序模版                        │
 * │ 4  │ /api/preparationProcessTemplate/{id}         │ PUT   │ 修改工序模版                        │
 * │ 5  │ /api/preparationProcessTemplate/{id}         │ DELETE│ 删除工序模版                        │
 * │ 6  │ /api/preparationProcessTemplate/byPreparation/{prepId} │ GET │ 根据制剂ID查询工序模版 │
 * │ 7  │ /api/preparationProcessTemplate/batch       │ POST  │ 批量保存工序模版                    │
 * └────┴──────────────────────────────────────────────┴────────┴─────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/preparationProcessTemplate")
public class PreparationProcessTemplateController extends BaseController {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 制剂工序模版服务
     */
    @Autowired
    private PreparationProcessTemplateService templateService;

    /**
     * 制剂服务
     */
    @Autowired
    private PreparationService preparationService;

    /**
     * 工序类型服务
     */
    @Autowired
    private ProcessTypeService processTypeService;

    /**
     * 单位服务
     */
    @Autowired
    private UnitService unitService;

    // endregion

    // region 工序模版CRUD接口
    // ===================================
    // 工序模版CRUD接口
    // ===================================

    /**
     * 分页查询工序模版列表
     * <p>
     * 支持按制剂ID筛选，按步骤顺序升序排列
     * </p>
     *
     * 示例请求：
     * GET /api/preparationProcessTemplate?preparationId=1&pageIndex=0&pageSize=10
     *
     * @param preparationId 制剂ID（可选）
     * @param pageIndex 页码，从0开始（默认0）
     * @param pageSize 每页大小（默认10）
     * @return ApiResponse&lt;PagedResult&lt;PreparationProcessTemplate&gt;&gt; 分页结果，包含工序模版列表
     */
    @GetMapping
    public ApiResponse<PagedResult<PreparationProcessTemplate>> getAll(
            @RequestParam(required = false) Long preparationId,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "10") int pageSize) {
        Page<PreparationProcessTemplate> page = new Page<>(pageIndex + 1, pageSize);
        QueryWrapper<PreparationProcessTemplate> wrapper = new QueryWrapper<>();
        
        if (preparationId != null) {
            wrapper.eq("preparation_id", preparationId);
        }
        wrapper.orderByAsc("step_order");
        
        Page<PreparationProcessTemplate> pageResult = templateService.page(page, wrapper);
        fillNameFieldsForList(pageResult.getRecords());
        PagedResult<PreparationProcessTemplate> pagedResult = new PagedResult<>();
        pagedResult.setItems(pageResult.getRecords());
        pagedResult.setTotalCount(pageResult.getTotal());
        pagedResult.setPageIndex(pageIndex);
        pagedResult.setPageSize(pageSize);
        
        return success(pagedResult);
    }

    /**
     * 根据ID查询工序模版详情
     * <p>
     * 返回工序模版详情，包含关联的制剂名称、工序类型名称及单位名称
     * </p>
     *
     * 示例请求：
     * GET /api/preparationProcessTemplate/1
     *
     * @param id 工序模版ID（路径参数）
     * @return ApiResponse&lt;PreparationProcessTemplate&gt; 工序模版详情
     */
    @GetMapping("/{id}")
    public ApiResponse<PreparationProcessTemplate> getById(@PathVariable Long id) {
        PreparationProcessTemplate template = templateService.getById(id);
        if (template == null) {
            return error("工序模版不存在");
        }
        fillNameFields(template);
        return success(template);
    }

    /**
     * 新增工序模版
     *
     * 示例请求：
     * POST /api/preparationProcessTemplate
     * Content-Type: application/json
     * {
     *   "preparationId": 1,
     *   "processTypeId": 1,
     *   "stepOrder": 1,
     *   "stepName": "配制",
     *   "processTime": 60,
     *   "unitId": 1
     * }
     *
     * @param template 工序模版实体对象
     * @return ApiResponse&lt;PreparationProcessTemplate&gt; 新增的工序模版
     */
    @PostMapping
    public ApiResponse<PreparationProcessTemplate> create(@RequestBody PreparationProcessTemplate template) {
        template.setIsDeleted(0);
        template.setVersion(0);
        templateService.save(template);
        return success(template, "新增成功");
    }

    /**
     * 修改工序模版
     *
     * 示例请求：
     * PUT /api/preparationProcessTemplate/1
     * Content-Type: application/json
     * {
     *   "stepName": "配制（更新）",
     *   "processTime": 90
     * }
     *
     * @param id 工序模版ID（路径参数）
     * @param template 工序模版实体对象
     * @return ApiResponse&lt;PreparationProcessTemplate&gt; 修改后的工序模版
     */
    @PutMapping("/{id}")
    public ApiResponse<PreparationProcessTemplate> update(@PathVariable Long id, @RequestBody PreparationProcessTemplate template) {
        PreparationProcessTemplate existing = templateService.getById(id);
        if (existing == null) {
            return error("工序模版不存在");
        }
        template.setTemplateId(id);
        templateService.updateById(template);
        return success(template, "修改成功");
    }

    /**
     * 删除工序模版
     *
     * 示例请求：
     * DELETE /api/preparationProcessTemplate/1
     *
     * @param id 工序模版ID（路径参数）
     * @return ApiResponse&lt;Void&gt; 删除结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        templateService.removeById(id);
        return success(null, "删除成功");
    }

    // endregion

    // region 工序模版查询接口
    // ===================================
    // 工序模版查询接口
    // ===================================

    /**
     * 根据制剂ID查询工序模版列表
     * <p>
     * 返回工序模版列表，包含关联的制剂名称、工序类型名称及单位名称
     * </p>
     *
     * 示例请求：
     * GET /api/preparationProcessTemplate/byPreparation/1
     *
     * @param prepId 制剂ID（路径参数）
     * @return ApiResponse&lt;List&lt;PreparationProcessTemplate&gt;&gt; 工序模版列表
     */
    @GetMapping("/byPreparation/{prepId}")
    public ApiResponse<List<PreparationProcessTemplate>> getByPreparationId(@PathVariable Long prepId) {
        List<PreparationProcessTemplate> list = templateService.findByPreparationId(prepId);
        fillNameFieldsForList(list);
        return success(list);
    }

    /**
     * 批量保存工序模版
     * <p>
     * 先删除指定制剂下的原有模版，再批量插入新的模版列表
     * </p>
     *
     * 示例请求：
     * POST /api/preparationProcessTemplate/batch?preparationId=1
     * Content-Type: application/json
     * [
     *   {
     *     "processTypeId": 1,
     *     "stepOrder": 1,
     *     "stepName": "配制",
     *     "processTime": 60,
     *     "unitId": 1
     *   },
     *   {
     *     "processTypeId": 2,
     *     "stepOrder": 2,
     *     "stepName": "制粒",
     *     "processTime": 120,
     *     "unitId": 1
     *   }
     * ]
     *
     * @param preparationId 制剂ID（请求参数）
     * @param templates 工序模版列表
     * @return ApiResponse&lt;List&lt;PreparationProcessTemplate&gt;&gt; 保存后的工序模版列表
     */
    @PostMapping("/batch")
    public ApiResponse<List<PreparationProcessTemplate>> batchSave(
            @RequestParam Long preparationId,
            @RequestBody List<PreparationProcessTemplate> templates) {
        templateService.batchSave(preparationId, templates);
        return success(templates, "保存成功");
    }

    // endregion

    // region 私有辅助方法
    // ===================================
    // 私有辅助方法
    // ===================================

    /**
     * 填充工序模版的关联名称字段
     *
     * @param template 工序模版对象
     */
    private void fillNameFields(PreparationProcessTemplate template) {
        if (template == null) return;

        // 填充制剂名称
        if (template.getPreparationId() != null) {
            Preparation preparation = preparationService.getById(template.getPreparationId());
            if (preparation != null) {
                template.setPreparationName(preparation.getPreparationName());
            }
        }

        // 填充工序类型名称
        if (template.getProcessTypeId() != null) {
            ProcessType processType = processTypeService.getById(template.getProcessTypeId());
            if (processType != null) {
                template.setProcessTypeName(processType.getProcessName());
                template.setProcessCode(processType.getProcessCode());
            }
        }

        // 填充单位名称
        if (template.getUnitId() != null) {
            Unit unit = unitService.getById(template.getUnitId());
            if (unit != null) {
                template.setUnitName(unit.getUnitName());
            }
        }
    }

    /**
     * 批量填充工序模版的关联名称字段
     *
     * @param list 工序模版列表
     */
    private void fillNameFieldsForList(List<PreparationProcessTemplate> list) {
        if (list == null) return;
        for (PreparationProcessTemplate template : list) {
            fillNameFields(template);
        }
    }

    // endregion
}
