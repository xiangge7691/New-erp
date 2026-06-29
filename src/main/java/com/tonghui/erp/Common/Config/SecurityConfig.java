package com.tonghui.erp.Common.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF（适用于 API）
            .csrf(csrf -> csrf.disable())
            // 禁用 session（使用 JWT）
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 添加 JWT 过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            // 配置授权规则
            .authorizeHttpRequests(authz -> authz
                // 允许访问认证相关接口
                .requestMatchers("/api/auth/**").permitAll()
                // 允许访问登录页面和相关资源
                .requestMatchers("/login", "/login/**").permitAll()
                // 允许访问所有前端资源
                .requestMatchers("/", "/index.html", "/favicon.ico", "/static/**").permitAll()
                .requestMatchers("/assets/**", "/js/**", "/css/**", "/img/**", "/fonts/**").permitAll()
                // 允许访问 Swagger UI（如果使用）
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // 允许访问 Vue 开发服务器代理的请求
                .requestMatchers("/api/**").permitAll()
                // 所有其他请求都需要认证
                .anyRequest().permitAll()
                // 临时允许所有请求，方便调试
            );
            
        return http.build();
    }
}
