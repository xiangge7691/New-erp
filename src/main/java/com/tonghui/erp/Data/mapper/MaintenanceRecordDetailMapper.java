package com.tonghui.erp.Data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tonghui.erp.Data.Entity.MaintenanceRecordDetail;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 库存检查明细数据访问Mapper接口
 * <p>
 * 提供库存检查明细的基础CRUD及更新/删除所需的物理删除方法
 * </p>
 */
public interface MaintenanceRecordDetailMapper extends BaseMapper<MaintenanceRecordDetail> {

    /**
     * 按养护记录ID物理删除全部明细行
     * <p>
     * 删除整张养护记录时使用：明细必须物理删除，残留明细会继续参与待养护提醒计算。
     * 必须使用原生DELETE（非MP的delete(wrapper)），因全局软删除开启后
     * MP的delete会被改写为UPDATE is_deleted=1，导致残留数据累积
     * </p>
     *
     * @param maintenanceId 养护记录主表ID
     * @return 删除的行数
     */
    @Delete("DELETE FROM maintenance_record_detail WHERE maintenance_id = #{maintenanceId}")
    int physicalDeleteByMaintenanceId(@Param("maintenanceId") Long maintenanceId);

    /**
     * 按明细ID集合物理删除（更新时删除被移除的行）
     * <p>
     * 绕过全局软删除配置直接物理删除，保留未被移除行的ID（明细附件可按ID继续定位）
     * </p>
     *
     * @param ids 明细ID集合
     * @return 删除的行数
     */
    @Delete("<script>DELETE FROM maintenance_record_detail WHERE id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    int physicalDeleteByIds(@Param("ids") List<Long> ids);
}
