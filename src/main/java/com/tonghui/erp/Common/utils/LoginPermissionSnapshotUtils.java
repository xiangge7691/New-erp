package com.tonghui.erp.Common.utils;

import com.tonghui.erp.Common.Dto.System.PermissionDto;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 登录权限快照工具类
 * <p>
 * 将登录响应中的权限树拍平成“菜单范围”和“接口权限”两组快照，
 * 用于登录日志记录账号在登录当时实际拥有的角色与权限
 * </p>
 */
public final class LoginPermissionSnapshotUtils {

    // region 常量
    // ===================================
    // 常量
    // ===================================

    /** 权限键分隔符 */
    public static final String KEY_SEPARATOR = ",";

    /** 权限名称分隔符 */
    public static final String NAME_SEPARATOR = "、";

    /** 菜单类型 */
    private static final String TYPE_MENU = "menu";

    // endregion

    // region 构造方法
    // ===================================
    // 构造方法
    // ===================================

    /**
     * 工具类禁止实例化
     */
    private LoginPermissionSnapshotUtils() {
    }

    // endregion

    // region 权限树拍平
    // ===================================
    // 权限树拍平
    // ===================================

    /**
     * 拍平权限树
     *
     * @param permissionTree 登录响应中的权限树
     * @return 菜单与接口权限快照
     */
    public static Snapshot flatten(List<PermissionDto> permissionTree) {
        Snapshot snapshot = new Snapshot();
        collect(permissionTree, snapshot, new HashSet<>());
        return snapshot;
    }

    /**
     * 递归收集权限节点
     *
     * @param nodes   权限节点列表
     * @param snapshot 快照
     * @param visited  已访问的权限ID，避免异常数据导致重复或死循环
     */
    private static void collect(List<PermissionDto> nodes, Snapshot snapshot, Set<Long> visited) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        for (PermissionDto node : nodes) {
            if (node == null) {
                continue;
            }
            if (node.getId() != null && !visited.add(node.getId())) {
                continue;
            }
            addPermission(node, snapshot);
            collect(node.getChildren(), snapshot, visited);
        }
    }

    /**
     * 将单个权限节点加入菜单或接口权限列表
     *
     * @param node     权限节点
     * @param snapshot 快照
     */
    private static void addPermission(PermissionDto node, Snapshot snapshot) {
        boolean menu = node.getPermType() != null
                && TYPE_MENU.equalsIgnoreCase(String.valueOf(node.getPermType()).trim());
        if (menu) {
            addIfPresent(snapshot.getMenuKeys(), node.getPermKey());
            addIfPresent(snapshot.getMenuNames(), node.getPermName());
        } else {
            addIfPresent(snapshot.getPermissionKeys(), node.getPermKey());
            addIfPresent(snapshot.getPermissionNames(), node.getPermName());
        }
    }

    /**
     * 有值时加入列表
     *
     * @param list  目标列表
     * @param value 值
     */
    private static void addIfPresent(List<String> list, String value) {
        if (value != null && !value.trim().isEmpty()) {
            list.add(value.trim());
        }
    }

    // endregion

    // region 字符串拼接与拆分
    // ===================================
    // 字符串拼接与拆分
    // ===================================

    /**
     * 拼接权限键
     *
     * @param keys 权限键列表
     * @return 逗号分隔字符串
     */
    public static String joinKeys(List<String> keys) {
        return join(keys, KEY_SEPARATOR);
    }

    /**
     * 拼接权限名称
     *
     * @param names 权限名称列表
     * @return 顿号分隔字符串
     */
    public static String joinNames(List<String> names) {
        return join(names, NAME_SEPARATOR);
    }

    /**
     * 拼接字符串列表
     *
     * @param values    值列表
     * @param separator 分隔符
     * @return 拼接结果
     */
    public static String join(List<?> values, String separator) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            String text = String.valueOf(value).trim();
            if (text.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(separator);
            }
            builder.append(text);
        }
        return builder.length() == 0 ? null : builder.toString();
    }

    /**
     * 拆分权限键
     *
     * @param text 逗号分隔字符串
     * @return 权限键列表
     */
    public static List<String> splitKeys(String text) {
        return split(text, KEY_SEPARATOR);
    }

    /**
     * 拆分权限名称
     *
     * @param text 顿号分隔字符串
     * @return 权限名称列表
     */
    public static List<String> splitNames(String text) {
        return split(text, NAME_SEPARATOR);
    }

    /**
     * 拆分角色ID
     *
     * @param text 逗号分隔字符串
     * @return 角色ID列表
     */
    public static List<Long> splitIds(String text) {
        List<Long> ids = new ArrayList<>();
        for (String item : split(text, KEY_SEPARATOR)) {
            try {
                ids.add(Long.parseLong(item));
            } catch (NumberFormatException ignored) {
                // 非法ID直接忽略
            }
        }
        return ids;
    }

    /**
     * 拆分字符串
     *
     * @param text      文本
     * @param separator 分隔符
     * @return 拆分结果
     */
    private static List<String> split(String text, String separator) {
        List<String> values = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return values;
        }
        for (String item : text.split(separator)) {
            String trimmed = item.trim();
            if (!trimmed.isEmpty()) {
                values.add(trimmed);
            }
        }
        return values;
    }

    // endregion

    // region 快照对象
    // ===================================
    // 快照对象
    // ===================================

    /**
     * 登录权限快照
     */
    @Data
    public static class Snapshot {

        /**
         * 菜单权限键
         */
        private List<String> menuKeys = new ArrayList<>();

        /**
         * 菜单名称
         */
        private List<String> menuNames = new ArrayList<>();

        /**
         * 接口权限键
         */
        private List<String> permissionKeys = new ArrayList<>();

        /**
         * 接口权限名称
         */
        private List<String> permissionNames = new ArrayList<>();
    }

    // endregion
}
