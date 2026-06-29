package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Data.Entity.Material;
import com.tonghui.erp.Data.mapper.MaterialMapper;
import com.tonghui.erp.Service.MaterialService;
import com.tonghui.erp.Common.utils.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MaterialServiceImpl implements MaterialService {

    @Autowired
    private MaterialMapper materialMapper;

    @Override
    public Material getMaterialById(Long id) {
        return materialMapper.selectById(id);
    }

    @Override
    public void addMaterial(Material material) {
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
    }

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

    @Override
    public void deleteMaterial(Long id) {
        materialMapper.deleteById(id);
    }

    // #region 查询操作

    /**
     * 根据编码查询物料
     *
     * @param materialCode 物料编码
     * @return 物料实体
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
     * @return 物料集合
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
     * @return 物料集合
     */
    @Override
    public List<Material> getAllMaterials() {
        return materialMapper.selectList(null);
    }

    // #endregion

    // #region 高级查询

    @Override
    public Page<Material> queryMaterials(Material material, int pageIndex, int pageSize) {
        // 将页码从0开始转换为1开始
        int actualPageIndex = pageIndex + 1;
        
        Page<Material> page = new Page<>(actualPageIndex, pageSize);
        QueryWrapper<Material> wrapper = new QueryWrapper<>();

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
        if (material.getCreatedTime() != null) {
            wrapper.ge("created_time", material.getCreatedTime());
        }
        // 更新时间范围筛选
        if (material.getUpdatedTime() != null) {
            wrapper.le("updated_time", material.getUpdatedTime());
        }
        
        // 默认按照物料编码倒序排列
        wrapper.orderByDesc("material_code");

        return materialMapper.selectPage(page, wrapper);
    }
    
    @Override
    public Page<Material> queryMaterials(Material material,
                                 java.time.LocalDateTime createdTimeStart, java.time.LocalDateTime createdTimeEnd,
                                 java.time.LocalDateTime updatedTimeStart, java.time.LocalDateTime updatedTimeEnd,
                                 int pageIndex, int pageSize) {
        // 将页码从0开始转换为1开始
        int actualPageIndex = pageIndex + 1;
        
        Page<Material> page = new Page<>(actualPageIndex, pageSize);
        QueryWrapper<Material> wrapper = new QueryWrapper<>();

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
        
        // 默认按照物料编码倒序排列
        wrapper.orderByDesc("material_code");

        return materialMapper.selectPage(page, wrapper);
    }

    // #endregion
}
