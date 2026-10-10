package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Common.Dto.System.DashboardExpiryConfigDto;
import com.tonghui.erp.Common.Dto.System.SystemConfigDto;
import com.tonghui.erp.Data.Entity.SystemConfig;

import java.util.List;

/**
 * 系统配置业务接口
 */
public interface SystemConfigService extends IService<SystemConfig> {

    // region 配置键常量
    // ===================================
    // 配置键常量
    // ===================================

    /** 自动新建配置时的默认业务模块 */
    String GROUP_DEFAULT = "系统管理";

    /** 库存效期提醒天数 */
    String KEY_STOCK_DAYS = "dashboard.expiry.stock.days";
    /** 设备维保提醒天数 */
    String KEY_EQUIPMENT_DAYS = "dashboard.expiry.equipment.days";
    /** 人员健康证提醒天数 */
    String KEY_HEALTH_CERT_DAYS = "dashboard.expiry.healthCert.days";
    /** 人员证书提醒天数 */
    String KEY_PERSONNEL_CERT_DAYS = "dashboard.expiry.personnelCert.days";
    /** 环境消毒提醒天数 */
    String KEY_DISINFECTION_DAYS = "dashboard.expiry.disinfection.days";
    /** 机构证照提醒天数 */
    String KEY_ORGANIZATION_DAYS = "dashboard.expiry.organization.days";
    /** 清洁提醒天数 */
    String KEY_CLEANING_DAYS = "dashboard.expiry.cleaning.days";
    /** 供应商审核提醒天数 */
    String KEY_SUPPLIER_AUDIT_DAYS = "dashboard.expiry.supplierAudit.days";
    /** 培训提醒天数 */
    String KEY_TRAINING_DAYS = "dashboard.expiry.training.days";
    /** 验证提醒天数 */
    String KEY_VERIFICATION_DAYS = "dashboard.expiry.verification.days";
    /** 留样到期提醒天数 */
    String KEY_RETAINED_SAMPLE_DAYS = "dashboard.expiry.retainedSample.days";
    /** 制剂批件过期提醒天数 */
    String KEY_APPROVAL_DAYS = "dashboard.expiry.approval.days";

    /** 默认提醒天数 */
    int DEFAULT_EXPIRY_DAYS = 30;

    // endregion

    // region 查询操作
    // ===================================
    // 查询操作
    // ===================================

    /**
     * 根据配置键查询配置
     *
     * @param key 配置键
     * @return 配置实体，不存在返回null
     */
    SystemConfig getByKey(String key);

    /**
     * 获取字符串配置值（带默认值）
     *
     * @param key          配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    String getString(String key, String defaultValue);

    /**
     * 获取整型配置值（带默认值）
     *
     * @param key          配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    int getInt(String key, int defaultValue);

    /**
     * 按业务模块查询配置项（模糊匹配，传全称时等价于精确查询）
     *
     * @param group 业务模块（如 库存 或 库存管理）
     * @return 配置项列表
     */
    List<SystemConfig> getByGroup(String group);

    /**
     * 查询全部业务模块（配置页分组/筛选下拉用）
     *
     * @return 业务模块列表（去重，按模块名升序）
     */
    List<String> listGroups();

    /**
     * 查询全部配置项
     *
     * @return 配置项列表
     */
    List<SystemConfig> listAll();

    /**
     * 按业务模块与关键字搜索配置项
     *
     * @param group   业务模块（可选，模糊匹配，如 库存 或 库存管理）
     * @param keyword 关键字（可选，模糊匹配配置名称或配置键）
     * @return 配置项列表（按模块、配置ID升序）
     */
    List<SystemConfig> search(String group, String keyword);

    /**
     * 校验配置键是否唯一（绕过软删除）
     *
     * @param key       配置键
     * @param excludeId 排除的配置ID（修改时传入）
     * @return true唯一/false重复
     */
    boolean isKeyUnique(String key, Long excludeId);

    // endregion

    // region 更新操作
    // ===================================
    // 更新操作
    // ===================================

    /**
     * 根据配置键更新配置值
     *
     * @param key   配置键
     * @param value 配置值
     */
    void updateValue(String key, String value);

    /**
     * 批量保存配置值
     *
     * @param items 配置项列表（仅使用 configKey 与 configValue）
     */
    void batchSave(List<SystemConfigDto> items);

    /**
     * 刷新内存缓存
     */
    void refreshCache();

    // endregion

    // region 聚合查询
    // ===================================
    // 聚合查询
    // ===================================

    /**
     * 获取首页到期提醒天数配置
     *
     * @return 各分类提醒天数
     */
    DashboardExpiryConfigDto getDashboardExpiryConfig();

    // endregion
}
