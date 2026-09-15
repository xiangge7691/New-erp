package com.tonghui.erp.Common.Config.InitDataConfig;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.stereotype.Component;

/**
 * Root用户/角色初始化配置类
 * <p>
 * 从init-data/root.yml读取root用户和角色的初始化配置
 * </p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "init-data.root")
@PropertySources({
    @PropertySource(value = "classpath:init-data/root.yml", encoding = "UTF-8")
})
public class RootInitConfig {

    // region 配置属性
    // ===================================
    // 配置属性
    // ===================================

    /**
     * 是否启用root用户/角色初始化
     */
    private boolean enabled;

    /**
     * root用户账号
     */
    private String userAccount;

    /**
     * root用户姓名
     */
    private String userName;

    /**
     * root用户密码（明文，启动时自动Argon2加密）
     */
    private String password;

    /**
     * root角色名称
     */
    private String roleName;

    /**
     * root角色描述
     */
    private String roleDesc;

    // endregion
}
