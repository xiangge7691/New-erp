package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 计量单位表
 * @TableName unit
 */
@TableName(value ="unit")
@Data
public class Unit {
    /**
     * 单位唯一标识
     */
    @TableId(value = "unit_id", type = IdType.AUTO)
    private Long unitId;

    /**
     * 单位中文名称（法定全称）
     */
    @TableField(value = "unit_name")
    private String unitName;

    /**
     * 单位符号（区分大小写）
     */
    @TableField(value = "symbol")
    private String symbol;
}