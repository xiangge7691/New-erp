package com.tonghui.erp.Common.Config;

import com.tonghui.erp.Common.utils.JwtHelper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT认证过滤器
 * <p>
 * 每次请求时从Authorization头中提取JWT令牌，验证有效性后
 * 将用户信息设置到Spring Security上下文中
 * </p>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // region 字段定义
    // ===================================
    // 字段定义
    // ===================================

    @Autowired
    private JwtConfig jwtConfig;

    // endregion

    // region 方法定义
    // ===================================
    // 方法定义
    // ===================================

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);

            if (JwtHelper.validateToken(token, jwtConfig.getSecretKey(), jwtConfig.getIssuer(), jwtConfig.getAudience())) {
                String userId = JwtHelper.getUserIdFromToken(token, jwtConfig.getSecretKey());

                if (userId != null && !userId.isEmpty() && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userId, token, null);
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    // endregion
}
