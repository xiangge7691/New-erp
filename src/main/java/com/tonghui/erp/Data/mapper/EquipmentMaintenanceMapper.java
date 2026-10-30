package com.tonghui.erp.Data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tonghui.erp.Common.Dto.Equipment.EquipmentMaintenanceConflictDto;
import com.tonghui.erp.Common.Dto.Equipment.EquipmentMaintenanceRecordDto;
import com.tonghui.erp.Data.Entity.EquipmentMaintenance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 设备维保记录Mapper接口
 */
@Mapper
public interface EquipmentMaintenanceMapper extends BaseMapper<EquipmentMaintenance> {

    /**
     * 查询指定设备在时间窗内的维保记录（用于工序排产时的维保时间冲突校验）
     * <p>
     * 按维保时间区间 [maintenance_start_time, maintenance_end_time) 与目标时间窗做重叠粗筛，
     * 仅返回未删除记录，并带出设备名称/固定资产编号用于错误提示。
     * </p>
     *
     * @param equipmentIds 设备ID集合
     * @param minStart     时间窗起点（不含）
     * @param maxEnd       时间窗终点（不含）
     * @return 维保冲突明细列表
     */
    List<EquipmentMaintenanceConflictDto> selectMaintenanceConflicts(
            @Param("equipmentIds") Collection<Integer> equipmentIds,
            @Param("minStart") LocalDateTime minStart,
            @Param("maxEnd") LocalDateTime maxEnd);

    /**
     * 查询指定设备的维保记录（用于设备使用记录导出，附带维保时间段）
     * <p>
     * 传入时间窗时按维保区间 [maintenance_start_time, maintenance_end_time) 与时间窗重叠过滤，
     * 不传时间窗则返回该设备的全部维保记录。
     * </p>
     *
     * @param equipmentIds 设备ID集合
     * @param minStart     时间窗起点（不含），可为 null
     * @param maxEnd       时间窗终点（不含），可为 null
     * @return 维保记录列表（按设备、维保开始时间升序）
     */
    List<EquipmentMaintenanceRecordDto> selectMaintenanceRecords(
            @Param("equipmentIds") Collection<Integer> equipmentIds,
            @Param("minStart") LocalDateTime minStart,
            @Param("maxEnd") LocalDateTime maxEnd);
}
