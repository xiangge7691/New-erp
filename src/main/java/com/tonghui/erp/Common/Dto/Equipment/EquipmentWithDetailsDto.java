package com.tonghui.erp.Common.Dto.Equipment;

import com.tonghui.erp.Data.Entity.Equipment;
import com.tonghui.erp.Data.Entity.EquipmentMaintenance;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 设备包含维保记录的扩展数据传输对象
 * <p>
 * 在设备基础上扩展了维保记录列表，用于展示完整的设备详情及维保历史
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EquipmentWithDetailsDto extends Equipment {

    /**
     * 该设备的维保记录列表
     */
    private List<EquipmentMaintenance> maintenanceRecords;
}
