package com.tonghui.erp.Common.Config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {
    
    /**
     * JWT密钥 - 用于签名和验证JWT令牌
     * 注意：在生产环境中应使用更安全的密钥，并从环境变量或配置中心获取
     */
    private String secretKey = "default_secret_key_which_should_be_replaced";
    
    /**
     * JWT签发者 - 标识此JWT由哪个系统或服务签发
     */
    private String issuer = "ErpSys";
    
    /**
     * JWT受众 - 标识此JWT的目标接收方
     */
    private String audience = "ErpSysUsers";
    
    /**
     * JWT过期时间 - 单位：分钟
     * 默认值：480分钟（8小时）
     */
    private int expiresInMinutes = 480;
    
    /**
     * 刷新令牌过期时间 - 单位：分钟
     * 默认值：10080分钟（7天）
     * 计算方式：7天 * 24小时/天 * 60分钟/小时 = 10080分钟
     */
    private int refreshExpiresInMinutes = 10080; // 默认7天
}
