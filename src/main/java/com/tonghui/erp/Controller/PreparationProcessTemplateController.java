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
 * 提供工序模版的CRUD操作及按制剂查询、批量保存
 */
@RestController
@RequestMapping("/api/preparationProcessTemplate")
public class PreparationProcessTemplateController extends BaseController {

    @Autowired
    private PreparationProcessTemplateService templateService;

    @Autowired
    private PreparationService preparationService;

    @Autowired
    private ProcessTypeService processTypeService;

    @Autowired
    private UnitService unitService;

    /**
     * 分页查询工序模版列表
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
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        templateService.removeById(id);
        return success(null, "删除成功");
    }

    /**
     * 根据制剂ID查询工序模版列表
     */
    @GetMapping("/byPreparation/{prepId}")
    public ApiResponse<List<PreparationProcessTemplate>> getByPreparationId(@PathVariable Long prepId) {
        List<PreparationProcessTemplate> list = templateService.findByPreparationId(prepId);
        fillNameFieldsForList(list);
        return success(list);
    }

    /**
     * 批量保存工序模版
     * 先删除原有模版，再批量插入新模版
     */
    @PostMapping("/batch")
    public ApiResponse<List<PreparationProcessTemplate>> batchSave(
            @RequestParam Long preparationId,
            @RequestBody List<PreparationProcessTemplate> templates) {
        templateService.batchSave(preparationId, templates);
        return success(templates, "保存成功");
    }

    /**
     * 填充工序模版的关联名称字段
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
     * @param list 工序模版列表
     */
    private void fillNameFieldsForList(List<PreparationProcessTemplate> list) {
        if (list == null) return;
        for (PreparationProcessTemplate template : list) {
            fillNameFields(template);
        }
    }
}
