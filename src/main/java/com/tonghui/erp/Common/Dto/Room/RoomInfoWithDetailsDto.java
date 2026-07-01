package com.tonghui.erp.Common.Dto.Room;

import com.tonghui.erp.Data.Entity.RoomInfo;
import com.tonghui.erp.Data.Entity.TemperatureHumidityRecord;
import com.tonghui.erp.Data.Entity.PressureDifferenceRecord;
import com.tonghui.erp.Data.Entity.CleanInspectionRecord;
import com.tonghui.erp.Data.Entity.DisinfectionRecord;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class RoomInfoWithDetailsDto extends RoomInfo {
    private List<TemperatureHumidityRecord> temperatureHumidityRecords;
    private List<PressureDifferenceRecord> pressureDifferenceRecords;
    private List<CleanInspectionRecord> cleanInspectionRecords;
    private List<DisinfectionRecord> disinfectionRecords;
}
