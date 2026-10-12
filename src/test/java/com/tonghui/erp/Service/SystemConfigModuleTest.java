package com.tonghui.erp.Service;

import com.tonghui.erp.Data.Entity.SystemConfig;
import com.tonghui.erp.Data.mapper.SystemConfigMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 系统配置业务模块分组/筛选专项测试
 * <p>
 * config_group 字段承载业务模块（如 库存管理），覆盖按模块筛选、模块字典与组合搜索
 * </p>
 */
@SpringBootTest
public class SystemConfigModuleTest {

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    /**
     * 系统配置服务
     */
    @Autowired
    private SystemConfigService systemConfigService;

    /**
     * 系统配置Mapper，用于模拟绕过服务直接改库的场景
     */
    @Autowired
    private SystemConfigMapper systemConfigMapper;

    // endregion

    // region 测试用例
    // ===================================
    // 测试用例
    // ===================================

    /**
     * 测试按业务模块筛选：库存管理模块仅包含库存效期提醒天数，且支持部分模块名模糊匹配
     */
    @Test
    @Transactional
    public void testSearchByModuleGroup() {
        List<SystemConfig> list = systemConfigService.search("库存管理", null);
        assertFalse(list.isEmpty(), "库存管理模块应有配置项");
        assertTrue(list.stream().anyMatch(c -> SystemConfigService.KEY_STOCK_DAYS.equals(c.getConfigKey())),
                "库存管理模块应包含库存效期提醒天数");
        assertTrue(list.stream().allMatch(c -> "库存管理".equals(c.getConfigGroup())),
                "筛选结果应全部属于库存管理模块");

        // 模糊匹配：传部分模块名"库存"应命中同样的结果
        List<SystemConfig> fuzzy = systemConfigService.search("库存", null);
        assertEquals(list.size(), fuzzy.size(), "部分模块名模糊筛选应命中相同结果");
        assertTrue(systemConfigService.getByGroup("库存").stream()
                        .anyMatch(c -> SystemConfigService.KEY_STOCK_DAYS.equals(c.getConfigKey())),
                "按分组模糊查询应命中库存效期提醒天数");
    }

    /**
     * 测试模块字典接口数据：应包含既有业务模块，且旧技术分组已被替换
     */
    @Test
    @Transactional
    public void testListGroupsContainsModules() {
        List<String> groups = systemConfigService.listGroups();
        assertTrue(groups.contains("库存管理"), "模块列表应包含库存管理");
        assertTrue(groups.contains("人员管理"), "模块列表应包含人员管理");
        assertTrue(groups.contains("质量管理"), "模块列表应包含质量管理");
        assertFalse(groups.contains("dashboard_expiry"), "旧技术分组 dashboard_expiry 应已被业务模块替换");
        assertEquals(groups.size(), groups.stream().distinct().count(), "模块列表应去重");
    }

    /**
     * 测试模块与关键字组合筛选：人员管理模块下搜索"证书"
     */
    @Test
    @Transactional
    public void testSearchModuleWithKeyword() {
        List<SystemConfig> list = systemConfigService.search("人员管理", "证书");
        assertFalse(list.isEmpty(), "人员管理模块搜索证书应有结果");
        assertTrue(list.stream().anyMatch(c -> SystemConfigService.KEY_PERSONNEL_CERT_DAYS.equals(c.getConfigKey())),
                "组合筛选应命中人员证书提醒天数");
        boolean allMatched = list.stream().allMatch(c -> "人员管理".equals(c.getConfigGroup())
                && (c.getConfigName().contains("证书") || c.getConfigKey().contains("证书")));
        assertTrue(allMatched, "组合筛选结果应同时满足模块与关键字条件");
    }

    /**
     * 测试配置读取始终取数据库最新值：绕过服务直接改库后，下一次读取应立即变化
     * <p>
     * 用于防止内存缓存导致配置值修改后其他页面长时间不生效
     * </p>
     */
    @Test
    @Transactional
    public void testConfigReadAlwaysUsesLatestDatabaseValue() {
        SystemConfig original = systemConfigService.getByKey(SystemConfigService.KEY_STOCK_DAYS);
        assertNotNull(original, "库存效期提醒配置应存在");

        SystemConfig firstPatch = new SystemConfig();
        firstPatch.setConfigId(original.getConfigId());
        firstPatch.setConfigValue("7");
        systemConfigMapper.updateById(firstPatch);
        assertEquals(7, systemConfigService.getInt(SystemConfigService.KEY_STOCK_DAYS, 30),
                "直接改库后读取应立即生效，不能命中旧缓存");

        SystemConfig secondPatch = new SystemConfig();
        secondPatch.setConfigId(original.getConfigId());
        secondPatch.setConfigValue("15");
        systemConfigMapper.updateById(secondPatch);
        assertEquals(15, systemConfigService.getInt(SystemConfigService.KEY_STOCK_DAYS, 30),
                "再次改库后读取应继续取最新值");
    }

    // endregion
}
