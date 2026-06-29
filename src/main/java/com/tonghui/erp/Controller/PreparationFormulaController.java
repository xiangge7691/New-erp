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
 */
@RestController
@RequestMapping("/api/preparation/formula")
public class PreparationFormulaController extends BaseCrudController<PreparationFormula, PreparationFormula, Long> {

    @Autowired
    private PreparationFormulaService preparationFormulaService;

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

    @Override
    protected PreparationFormula getDataById(Long id) {
        return preparationFormulaService.getFormulaById(id);
    }

    @Override
    protected PreparationFormula doCreate(PreparationFormula preparationFormula) {
        preparationFormulaService.addFormula(preparationFormula);
        return preparationFormula;
    }

    @Override
    protected PreparationFormula doUpdate(Long id, PreparationFormula preparationFormula) {
        preparationFormula.setFormulaId(id);
        preparationFormulaService.updateFormula(preparationFormula);
        return preparationFormula;
    }

    @Override
    protected boolean doDelete(Long id) {
        try {
            preparationFormulaService.deleteFormula(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // #region 高级查询

    /**
     * 根据制剂编码查询处方信息
     *
     * 示例请求：
     * GET /preparation/formula/byPreparationCode?preparationCode=Z000001
     *
     * @param preparationCode 制剂编码
     * @return 处方信息列表
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

    // #endregion
}
