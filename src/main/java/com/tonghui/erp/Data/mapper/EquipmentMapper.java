package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Data.Entity.Equipment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 设备数据访问Mapper接口
 */
public interface EquipmentMapper extends BaseMapper<Equipment> {

    /**
     * 根据固定资产编号物理删除已软删除的记录（释放唯一键约束）
     *
     * @param fixedAssetCode 固定资产编号
     * @return 删除的记录数
     */
    @Delete("DELETE FROM equipment WHERE fixed_asset_code = #{fixedAssetCode} AND is_deleted = 1")
    int physicalDeleteByFixedAssetCode(@Param("fixedAssetCode") String fixedAssetCode);
}




