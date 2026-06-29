package com.tonghui.erp.Common.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Base64编码解码工具类
 * <p>
 * 提供文件与Base64字符串之间的相互转换功能，以及字节数组与Base64字符串之间的相互转换功能
 * </p>
 */
public class Base64 {
    
    //#region 文件与Base64互转方法
    // ===================================
    // 文件与Base64互转方法
    // ===================================
    
    /**
     * 将文件转换为Base64字符串
     * 
     * @param filePath 文件路径
     * @return Base64字符串
     * @throws IOException 当文件不存在或读取文件时发生错误
     */
    public static String fileToBase64(String filePath) throws IOException {
        if (!Files.exists(Paths.get(filePath))) {
            throw new IOException("文件未找到: " + filePath);
        }

        byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));
        return java.util.Base64.getEncoder().encodeToString(fileBytes);
    }

    /**
     * 将Base64字符串转换为文件
     * 
     * @param base64String Base64字符串
     * @param filePath 保存文件的路径
     * @throws IOException 当创建目录或写入文件时发生错误
     */
    public static void base64ToFile(String base64String, String filePath) throws IOException {
        byte[] fileBytes = java.util.Base64.getDecoder().decode(base64String);
        
        // 确保目录存在
        Path path = Paths.get(filePath);
        Files.createDirectories(path.getParent());
        
        Files.write(path, fileBytes);
    }
    
    //#endregion

    //#region 字节数组与Base64互转方法
    // ===================================
    // 字节数组与Base64互转方法
    // ===================================
    
    /**
     * 将字节数组转换为Base64字符串
     * 
     * @param bytes 字节数组
     * @return Base64字符串
     * @throws IllegalArgumentException 当字节数组为空时抛出
     */
    public static String bytesToBase64(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("字节数组不能为空");
        }

        return java.util.Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * 将Base64字符串转换为字节数组
     * 
     * @param base64String Base64字符串
     * @return 字节数组
     * @throws IllegalArgumentException 当Base64字符串为空时抛出
     */
    public static byte[] base64ToBytes(String base64String) {
        if (base64String == null || base64String.isEmpty()) {
            throw new IllegalArgumentException("Base64字符串不能为空");
        }

        return java.util.Base64.getDecoder().decode(base64String);
    }
    
    //#endregion
}
