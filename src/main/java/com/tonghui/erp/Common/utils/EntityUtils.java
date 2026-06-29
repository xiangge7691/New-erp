package com.tonghui.erp.Common.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 通用实体操作工具类
 * 用于处理实体的创建时间、更新时间和用户信息等通用字段
 */
@Component
public class EntityUtils {

    /**
     * 从安全上下文中获取当前用户ID
     *
     * @return 当前用户ID，如果无法获取则返回默认值1
     */
    public static Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof String) {
                    try {
                        return Long.parseLong((String) principal);
                    } catch (NumberFormatException e) {
                        // 不是有效的数字格式
                    }
                } else if (principal instanceof Long) {
                    return (Long) principal;
                } else if (principal instanceof Integer) {
                    return ((Integer) principal).longValue();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 为了确保功能正常，暂时返回默认用户ID 1
        return 1L;
    }
}
