package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.Dto.System.DashboardExpiryConfigDto;
import com.tonghui.erp.Common.Dto.System.SystemConfigDto;
import com.tonghui.erp.Common.utils.CodeUniqueChecker;
import com.tonghui.erp.Data.Entity.SystemConfig;
import com.tonghui.erp.Data.mapper.SystemConfigMapper;
import com.tonghui.erp.Service.SystemConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统配置业务实现类
 * <p>
 * 以键值对方式管理运行时配置，内部维护内存缓存以减少高频读取（如首页待办聚合）时的数据库访问；
 * 更新配置后自动刷新缓存
 * </p>
 */
@Service
public class SystemConfigServiceImpl extends ServiceImpl<SystemConfigMapper, SystemConfig>
        implements SystemConfigService {

    // region 字段定义
    // ===================================
    // 字段定义
    // ===================================

    /**
     * 配置缓存：configKey -> 配置实体
     */
    private final Map<String, SystemConfig> cache = new ConcurrentHashMap<>();

    /**
     * 缓存是否已加载
     */
    private volatile boolean loaded = false;

    // endregion

    // region 缓存与查询
    // ===================================
    // 缓存与查询
    // ===================================

    /**
     * 确保缓存已加载（懒加载）
     */
    private void ensureCache() {
        if (!loaded) {
            synchronized (this) {
                if (!loaded) {
                    refreshCache();
                }
            }
        }
    }

    @Override
    public void refreshCache() {
        cache.clear();
        List<SystemConfig> all = this.baseMapper.selectList(
                new QueryWrapper<SystemConfig>().eq("status", 1));
        for (SystemConfig config : all) {
            if (StringUtils.hasText(config.getConfigKey())) {
                cache.put(config.getConfigKey(), config);
            }
        }
        loaded = true;
    }

    @Override
    public SystemConfig getByKey(String key) {
        ensureCache();
        SystemConfig config = cache.get(key);
        if (config != null) {
            return config;
        }
        // 缓存未命中时回退数据库查询
        return this.baseMapper.selectOne(
                new QueryWrapper<SystemConfig>().eq("config_key", key).last("LIMIT 1"));
    }

    @Override
    public String getString(String key, String defaultValue) {
        SystemConfig config = getByKey(key);
        if (config == null || !StringUtils.hasText(config.getConfigValue())) {
            return defaultValue;
        }
        return config.getConfigValue().trim();
    }

    @Override
    public int getInt(String key, int defaultValue) {
        String value = getString(key, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public List<SystemConfig> getByGroup(String group) {
        return this.baseMapper.selectList(
                new QueryWrapper<SystemConfig>()
                        .eq("config_group", group)
                        .orderByAsc("config_id"));
    }

    @Override
    public List<SystemConfig> listAll() {
        return this.baseMapper.selectList(
                new QueryWrapper<SystemConfig>().orderByAsc("config_group").orderByAsc("config_id"));
    }

    @Override
    public boolean isKeyUnique(String key, Long excludeId) {
        return CodeUniqueChecker.isCodeUnique(key, excludeId, baseMapper::countByConfigKeyIncludeDeleted);
    }

    // endregion

    // region 更新操作
    // ===================================
    // 更新操作
    // ===================================

    @Override
    @Transactional
    public void updateValue(String key, String value) {
        SystemConfig existing = this.baseMapper.selectOne(
                new QueryWrapper<SystemConfig>().eq("config_key", key).last("LIMIT 1"));
        if (existing == null) {
            SystemConfig config = new SystemConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setConfigName(key);
            config.setConfigGroup(GROUP_DASHBOARD_EXPIRY);
            config.setValueType("string");
            config.setStatus(1);
            config.setIsDeleted(0);
            config.setVersion(0);
            this.baseMapper.insert(config);
        } else {
            existing.setConfigValue(value);
            this.baseMapper.updateById(existing);
        }
        refreshCache();
    }

    @Override
    @Transactional
    public void batchSave(List<SystemConfigDto> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (SystemConfigDto item : items) {
            if (item == null || !StringUtils.hasText(item.getConfigKey())) {
                continue;
            }
            updateValue(item.getConfigKey(), item.getConfigValue());
        }
    }

    // endregion

    // region 聚合查询
    // ===================================
    // 聚合查询
    // ===================================

    @Override
    public DashboardExpiryConfigDto getDashboardExpiryConfig() {
        DashboardExpiryConfigDto dto = new DashboardExpiryConfigDto();
        dto.setStockDays(getInt(KEY_STOCK_DAYS, DEFAULT_EXPIRY_DAYS));
        dto.setEquipmentDays(getInt(KEY_EQUIPMENT_DAYS, DEFAULT_EXPIRY_DAYS));
        dto.setHealthCertDays(getInt(KEY_HEALTH_CERT_DAYS, DEFAULT_EXPIRY_DAYS));
        dto.setPersonnelCertDays(getInt(KEY_PERSONNEL_CERT_DAYS, DEFAULT_EXPIRY_DAYS));
        dto.setDisinfectionDays(getInt(KEY_DISINFECTION_DAYS, DEFAULT_EXPIRY_DAYS));
        dto.setOrganizationDays(getInt(KEY_ORGANIZATION_DAYS, DEFAULT_EXPIRY_DAYS));
        dto.setCleaningDays(getInt(KEY_CLEANING_DAYS, DEFAULT_EXPIRY_DAYS));
        dto.setSupplierAuditDays(getInt(KEY_SUPPLIER_AUDIT_DAYS, DEFAULT_EXPIRY_DAYS));
        dto.setTrainingDays(getInt(KEY_TRAINING_DAYS, DEFAULT_EXPIRY_DAYS));
        dto.setVerificationDays(getInt(KEY_VERIFICATION_DAYS, DEFAULT_EXPIRY_DAYS));
        dto.setRetainedSampleDays(getInt(KEY_RETAINED_SAMPLE_DAYS, DEFAULT_EXPIRY_DAYS));
        dto.setApprovalDays(getInt(KEY_APPROVAL_DAYS, DEFAULT_EXPIRY_DAYS));
        return dto;
    }

    // endregion
}
