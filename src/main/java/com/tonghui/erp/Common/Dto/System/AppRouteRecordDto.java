package com.tonghui.erp.Common.Dto.System;

import lombok.Data;
import java.util.List;
import java.util.ArrayList;

/**
 * 应用路由记录数据传输对象
 * <p>
 * 用于前端路由配置，定义菜单路由的路径、名称、组件及子路由等信息
 * </p>
 */
@Data
public class AppRouteRecordDto {

    /**
     * 路由路径
     */
    private String path;

    /**
     * 路由名称
     */
    private String name;

    /**
     * 对应的前端组件名称
     */
    private String component;

    /**
     * 重定向路径
     */
    private String redirect;

    /**
     * 路由元信息（标题、国际化等）
     */
    private RouteMetaDto meta;

    /**
     * 子路由列表
     */
    private List<AppRouteRecordDto> children = new ArrayList<>();
}
