package com.tonghui.erp;

import com.tonghui.erp.Common.utils.JwtHelper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * ERP系统主应用启动类
 * <p>
 * 系统入口点，负责启动Spring Boot应用程序，配置MyBatis映射器扫描路径，
 * 并配置JWT拦截器用于API接口的身份验证
 * </p>
 */
@SpringBootApplication
@MapperScan("com.tonghui.erp.Data.mapper")
public class ErpApplication {

    //#region 应用启动方法
    // ===================================
    // 应用启动方法
    // ===================================
    
    /**
     * 应用程序入口点
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ErpApplication.class, args);
    }
    
    //#endregion

    //#region Web配置
    // ===================================
    // Web配置
    // ===================================
    
    /**
     * 配置Web MVC相关设置
     * 
     * @return WebMvcConfigurer对象
     */
    @Bean
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new JwtInterceptor())
                        .addPathPatterns("/api/**")  // 拦截所有/api开头的请求
                        .excludePathPatterns("/api/auth/login"); // 排除登录接口
            }
        };
    }
    
    //#endregion

    //#region JWT拦截器内部类
    // ===================================
    // JWT拦截器内部类
    // ===================================
    
    /**
     * JWT令牌验证拦截器
     * <p>
     * 用于拦截API请求并验证JWT令牌的有效性，确保只有经过身份验证的用户才能访问受保护的资源
     * </p>
     */
    static class JwtInterceptor implements HandlerInterceptor {
        
        //#region JWT配置属性
        // ===================================
        // JWT配置属性
        // ===================================
        
        /**
         * JWT密钥
         * <p>用于签名和验证JWT令牌的密钥</p>
         */
        @Value("${jwt.secret-key:default_secret_key_which_should_be_replaced}")
        private String secretKey = "default_secret_key_which_should_be_replaced";

        /**
         * JWT签发者
         * <p>JWT令牌的签发者标识</p>
         */
        @Value("${jwt.issuer:ErpSys}")
        private String issuer = "ErpSys";

        /**
         * JWT受众
         * <p>JWT令牌的目标受众标识</p>
         */
        @Value("${jwt.audience:ErpSysUsers}")
        private String audience = "ErpSysUsers";
        
        //#endregion

        //#region 请求拦截方法
        // ===================================
        // 请求拦截方法
        // ===================================
        
        /**
         * 在请求处理之前进行拦截验证
         * 
         * @param request HTTP请求对象
         * @param response HTTP响应对象
         * @param handler 处理器对象
         * @return true表示放行，false表示拦截
         * @throws Exception 异常信息
         */
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            // 获取请求路径
            String requestURI = request.getRequestURI();
            
            // 如果是登录接口，直接放行（兼容Bearer拦截配置的排除项）
            if (requestURI.contains("/api/auth/login") ){
                return true;
            }
            
            // 从请求头中获取token
            String token = request.getHeader("Authorization");
            
            // 如果没有token，返回401
            if (token == null || token.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"success\":false,\"message\":\"缺少访问令牌\",\"code\":401}");
                return false;
            }
            
            // 如果token以Bearer开头，去掉前缀
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            // 验证token
            boolean isValid = JwtHelper.validateToken(token, secretKey, issuer, audience);
            
            if (!isValid) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"success\":false,\"message\":\"令牌无效或已过期\",\"code\":401}");
                return false;
            }
            
            // 可以将用户信息存入request中供后续使用
            String userId = JwtHelper.getUserIdFromToken(token, secretKey);
            String username = JwtHelper.getUsernameFromToken(token, secretKey);
            request.setAttribute("userId", userId);
            request.setAttribute("username", username);
            
            return true;
        }
        
        //#endregion
    }
    
    //#endregion
}
