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
    /**
     * 日期
     */
    @TableId(value = "seq_date")
    private LocalDate seqDate;

    /**
     * 序列号
     */
    @TableField(value = "seq_number")
    private Integer seqNumber;
}