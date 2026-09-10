package com.tonghui.erp.Common.Dto.System;

import lombok.Data;

/**
 * 路由元信息数据传输对象
 * <p>
 * 用于前端路由的元信息配置，包括菜单标题和是否支持国际化
 * </p>
 */
@Data
public class RouteMetaDto {

    /**
     * 路由/菜单标题
     */
    private String title;

    /**
     * 是否支持国际化
     */
    private boolean i18n;
}
