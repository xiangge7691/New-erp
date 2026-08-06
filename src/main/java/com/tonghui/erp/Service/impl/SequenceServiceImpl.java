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

/**
 * 序列号生成服务实现类
 * <p>
 * 提供各类业务单号的自动生成能力，通过数据库查询获取当天最大编号并自增，
 * 保证编号的唯一性和连续性
 * </p>
 *
 */
@Service
public class SequenceServiceImpl {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /** JDBC模板，用于直接执行SQL查询获取最大编号 */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // endregion

    // region 入库单号生成
    // ===================================
    // 入库单号生成
    // ===================================

    /**
     * 生成入库单号
     * <p>
     * 编号格式：IN + 年月日(8位) + 序号(4位)，例如：IN202512010001
     * 通过查询当天入库单表中最大编号并递增生成，保证每天编号唯一
     * </p>
     *
     * @return 生成的唯一入库单号
     */
    public String generateStockInCode() {
        // 日期部分，格式为yyyyMMdd
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 查询当天最大的入库单号并加1
        try {
            String maxCode = jdbcTemplate.queryForObject(
                    "SELECT MAX(in_code) FROM stock_in WHERE in_code LIKE 'IN" + dateStr + "%'",
                    String.class);

            if (maxCode != null) {
                // 提取序号部分并加1
                // 格式为IN(2) + 日期(8位) = 前10位，从第10位开始是序号
                String seqStr = maxCode.substring(10);
                int seq = Integer.parseInt(seqStr);
                return String.format("IN%s%04d", dateStr, seq + 1);
            } else {
                // 当天无记录，从1开始
                return String.format("IN%s%04d", dateStr, 1);
            }
        } catch (Exception e) {
            // 出现异常时返回默认值
            return String.format("IN%s%04d", dateStr, 1);
        }
    }

    // endregion

    // region 出库单号生成
    // ===================================
    // 出库单号生成
    // ===================================

    /**
     * 生成出库单号
     * <p>
     * 编号格式：OUT + 年月日(8位) + 序号(4位)，例如：OUT202512010001
     * 通过查询当天出库单表中最大编号并递增生成，保证每天编号唯一
     * </p>
     *
     * @return 生成的唯一出库单号
     */
    public String generateStockOutCode() {
        // 日期部分，格式为yyyyMMdd
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 查询当天最大的出库单号并加1
        try {
            String maxCode = jdbcTemplate.queryForObject(
                    "SELECT MAX(out_code) FROM stock_out WHERE out_code LIKE 'OUT" + dateStr + "%'",
                    String.class);

            if (maxCode != null) {
                // 提取序号部分并加1
                // 格式为OUT(3) + 日期(8位) = 前11位，从第11位开始是序号
                String seqStr = maxCode.substring(11);
                int seq = Integer.parseInt(seqStr);
                return String.format("OUT%s%04d", dateStr, seq + 1);
            } else {
                // 当天无记录，从1开始
                return String.format("OUT%s%04d", dateStr, 1);
            }
        } catch (Exception e) {
            // 出现异常时返回默认值
            return String.format("OUT%s%04d", dateStr, 1);
        }
    }

    // endregion

    // region 验收单号生成
    // ===================================
    // 验收单号生成
    // ===================================

    /**
     * 生成货物验收单号
     * <p>
     * 编号格式：YS + 年月日(8位) + 序号(3位)，例如：YS-20260723-001
     * 通过查询当天验收单表中最大编号并递增生成，保证每天编号唯一
     * </p>
     *
     * @return 生成的唯一验收单号
     */
    public String generateAcceptanceCode() {
        // 日期部分，格式为yyyyMMdd
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 查询当天最大的验收单号并加1
        try {
            String maxCode = jdbcTemplate.queryForObject(
                    "SELECT MAX(acceptance_code) FROM acceptance_order WHERE acceptance_code LIKE 'YS-" + dateStr + "%'",
                    String.class);

            if (maxCode != null) {
                // 提取序号部分并加1，格式为YS-(3) + 日期(8位) = 前12位，从第12位开始是序号
                String seqStr = maxCode.substring(12);
                int seq = Integer.parseInt(seqStr);
                return String.format("YS-%s-%03d", dateStr, seq + 1);
            } else {
                // 当天无记录，从1开始
                return String.format("YS-%s-%03d", dateStr, 1);
            }
        } catch (Exception e) {
            // 出现异常时返回默认值
            return String.format("YS-%s-%03d", dateStr, 1);
        }
    }

    // endregion
}
