package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.Preparation;
import com.tonghui.erp.Service.PreparationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 制剂控制器
 */
@RestController
@RequestMapping("/api/preparation")
public class PreparationController extends BaseCrudController<Preparation, Preparation, Long> {

    @Autowired
    private PreparationService preparationService;

    @Override
    protected PagedResult<Preparation> getAllData(int pageIndex, int pageSize) {
        // 页码从0开始的处理，确保不为负数
        int safePageIndex = Math.max(0, pageIndex);
        // 当pageSize<=0时，设置一个合理的默认值
        int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);

        // 使用PreparationService的queryPreparations方法进行查询
        Preparation preparation = new Preparation();
        Page<Preparation> pageResult = preparationService.queryPreparations(preparation, safePageIndex, safePageSize);

        // 转换为PagedResult
        PagedResult<Preparation> pagedResult = new PagedResult<>();
        pagedResult.setItems(pageResult.getRecords());
        pagedResult.setTotalCount(pageResult.getTotal());
        pagedResult.setPageIndex(safePageIndex);
        pagedResult.setPageSize((int) pageResult.getSize());

        return pagedResult;
    }

    @Override
    protected Preparation getDataById(Long id) {
        return preparationService.getPreparationById(id);
    }

    @Override
    protected Preparation doCreate(Preparation preparation) {
        preparationService.addPreparation(preparation);
        return preparation;
    }

    @Override
    protected Preparation doUpdate(Long id, Preparation preparation) {
        preparation.setPreparationId(id);
        preparationService.updatePreparation(preparation);
        return preparation;
    }

    @Override
    protected boolean doDelete(Long id) {
        try {
            preparationService.deletePreparation(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // #region 高级查询

    /**
     * 高级查询制剂（支持多条件 + 分页）
     *
     * 可选查询条件：
     * - preparationCode：模糊匹配
     * -preparationName：模糊匹配
     * -spec：模糊匹配
     * -processAttr：精确匹配
     * -packageSpec：精确匹配
     * -dosageForm：精确匹配
     * -status：状态过滤
     * -createdTime：开始时间（大于等于）
     * -updatedTime：结束时间（小于等于）
     * -unitName：单位名称（模糊匹配）
     * -producer：生产商（模糊匹配）
     * -recordInfo：制剂备案（模糊匹配）
     * -functionMain：功能主治（模糊匹配）
     * -method：制法（模糊匹配）
     *
     * 示例请求：
     * GET /preparation/search?pageIndex=1&pageSize=20&preparationCode=Z00&preparationName=感冒&spec=胶囊&processAttr=自制&status=1&unitName=株洲
     *
     * @param preparationCode 制剂编码（模糊匹配）
     * @param preparationName 制剂品名（模糊匹配）
     * @param spec 规格描述（模糊匹配）
     * @param processAttr 加工性质（精确匹配）
     * @param packageSpec 包装规格（精确匹配）
     * @param dosageForm 剂型（精确匹配）
     * @param status 状态（精确匹配）
     * @param unitName 单位名称（模糊匹配）
     * @param producer 生产商（模糊匹配）
     * @param recordInfo 制剂备案（模糊匹配）
     * @param functionMain 功能主治（模糊匹配）
     * @param method 制法（模糊匹配）
     * @param pageIndex   页码
     * @param pageSize    每页大小
     * @return 分页结果
     */
    @GetMapping("/search")
    public ApiResponse<PagedResult<Preparation>> queryPreparations(
            @RequestParam(required = false) String preparationCode,
            @RequestParam(required = false) String preparationName,
            @RequestParam(required = false) String spec,
            @RequestParam(required = false) String processAttr,
            @RequestParam(required = false) String packageSpec,
            @RequestParam(required = false) String dosageForm,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String unitName,
            @RequestParam(required = false) String producer,
            @RequestParam(required = false) String recordInfo,
            @RequestParam(required = false) String functionMain,
            @RequestParam(required = false) String method,
            @RequestParam int pageIndex,
            @RequestParam int pageSize) {
        try {
            // 构造查询条件对象
            Preparation preparation = new Preparation();
            preparation.setPreparationCode(preparationCode);
            preparation.setPreparationName(preparationName);
            preparation.setSpec(spec);
            preparation.setProcessAttr(processAttr);
            preparation.setPackageSpec(packageSpec);
            preparation.setDosageForm(dosageForm);
            preparation.setStatus(status);
            preparation.setUnitName(unitName);
            preparation.setProducer(producer);
            preparation.setRecordInfo(recordInfo);
            preparation.setFunctionMain(functionMain);
            preparation.setMethod(method);

            // 获取分页结果（直接传递原始参数，不需要处理-1的情况）
            Page<Preparation> pageResult = preparationService.queryPreparations(preparation, pageIndex, pageSize);

            // 转换为统一的PagedResult格式
            PagedResult<Preparation> pagedResult = new PagedResult<>();
            pagedResult.setItems(pageResult.getRecords());
            pagedResult.setTotalCount(pageResult.getTotal());
            
            // 如果是返回全部数据(-1,-1)，则设置相应标志
            if (pageIndex == -1 && pageSize == -1) {
                pagedResult.setPageIndex(-1);
                pagedResult.setPageSize(-1);
            } else {
                // 正常情况下设置页面索引和大小
                pagedResult.setPageIndex(pageIndex);
                pagedResult.setPageSize((int) pageResult.getSize());
            }

            return success(pagedResult);
        } catch (Exception ex) {
            return exception(ex, "搜索制剂");
        }
    }

    // #endregion
}
