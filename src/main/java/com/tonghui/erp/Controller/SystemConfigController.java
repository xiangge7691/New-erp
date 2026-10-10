package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.System.DashboardExpiryConfigDto;
import com.tonghui.erp.Common.Dto.System.SystemConfigDto;
import com.tonghui.erp.Data.Entity.SystemConfig;
import com.tonghui.erp.Service.SystemConfigService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统配置控制器
 *
 * 接口清单：
 * ┌────┬─────────────────────────────────────────────┬────────┬──────────────────────────────────┐
 * │ #  │ 接口                                        │ 方法   │ 说明                             │
 * ├────┼─────────────────────────────────────────────┼────────┼──────────────────────────────────┤
 * │ 1  │ /api/system/config                          │ GET    │ 查询配置项列表（分组过滤/关键字搜索）│
 * │ 2  │ /api/system/config/{key}                    │ GET    │ 根据配置键查询单个配置项          │
 * │ 3  │ /api/system/config/group/{group}            │ GET    │ 按分组查询配置项                  │
 * │ 4  │ /api/system/config/{key}                    │ PUT    │ 更新单个配置值                    │
 * │ 5  │ /api/system/config/batch                    │ PUT    │ 批量更新配置值                    │
 * │ 6  │ /api/system/config/dashboard-expiry         │ GET    │ 查询首页到期提醒天数聚合配置       │
 * └────┴─────────────────────────────────────────────┴────────┴──────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/system/config")
public class SystemConfigController extends BaseController {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 系统配置服务
     */
    @Autowired
    private SystemConfigService systemConfigService;

    // endregion

    // region 查询接口
    // ===================================
    // 查询接口
    // ===================================

    /**
     * 查询配置项列表（可根据分组过滤，并按关键字搜索）
     *
     * 示例请求：
     * GET /api/system/config
     * GET /api/system/config?group=dashboard_expiry
     * GET /api/system/config?keyword=到期
     * GET /api/system/config?group=dashboard_expiry&keyword=库存
     *
     * @param group   配置分组（可选，精确匹配）
     * @param keyword 关键字（可选，模糊匹配配置名称或配置键）
     * @return ApiResponse&lt;List&lt;SystemConfigDto&gt;&gt; 配置项列表
     */
    @GetMapping
    public ApiResponse<List<SystemConfigDto>> list(@RequestParam(required = false) String group,
                                                   @RequestParam(required = false) String keyword) {
        try {
            boolean filtered = StringUtils.hasText(group) || StringUtils.hasText(keyword);
            List<SystemConfig> configs = filtered
                    ? systemConfigService.search(group, keyword)
                    : systemConfigService.listAll();
            return success(configs.stream().map(this::toDto).collect(Collectors.toList()));
        } catch (Exception ex) {
            return exception(ex, "查询系统配置");
        }
    }

    /**
     * 根据配置键查询单个配置项
     *
     * 示例请求：
     * GET /api/system/config/dashboard.expiry.stock.days
     *
     * @param key 配置键（路径参数）
     * @return ApiResponse&lt;SystemConfigDto&gt; 配置项
     */
    @GetMapping("/{key}")
    public ApiResponse<SystemConfigDto> getByKey(@PathVariable String key) {
        try {
            SystemConfig config = systemConfigService.getByKey(key);
            if (config == null) {
                return error("配置项不存在");
            }
            return success(toDto(config));
        } catch (Exception ex) {
            return exception(ex, "查询系统配置");
        }
    }

    /**
     * 按分组查询配置项
     *
     * 示例请求：
     * GET /api/system/config/group/dashboard_expiry
     *
     * @param group 配置分组（路径参数）
     * @return ApiResponse&lt;List&lt;SystemConfigDto&gt;&gt; 配置项列表
     */
    @GetMapping("/group/{group}")
    public ApiResponse<List<SystemConfigDto>> getByGroup(@PathVariable String group) {
        try {
            return success(systemConfigService.getByGroup(group).stream()
                    .map(this::toDto).collect(Collectors.toList()));
        } catch (Exception ex) {
            return exception(ex, "查询系统配置");
        }
    }

    /**
     * 查询首页到期提醒天数聚合配置
     *
     * 示例请求：
     * GET /api/system/config/dashboard-expiry
     *
     * @return ApiResponse&lt;DashboardExpiryConfigDto&gt; 各类到期提醒天数
     */
    @GetMapping("/dashboard-expiry")
    public ApiResponse<DashboardExpiryConfigDto> getDashboardExpiry() {
        try {
            return success(systemConfigService.getDashboardExpiryConfig());
        } catch (Exception ex) {
            return exception(ex, "查询到期提醒配置");
        }
    }

    // endregion

    // region 更新接口
    // ===================================
    // 更新接口
    // ===================================

    /**
     * 更新单个配置值
     *
     * 示例请求：
     * PUT /api/system/config/dashboard.expiry.stock.days
     * 请求体：{"configValue": "7"}
     *
     * @param key  配置键（路径参数）
     * @param body 配置项（使用 configValue 字段）
     * @return ApiResponse&lt;Void&gt; 操作结果
     */
    @PutMapping("/{key}")
    public ApiResponse<Void> updateValue(@PathVariable String key, @RequestBody SystemConfigDto body) {
        try {
            if (body == null) {
                return error("请求体不能为空");
            }
            systemConfigService.updateValue(key, body.getConfigValue());
            return success(null, "更新成功");
        } catch (Exception ex) {
            return exception(ex, "更新系统配置");
        }
    }

    /**
     * 批量更新配置值
     *
     * 示例请求：
     * PUT /api/system/config/batch
     * 请求体：
     * [
     *   {"configKey": "dashboard.expiry.stock.days", "configValue": "7"},
     *   {"configKey": "dashboard.expiry.equipment.days", "configValue": "15"}
     * ]
     *
     * @param items 配置项列表（每项使用 configKey 与 configValue）
     * @return ApiResponse&lt;Void&gt; 操作结果
     */
    @PutMapping("/batch")
    public ApiResponse<Void> batchUpdate(@RequestBody List<SystemConfigDto> items) {
        try {
            if (items == null || items.isEmpty()) {
                return error("配置项不能为空");
            }
            systemConfigService.batchSave(items);
            return success(null, "批量更新成功");
        } catch (Exception ex) {
            return exception(ex, "批量更新系统配置");
        }
    }

    // endregion

    // region 内部方法
    // ===================================
    // 内部方法
    // ===================================

    /**
     * 实体转DTO
     *
     * @param config 配置实体
     * @return 配置项DTO
     */
    private SystemConfigDto toDto(SystemConfig config) {
        SystemConfigDto dto = new SystemConfigDto();
        BeanUtils.copyProperties(config, dto);
        return dto;
    }

    // endregion
}
