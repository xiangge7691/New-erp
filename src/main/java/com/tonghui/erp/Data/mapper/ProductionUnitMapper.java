package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Data.Entity.ProductionUnit;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 生产单元数据访问Mapper接口
 */
public interface ProductionUnitMapper extends BaseMapper<ProductionUnit> {

    /**
     * 根据生产单位编码物理删除已软删除的记录（释放唯一键约束）
     *
     * @param prodUnitCode 生产单位编码
     * @return 删除的记录数
     */
    @Delete("DELETE FROM production_unit WHERE prod_unit_code = #{prodUnitCode} AND is_deleted = 1")
    int physicalDeleteByProdUnitCode(@Param("prodUnitCode") String prodUnitCode);
}




