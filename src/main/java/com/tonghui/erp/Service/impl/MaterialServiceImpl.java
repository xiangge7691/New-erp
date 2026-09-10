package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.Material.MaterialWithDetailsDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.Material;
import com.tonghui.erp.Data.Entity.PreparationFormula;
import com.tonghui.erp.Data.Entity.ProductionUnit;
import com.tonghui.erp.Data.Entity.PurchaseOrderItems;
import com.tonghui.erp.Data.Entity.Stock;
import com.tonghui.erp.Data.mapper.MaterialMapper;
import com.tonghui.erp.Data.mapper.PreparationFormulaMapper;
import com.tonghui.erp.Data.mapper.ProductionUnitMapper;
import com.tonghui.erp.Data.mapper.PurchaseOrderItemsMapper;
import com.tonghui.erp.Data.mapper.StockMapper;
import com.tonghui.erp.Service.MaterialService;
import com.tonghui.erp.Common.utils.EntityUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 物料服务实现类
 * <p>
 * 实现MaterialService接口，提供物料相关的业务逻辑处理，包括物料的增删改查、
 * 高级查询、带子表关联查询等功能的具体实现
 * </p>
 *
 */
@Service
public class MaterialServiceImpl implements MaterialService {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /** 物料数据访问层 */
    @Autowired
    private MaterialMapper materialMapper;

    /** 制剂处方明细数据访问层，用于关联查询物料关联的处方信息 */
    @Autowired
    private PreparationFormulaMapper preparationFormulaMapper;

    /** 采购订单明细数据访问层，用于关联查询物料关联的采购订单项 */
    @Autowired
    private PurchaseOrderItemsMapper purchaseOrderItemsMapper;

    /** 生产单位数据访问层，用于查询所有启用的生产单位 */
    @Autowired
    private ProductionUnitMapper productionUnitMapper;

    /** 库存数据访问层，用于创建物料时自动生成库存记录 */
    @Autowired
    private StockMapper stockMapper;

    // endregion

    // region 基础CRUD操作
    // ===================================
    // 基础CRUD操作
    // ===================================

    /**
     * 根据ID查询物料
     *
     * @param id 物料ID
     * @return 物料实体，不存在则返回null
     */
    @Override
    public Material getMaterialById(Long id) {
        return materialMapper.selectById(id);
    }

    /**
     * 新增物料
     * <p>自动设置创建时间、更新时间、创建人和更新人。
     * 创建成功后自动为每个生产单位创建库存基础记录。</p>
     *
     * @param material 物料实体
     */
    @Override
    public void addMaterial(Material material) {
        // 清理已软删除的相同编码记录（避免唯一键冲突）
        if (material.getMaterialCode() != null && !material.getMaterialCode().isEmpty()) {
            materialMapper.physicalDeleteByMaterialCode(material.getMaterialCode());
        }

        // 设置创建时间和更新时间
        LocalDateTime now = LocalDateTime.now();
        material.setCreatedTime(now);
        material.setUpdatedTime(now);

        // 获取当前用户ID
        Long currentUserId = EntityUtils.getCurrentUserId();
        if (currentUserId != null) {
            material.setCreatedBy(currentUserId);
            material.setUpdatedBy(currentUserId);
        }

        materialMapper.insert(material);

        // 为每个生产单位创建库存基础记录
        createStockRecordsForAllProdUnits(material);
    }

    /**
     * 为所有启用的生产单位创建该物料的库存记录
     *
     * @param material 已插入的物料实体
     */
    private void createStockRecordsForAllProdUnits(Material material) {
        // 查询所有启用且未删除的生产单位
        QueryWrapper<ProductionUnit> puWrapper = new QueryWrapper<>();
        puWrapper.eq("prod_unit_status", 1);
        puWrapper.eq("is_deleted", 0);
        List<ProductionUnit> prodUnits = productionUnitMapper.selectList(puWrapper);

        if (prodUnits.isEmpty()) {
            return;
        }

        // 为每个生产单位创建库存记录
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();
        for (ProductionUnit pu : prodUnits) {
            Stock stock = new Stock();
            stock.setProdUnitId(pu.getProdUnitId());
            stock.setItemType("material");
            stock.setItemId(material.getMaterialId());
            stock.setItemCode(material.getMaterialCode());
            stock.setItemName(material.getMaterialName());
            stock.setCategoryName(material.getCategoryName());
            stock.setUnitName(material.getUnitName());
            stock.setQuantity(java.math.BigDecimal.ZERO);
            stock.setBatchNumber("");
            stock.setProductionDate(today);
            stock.setExpiryDate(today.plusYears(5));
            stock.setStorageLocation("");
            stock.setRemark("");
            stock.setStockStatus(1);
            stock.setIsDeleted(0);
            stock.setVersion(1);
            stock.setCreatedBy(material.getCreatedBy());
            stock.setUpdatedBy(material.getUpdatedBy());
            stock.setCreatedTime(now);
            stock.setUpdatedTime(now);
            stockMapper.insert(stock);
        }
    }

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
    @Override
    public String generateMaterialCode(String categoryName) {
        // 获取分类前缀
        String prefix = getCategoryPrefix(categoryName);

        // 查询该前缀下最大的物料编码
        String maxCode = materialMapper.getMaxCodeByPrefix(prefix);

        int nextSeq = 1;
        if (maxCode != null && maxCode.length() > prefix.length()) {
            try {
                String seqStr = maxCode.substring(prefix.length());
                nextSeq = Integer.parseInt(seqStr) + 1;
            } catch (NumberFormatException e) {
                nextSeq = 1;
            }
        }

        return prefix + String.format("%04d", nextSeq);
    }

    /**
     * 根据分类名称获取编码前缀
     *
     * @param categoryName 分类名称
     * @return 编码前缀
     * @throws RuntimeException 如果分类名称不在支持范围内
     */
    private String getCategoryPrefix(String categoryName) {
        if (!StringUtils.hasText(categoryName)) {
            throw new RuntimeException("分类名称不能为空");
        }
        return switch (categoryName) {
            case "原料" -> "Y";
            case "辅料" -> "F";
            case "包材" -> "B";
            default -> throw new RuntimeException("不支持的物料分类：" + categoryName + "，仅支持原料、辅料、包材");
        };
    }

    /**
     * 更新物料
     * <p>自动更新更新时间和更新人</p>
     *
     * @param material 物料实体，包含要更新的字段信息
     */
    @Override
    public void updateMaterial(Material material) {
        // 设置更新时间
        material.setUpdatedTime(LocalDateTime.now());
        
        // 获取当前用户ID
        Long currentUserId = EntityUtils.getCurrentUserId();
        if (currentUserId != null) {
            material.setUpdatedBy(currentUserId);
        }
        
        materialMapper.updateById(material);
    }

    /**
     * 删除物料
     *
     * @param id 物料ID
     */
    @Override
    public void deleteMaterial(Long id) {
        materialMapper.deleteById(id);
    }

    // endregion

    // region 查询操作
    // ===================================
    // 查询操作
    // ===================================

    /**
     * 根据编码查询物料
     *
     * @param materialCode 物料编码
     * @return 物料实体，不存在则返回null
     */
    @Override
    public Material getMaterialByCode(String materialCode) {
        QueryWrapper<Material> wrapper = new QueryWrapper<>();
        wrapper.eq("material_code", materialCode);
        return materialMapper.selectOne(wrapper);
    }

    /**
     * 查询所有启用状态的物料
     *
     * @return 启用状态物料集合
     */
    @Override
    public List<Material> getEnabledMaterials() {
        QueryWrapper<Material> wrapper = new QueryWrapper<>();
        wrapper.eq("material_status", 1);
        return materialMapper.selectList(wrapper);
    }

    /**
     * 查询所有物料
     *
     * @return 全部物料集合
     */
    @Override
    public List<Material> getAllMaterials() {
        return materialMapper.selectList(null);
    }

    // endregion

    // region 高级查询
    // ===================================
    // 高级查询
    // ===================================

    /**
     * 高级查询物料（支持多条件组合查询，使用内置时间字段）
     * <p>支持按物料编码、名称、分类、单位、规格、状态等条件筛选，默认按编码倒序排列</p>
     *
     * @param material  查询条件实体，非null字段将作为等值或模糊查询条件
     * @param keyword   关键字（对物料编码、物料名称进行模糊匹配，可选）
     * @param pageIndex 页码，从0开始
     * @param pageSize  每页数量
     * @return 物料分页结果
     */
    @Override
    public Page<Material> queryMaterials(Material material, String keyword, int pageIndex, int pageSize) {
        // 将页码从0开始转换为1开始
        int actualPageIndex = pageIndex + 1;
        
        Page<Material> page = new Page<>(actualPageIndex, pageSize);
        QueryWrapper<Material> wrapper = new QueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            // 关键字对物料编码、物料名称进行模糊匹配
            wrapper.and(w -> w.like("material_code", keyword).or().like("material_name", keyword));
        }
        if (material.getMaterialId() != null) {
            wrapper.eq("material_id", material.getMaterialId());
        }
        if (StringUtils.hasText(material.getMaterialCode())) {
            // 采用与制剂编码相同的模糊匹配逻辑，在每个字符之间插入%实现子串匹配
            StringBuilder patternBuilder = new StringBuilder();
            for (char c : material.getMaterialCode().toCharArray()) {
                patternBuilder.append(c).append("%");
            }
            String pattern = patternBuilder.toString();
            wrapper.like("material_code", pattern);
        }
        if (StringUtils.hasText(material.getMaterialName())) {
            wrapper.like("material_name", material.getMaterialName());
        }
        if (StringUtils.hasText(material.getCategoryName())) {
            wrapper.eq("category_name", material.getCategoryName());
        }
        if (StringUtils.hasText(material.getUnitName())) {
            wrapper.eq("unit_name", material.getUnitName());
        }
        if (StringUtils.hasText(material.getSpec())) {
            wrapper.like("spec", material.getSpec());
        }
        if (material.getMaterialStatus() != null) {
            wrapper.eq("material_status", material.getMaterialStatus());
        }
        // 创建时间范围筛选
        if (material.getCreatedTimeStart() != null) {
            wrapper.ge("created_time", material.getCreatedTimeStart());
        }
        if (material.getCreatedTimeEnd() != null) {
            wrapper.le("created_time", material.getCreatedTimeEnd());
        }
        // 更新时间范围筛选
        if (material.getUpdatedTimeStart() != null) {
            wrapper.ge("updated_time", material.getUpdatedTimeStart());
        }
        if (material.getUpdatedTimeEnd() != null) {
            wrapper.le("updated_time", material.getUpdatedTimeEnd());
        }

        // 默认按照创建时间倒序排列，创建时间相同则按编码倒序排列
        wrapper.orderByDesc("created_time");
        wrapper.orderByDesc("material_code");

        return materialMapper.selectPage(page, wrapper);
    }

    /**
     * 高级查询物料（支持多条件组合查询，支持自定义时间范围筛选）
     * <p>与queryMaterials(Material, int, int)类似，但支持自定义创建时间和更新时间的范围</p>
     *
     * @param material          查询条件实体
     * @param keyword           关键字（对物料编码、物料名称进行模糊匹配，可选）
     * @param createdTimeStart  创建时间起始值（含）
     * @param createdTimeEnd    创建时间结束值（含）
     * @param updatedTimeStart  更新时间起始值（含）
     * @param updatedTimeEnd    更新时间结束值（含）
     * @param pageIndex         页码，从0开始
     * @param pageSize          每页数量
     * @return 物料分页结果
     */
    @Override
    public Page<Material> queryMaterials(Material material,
                                 String keyword,
                                 java.time.LocalDateTime createdTimeStart, java.time.LocalDateTime createdTimeEnd,
                                 java.time.LocalDateTime updatedTimeStart, java.time.LocalDateTime updatedTimeEnd,
                                 int pageIndex, int pageSize) {
        // 将页码从0开始转换为1开始
        int actualPageIndex = pageIndex + 1;
        
        Page<Material> page = new Page<>(actualPageIndex, pageSize);
        QueryWrapper<Material> wrapper = new QueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            // 关键字对物料编码、物料名称进行模糊匹配
            wrapper.and(w -> w.like("material_code", keyword).or().like("material_name", keyword));
        }
        if (material.getMaterialId() != null) {
            wrapper.eq("material_id", material.getMaterialId());
        }
        if (StringUtils.hasText(material.getMaterialCode())) {
            // 采用与制剂编码相同的模糊匹配逻辑，在每个字符之间插入%实现子串匹配
            StringBuilder patternBuilder = new StringBuilder();
            for (char c : material.getMaterialCode().toCharArray()) {
                patternBuilder.append(c).append("%");
            }
            String pattern = patternBuilder.toString();
            wrapper.like("material_code", pattern);
        }
        if (StringUtils.hasText(material.getMaterialName())) {
            wrapper.like("material_name", material.getMaterialName());
        }
        if (StringUtils.hasText(material.getCategoryName())) {
            wrapper.eq("category_name", material.getCategoryName());
        }
        if (StringUtils.hasText(material.getUnitName())) {
            wrapper.eq("unit_name", material.getUnitName());
        }
        if (StringUtils.hasText(material.getSpec())) {
            wrapper.like("spec", material.getSpec());
        }
        if (material.getMaterialStatus() != null) {
            wrapper.eq("material_status", material.getMaterialStatus());
        }
        
        // 创建时间范围筛选
        if (createdTimeStart != null) {
            wrapper.ge("created_time", createdTimeStart);
        }
        if (createdTimeEnd != null) {
            wrapper.le("created_time", createdTimeEnd);
        }
        
        // 更新时间范围筛选
        if (updatedTimeStart != null) {
            wrapper.ge("updated_time", updatedTimeStart);
        }
        if (updatedTimeEnd != null) {
            wrapper.le("updated_time", updatedTimeEnd);
        }

        // 默认按照创建时间倒序排列，创建时间相同则按编码倒序排列
        wrapper.orderByDesc("created_time");
        wrapper.orderByDesc("material_code");

        return materialMapper.selectPage(page, wrapper);
    }

    // endregion

    // region 带子表关联查询
    // ===================================
    // 带子表关联查询
    // ===================================

    /**
     * 查询物料列表并关联处方明细和采购订单项信息
     * <p>先分页查询物料主表数据，再批量查询关联的处方明细和采购订单明细</p>
     *
     * @param material 查询条件实体
     * @param keyword  关键字（对物料编码、物料名称进行模糊匹配，可选）
     * @param pageNum  页码，从0开始
     * @param pageSize 每页数量
     * @return 带子表关联数据的物料分页结果
     */
    @Override
    public PagedResult<MaterialWithDetailsDto> searchWithDetails(Material material, String keyword, int pageNum, int pageSize) {
        // 查询物料主表分页数据
        Page<Material> parentPage = queryMaterials(material, keyword, pageNum, pageSize);
        List<Material> parents = parentPage.getRecords();

        PagedResult<MaterialWithDetailsDto> result = new PagedResult<>();
        if (parents.isEmpty()) {
            result.setItems(List.of());
            result.setTotalCount(parentPage.getTotal());
            result.setPageIndex(pageNum);
            result.setPageSize(pageSize);
            return result;
        }

        // 批量查询关联的处方明细
        List<Long> parentIds = parents.stream().map(Material::getMaterialId).collect(Collectors.toList());
        QueryWrapper<PreparationFormula> formulaWrapper = new QueryWrapper<>();
        formulaWrapper.in("material_id", parentIds);
        List<PreparationFormula> allFormulas = preparationFormulaMapper.selectList(formulaWrapper);
        Map<Long, List<PreparationFormula>> formulasMap = allFormulas.stream()
                .collect(Collectors.groupingBy(PreparationFormula::getMaterialId));

        // 批量查询关联的采购订单明细
        QueryWrapper<PurchaseOrderItems> itemWrapper = new QueryWrapper<>();
        itemWrapper.in("material_id", parentIds);
        List<PurchaseOrderItems> allItems = purchaseOrderItemsMapper.selectList(itemWrapper);
        Map<Long, List<PurchaseOrderItems>> itemsMap = allItems.stream()
                .collect(Collectors.groupingBy(PurchaseOrderItems::getMaterialId));

        // 组装带子表数据的DTO
        List<MaterialWithDetailsDto> dtos = parents.stream().map(parent -> {
            MaterialWithDetailsDto dto = new MaterialWithDetailsDto();
            BeanUtils.copyProperties(parent, dto);
            dto.setFormulas(formulasMap.getOrDefault(parent.getMaterialId(), List.of()));
            dto.setItems(itemsMap.getOrDefault(parent.getMaterialId(), List.of()));
            return dto;
        }).collect(Collectors.toList());

        result.setItems(dtos);
        result.setTotalCount(parentPage.getTotal());
        result.setPageIndex(pageNum);
        result.setPageSize(pageSize);
        return result;
    }

    // endregion
}
