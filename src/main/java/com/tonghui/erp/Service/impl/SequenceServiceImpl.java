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

    // region 质量检验模块编号生成
    // ===================================
    // 质量检验模块编号生成
    // ===================================

    /**
     * 生成检验计划编号
     * <p>
     * 编号格式：JH + 日期(8位) + 序号(3位)，例如：JH-20260723-001
     * 通过查询当天检验计划表中最大编号并递增生成，保证每天编号唯一
     * </p>
     *
     * @return 生成的唯一计划编号
     */
    public String generateInspectionPlanCode() {
        return generateMaxCodeByPrefix("JH");
    }

    /**
     * 生成取样编号
     * <p>
     * 编号格式：QY + 日期(8位) + 序号(3位)，例如：QY-20260723-001
     * 通过查询当天取样记录表中最大编号并递增生成，保证每天编号唯一
     * </p>
     *
     * @return 生成的唯一取样编号
     */
    public String generateSamplingCode() {
        return generateMaxCodeByPrefix("QY");
    }

    /**
     * 生成检验编号
     * <p>
     * 编号格式：JY + 日期(8位) + 序号(3位)，例如：JY-20260723-001
     * 通过查询当天检验记录表中最大编号并递增生成，保证每天编号唯一
     * </p>
     *
     * @return 生成的唯一检验编号
     */
    public String generateInspectionRecordCode() {
        return generateMaxCodeByPrefix("JY");
    }

    /**
     * 生成放行编号
     * <p>
     * 编号格式：FX + 日期(8位) + 序号(3位)，例如：FX-20260723-001
     * 通过查询当天审核放行表中最大编号并递增生成，保证每天编号唯一
     * </p>
     *
     * @return 生成的唯一放行编号
     */
    public String generateReleaseCode() {
        return generateMaxCodeByPrefix("FX");
    }

    /**
     * 生成留样编号
     * <p>
     * 编号格式：LY + 日期(8位) + 序号(3位)，例如：LY-20260723-001
     * 通过查询当天留样记录表中最大编号并递增生成，保证每天编号唯一
     * </p>
     *
     * @return 生成的唯一留样编号
     */
    public String generateRetainedSampleCode() {
        return generateMaxCodeByPrefix("LY");
    }

    /**
     * 通用编号生成器（绕过软删除过滤查询当天最大编号并递增）
     * <p>
     * 编号格式：{前缀}-{yyyyMMdd}-{3位序号}，通过原生SQL MAX查询当天的最大编号，
     * 不受 MyBatis-Plus 全局软删除过滤影响，避免已删除记录占用的编号被复用
     * </p>
     *
     * @param prefix 编号前缀，如 JH/QY/JY/FX/LY
     * @return 生成的唯一编号
     */
    private String generateMaxCodeByPrefix(String prefix) {
        // 日期部分，格式为yyyyMMdd
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 按前缀动态查询对应编号来源：前缀决定了表名与编号列名
        String maxCode = null;
        try {
            switch (prefix) {
                case "JH":
                    maxCode = jdbcTemplate.queryForObject(
                            "SELECT MAX(plan_code) FROM inspection_plan WHERE plan_code LIKE 'JH-" + dateStr + "%'",
                            String.class);
                    break;
                case "QY":
                    maxCode = jdbcTemplate.queryForObject(
                            "SELECT MAX(sampling_code) FROM sampling_record WHERE sampling_code LIKE 'QY-" + dateStr + "%'",
                            String.class);
                    break;
                case "JY":
                    maxCode = jdbcTemplate.queryForObject(
                            "SELECT MAX(inspection_code) FROM inspection_record WHERE inspection_code LIKE 'JY-" + dateStr + "%'",
                            String.class);
                    break;
                case "FX":
                    maxCode = jdbcTemplate.queryForObject(
                            "SELECT MAX(release_code) FROM release_review WHERE release_code LIKE 'FX-" + dateStr + "%'",
                            String.class);
                    break;
                case "LY":
                    maxCode = jdbcTemplate.queryForObject(
                            "SELECT MAX(retained_code) FROM retained_sample WHERE retained_code LIKE 'LY-" + dateStr + "%'",
                            String.class);
                    break;
                default:
                    return String.format("%s-%s-%03d", prefix, dateStr, 1);
            }
            if (maxCode != null) {
                // 编号格式为 前缀(2位) + "-"(1位) + 日期(8位) + "-"(1位) = 前12位，从第12位开始是序号
                String seqStr = maxCode.substring(12);
                int seq = Integer.parseInt(seqStr);
                return String.format("%s-%s-%03d", prefix, dateStr, seq + 1);
            } else {
                // 当天无记录，从1开始
                return String.format("%s-%s-%03d", prefix, dateStr, 1);
            }
        } catch (Exception e) {
            // 出现异常时返回默认值
            return String.format("%s-%s-%03d", prefix, dateStr, 1);
        }
    }

    // endregion
}
