package com.tonghui.erp.Common.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * JWT令牌工具类
 * <p>
 * 提供JWT令牌的生成、验证和解析功能，包括生成令牌、验证令牌有效性、
 * 从令牌中提取用户信息（ID、用户名、角色）以及获取令牌过期时间等功能
 * </p>
 */
public class JwtHelper {
    
    //#region JWT令牌生成方法
    // ===================================
    // JWT令牌生成方法
    // ===================================
    
    /**
     * 生成 JWT 令牌
     * 
     * @param userId 用户ID
     * @param username 用户名
     * @param roles 用户角色数组
     * @param secretKey 签名密钥
     * @param issuer 令牌发行方
     * @param audience 令牌受众
     * @param expiresInMinutes 过期时间（分钟）
     * @return JWT 令牌字符串
     */
    public static String generateToken(
            String userId,
            String username,
            String[] roles,
            String secretKey,
            String issuer,
            String audience,
            int expiresInMinutes) {
        
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        
        // 创建用户声明
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expiresInMinutes * 60 * 1000L);
        
        JwtBuilder builder = Jwts.builder()
                .claim("userId", userId)
                .claim("username", username)
                .setId(UUID.randomUUID().toString())
                .claim("iat", now.getTime() / 1000)
                .setExpiration(expiration)
                .setIssuer(issuer)
                .setAudience(audience)
                .signWith(key, SignatureAlgorithm.HS256);
        
        // 添加角色声明
        if (roles != null) {
            builder.claim("roles", Arrays.asList(roles));
        }
        
        // 创建并返回令牌
        return builder.compact();
    }
    
    //#endregion
    
    //#region JWT令牌验证方法
    // ===================================
    // JWT令牌验证方法
    // ===================================
    
    /**
     * 验证 JWT 令牌
     * 
     * @param token JWT 令牌
     * @param secretKey 签名密钥
     * @param issuer 令牌发行方
     * @param audience 令牌受众
     * @return 验证是否成功，true表示验证通过，false表示验证失败
     */
    public static boolean validateToken(
            String token,
            String secretKey,
            String issuer,
            String audience) {
        
        try {
            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
            
            Jws<Claims> claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .requireIssuer(issuer)
                    .requireAudience(audience)
                    .build()
                    .parseClaimsJws(token);
            
            // 检查令牌是否过期
            Date expiration = claims.getBody().getExpiration();
            return expiration != null && expiration.after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
    
    //#endregion
    
    //#region JWT令牌信息解析方法
    // ===================================
    // JWT令牌信息解析方法
    // ===================================
    
    /**
     * 从 JWT 令牌中获取用户ID
     * 
     * @param token JWT 令牌
     * @param secretKey 签名密钥
     * @return 用户ID，解析失败时返回空字符串
     */
    public static String getUserIdFromToken(String token, String secretKey) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
            
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            return claims.get("userId", String.class);
        } catch (JwtException e) {
            return "";
        }
    }
    
    /**
     * 从 JWT 令牌中获取用户名
     * 
     * @param token JWT 令牌
     * @param secretKey 签名密钥
     * @return 用户名，解析失败时返回空字符串
     */
    public static String getUsernameFromToken(String token, String secretKey) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
            
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            return claims.get("username", String.class);
        } catch (JwtException e) {
            return "";
        }
    }
    
    /**
     * 从 JWT 令牌中获取用户角色
     * 
     * @param token JWT 令牌
     * @param secretKey 签名密钥
     * @return 用户角色数组，解析失败时返回空数组
     */
    public static String[] getRolesFromToken(String token, String secretKey) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
            
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            List<String> roles = claims.get("roles", List.class);
            return roles != null ? roles.toArray(new String[0]) : new String[0];
        } catch (JwtException e) {
            return new String[0];
        }
    }
    
    /**
     * 获取令牌过期时间
     * 
     * @param token JWT 令牌
     * @param secretKey 签名密钥
     * @return 过期时间，解析失败时返回初始时间
     */
    public static Date getExpirationDateFromToken(String token, String secretKey) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
            
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            return claims.getExpiration();
        } catch (JwtException e) {
            return new Date(0);
        }
    }
    
    //#endregion
    
    //#region JWT 令牌刷新方法
    // ===================================
    // JWT 令牌刷新方法
    // ===================================
    
    /**
     * 刷新 JWT 令牌
     * <p>
     * 验证旧令牌的有效性，如果有效则基于旧令牌信息生成新的访问令牌
     * 此方法不会改变用户的角色和权限信息，仅更新令牌的有效期
     * </p>
     * 
     * @param refreshToken 刷新令牌
     * @param secretKey 签名密钥
     * @param issuer 令牌发行方
     * @param audience 令牌受众
     * @param expiresInMinutes 新令牌的过期时间（分钟）
     * @return 新的 JWT 访问令牌，如果刷新失败则返回空字符串
     */
    public static String refreshAccessToken(
            String refreshToken,
            String secretKey,
            String issuer,
            String audience,
            int expiresInMinutes) {
        
        try {
            // 首先验证刷新令牌是否有效
            if (!validateToken(refreshToken, secretKey, issuer, audience)) {
                return "";
            }
            
            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
            
            // 解析刷新令牌中的用户信息
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(refreshToken)
                    .getBody();
            
            // 提取用户信息
            String userId = claims.get("userId", String.class);
            String username = claims.get("username", String.class);
            List<String> roles = claims.get("roles", List.class);
            
            // 如果必要信息缺失，返回空字符串
            if (userId == null || username == null) {
                return "";
            }
            
            // 生成新的访问令牌
            return generateToken(
                    userId,
                    username,
                    roles != null ? roles.toArray(new String[0]) : new String[0],
                    secretKey,
                    issuer,
                    audience,
                    expiresInMinutes
            );
        } catch (JwtException | IllegalArgumentException e) {
            return "";
        }
    }
    
    /**
     * 从刷新令牌中提取用户信息
     * <p>
     * 用于在刷新令牌前验证用户身份和权限
     * </p>
     * 
     * @param refreshToken 刷新令牌
     * @param secretKey 签名密钥
     * @return 包含用户信息的 Map，包含 userId、username 和 roles
     */
    public static Map<String, Object> getUserInfoFromRefreshToken(String refreshToken, String secretKey) {
        Map<String, Object> userInfo = new HashMap<>();
        
        try {
            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
            
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(refreshToken)
                    .getBody();
            
            userInfo.put("userId", claims.get("userId", String.class));
            userInfo.put("username", claims.get("username", String.class));
            userInfo.put("roles", claims.get("roles", List.class));
            
        } catch (JwtException e) {
            // 解析失败时返回空 map
        }
        
        return userInfo;
    }
    
    //#endregion
}
