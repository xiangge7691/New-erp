package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.PreparationDocument;
import com.tonghui.erp.Service.PreparationDocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 制剂文档控制器
 * 提供制剂文档的CRUD操作及按制剂/类型查询
 */
@RestController
@RequestMapping("/api/preparationDocument")
public class PreparationDocumentController extends BaseController {

    @Autowired
    private PreparationDocumentService documentService;

    /**
     * 分页查询制剂文档列表
     */
    @GetMapping
    public ApiResponse<PagedResult<PreparationDocument>> getAll(
            @RequestParam(required = false) Long preparationId,
            @RequestParam(required = false) String docType,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "10") int pageSize) {
        Page<PreparationDocument> page = new Page<>(pageIndex + 1, pageSize);
        QueryWrapper<PreparationDocument> wrapper = new QueryWrapper<>();
        
        if (preparationId != null) {
            wrapper.eq("preparation_id", preparationId);
        }
        if (docType != null && !docType.isEmpty()) {
            wrapper.eq("doc_type", docType);
        }
        wrapper.orderByDesc("created_time");
        
        Page<PreparationDocument> pageResult = documentService.page(page, wrapper);
        PagedResult<PreparationDocument> pagedResult = new PagedResult<>();
        pagedResult.setItems(pageResult.getRecords());
        pagedResult.setTotalCount(pageResult.getTotal());
        pagedResult.setPageIndex(pageIndex);
        pagedResult.setPageSize(pageSize);
        
        return success(pagedResult);
    }

    /**
     * 根据ID查询制剂文档详情
     */
    @GetMapping("/{id}")
    public ApiResponse<PreparationDocument> getById(@PathVariable Long id) {
        PreparationDocument document = documentService.getById(id);
        if (document == null) {
            return error("文档不存在");
        }
        return success(document);
    }

    /**
     * 新增制剂文档
     */
    @PostMapping
    public ApiResponse<PreparationDocument> create(@RequestBody PreparationDocument document) {
        document.setIsDeleted(0);
        document.setVersion(0);
        documentService.save(document);
        return success(document, "新增成功");
    }

    /**
     * 修改制剂文档
     */
    @PutMapping("/{id}")
    public ApiResponse<PreparationDocument> update(@PathVariable Long id, @RequestBody PreparationDocument document) {
        PreparationDocument existing = documentService.getById(id);
        if (existing == null) {
            return error("文档不存在");
        }
        document.setDocId(id);
        documentService.updateById(document);
        return success(document, "修改成功");
    }

    /**
     * 删除制剂文档
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        documentService.removeById(id);
        return success(null, "删除成功");
    }

    /**
     * 根据制剂ID查询文档列表
     */
    @GetMapping("/byPreparation/{prepId}")
    public ApiResponse<List<PreparationDocument>> getByPreparationId(@PathVariable Long prepId) {
        List<PreparationDocument> list = documentService.findByPreparationId(prepId);
        return success(list);
    }

    /**
     * 根据文档类型查询文档列表
     */
    @GetMapping("/byType/{docType}")
    public ApiResponse<List<PreparationDocument>> getByDocType(@PathVariable String docType) {
        List<PreparationDocument> list = documentService.findByDocType(docType);
        return success(list);
    }
}
