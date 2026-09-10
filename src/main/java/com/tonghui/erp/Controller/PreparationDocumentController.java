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
 * <p>
 * 提供制剂文档的CRUD操作、按制剂ID查询及按文档类型查询功能，用于制剂生产中的SOP、工艺规程等文档管理
 * </p>
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────────┬────────┬─────────────────────────────────────┐
 * │ #  │ 接口                                 │ 方法   │ 说明                                │
 * ├────┼──────────────────────────────────────┼────────┼─────────────────────────────────────┤
 * │ 1  │ /api/preparationDocument             │ GET   │ 分页查询制剂文档列表                │
 * │ 2  │ /api/preparationDocument/{id}        │ GET   │ 根据ID查询制剂文档详情              │
 * │ 3  │ /api/preparationDocument             │ POST  │ 新增制剂文档                        │
 * │ 4  │ /api/preparationDocument/{id}        │ PUT   │ 修改制剂文档                        │
 * │ 5  │ /api/preparationDocument/{id}        │ DELETE│ 删除制剂文档                        │
 * │ 6  │ /api/preparationDocument/byPreparation/{prepId} │ GET │ 根据制剂ID查询文档列表  │
 * │ 7  │ /api/preparationDocument/byType/{docType} │ GET │ 根据文档类型查询文档列表  │
 * └────┴──────────────────────────────────────┴────────┴─────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/preparationDocument")
public class PreparationDocumentController extends BaseController {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 制剂文档服务
     */
    @Autowired
    private PreparationDocumentService documentService;

    // endregion

    // region 制剂文档CRUD接口
    // ===================================
    // 制剂文档CRUD接口
    // ===================================

    /**
     * 分页查询制剂文档列表
     * <p>
     * 支持按制剂ID和文档类型筛选，按创建时间倒序排列
     * </p>
     *
     * 示例请求：
     * GET /api/preparationDocument?preparationId=1&docType=SOP&pageIndex=0&pageSize=10
     *
     * @param preparationId 制剂ID（可选）
     * @param docType 文档类型（可选），如SOP、工艺规程等
     * @param pageIndex 页码，从0开始（默认0）
     * @param pageSize 每页大小（默认10）
     * @return ApiResponse&lt;PagedResult&lt;PreparationDocument&gt;&gt; 分页结果，包含制剂文档列表
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
     *
     * 示例请求：
     * GET /api/preparationDocument/1
     *
     * @param id 文档ID（路径参数）
     * @return ApiResponse&lt;PreparationDocument&gt; 制剂文档详情
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
     *
     * 示例请求：
     * POST /api/preparationDocument
     * Content-Type: application/json
     * {
     *   "preparationId": 1,
     *   "docType": "SOP",
     *   "docName": "制粒工序标准操作规程",
     *   "docContent": "1. 准备工作...",
     *   "version": "1.0"
     * }
     *
     * @param document 制剂文档实体对象
     * @return ApiResponse&lt;PreparationDocument&gt; 新增的制剂文档
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
     *
     * 示例请求：
     * PUT /api/preparationDocument/1
     * Content-Type: application/json
     * {
     *   "docName": "制粒工序标准操作规程（更新）",
     *   "docContent": "1. 准备工作（更新）..."
     * }
     *
     * @param id 文档ID（路径参数）
     * @param document 制剂文档实体对象
     * @return ApiResponse&lt;PreparationDocument&gt; 修改后的制剂文档
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
     *
     * 示例请求：
     * DELETE /api/preparationDocument/1
     *
     * @param id 文档ID（路径参数）
     * @return ApiResponse&lt;Void&gt; 删除结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        documentService.removeById(id);
        return success(null, "删除成功");
    }

    // endregion

    // region 文档查询接口
    // ===================================
    // 文档查询接口
    // ===================================

    /**
     * 根据制剂ID查询文档列表
     *
     * 示例请求：
     * GET /api/preparationDocument/byPreparation/1
     *
     * @param prepId 制剂ID（路径参数）
     * @return ApiResponse&lt;List&lt;PreparationDocument&gt;&gt; 文档列表
     */
    @GetMapping("/byPreparation/{prepId}")
    public ApiResponse<List<PreparationDocument>> getByPreparationId(@PathVariable Long prepId) {
        List<PreparationDocument> list = documentService.findByPreparationId(prepId);
        return success(list);
    }

    /**
     * 根据文档类型查询文档列表
     *
     * 示例请求：
     * GET /api/preparationDocument/byType/SOP
     *
     * @param docType 文档类型（路径参数）
     * @return ApiResponse&lt;List&lt;PreparationDocument&gt;&gt; 文档列表
     */
    @GetMapping("/byType/{docType}")
    public ApiResponse<List<PreparationDocument>> getByDocType(@PathVariable String docType) {
        List<PreparationDocument> list = documentService.findByDocType(docType);
        return success(list);
    }

    // endregion
}
