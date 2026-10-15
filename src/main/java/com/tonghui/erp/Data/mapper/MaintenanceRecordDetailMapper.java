package com.tonghui.erp.Data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tonghui.erp.Data.Entity.MaintenanceRecordDetail;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 库存检查明细数据访问Mapper接口
 * <p>
 * 提供库存检查明细的基础CRUD及更新重建所需的物理删除方法
 * </p>
 */
public interface MaintenanceRecordDetailMapper extends BaseMapper<MaintenanceRecordDetail> {

    /**
     * 按养护记录ID物理删除全部明细行
     * <p>
     * 更新记录时明细整体重建：先物理删除旧行再整单重插。
     * 必须使用原生DELETE（非MP的delete(wrapper)），因全局软删除开启后
     * MP的delete会被改写为UPDATE is_deleted=1，导致重复插入时数据累积
     * </p>
     *
     * @param maintenanceId 养护记录主表ID
     * @return 删除的行数
     */
    @Delete("DELETE FROM maintenance_record_detail WHERE maintenance_id = #{maintenanceId}")
    int physicalDeleteByMaintenanceId(@Param("maintenanceId") Long maintenanceId);
}
