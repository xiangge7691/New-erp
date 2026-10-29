package com.tonghui.erp.Data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tonghui.erp.Common.Dto.Equipment.EquipmentMaintenanceConflictDto;
import com.tonghui.erp.Data.Entity.EquipmentMaintenance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * 设备维保记录Mapper接口
 */
@Mapper
public interface EquipmentMaintenanceMapper extends BaseMapper<EquipmentMaintenance> {

    /**
     * 查询指定设备在日期范围内的维保记录（用于工序排产时的维保时间冲突校验）
     * <p>
     * 维保表仅有 maintenance_date（DATE），按"维保当天整日占用"处理，
     * 仅返回未删除记录，并带出设备名称/固定资产编号用于错误提示。
     * </p>
     *
     * @param equipmentIds 设备ID集合
     * @param minDate      日期范围下界（含）
     * @param maxDate      日期范围上界（含）
     * @return 维保冲突明细列表
     */
    List<EquipmentMaintenanceConflictDto> selectMaintenanceConflicts(
            @Param("equipmentIds") Collection<Integer> equipmentIds,
            @Param("minDate") LocalDate minDate,
            @Param("maxDate") LocalDate maxDate);
}
