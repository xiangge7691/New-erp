package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.utils.EntityUtils;
import com.tonghui.erp.Data.Entity.PreparationProcessTemplate;
import com.tonghui.erp.Service.PreparationProcessTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
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

    /**
     * 分页查询工序模版列表
     */
    @GetMapping
    public ApiResponse<PagedResult<PreparationProcessTemplate>> getAll(
            @RequestParam(required = false) Long preparationId,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            Page<PreparationProcessTemplate> page = new Page<>(pageIndex + 1, pageSize);
            QueryWrapper<PreparationProcessTemplate> wrapper = new QueryWrapper<>();
            
            if (preparationId != null) {
                wrapper.eq("preparation_id", preparationId);
            }
            wrapper.orderByAsc("step_order");
            
            Page<PreparationProcessTemplate> pageResult = templateService.page(page, wrapper);
            PagedResult<PreparationProcessTemplate> pagedResult = new PagedResult<>();
            pagedResult.setItems(pageResult.getRecords());
            pagedResult.setTotalCount(pageResult.getTotal());
            pagedResult.setPageIndex(pageIndex);
            pagedResult.setPageSize(pageSize);
            
            return success(pagedResult);
        } catch (Exception e) {
            return exception(e, "操作");
        }
    }

    /**
     * 根据ID查询工序模版详情
     */
    @GetMapping("/{id}")
    public ApiResponse<PreparationProcessTemplate> getById(@PathVariable Long id) {
        try {
            PreparationProcessTemplate template = templateService.getById(id);
            if (template == null) {
                return error("工序模版不存在");
            }
            return success(template);
        } catch (Exception e) {
            return exception(e, "操作");
        }
    }

    /**
     * 新增工序模版
     */
    @PostMapping
    public ApiResponse<PreparationProcessTemplate> create(@RequestBody PreparationProcessTemplate template) {
        try {
            template.setCreatedBy(EntityUtils.getCurrentUserId());
            template.setCreatedAt(LocalDateTime.now());
            template.setIsDeleted(0);
            template.setVersion(0);
            templateService.save(template);
            return success(template, "新增成功");
        } catch (Exception e) {
            return exception(e, "操作");
        }
    }

    /**
     * 修改工序模版
     */
    @PutMapping("/{id}")
    public ApiResponse<PreparationProcessTemplate> update(@PathVariable Long id, @RequestBody PreparationProcessTemplate template) {
        try {
            PreparationProcessTemplate existing = templateService.getById(id);
            if (existing == null) {
                return error("工序模版不存在");
            }
            template.setTemplateId(id);
            template.setUpdatedBy(EntityUtils.getCurrentUserId());
            template.setUpdatedAt(LocalDateTime.now());
            templateService.updateById(template);
            return success(template, "修改成功");
        } catch (Exception e) {
            return exception(e, "操作");
        }
    }

    /**
     * 删除工序模版
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        try {
            templateService.removeById(id);
            return success(null, "删除成功");
        } catch (Exception e) {
            return exception(e, "操作");
        }
    }

    /**
     * 根据制剂ID查询工序模版列表
     */
    @GetMapping("/byPreparation/{prepId}")
    public ApiResponse<List<PreparationProcessTemplate>> getByPreparationId(@PathVariable Long prepId) {
        try {
            List<PreparationProcessTemplate> list = templateService.findByPreparationId(prepId);
            return success(list);
        } catch (Exception e) {
            return exception(e, "操作");
        }
    }

    /**
     * 批量保存工序模版
     * 先删除原有模版，再批量插入新模版
     */
    @PostMapping("/batch")
    public ApiResponse<Void> batchSave(
            @RequestParam Long preparationId,
            @RequestBody List<PreparationProcessTemplate> templates) {
        try {
            // 设置创建人
            Long userId = EntityUtils.getCurrentUserId();
            for (PreparationProcessTemplate template : templates) {
                template.setCreatedBy(userId);
            }
            templateService.batchSave(preparationId, templates);
            return success(null, "保存成功");
        } catch (Exception e) {
            return exception(e, "操作");
        }
    }
}
