package com.tonghui.erp.Common.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * 通用数据导入导出工具类，支持Excel和TXT格式
 * <p>
 * 提供将实体列表导出为Excel或TXT文件，以及从TXT文件导入数据为实体列表的功能
 * </p>
 */
public class DataExporterImporter {

    //#region Excel导出方法
    // ===================================
    // Excel导出方法
    // ===================================
    
    /**
     * 将实体列表导出为 Excel 文件
     * 
     * @param <T> 实体类型泛型参数
     * @param data 数据列表
     * @param fileName 文件名
     * @return CompletableFuture<Void> 异步操作结果
     * @throws IllegalArgumentException 当导出数据为空或文件名为空时抛出
     */
    public static <T> CompletableFuture<Void> exportToExcel(List<T> data, String fileName) {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("导出数据不能为空");
        }

        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        // 确保文件扩展名为.xlsx
        if (!fileName.endsWith(".xlsx")) {
            fileName += ".xlsx";
        }

        final String finalFileName = fileName;
        final List<T> finalData = data;
        
        return CompletableFuture.runAsync(() -> {
            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet worksheet = workbook.createSheet();
                
                Class<?> clazz = finalData.get(0).getClass();
                Field[] fields = Arrays.stream(clazz.getDeclaredFields())
                        .filter(f -> !isComplexType(f.getType()) || isNullableType(f.getType()))
                        .toArray(Field[]::new);

                // 写入表头
                Row headerRow = worksheet.createRow(0);
                for (int i = 0; i < fields.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(fields[i].getName());
                    
                    // 设置表头样式
                    CellStyle headerStyle = workbook.createCellStyle();
                    Font font = workbook.createFont();
                    font.setBold(true);
                    headerStyle.setFont(font);
                    headerStyle.setAlignment(HorizontalAlignment.CENTER);
                    headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                    headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                    cell.setCellStyle(headerStyle);
                }

                // 写入数据
                for (int rowIdx = 0; rowIdx < finalData.size(); rowIdx++) {
                    T item = finalData.get(rowIdx);
                    Row row = worksheet.createRow(rowIdx + 1);
                    
                    for (int colIdx = 0; colIdx < fields.length; colIdx++) {
                        Field field = fields[colIdx];
                        field.setAccessible(true);
                        Cell cell = row.createCell(colIdx);
                        
                        Object value = field.get(item);
                        // 处理不同类型的值
                        if (value instanceof Date) {
                            cell.setCellValue((Date) value);
                            CellStyle style = workbook.createCellStyle();
                            style.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));
                            cell.setCellStyle(style);
                        } else if (value != null) {
                            cell.setCellValue(value.toString());
                        } else {
                            cell.setCellValue("");
                        }
                    }
                }

                // 自动调整列宽
                for (int i = 0; i < fields.length; i++) {
                    worksheet.autoSizeColumn(i);
                }

                // 确保目录存在
                Path path = Paths.get(finalFileName);
                Files.createDirectories(path.getParent());

                // 保存文件
                try (FileOutputStream fileOut = new FileOutputStream(finalFileName)) {
                    workbook.write(fileOut);
                }
                
                System.out.println("Excel数据已导出至: " + new File(finalFileName).getAbsolutePath());
            } catch (Exception ex) {
                System.out.println("Excel导出失败: " + ex.getMessage());
                throw new CompletionException(ex);
            }
        });
    }
    
    //#endregion

    //#region TXT导出方法
    // ===================================
    // TXT导出方法
    // ===================================
    
    /**
     * 将实体列表导出为 TXT 文件
     * 
     * @param <T> 实体类型泛型参数
     * @param data 数据列表
     * @param fileName 文件名
     * @param separator 分隔符，默认为制表符
     * @return CompletableFuture<Void> 异步操作结果
     * @throws IllegalArgumentException 当导出数据为空或文件名为空时抛出
     */
    public static <T> CompletableFuture<Void> exportToTxt(List<T> data, String fileName, String separator) {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("导出数据不能为空");
        }

        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        if (separator == null) {
            separator = "\t";
        }

        // 确保文件扩展名为.txt
        if (!fileName.endsWith(".txt")) {
            fileName += ".txt";
        }

        final String finalFileName = fileName;
        final String finalSeparator = separator;
        final List<T> finalData = data;
        
        return CompletableFuture.runAsync(() -> {
            try {
                Class<?> clazz = finalData.get(0).getClass();
                Field[] fields = Arrays.stream(clazz.getDeclaredFields())
                        .filter(f -> !isComplexType(f.getType()) || isNullableType(f.getType()))
                        .toArray(Field[]::new);
                
                // 准备数据行
                List<String> lines = new ArrayList<>();
                
                // 添加表头
                String[] headers = new String[fields.length];
                for (int i = 0; i < fields.length; i++) {
                    headers[i] = fields[i].getName();
                }
                lines.add(String.join(finalSeparator, headers));
                
                // 添加数据行
                for (T item : finalData) {
                    String[] values = new String[fields.length];
                    for (int i = 0; i < fields.length; i++) {
                        fields[i].setAccessible(true);
                        Object value = fields[i].get(item);
                        values[i] = formatValueForTxt(value, finalSeparator);
                    }
                    lines.add(String.join(finalSeparator, values));
                }
                
                // 确保目录存在
                Path path = Paths.get(finalFileName);
                Files.createDirectories(path.getParent());
                
                // 写入文件
                Files.write(path, lines, StandardCharsets.UTF_8);
                System.out.println("TXT数据已导出至: " + path.toAbsolutePath());
            } catch (Exception ex) {
                System.out.println("TXT导出失败: " + ex.getMessage());
                throw new CompletionException(ex);
            }
        });
    }
    
    //#endregion

    //#region TXT导入方法
    // ===================================
    // TXT导入方法
    // ===================================
    
    /**
     * 从 TXT 文件导入数据
     * 
     * @param <T> 实体类型泛型参数
     * @param fileName TXT文件路径
     * @param clazz 实体类类型
     * @param separator 分隔符，默认为制表符
     * @param hasHeader 是否包含表头，默认为true
     * @return CompletableFuture<List<T>> 异步操作结果，包含导入的实体列表
     * @throws IllegalArgumentException 当文件名为空时抛出
     */
    public static <T> CompletableFuture<List<T>> importFromTxt(
            String fileName, 
            Class<T> clazz, 
            String separator, 
            boolean hasHeader) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        if (separator == null) {
            separator = "\t";
        }

        final String finalFileName = fileName;
        final String finalSeparator = separator;
        final Class<T> finalClazz = clazz;
        final boolean finalHasHeader = hasHeader;
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<String> lines = Files.readAllLines(Paths.get(finalFileName), StandardCharsets.UTF_8);
                if (lines.isEmpty()) {
                    return new ArrayList<>();
                }

                Field[] fields = Arrays.stream(finalClazz.getDeclaredFields())
                        .filter(f -> !isComplexType(f.getType()) || isNullableType(f.getType()))
                        .toArray(Field[]::new);
                
                List<T> result = new ArrayList<>();
                
                int startIndex = finalHasHeader ? 1 : 0;
                
                for (int i = startIndex; i < lines.size(); i++) {
                    String[] values = parseLine(lines.get(i), finalSeparator);
                    T item = finalClazz.getDeclaredConstructor().newInstance();
                    
                    for (int j = 0; j < Math.min(values.length, fields.length); j++) {
                        Field field = fields[j];
                        field.setAccessible(true);
                        Object convertedValue = convertValue(values[j], field.getType());
                        field.set(item, convertedValue);
                    }
                    result.add(item);
                }
                
                System.out.println("从TXT文件导入了 " + result.size() + " 条记录");
                return result;
            } catch (Exception ex) {
                System.out.println("TXT导入失败: " + ex.getMessage());
                throw new CompletionException(ex);
            }
        });
    }
    
    //#endregion

    //#region 私有辅助方法
    // ===================================
    // 私有辅助方法
    // ===================================
    
    /**
     * 格式化值用于TXT导出
     * 
     * @param value 值对象
     * @param separator 分隔符
     * @return 格式化后的字符串
     */
    private static String formatValueForTxt(Object value, String separator) {
        if (value == null)
            return "";

        // 如果值中包含分隔符或换行符，需要用引号包围
        String strValue = value.toString();
        if (strValue.contains(separator) || strValue.contains("\n") || strValue.contains("\r") || strValue.contains("\"")) {
            // 转义引号并用引号包围
            return "\"" + strValue.replace("\"", "\"\"") + "\"";
        }
        return strValue;
    }
    
    /**
     * 解析TXT文件中的一行数据，处理引号包围的字段
     * 
     * @param line TXT文件中的一行数据
     * @param separator 分隔符
     * @return 解析后的字符串数组
     */
    private static String[] parseLine(String line, String separator) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"' && !inQuotes) {
                // 开始引号
                inQuotes = true;
            }
            else if (c == '"' && inQuotes) {
                // 可能是结束引号或者转义引号
                if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // 转义引号
                    current.append('"');
                    i++; // 跳过下一个引号
                }
                else {
                    // 结束引号
                    inQuotes = false;
                }
            }
            else if (c == separator.charAt(0) && !inQuotes) {
                // 分隔符且不在引号内
                values.add(current.toString());
                current.setLength(0);
            }
            else {
                // 普通字符
                current.append(c);
            }
        }
        
        // 添加最后一个字段
        values.add(current.toString());
        
        return values.toArray(new String[0]);
    }
    
    /**
     * 类型转换方法
     * 
     * @param value 字符串值
     * @param targetType 目标类型
     * @return 转换后的对象
     */
    private static Object convertValue(String value, Class<?> targetType) {
        if (value == null || value.isEmpty())
            return null;
            
        // 获取目标类型的底层类型（如果是可空类型）
        Class<?> underlyingType = isNullableType(targetType) ? 
                targetType : targetType;
        
        // 如果值已经是目标类型，直接返回
        if (underlyingType.isAssignableFrom(String.class) && !value.isEmpty())
            return value;
            
        // 处理特殊类型
        if (underlyingType == boolean.class || underlyingType == Boolean.class) {
            // 处理布尔值转换
            if ("true".equalsIgnoreCase(value) || "1".equals(value))
                return true;
            if ("false".equalsIgnoreCase(value) || "0".equals(value))
                return false;
            return false;
        } else if (underlyingType == long.class || underlyingType == Long.class) {
            // 处理long转换
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                return 0L;
            }
        } else if (underlyingType == Date.class) {
            // 处理日期时间转换
            try {
                return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(value);
            } catch (Exception e) {
                return new Date();
            }
        } else if (underlyingType.isPrimitive()) {
            // 处理其他基本类型
            try {
                if (underlyingType == int.class || underlyingType == Integer.class) {
                    return Integer.parseInt(value);
                } else if (underlyingType == double.class || underlyingType == Double.class) {
                    return Double.parseDouble(value);
                } else if (underlyingType == float.class || underlyingType == Float.class) {
                    return Float.parseFloat(value);
                } else if (underlyingType == short.class || underlyingType == Short.class) {
                    return Short.parseShort(value);
                } else if (underlyingType == byte.class || underlyingType == Byte.class) {
                    return Byte.parseByte(value);
                }
            } catch (NumberFormatException e) {
                // 返回默认值
                if (underlyingType == int.class || underlyingType == Integer.class) return 0;
                if (underlyingType == double.class || underlyingType == Double.class) return 0.0;
                if (underlyingType == float.class || underlyingType == Float.class) return 0.0f;
                if (underlyingType == short.class || underlyingType == Short.class) return (short) 0;
                if (underlyingType == byte.class || underlyingType == Byte.class) return (byte) 0;
            }
        }
        
        // 其他类型默认返回字符串
        return value;
    }
    
    /**
     * 判断是否为复杂类型
     * 
     * @param type 类型
     * @return 是否为复杂类型
     */
    private static boolean isComplexType(Class<?> type) {
        // 基本类型和字符串不是复杂类型
        if (type.isPrimitive() || type == String.class || type == Date.class)
            return false;
            
        // 枚举类型不是复杂类型
        if (type.isEnum())
            return false;
            
        // 其他类型视为复杂类型
        return true;
    }
    
    /**
     * 判断是否为可空类型 (Java中通过其他方式处理)
     * 
     * @param type 类型
     * @return 是否为可空类型
     */
    private static boolean isNullableType(Class<?> type) {
        // 在Java中，所有引用类型都是可空的，基本类型不可空
        return !type.isPrimitive();
    }
    
    //#endregion
}
