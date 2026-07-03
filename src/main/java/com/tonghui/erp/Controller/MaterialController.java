package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.Material.MaterialWithDetailsDto;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.Material;
import com.tonghui.erp.Service.MaterialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 物料控制器
 * <p>
 * 处理物料相关的HTTP请求，提供RESTful API接口，包括物料的增删改查及高级查询操作
 * </p>
 *
 * 接口清单：
 * ┌────┬────────────────────────────────────┬────────┬──────────────────────────────┐
 * │ #  │ 接口                               │ 方法   │ 说明                         │
 * ├────┼────────────────────────────────────┼────────┼──────────────────────────────┤
 * │ 1  │ /api/material                      │ GET    │ 分页查询物料列表             │
 * │ 2  │ /api/material/{id}                 │ GET    │ 获取物料详情                 │
 * │ 3  │ /api/material                      │ POST   │ 新增物料                     │
 * │ 4  │ /api/material/{id}                 │ PUT    │ 修改物料                     │
 * │ 5  │ /api/material/{id}                 │ DELETE │ 删除物料                     │
 * │ 6  │ /api/material/search               │ GET    │ 高级查询物料（多条件+分页）  │
 * │ 7  │ /api/material/search-with-details  │ GET    │ 带子表查询物料               │
 * └────┴────────────────────────────────────┴────────┴──────────────────────────────┘
 */
@RestController
@RequestMapping("/api/material")
public class MaterialController extends BaseCrudController<Material, Material, Long> {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    @Autowired
    private MaterialService materialService;

    // endregion

    // region CRUD操作实现方法
    // ===================================
    // CRUD操作实现方法
    // ===================================

    @Override
    protected PagedResult<Material> getAllData(int pageIndex, int pageSize) {
        int safePageIndex = Math.max(0, pageIndex);
        int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);

        Material material = new Material();
        Page<Material> pageResult = materialService.queryMaterials(material, safePageIndex, safePageSize);

        PagedResult<Material> pagedResult = new PagedResult<>();
        pagedResult.setItems(pageResult.getRecords());
        pagedResult.setTotalCount(pageResult.getTotal());
        pagedResult.setPageIndex(safePageIndex);
        pagedResult.setPageSize((int) pageResult.getSize());

        return pagedResult;
    }

    @Override
    protected Material getDataById(Long id) {
        return materialService.getMaterialById(id);
    }

    @Override
    protected Material doCreate(Material material) {
        materialService.addMaterial(material);
        return material;
    }

    @Override
    protected Material doUpdate(Long id, Material material) {
        material.setMaterialId(id);
        materialService.updateMaterial(material);
        return material;
    }

    @Override
    protected boolean doDelete(Long id) {
        try {
            materialService.deleteMaterial(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // endregion

    // region 搜索与查询
    // ===================================
    // 搜索与查询
    // ===================================

    /**
     * 高级查询物料（支持多条件 + 分页）
     * <p>
     * 可选查询条件：物料名称（模糊匹配）、分类名称（精确匹配）、单位名称（精确匹配）、
     * 规格（模糊匹配）、物料状态、创建时间范围、更新时间范围。
     * 当pageIndex和pageSize都为-1时返回所有结果
     * </p>
     *
     * 示例请求：
     * GET /api/material/search?pageIndex=0&pageSize=20&materialName=瓶&categoryName=包材&unitName=个&spec=500ml&materialStatus=1
     *
     * @param material 查询条件（自动从query参数映射）
     * @param createdTimeStart 创建时间起始（可选）
     * @param createdTimeEnd 创建时间结束（可选）
     * @param updatedTimeStart 更新时间起始（可选）
     * @param updatedTimeEnd 更新时间结束（可选）
     * @param pageIndex 页码，从0开始
     * @param pageSize 每页大小
     * @return ApiResponse&lt;PagedResult&lt;Material&gt;&gt; 物料分页列表
     */
    @GetMapping("/search")
    public ApiResponse<PagedResult<Material>> queryMaterials(Material material,
                                                             @RequestParam(required = false) java.time.LocalDateTime createdTimeStart,
                                                             @RequestParam(required = false) java.time.LocalDateTime createdTimeEnd,
                                                             @RequestParam(required = false) java.time.LocalDateTime updatedTimeStart,
                                                             @RequestParam(required = false) java.time.LocalDateTime updatedTimeEnd,
                                                             @RequestParam int pageIndex,
                                                             @RequestParam int pageSize) {
        try {
            // 当传入的页码和page大小都为-1时，返回所有结果
            if (pageIndex == -1 && pageSize == -1) {
                // 获取所有结果
                Page<Material> pageResult = materialService.queryMaterials(material, createdTimeStart, createdTimeEnd, updatedTimeStart, updatedTimeEnd, 0,Integer.MAX_VALUE);
                
                // 转换为统一的pagedResult格式
                PagedResult<Material> pagedResult = new PagedResult<>();
                pagedResult.setItems(pageResult.getRecords());
                pagedResult.setTotalCount(pageResult.getTotal());
                pagedResult.setPageIndex(0);
                pagedResult.setPageSize((int) pageResult.getSize());
                
                return success(pagedResult);
            }
            
            // 页码从0开始的处理，确保不为负数
            int safePageIndex = Math.max(0, pageIndex);
            // 当pageSize<=0时，设置一个合理的默认值
            int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);

            // 获取分页结果
            Page<Material> pageResult = materialService.queryMaterials(material, createdTimeStart,createdTimeEnd, updatedTimeStart, updatedTimeEnd, safePageIndex, safePageSize);

            // 转换为统一的pagedResult格式
            PagedResult<Material> pagedResult = new PagedResult<>();
            pagedResult.setItems(pageResult.getRecords());
            pagedResult.setTotalCount(pageResult.getTotal());
            pagedResult.setPageIndex(safePageIndex);
            pagedResult.setPageSize((int) pageResult.getSize());

            return success(pagedResult);
        } catch (Exception ex) {
            return exception(ex, "搜索物料");
        }
    }

    // endregion

    // region 带子表查询
    // ===================================
    // 带子表查询
    // ===================================

    /**
     * 带子表查询物料（支持多条件 + 分页）
     * <p>
     * 返回物料信息及其关联的分类、单位等子表数据
     * </p>
     *
     * 示例请求：
     * GET /api/material/search-with-details?pageIndex=0&pageSize=20
     *
     * @param material 查询条件（自动从query参数映射）
     * @param pageIndex 页码，从0开始
     * @param pageSize 每页大小
     * @return ApiResponse&lt;PagedResult&lt;MaterialWithDetailsDto&gt;&gt; 物料详情分页列表
     */
    @GetMapping("/search-with-details")
    public ApiResponse<PagedResult<MaterialWithDetailsDto>> searchWithDetails(Material material,
                                                                              @RequestParam int pageIndex,
                                                                              @RequestParam int pageSize) {
        try {
            int safePageIndex = Math.max(0, pageIndex);
            int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);
            PagedResult<MaterialWithDetailsDto> result = materialService.searchWithDetails(material, safePageIndex, safePageSize);
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "查询失败");
        }
    }

    // endregion
}
