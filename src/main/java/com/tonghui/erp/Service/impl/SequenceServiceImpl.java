package com.tonghui.erp.Service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.stereotype.Service;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class SequenceServiceImpl {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public String generateStockInCode() {
        // 如果存储过程调用失败，使用Java代码生成单号
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 查询当天最大的入库单号并加1
        try {
            String maxCode = jdbcTemplate.queryForObject(
                    "SELECT MAX(in_code) FROM stock_in WHERE in_code LIKE 'IN" + dateStr + "%'",
                    String.class);

            if (maxCode != null) {
                // 提取序号部分并加1
                String seqStr = maxCode.substring(10); // 假设格式为IN+日期(8位)+序号(4位)
                int seq = Integer.parseInt(seqStr);
                return String.format("IN%s%04d", dateStr, seq + 1);
            } else {
                return String.format("IN%s%04d", dateStr, 1);
            }
        } catch (Exception e) {
            // 出现异常时返回默认值
            return String.format("IN%s%04d", dateStr, 1);
        }
    }

    public String generateStockOutCode() {
        // 如果存储过程调用失败，使用Java代码生成单号
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 查询当天最大的出库单号并加1
        try {
            String maxCode = jdbcTemplate.queryForObject(
                    "SELECT MAX(out_code) FROM stock_out WHERE out_code LIKE 'OUT" + dateStr + "%'",
                    String.class);

            if (maxCode != null) {
                // 提取序号部分并加1（OUT是3个字符，日期是8个字符，所以从第11位开始是序号）
                String seqStr = maxCode.substring(11); // OUT(3) + 日期(8) = 11位后是序号
                int seq = Integer.parseInt(seqStr);
                return String.format("OUT%s%04d", dateStr, seq + 1);
            } else {
                return String.format("OUT%s%04d", dateStr, 1);
            }
        } catch (Exception e) {
            // 出现异常时返回默认值
            return String.format("OUT%s%04d", dateStr, 1);
        }
    }
}
