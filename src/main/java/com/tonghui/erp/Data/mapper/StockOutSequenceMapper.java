package com.tonghui.erp.Data.mapper;

import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.mapping.StatementType;

/**
 * 出库单号生成Mapper
 */
public interface StockOutSequenceMapper {

    /**
     * 调用存储过程生成出库单号
     * @param outCode 出库单号输出参数
     */
    @Select("{ CALL GenerateStockOutCode(#{outCode, mode=OUT, jdbcType=VARCHAR}) }")
    @Options(statementType = StatementType.CALLABLE)
    void generateStockOutCode(@Param("outCode") StringBuilder outCode);
}


