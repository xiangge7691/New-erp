package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.Material.MaterialWithDetailsDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.Material;

import java.util.List;

/**
 * 物料信息业务接口
 */
public interface MaterialService {

    // region 基础操作
    // ===================================
    // 基础操作
    // ===================================

    /**
     * 新增物料
     *
     * @param material 物料实体
     */
    void addMaterial(Material material);

    /**
     * 根据分类名称生成物料编码
     * <p>
     * 编码规则：分类前缀 + 4位流水号
     * 原料→Y，辅料→F，包材→B
     * </p>
     *
     * @param categoryName 分类名称（原料/辅料/包材）
     * @return 生成的物料编码
     */
    String generateMaterialCode(String categoryName);

    /**
     * 更新物料
     *
     * @param material 物料实体
     */
    void updateMaterial(Material material);

    /**
     * 删除物料
     *
     * @param materialId 物料ID
     */
    void deleteMaterial(Long materialId);

    // endregion

    // region 查询操作
    // ===================================
    // 查询操作
    // ===================================

    /**
     * 根据ID查询物料
     *
     * @param materialId 物料ID
     * @return 物料实体
     */
    Material getMaterialById(Long materialId);

    /**
     * 根据编码查询物料
     *
     * @param materialCode 物料编码
     * @return 物料实体
     */
    Material getMaterialByCode(String materialCode);

    /**
     * 查询所有启用状态的物料
     *
     * @return 物料集合
     */
    List<Material> getEnabledMaterials();

    /**
     * 查询所有物料
     *
     * @return 物料集合
     */
    List<Material> getAllMaterials();

    // endregion

    // region 高级查询
    // ===================================
    // 高级查询
    // ===================================

    /**
     * 高级查询物料（支持分页）
     *
     * @param material  查询条件
     * @param pageNum   页码
     * @param pageSize  每页大小
     * @return 分页结果
     */
    Page<Material> queryMaterials(Material material, int pageNum, int pageSize);
    
    /**
     * 高级查询物料（支持时间段筛选和分页）
     *
     * @param material  查询条件
     * @param createdTimeStart 创建时间起始
     * @param createdTimeEnd 创建时间结束
     * @param updatedTimeStart 更新时间起始
     * @param updatedTimeEnd 更新时间结束
     * @param pageNum   页码
     * @param pageSize  每页大小
     * @return 分页结果
     */
    Page<Material> queryMaterials(Material material, 
                                 java.time.LocalDateTime createdTimeStart, java.time.LocalDateTime createdTimeEnd,
                                 java.time.LocalDateTime updatedTimeStart, java.time.LocalDateTime updatedTimeEnd,
                                 int pageNum, int pageSize);

    PagedResult<MaterialWithDetailsDto> searchWithDetails(Material material, int pageNum, int pageSize);

    // endregion
}
