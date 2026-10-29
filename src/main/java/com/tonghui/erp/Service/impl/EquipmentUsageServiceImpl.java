package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.Equipment.EquipmentUsageQueryDto;
import com.tonghui.erp.Common.Dto.Equipment.EquipmentUsageRecordDto;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.mapper.EquipmentUsageMapper;
import com.tonghui.erp.Service.EquipmentUsageService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 设备使用记录服务实现类
 * <p>
 * 设备使用记录不单独建表，统一来源于工单工序执行记录（work_order_process_execution），
 * 通过关联设备、工单、工序类型、房间形成使用台账，支持分页查询与按设备导出 Excel。
 * </p>
 */
@Service
public class EquipmentUsageServiceImpl implements EquipmentUsageService {

    // region 常量定义
    // ===================================
    // 常量定义
    // ===================================

    /**
     * 导出最大行数（防止一次性导出过多数据导致内存溢出）
     */
    private static final int MAX_EXPORT_ROWS = 100000;

    /**
     * Excel sheet 名称最大长度
     */
    private static final int MAX_SHEET_NAME_LENGTH = 31;

    /**
     * 时间展示格式
     */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * 明细 sheet 表头
     */
    private static final String[] DETAIL_HEADERS = {
            "序号", "设备名称", "固定资产编号", "设备型号", "设备类型", "所在房间",
            "工单编号", "生产计划", "制剂名称", "批号", "工序名称", "工序顺序",
            "操作人", "开始时间", "结束时间", "使用时长(分钟)", "执行状态", "备注"
    };

    /**
     * 汇总 sheet 表头
     */
    private static final String[] SUMMARY_HEADERS = {
            "设备名称", "固定资产编号", "设备型号", "设备类型", "所在房间",
            "使用次数", "累计使用时长(分钟)", "首次使用时间", "最近使用时间"
    };

    /**
     * 明细 sheet 列宽（字符数）
     */
    private static final int DETAIL_COLUMN_WIDTH = 16;

    // endregion

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    /**
     * 设备使用记录数据访问Mapper
     */
    @Autowired
    private EquipmentUsageMapper equipmentUsageMapper;

    // endregion

    // region 查询实现
    // ===================================
    // 查询实现
    // ===================================

    /**
     * 分页查询设备使用记录
     *
     * @param equipmentId   设备ID（可选）
     * @param equipmentType 设备类型（可选）
     * @param keyword       关键字（可选）
     * @param status        执行状态（可选）
     * @param startDate     使用开始日期（可选，格式 yyyy-MM-dd）
     * @param endDate       使用结束日期（可选，格式 yyyy-MM-dd）
     * @param pageRequest   分页参数
     * @return 分页结果
     */
    @Override
    public PagedResult<EquipmentUsageRecordDto> search(Integer equipmentId, String equipmentType, String keyword,
                                                       String status, String startDate, String endDate,
                                                       PageRequestDto pageRequest) {
        EquipmentUsageQueryDto query = buildQuery(equipmentId, equipmentType, keyword, status, startDate, endDate);
        Page<EquipmentUsageRecordDto> page = PagedResult.toMybatisPage(pageRequest);
        Page<EquipmentUsageRecordDto> resultPage = equipmentUsageMapper.selectUsagePage(page, query);
        return PagedResult.fromPage(resultPage, pageRequest);
    }

    // endregion

    // region 导出实现
    // ===================================
    // 导出实现
    // ===================================

    /**
     * 导出设备使用记录 Excel
     * <p>
     * 指定设备ID时导出单个设备（单 sheet）；未指定时按设备分 sheet 导出，并在首个 sheet 输出汇总。
     * </p>
     *
     * @param equipmentId   设备ID（可选）
     * @param equipmentType 设备类型（可选）
     * @param keyword       关键字（可选）
     * @param status        执行状态（可选）
     * @param startDate     使用开始日期（可选，格式 yyyy-MM-dd）
     * @param endDate       使用结束日期（可选，格式 yyyy-MM-dd）
     * @param out           输出流
     * @throws IOException              写流失败
     * @throws IllegalArgumentException 参数非法或导出行数超限
     */
    @Override
    public void exportToExcel(Integer equipmentId, String equipmentType, String keyword, String status,
                              String startDate, String endDate, OutputStream out) throws IOException {
        EquipmentUsageQueryDto query = buildQuery(equipmentId, equipmentType, keyword, status, startDate, endDate);

        // 多取一行用于判断是否超限
        List<EquipmentUsageRecordDto> rows = equipmentUsageMapper.selectUsageList(query, MAX_EXPORT_ROWS + 1);
        if (rows.size() > MAX_EXPORT_ROWS) {
            throw new IllegalArgumentException("导出数据超过 " + MAX_EXPORT_ROWS
                    + " 行，请缩小时间范围，或按设备逐个导出");
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = buildHeaderStyle(workbook);

            if (query.getEquipmentId() != null) {
                // 按设备导出：单个 sheet
                String sheetName = buildSheetName(rows.isEmpty() ? null : rows.get(0),
                        query.getEquipmentId(), new HashSet<>());
                writeDetailSheet(workbook, sheetName, rows, headerStyle);
            } else {
                // 导出全部设备：汇总 sheet + 每台设备一个 sheet
                Map<Integer, List<EquipmentUsageRecordDto>> grouped = groupByEquipment(rows);
                writeSummarySheet(workbook, grouped, headerStyle);
                Set<String> usedNames = new HashSet<>();
                usedNames.add("汇总");
                for (Map.Entry<Integer, List<EquipmentUsageRecordDto>> entry : grouped.entrySet()) {
                    String sheetName = buildSheetName(entry.getValue().get(0), entry.getKey(), usedNames);
                    writeDetailSheet(workbook, sheetName, entry.getValue(), headerStyle);
                }
            }

            workbook.write(out);
        }
    }

    // endregion

    // region Excel 写入方法
    // ===================================
    // Excel 写入方法
    // ===================================

    /**
     * 写入单台设备的明细 sheet
     *
     * @param workbook    工作簿
     * @param sheetName   sheet名称
     * @param rows        使用记录
     * @param headerStyle 表头样式
     */
    private void writeDetailSheet(Workbook workbook, String sheetName,
                                  List<EquipmentUsageRecordDto> rows, CellStyle headerStyle) {
        Sheet sheet = workbook.createSheet(sheetName);
        writeHeader(sheet, DETAIL_HEADERS, headerStyle);

        int rowNum = 1;
        int index = 1;
        for (EquipmentUsageRecordDto record : rows) {
            Row row = sheet.createRow(rowNum++);
            int col = 0;
            row.createCell(col++).setCellValue(index++);
            row.createCell(col++).setCellValue(nvl(record.getEquipmentName()));
            row.createCell(col++).setCellValue(nvl(record.getFixedAssetCode()));
            row.createCell(col++).setCellValue(nvl(record.getEquipmentModel()));
            row.createCell(col++).setCellValue(nvl(record.getEquipmentType()));
            row.createCell(col++).setCellValue(nvl(record.getRoomName()));
            row.createCell(col++).setCellValue(nvl(record.getWorkOrderCode()));
            row.createCell(col++).setCellValue(nvl(record.getPlanName()));
            row.createCell(col++).setCellValue(nvl(record.getPreparationName()));
            row.createCell(col++).setCellValue(nvl(record.getBatchNumber()));
            row.createCell(col++).setCellValue(nvl(record.getProcessName()));
            setNumberCell(row.createCell(col++), record.getStepOrder());
            row.createCell(col++).setCellValue(nvl(record.getOperatorName()));
            row.createCell(col++).setCellValue(formatTime(record.getStartTime()));
            row.createCell(col++).setCellValue(formatTime(record.getEndTime()));
            setNumberCell(row.createCell(col++), record.getDurationMinutes());
            row.createCell(col++).setCellValue(nvl(record.getStatus()));
            row.createCell(col++).setCellValue(nvl(record.getRemark()));
        }

        finishSheet(sheet, DETAIL_HEADERS.length, rows.size());
    }

    /**
     * 写入设备使用汇总 sheet
     *
     * @param workbook    工作簿
     * @param grouped     按设备分组的使用记录
     * @param headerStyle 表头样式
     */
    private void writeSummarySheet(Workbook workbook,
                                   Map<Integer, List<EquipmentUsageRecordDto>> grouped,
                                   CellStyle headerStyle) {
        Sheet sheet = workbook.createSheet("汇总");
        writeHeader(sheet, SUMMARY_HEADERS, headerStyle);

        int rowNum = 1;
        for (List<EquipmentUsageRecordDto> records : grouped.values()) {
            EquipmentUsageRecordDto sample = records.get(0);
            long totalMinutes = records.stream()
                    .map(EquipmentUsageRecordDto::getDurationMinutes)
                    .filter(java.util.Objects::nonNull)
                    .mapToLong(Long::longValue)
                    .sum();
            LocalDateTime firstStart = records.stream()
                    .map(EquipmentUsageRecordDto::getStartTime)
                    .filter(java.util.Objects::nonNull)
                    .min(LocalDateTime::compareTo)
                    .orElse(null);
            LocalDateTime lastEnd = records.stream()
                    .map(EquipmentUsageRecordDto::getEndTime)
                    .filter(java.util.Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);
            String roomName = records.stream()
                    .map(EquipmentUsageRecordDto::getRoomName)
                    .filter(StringUtils::hasText)
                    .findFirst()
                    .orElse("");

            Row row = sheet.createRow(rowNum++);
            int col = 0;
            row.createCell(col++).setCellValue(nvl(sample.getEquipmentName()));
            row.createCell(col++).setCellValue(nvl(sample.getFixedAssetCode()));
            row.createCell(col++).setCellValue(nvl(sample.getEquipmentModel()));
            row.createCell(col++).setCellValue(nvl(sample.getEquipmentType()));
            row.createCell(col++).setCellValue(roomName);
            row.createCell(col++).setCellValue(records.size());
            row.createCell(col++).setCellValue(totalMinutes);
            row.createCell(col++).setCellValue(formatTime(firstStart));
            row.createCell(col++).setCellValue(formatTime(lastEnd));
        }

        finishSheet(sheet, SUMMARY_HEADERS.length, grouped.size());
    }

    /**
     * 写表头并设置列宽
     *
     * @param sheet       sheet
     * @param headers     表头
     * @param headerStyle 表头样式
     */
    private void writeHeader(Sheet sheet, String[] headers, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, DETAIL_COLUMN_WIDTH * 256);
        }
    }

    /**
     * 冻结首行并启用自动筛选
     *
     * @param sheet       sheet
     * @param columnCount 列数
     * @param dataRows    数据行数
     */
    private void finishSheet(Sheet sheet, int columnCount, int dataRows) {
        sheet.createFreezePane(0, 1);
        if (dataRows > 0) {
            sheet.setAutoFilter(new CellRangeAddress(0, dataRows, 0, columnCount - 1));
        }
    }

    /**
     * 构建表头样式（灰底加粗居中）
     *
     * @param workbook 工作簿
     * @return 表头样式
     */
    private CellStyle buildHeaderStyle(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        return headerStyle;
    }

    // endregion

    // region 私有辅助方法
    // ===================================
    // 私有辅助方法
    // ===================================

    /**
     * 构建查询条件（解析日期参数）
     *
     * @param equipmentId   设备ID（可选）
     * @param equipmentType 设备类型（可选）
     * @param keyword       关键字（可选）
     * @param status        执行状态（可选）
     * @param startDate     使用开始日期（可选，格式 yyyy-MM-dd）
     * @param endDate       使用结束日期（可选，格式 yyyy-MM-dd）
     * @return 查询条件
     * @throws IllegalArgumentException 日期格式非法或开始日期晚于结束日期
     */
    private EquipmentUsageQueryDto buildQuery(Integer equipmentId, String equipmentType, String keyword,
                                              String status, String startDate, String endDate) {
        LocalDate start = parseDate(startDate, "开始日期");
        LocalDate end = parseDate(endDate, "结束日期");
        if (start != null && end != null && start.isAfter(end)) {
            throw new IllegalArgumentException("开始日期不能晚于结束日期");
        }

        EquipmentUsageQueryDto query = new EquipmentUsageQueryDto();
        query.setEquipmentId(equipmentId);
        query.setEquipmentType(trimToNull(equipmentType));
        query.setKeyword(trimToNull(keyword));
        query.setStatus(trimToNull(status));
        if (start != null) {
            query.setStartTime(start.atStartOfDay());
        }
        if (end != null) {
            // 结束日期包含当天，使用次日 0 点作为开区间上界
            query.setEndTime(end.plusDays(1).atStartOfDay());
        }
        return query;
    }

    /**
     * 按设备分组（保持查询结果中设备名称的排序）
     *
     * @param rows 使用记录
     * @return 设备ID -> 使用记录列表
     */
    private Map<Integer, List<EquipmentUsageRecordDto>> groupByEquipment(List<EquipmentUsageRecordDto> rows) {
        Map<Integer, List<EquipmentUsageRecordDto>> grouped = new LinkedHashMap<>();
        for (EquipmentUsageRecordDto row : rows) {
            grouped.computeIfAbsent(row.getEquipmentId(), key -> new ArrayList<>()).add(row);
        }
        return grouped;
    }

    /**
     * 构建合法且不重复的 sheet 名称
     *
     * @param sample      设备使用记录（用于取设备名称/编号，可为空）
     * @param equipmentId 设备ID
     * @param usedNames   已使用的 sheet 名称（小写比较）
     * @return sheet 名称
     */
    private String buildSheetName(EquipmentUsageRecordDto sample, Integer equipmentId, Set<String> usedNames) {
        StringBuilder base = new StringBuilder();
        if (sample != null && StringUtils.hasText(sample.getEquipmentName())) {
            base.append(sample.getEquipmentName().trim());
        } else {
            base.append("设备").append(equipmentId);
        }
        if (sample != null && StringUtils.hasText(sample.getFixedAssetCode())) {
            base.append("-").append(sample.getFixedAssetCode().trim());
        }
        // 去除 Excel sheet 名称非法字符
        String cleaned = base.toString().replaceAll("[\\\\/*?:\\[\\]]", "").trim();
        if (cleaned.isEmpty()) {
            cleaned = "设备" + equipmentId;
        }
        if (cleaned.length() > MAX_SHEET_NAME_LENGTH) {
            cleaned = cleaned.substring(0, MAX_SHEET_NAME_LENGTH);
        }

        String candidate = cleaned;
        int suffix = 1;
        while (!usedNames.add(candidate.toLowerCase(Locale.ROOT))) {
            String suffixText = "~" + suffix++;
            int keep = MAX_SHEET_NAME_LENGTH - suffixText.length();
            candidate = (cleaned.length() > keep ? cleaned.substring(0, keep) : cleaned) + suffixText;
        }
        return candidate;
    }

    /**
     * 解析日期参数
     *
     * @param value     日期文本（yyyy-MM-dd）
     * @param fieldName 字段名称（用于错误提示）
     * @return 解析结果，空值返回 null
     * @throws IllegalArgumentException 格式非法
     */
    private LocalDate parseDate(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(fieldName + "格式错误，应为 yyyy-MM-dd");
        }
    }

    /**
     * 去除空白并转 null
     *
     * @param value 原值
     * @return 去空白后的值，空值返回 null
     */
    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    /**
     * 空值安全的文本输出
     *
     * @param value 原值
     * @return 原值，空值返回空串
     */
    private String nvl(String value) {
        return value == null ? "" : value;
    }

    /**
     * 格式化时间
     *
     * @param time 时间
     * @return yyyy-MM-dd HH:mm，空值返回空串
     */
    private String formatTime(LocalDateTime time) {
        return time == null ? "" : TIME_FORMATTER.format(time);
    }

    /**
     * 数字单元格赋值（空值留空）
     *
     * @param cell  单元格
     * @param value 数值
     */
    private void setNumberCell(Cell cell, Number value) {
        if (value == null) {
            cell.setCellValue("");
        } else {
            cell.setCellValue(value.doubleValue());
        }
    }

    // endregion
}
