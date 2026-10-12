package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.Dto.System.DashboardExpiryConfigDto;
import com.tonghui.erp.Common.Dto.System.SystemConfigDto;
import com.tonghui.erp.Common.utils.CodeUniqueChecker;
import com.tonghui.erp.Common.utils.SoftDeleteCleanHelper;
import com.tonghui.erp.Data.Entity.SystemConfig;
import com.tonghui.erp.Data.mapper.SystemConfigMapper;
import com.tonghui.erp.Service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统配置业务实现类
 * <p>
 * 以键值对方式管理运行时配置。读取时始终查询数据库最新值，
 * 保证通过配置页、其他实例或直接改库后，各业务页面立即取到新配置，不会读取到过期缓存
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
     * 软删除统一清理工具，用于物理清理同 config_key 的软删除记录以释放唯一索引 uk_config_key
     */
    @Autowired
    private SoftDeleteCleanHelper softDeleteCleanHelper;

    // endregion

    // region 缓存与查询
    // ===================================
    // 缓存与查询
    // ===================================

    /**
     * 根据配置键查询配置
     * <p>
     * 每次均查询数据库，避免内存缓存导致配置修改后其他页面长时间不生效
     * </p>
     *
     * @param key 配置键
     * @return 配置实体，不存在返回null
     */
    @Override
    public SystemConfig getByKey(String key) {
        if (!StringUtils.hasText(key)) {
            return null;
        }
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
        return parseInt(getString(key, null), defaultValue);
    }

    /**
     * 将配置值解析为整数，解析失败时返回默认值
     *
     * @param value        配置值
     * @param defaultValue 默认值
     * @return 解析后的整数
     */
    private int parseInt(String value, int defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 一次性加载所有启用配置，供首页到期提醒等聚合查询使用，避免逐项查询数据库
     *
     * @return 配置键 -> 配置值
     */
    private Map<String, String> loadEnabledValues() {
        List<SystemConfig> all = this.baseMapper.selectList(
                new QueryWrapper<SystemConfig>().eq("status", 1));
        Map<String, String> values = new HashMap<>();
        for (SystemConfig config : all) {
            if (StringUtils.hasText(config.getConfigKey())) {
                values.put(config.getConfigKey(), config.getConfigValue());
            }
        }
        return values;
    }

    /**
     * 按业务模块查询配置项（模糊匹配，传全称时等价于精确查询）
     *
     * @param group 业务模块（如 库存 或 库存管理）
     * @return 配置项列表
     */
    @Override
    public List<SystemConfig> getByGroup(String group) {
        return this.baseMapper.selectList(
                new QueryWrapper<SystemConfig>()
                        .like("config_group", group)
                        .orderByAsc("config_id"));
    }

    @Override
    public List<SystemConfig> listAll() {
        return this.baseMapper.selectList(
                new QueryWrapper<SystemConfig>().orderByAsc("config_group").orderByAsc("config_id"));
    }

    /**
     * 查询全部业务模块（配置页分组/筛选下拉用）
     * <p>
     * 仅取未删除记录的非空 config_group，去重后按模块名升序返回
     * </p>
     *
     * @return 业务模块列表（去重，按模块名升序）
     */
    @Override
    public List<String> listGroups() {
        List<SystemConfig> rows = this.baseMapper.selectList(
                new QueryWrapper<SystemConfig>()
                        .select("config_group")
                        .isNotNull("config_group")
                        .ne("config_group", "")
                        .orderByAsc("config_group"));
        return rows.stream()
                .map(SystemConfig::getConfigGroup)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 按业务模块与关键字搜索配置项
     * <p>
     * 模块为空则不限模块，否则模糊匹配（传"库存"可命中"库存管理"）；
     * 关键字为空则不限关键字（两者均为空时等价于查询全部）；
     * 关键字同时模糊匹配配置名称与配置键；按模块、配置ID升序（便于前端按模块分组渲染）
     * </p>
     *
     * @param group   业务模块（可选，模糊匹配，如 库存 或 库存管理）
     * @param keyword 关键字（可选，模糊匹配配置名称或配置键）
     * @return 配置项列表
     */
    @Override
    public List<SystemConfig> search(String group, String keyword) {
        QueryWrapper<SystemConfig> wrapper = new QueryWrapper<SystemConfig>()
                .like(StringUtils.hasText(group), "config_group", group)
                .and(StringUtils.hasText(keyword),
                        w -> w.like("config_name", keyword)
                                .or().like("config_key", keyword))
                .orderByAsc("config_group")
                .orderByAsc("config_id");
        return this.baseMapper.selectList(wrapper);
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
            // 清理同 config_key 的软删除记录（物理删除，释放唯一索引 uk_config_key，允许复用键名）
            softDeleteCleanHelper.cleanByUniqueField(baseMapper, "config_key", key);

            SystemConfig config = new SystemConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setConfigName(key);
            config.setConfigGroup(GROUP_DEFAULT);
            config.setValueType("string");
            config.setStatus(1);
            config.setIsDeleted(0);
            config.setVersion(0);
            this.baseMapper.insert(config);
        } else {
            existing.setConfigValue(value);
            this.baseMapper.updateById(existing);
        }
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
        Map<String, String> values = loadEnabledValues();
        dto.setStockDays(parseInt(values.get(KEY_STOCK_DAYS), DEFAULT_EXPIRY_DAYS));
        dto.setEquipmentDays(parseInt(values.get(KEY_EQUIPMENT_DAYS), DEFAULT_EXPIRY_DAYS));
        dto.setHealthCertDays(parseInt(values.get(KEY_HEALTH_CERT_DAYS), DEFAULT_EXPIRY_DAYS));
        dto.setPersonnelCertDays(parseInt(values.get(KEY_PERSONNEL_CERT_DAYS), DEFAULT_EXPIRY_DAYS));
        dto.setDisinfectionDays(parseInt(values.get(KEY_DISINFECTION_DAYS), DEFAULT_EXPIRY_DAYS));
        dto.setOrganizationDays(parseInt(values.get(KEY_ORGANIZATION_DAYS), DEFAULT_EXPIRY_DAYS));
        dto.setCleaningDays(parseInt(values.get(KEY_CLEANING_DAYS), DEFAULT_EXPIRY_DAYS));
        dto.setSupplierAuditDays(parseInt(values.get(KEY_SUPPLIER_AUDIT_DAYS), DEFAULT_EXPIRY_DAYS));
        dto.setTrainingDays(parseInt(values.get(KEY_TRAINING_DAYS), DEFAULT_EXPIRY_DAYS));
        dto.setVerificationDays(parseInt(values.get(KEY_VERIFICATION_DAYS), DEFAULT_EXPIRY_DAYS));
        dto.setRetainedSampleDays(parseInt(values.get(KEY_RETAINED_SAMPLE_DAYS), DEFAULT_EXPIRY_DAYS));
        dto.setApprovalDays(parseInt(values.get(KEY_APPROVAL_DAYS), DEFAULT_EXPIRY_DAYS));
        return dto;
    }

    // endregion
}
