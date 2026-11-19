package com.tonghui.erp.Data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tonghui.erp.Data.Entity.StockInLabel;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 入库明细逐条标签数据访问
 */
public interface StockInLabelMapper extends BaseMapper<StockInLabel> {

    /** 按入库单查询标签（含明细与单据显示字段） */
    @Select("SELECT l.*, si.in_code AS inCode, si.in_status AS inStatus, si.prod_unit_id AS prodUnitId,"
            + " sd.item_code AS materialCode, sd.item_name AS materialName, sd.batch_number AS batchNumber,"
            + " sd.quantity AS quantity, sd.unit_name AS unitName"
            + " FROM stock_in_label l"
            + " JOIN stock_in si ON si.in_id = l.in_id"
            + " JOIN stock_in_detail sd ON sd.in_detail_id = l.in_detail_id"
            + " WHERE l.is_deleted = 0 AND l.in_id = #{inId}"
            + " ORDER BY l.in_detail_id")
    List<StockInLabel> selectByInIdWithDetail(@Param("inId") Long inId);

    /** 查询全部标签（含明细与单据显示字段） */
    @Select("SELECT l.*, si.in_code AS inCode, si.in_status AS inStatus, si.prod_unit_id AS prodUnitId,"
            + " sd.item_code AS materialCode, sd.item_name AS materialName, sd.batch_number AS batchNumber,"
            + " sd.quantity AS quantity, sd.unit_name AS unitName"
            + " FROM stock_in_label l"
            + " JOIN stock_in si ON si.in_id = l.in_id"
            + " JOIN stock_in_detail sd ON sd.in_detail_id = l.in_detail_id"
            + " WHERE l.is_deleted = 0"
            + " ORDER BY l.id DESC")
    List<StockInLabel> selectAllWithDetail();

    /** 按入库明细查询标签 */
    @Select("SELECT l.*, si.in_code AS inCode, si.in_status AS inStatus, si.prod_unit_id AS prodUnitId,"
            + " sd.item_code AS materialCode, sd.item_name AS materialName, sd.batch_number AS batchNumber,"
            + " sd.quantity AS quantity, sd.unit_name AS unitName"
            + " FROM stock_in_label l"
            + " JOIN stock_in si ON si.in_id = l.in_id"
            + " JOIN stock_in_detail sd ON sd.in_detail_id = l.in_detail_id"
            + " WHERE l.is_deleted = 0 AND l.in_detail_id = #{inDetailId} LIMIT 1")
    StockInLabel selectByInDetailIdWithDetail(@Param("inDetailId") Long inDetailId);

    /** 按标签码查询（含显示字段） */
    @Select("SELECT l.*, si.in_code AS inCode, si.in_status AS inStatus, si.prod_unit_id AS prodUnitId,"
            + " sd.item_code AS materialCode, sd.item_name AS materialName, sd.batch_number AS batchNumber,"
            + " sd.quantity AS quantity, sd.unit_name AS unitName"
            + " FROM stock_in_label l"
            + " JOIN stock_in si ON si.in_id = l.in_id"
            + " JOIN stock_in_detail sd ON sd.in_detail_id = l.in_detail_id"
            + " WHERE l.is_deleted = 0 AND l.label_code = #{code} LIMIT 1")
    StockInLabel selectByCodeWithDetail(@Param("code") String code);
}
