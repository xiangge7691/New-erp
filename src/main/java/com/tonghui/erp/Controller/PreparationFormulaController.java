package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.PreparationFormula;
import com.tonghui.erp.Service.PreparationFormulaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 制剂处方信息控制器
 * <p>
 * 提供制剂处方信息的CRUD操作及按制剂编码查询功能，用于制剂生产中的配方管理
 * </p>
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────────┬────────┬─────────────────────────────────────┐
 * │ #  │ 接口                                 │ 方法   │ 说明                                │
 * ├────┼──────────────────────────────────────┼────────┼─────────────────────────────────────┤
 * │ 1  │ /api/preparation/formula             │ GET   │ 获取所有处方信息（分页）            │
 * │ 2  │ /api/preparation/formula/{id}        │ GET   │ 根据ID获取处方详情                  │
 * │ 3  │ /api/preparation/formula             │ POST  │ 新增处方信息                        │
 * │ 4  │ /api/preparation/formula/{id}        │ PUT   │ 修改处方信息                        │
 * │ 5  │ /api/preparation/formula/{id}        │ DELETE│ 删除处方信息                        │
 * │ 6  │ /api/preparation/formula/byPreparationCode │ GET │ 根据制剂编码查询处方信息      │
 * └────┴──────────────────────────────────────┴────────┴─────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/preparation/formula")
public class PreparationFormulaController extends BaseCrudController<PreparationFormula, PreparationFormula, Long> {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 制剂处方服务
     */
    @Autowired
    private PreparationFormulaService preparationFormulaService;

    // endregion

    // region CRUD操作实现
    // ===================================
    // CRUD操作实现
    // ===================================

    /**
     * 获取所有处方信息（分页）
     *
     * 示例请求：
     * GET /api/preparation/formula?pageIndex=0&pageSize=10
     *
     * @param pageIndex 页码，从0开始
     * @param pageSize 每页大小
     * @return PagedResult&lt;PreparationFormula&gt; 分页结果，包含处方信息列表
     */
    @Override
    protected PagedResult<PreparationFormula> getAllData(int pageIndex, int pageSize) {
        // 页码从0开始的处理
        int safePageIndex = Math.max(0, pageIndex);
        // 当pageSize<=0时，视为获取全部数据
        int safePageSize = pageSize <= 0 ? Integer.MAX_VALUE : Math.max(1, pageSize);

        // 获取所有处方信息
        List<PreparationFormula> allFormulas = preparationFormulaService.getAllFormulas();

        // 安全的分页处理 - 页码从0开始
        int fromIndex = safePageIndex * safePageSize;
        // 如果pageSize为Integer.MAX_VALUE，则获取到列表末尾
        int toIndex = fromIndex + safePageSize > allFormulas.size() ? allFormulas.size() : fromIndex + safePageSize;


        // 确保索引有效
        fromIndex = Math.max(0, Math.min(fromIndex, allFormulas.size()));
        toIndex = Math.max(fromIndex, Math.min(toIndex, allFormulas.size()));

        // 处理边界情况：如果fromIndex已经超出范围，则返回空列表
        List<PreparationFormula> pageData = fromIndex >= allFormulas.size() ?
            List.of() : allFormulas.subList(fromIndex, toIndex);

        // 转换为PagedResult
        PagedResult<PreparationFormula> pagedResult = new PagedResult<>();
        pagedResult.setItems(pageData);
        pagedResult.setTotalCount(allFormulas.size());
        pagedResult.setPageIndex(safePageIndex);
        // 设置实际的页面大小，而不是Integer.MAX_VALUE
        pagedResult.setPageSize(pageSize <= 0 ? allFormulas.size() : safePageSize);

        return pagedResult;
    }

    /**
     * 根据ID获取处方详情
     *
     * 示例请求：
     * GET /api/preparation/formula/1
     *
     * @param id 处方ID
     * @return PreparationFormula 处方详情
     */
    @Override
    protected PreparationFormula getDataById(Long id) {
        return preparationFormulaService.getFormulaById(id);
    }

    /**
     * 新增处方信息
     *
     * 示例请求：
     * POST /api/preparation/formula
     * Content-Type: application/json
     * {
     *   "preparationCode": "Z000001",
     *   "materialName": "原料A",
     *   "quantity": 100.00,
     *   "unit": "kg"
     * }
     *
     * @param preparationFormula 处方实体对象
     * @return PreparationFormula 新增的处方
     */
    @Override
    protected PreparationFormula doCreate(PreparationFormula preparationFormula) {
        preparationFormulaService.addFormula(preparationFormula);
        return preparationFormula;
    }

    /**
     * 修改处方信息
     *
     * 示例请求：
     * PUT /api/preparation/formula/1
     * Content-Type: application/json
     * {
     *   "quantity": 200.00
     * }
     *
     * @param id 处方ID
     * @param preparationFormula 处方实体对象
     * @return PreparationFormula 修改后的处方
     */
    @Override
    protected PreparationFormula doUpdate(Long id, PreparationFormula preparationFormula) {
        preparationFormula.setFormulaId(id);
        preparationFormulaService.updateFormula(preparationFormula);
        return preparationFormula;
    }

    /**
     * 删除处方信息
     *
     * 示例请求：
     * DELETE /api/preparation/formula/1
     *
     * @param id 处方ID
     * @return boolean 删除结果
     */
    @Override
    protected boolean doDelete(Long id) {
        try {
            preparationFormulaService.deleteFormula(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // endregion

    // region 高级查询接口
    // ===================================
    // 高级查询接口
    // ===================================

    /**
     * 根据制剂编码查询处方信息
     *
     * 示例请求：
     * GET /api/preparation/formula/byPreparationCode?preparationCode=Z000001
     *
     * @param preparationCode 制剂编码
     * @return ApiResponse&lt;List&lt;PreparationFormula&gt;&gt; 处方信息列表
     */
    @GetMapping("/byPreparationCode")
    public ApiResponse<List<PreparationFormula>> getFormulasByPreparationCode(@RequestParam String preparationCode) {
        try {
            List<PreparationFormula> formulas = preparationFormulaService.getFormulasByPreparationCode(preparationCode);
            return success(formulas);
        } catch (Exception ex) {
            return exception(ex, "根据制剂编码查询处方信息");
        }
    }

    // endregion
}
