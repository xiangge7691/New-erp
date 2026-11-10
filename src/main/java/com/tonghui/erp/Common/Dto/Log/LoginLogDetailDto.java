package com.tonghui.erp.Common.Dto.Log;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 登录日志详情
 * <p>
 * 在列表字段基础上补充登录当时的菜单范围与接口权限明细，
 * 用于前端“查看详情”抽屉展示账号实际可操作的内容
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LoginLogDetailDto extends LoginLogDto {

    // region 菜单范围字段
    // ===================================
    // 菜单范围字段
    // ===================================

    /**
     * 菜单权限键快照
     */
    private List<String> menuKeys = new ArrayList<>();

    /**
     * 菜单名称快照
     */
    private List<String> menuNames = new ArrayList<>();

    // endregion

    // region 接口权限字段
    // ===================================
    // 接口权限字段
    // ===================================

    /**
     * 接口权限键快照
     */
    private List<String> permissionKeys = new ArrayList<>();

    /**
     * 接口权限名称快照
     */
    private List<String> permissionNames = new ArrayList<>();

    // endregion
}
