package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Data.Entity.Equipment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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

    /**
     * 按设备ID加行锁并查询设备（含已软删除记录）
     * <p>
     * 用于工序执行记录保存前串行化同一设备的使用登记：先按 equipment_id 升序加锁，
     * 再校验状态与时间冲突，避免并发提交绕过校验。
     * 注意：此处刻意不过滤 is_deleted，否则已软删除的行不会被锁定，由调用方判断是否已删除。
     * </p>
     *
     * @param equipmentId 设备ID
     * @return 设备信息（不存在时返回 null）
     */
    @Select("SELECT equipment_id, equipment_name, fixed_asset_code, equipment_status, is_deleted " +
            "FROM equipment WHERE equipment_id = #{equipmentId} FOR UPDATE")
    Equipment selectByIdForUpdate(@Param("equipmentId") Integer equipmentId);
}




