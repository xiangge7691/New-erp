package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Data.Entity.ProductionUnit;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 生产单元数据访问Mapper接口
 */
public interface ProductionUnitMapper extends BaseMapper<ProductionUnit> {

    /**
     * 根据生产单位编码查询已软删除的记录ID
     *
     * @param prodUnitCode 生产单位编码
     * @return 记录ID
     */
    @Select("SELECT prod_unit_id FROM production_unit WHERE prod_unit_code = #{prodUnitCode} AND is_deleted = 1")
    Long selectDeletedIdByCode(@Param("prodUnitCode") String prodUnitCode);

    /**
     * 根据生产单位ID物理删除已软删除的记录（释放唯一键约束）
     *
     * @param prodUnitId 生产单位ID
     * @return 删除的记录数
     */
    @Delete("DELETE FROM production_unit WHERE prod_unit_id = #{prodUnitId} AND is_deleted = 1")
    int physicalDeleteByProdUnitId(@Param("prodUnitId") Long prodUnitId);
}




