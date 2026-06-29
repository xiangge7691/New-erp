package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.Material;
import com.tonghui.erp.Service.MaterialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 物料控制器
 */
@RestController
@RequestMapping("/api/material")
public class MaterialController extends BaseCrudController<Material, Material, Long> {

    @Autowired
    private MaterialService materialService;

    @Override
    protected PagedResult<Material> getAllData(int pageIndex, int pageSize) {
        // 页码从0开始的处理，确保不为负数
        int safePageIndex = Math.max(0, pageIndex);
        // 当pageSize<=0时，设置一个合理的默认值
        int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);

        // 使用MaterialService的queryMaterials方法进行查询
        Material material = new Material();
        Page<Material> pageResult = materialService.queryMaterials(material, safePageIndex, safePageSize);

        // 转换为PagedResult
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

    // #region 高级查询

    /**
     * 高级查询物料（支持多条件 + 分页）
     *
     * 可选查询条件：
     * -materialName：模糊匹配
     * -categoryName：精确匹配
     * -unitName：精确匹配
     * -spec：模糊匹配
     * -materialStatus：state过滤
     * -createdTimeStart/createdTimeEnd：创建时间范围筛选
     * -updatedTimeStart/updatedTimeEnd：更新时间范围筛选
     *
     * 示例请求：
     *GET /material/search?pageIndex=1&pageSize=20&materialName=瓶&categoryName=包材&unitName=个&spec=500ml&materialStatus=1&createdTimeStart=2025-01-01%2000:00:00&createdTimeEnd=2025-09-01%2023:59:59
     *
     * @param material   查询条件（自动从query参数映射）
     * @param createdTimeStart 创建时间起始
     * @param createdTimeEnd 创建时间结束
     * @param updatedTimeStart 更新时间起始
     * @param updatedTimeEnd 更新时间结束
     * @param pageIndex  页码
     * @param pageSize   每页大小
     * @return 分页结果
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

    // #endregion
}
