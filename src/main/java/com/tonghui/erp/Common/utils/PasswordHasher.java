package com.tonghui.erp.Common.utils;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * 提供安全的密码哈希和验证功能，使用 Argon2 算法。
 * <p>
 * Argon2 是密码哈希竞赛的获胜者，提供比 PBKDF2 更好的安全性和性能。
 * 该类提供密码加密和验证功能，确保用户密码的安全存储和验证。
 * </p>
 */
public class PasswordHasher {
    
    //#region 常量定义
    // ===================================
    // 常量定义
    // ===================================
    
    private static final SecureRandom random = new SecureRandom();
    private static final int SALT_LENGTH = 16;
    private static final int HASH_LENGTH = 32;
    
    //#endregion
    
    //#region 密码哈希方法
    // ===================================
    // 密码哈希方法
    // ===================================
    
    /**
     * 对明文密码进行加密处理
     * 
     * @param password 待加密的明文密码
     * @return Base64 格式的加密字符串
     * @throws IllegalArgumentException 当 password 为 null 或空字符串时抛出
     */
    public static String hashPassword(String password) {
        // 参数验证（防止空密码攻击）
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }

        // 生成随机盐值
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);

        // 配置 Argon2 参数
        Argon2Parameters.Builder builder = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withIterations(3)
                .withMemoryAsKB(65536)
                .withParallelism(4)
                .withSalt(salt);

        Argon2Parameters parameters = builder.build();

        // 创建 Argon2 实例并进行哈希计算
        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(parameters);
        
        byte[] hash = new byte[HASH_LENGTH];
        generator.generateBytes(password.toCharArray(), hash);
        
        // 组合盐值和哈希值
        byte[] combinedBytes = new byte[SALT_LENGTH + HASH_LENGTH]; // 16字节盐值 + 32字节哈希值
        System.arraycopy(salt, 0, combinedBytes, 0, SALT_LENGTH);
        System.arraycopy(hash, 0, combinedBytes, SALT_LENGTH, HASH_LENGTH);
        
        // 返回 Base64 编码的组合结果
        return Base64.getEncoder().encodeToString(combinedBytes);
    }
    
    //#endregion

    //#region 密码验证方法
    // ===================================
    // 密码验证方法
    // ===================================
    
    /**
     * 验证输入的密码是否与存储的哈希值匹配
     * 
     * @param password 待验证的明文密码
     * @param hashedPassword Base64 格式的加密密码
     * @return true: 密证匹配, false: 密码不匹配或格式错误
     */
    public static boolean verifyPassword(String password, String hashedPassword) {
        // 参数验证
        if (hashedPassword == null || hashedPassword.isEmpty() || 
            password == null || password.isEmpty()) {
            return false;
        }

        try {
            // 解码存储的哈希值
            byte[] combinedBytes = Base64.getDecoder().decode(hashedPassword);
            
            // 检查长度是否正确
            if (combinedBytes.length != SALT_LENGTH + HASH_LENGTH) {
                return false;
            }
            
            // 提取盐值和哈希值
            byte[] salt = new byte[SALT_LENGTH];
            byte[] storedHash = new byte[HASH_LENGTH];
            System.arraycopy(combinedBytes, 0, salt, 0, SALT_LENGTH);
            System.arraycopy(combinedBytes, SALT_LENGTH, storedHash, 0, HASH_LENGTH);

            // 配置 Argon2 参数
            Argon2Parameters.Builder builder = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                    .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                    .withIterations(3)
                    .withMemoryAsKB(65536)
                    .withParallelism(4)
                    .withSalt(salt);

            Argon2Parameters parameters = builder.build();

            // 使用相同盐值计算输入密码的哈希值
            Argon2BytesGenerator generator = new Argon2BytesGenerator();
            generator.init(parameters);
            
            byte[] computedHash = new byte[HASH_LENGTH];
            generator.generateBytes(password.toCharArray(), computedHash);
            
            // 安全比较哈希值
            return Arrays.equals(storedHash, computedHash);
        } catch (Exception e) {
            // 处理任何验证异常
            return false;
        }
    }
    
    //#endregion
}
