package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import lombok.Data;

/**
 * 出库单号序列表
 * @TableName stock_out_sequence
 */
@TableName(value ="stock_out_sequence")
@Data
public class StockOutSequence {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 日期
     */
    @TableId(value = "seq_date")
    private LocalDate seqDate;

    // endregion

    // region 业务字段
    // ===================================
    // 业务字段
    // ===================================

    /**
     * 序列号
     */
    @TableField(value = "seq_number")
    private Integer seqNumber;

    // endregion
}