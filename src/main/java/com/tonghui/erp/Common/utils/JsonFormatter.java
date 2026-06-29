package com.tonghui.erp.Common.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;

/**
 * 提供JSON格式化和解析功能的工具类
 * <p>
 * 提供对象与JSON字符串之间的序列化和反序列化功能，以及JSON格式验证、格式化和压缩功能
 * </p>
 */
public class JsonFormatter {
    
    //#region ObjectMapper实例定义
    // ===================================
    // ObjectMapper实例定义
    // ===================================
    
    private static final ObjectMapper defaultMapper = new ObjectMapper();
    private static final ObjectMapper strictMapper = new ObjectMapper();
    
    static {
        // 默认的JSON序列化选项
        defaultMapper.enable(SerializationFeature.INDENT_OUTPUT);
        defaultMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        
        // 严格模式的JSON序列化选项（用于数据验证）
        strictMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        strictMapper.disable(JsonParser.Feature.ALLOW_COMMENTS);
        strictMapper.disable(JsonParser.Feature.ALLOW_TRAILING_COMMA);
    }
    
    //#endregion
    
    //#region JSON序列化方法
    // ===================================
    // JSON序列化方法
    // ===================================
    
    /**
     * 将对象序列化为JSON字符串
     * 
     * @param obj 要序列化的对象
     * @param useStrictMode 是否使用严格模式
     * @return JSON字符串
     * @throws RuntimeException 当序列化过程中发生错误时抛出
     */
    public static String serialize(Object obj, boolean useStrictMode) {
        try {
            ObjectMapper mapper = useStrictMode ? strictMapper : defaultMapper;
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化对象为JSON时出错", e);
        }
    }
    
    //#endregion
    
    //#region JSON反序列化方法
    // ===================================
    // JSON反序列化方法
    // ===================================
    
    /**
     * 将JSON字符串反序列化为对象
     * 
     * @param <T> 对象类型泛型参数
     * @param json JSON字符串
     * @param clazz 目标对象类型
     * @param useStrictMode 是否使用严格模式
     * @return 反序列化的对象
     * @throws RuntimeException 当反序列化过程中发生错误时抛出
     */
    public static <T> T deserialize(String json, Class<T> clazz, boolean useStrictMode) {
        try {
            ObjectMapper mapper = useStrictMode ? strictMapper : defaultMapper;
            return mapper.readValue(json, clazz);
        } catch (IOException e) {
            throw new RuntimeException("反序列化JSON为对象时出错", e);
        }
    }
    
    //#endregion
    
    //#region JSON验证方法
    // ===================================
    // JSON验证方法
    // ===================================
    
    /**
     * 验证JSON字符串格式是否正确
     * 
     * @param json 要验证的JSON字符串
     * @return 验证结果，true表示格式正确，false表示格式错误
     */
    public static boolean isValidJson(String json) {
        if (json == null || json.trim().isEmpty())
            return false;
            
        try {
            defaultMapper.readTree(json);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    //#endregion
    
    //#region JSON格式化方法
    // ===================================
    // JSON格式化方法
    // ===================================
    
    /**
     * 格式化JSON字符串
     * 
     * @param json 原始JSON字符串
     * @return 格式化后的JSON字符串
     * @throws IllegalArgumentException 当输入的JSON字符串格式不正确时抛出
     * @throws RuntimeException 当格式化过程中发生错误时抛出
     */
    public static String formatJson(String json) {
        if (!isValidJson(json))
            throw new IllegalArgumentException("Invalid JSON string");
            
        try {
            ObjectNode jsonObject = (ObjectNode) defaultMapper.readTree(json);
            return defaultMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
        } catch (IOException e) {
            throw new RuntimeException("格式化JSON时出错", e);
        }
    }
    
    //#endregion
    
    //#region JSON压缩方法
    // ===================================
    // JSON压缩方法
    // ===================================
    
    /**
     * 压缩JSON字符串（移除空格和换行符）
     * 
     * @param json 原始JSON字符串
     * @return 压缩后的JSON字符串
     * @throws IllegalArgumentException 当输入的JSON字符串格式不正确时抛出
     * @throws RuntimeException 当压缩过程中发生错误时抛出
     */
    public static String minifyJson(String json) {
        if (!isValidJson(json))
            throw new IllegalArgumentException("Invalid JSON string");
            
        try {
            JsonNode jsonObject = defaultMapper.readTree(json);
            ObjectMapper minifyMapper = new ObjectMapper();
            minifyMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            return minifyMapper.writeValueAsString(jsonObject);
        } catch (IOException e) {
            throw new RuntimeException("压缩JSON时出错", e);
        }
    }
    
    //#endregion
}
