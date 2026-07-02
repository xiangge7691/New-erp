package com.tonghui.erp.Common.Dto.Equipment;

import com.tonghui.erp.Data.Entity.Equipment;
import com.tonghui.erp.Data.Entity.EquipmentMaintenance;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class EquipmentWithDetailsDto extends Equipment {
    private List<EquipmentMaintenance> maintenanceRecords;
}
