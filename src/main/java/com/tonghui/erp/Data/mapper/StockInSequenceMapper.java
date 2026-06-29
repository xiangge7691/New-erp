package com.tonghui.erp.Data.mapper;

import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.mapping.StatementType;

/**
 * 入库单号生成Mapper
 */
public interface StockInSequenceMapper {

    /**
     * 调用存储过程生成入库单号
     * @param inCode 入库单号输出参数
     */
    @Select("{ CALL GenerateStockInCode(#{inCode, mode=OUT, jdbcType=VARCHAR}) }")
    @Options(statementType = StatementType.CALLABLE)
    void generateStockInCode(@Param("inCode") StringBuilder inCode);
}
