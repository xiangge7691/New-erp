package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.PreparationWithDetailsDto;
import com.tonghui.erp.Data.Entity.Preparation;
import com.tonghui.erp.Service.PreparationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 制剂控制器
 *
 * 接口清单：
 * ┌────┬───────────────────────────────┬────────┬─────────────────────────────────┐
 * │ #  │ 接口                          │ 方法   │ 说明                            │
 * ├────┼───────────────────────────────┼────────┼─────────────────────────────────┤
 * │ 1  │ /api/preparation              │ GET    │ 分页查询制剂列表                │
 * │ 2  │ /api/preparation/{id}         │ GET    │ 获取制剂详情                    │
 * │ 3  │ /api/preparation              │ POST   │ 新增制剂                        │
 * │ 4  │ /api/preparation/{id}         │ PUT    │ 修改制剂                        │
 * │ 5  │ /api/preparation/{id}         │ DELETE │ 删除制剂                        │
 * │ 6  │ /api/preparation/search       │ GET    │ 高级查询制剂（多条件+分页）      │
 * │ 7  │ /api/preparation/search-with-details │ GET │ 高级查询制剂（含子表）        │
 * └────┴───────────────────────────────┴────────┴─────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/preparation")
public class PreparationController extends BaseCrudController<Preparation, Preparation, Long> {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 制剂服务
     */
    @Autowired
    private PreparationService preparationService;

    // endregion

    // region CRUD基础方法实现
    // ===================================
    // CRUD基础方法实现
    // ===================================

    @Override
    protected PagedResult<Preparation> getAllData(int pageIndex, int pageSize) {
        int safePageIndex = Math.max(0, pageIndex);
        int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);

        Preparation preparation = new Preparation();
        Page<Preparation> pageResult = preparationService.queryPreparations(preparation, safePageIndex, safePageSize);

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

    // endregion

    // region 高级查询
    // ===================================
    // 高级查询
    // ===================================

    /**
     * 高级查询制剂（支持多条件 + 分页）
     *
     * 示例请求：
     * GET /api/preparation/search?pageIndex=1&pageSize=20&preparationCode=Z00&preparationName=感冒&spec=胶囊&processAttr=自制&status=1&unitName=株洲
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
     * @param pageIndex 页码
     * @param pageSize 每页大小
     * @return ApiResponse&lt;PagedResult&lt;Preparation&gt;&gt; 分页查询结果
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

    // endregion

    // region 带子表查询
    // ===================================
    // 带子表查询
    // ===================================

    /**
     * 高级查询制剂（包含处方、文档、工序模版子表）
     *
     * 示例请求：
     * GET /api/preparation/search-with-details?pageIndex=1&pageSize=20&preparationName=感冒
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
     * @param pageIndex 页码
     * @param pageSize 每页大小
     * @return ApiResponse&lt;PagedResult&lt;PreparationWithDetailsDto&gt;&gt; 分页结果（包含子表）
     */
    @GetMapping("/search-with-details")
    public ApiResponse<PagedResult<PreparationWithDetailsDto>> searchWithDetails(
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

            int safePageIndex = Math.max(0, pageIndex);
            int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);
            PagedResult<PreparationWithDetailsDto> result = preparationService.searchWithDetails(preparation, safePageIndex, safePageSize);
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "查询失败");
        }
    }

    // endregion
}
