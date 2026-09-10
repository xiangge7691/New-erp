package com.tonghui.erp.Common.Dto.Room;

import com.tonghui.erp.Data.Entity.RoomInfo;
import com.tonghui.erp.Data.Entity.Equipment;
import com.tonghui.erp.Data.Entity.TemperatureHumidityRecord;
import com.tonghui.erp.Data.Entity.PressureDifferenceRecord;
import com.tonghui.erp.Data.Entity.CleanInspectionRecord;
import com.tonghui.erp.Data.Entity.DisinfectionRecord;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 车间房间包含完整环境信息的扩展数据传输对象
 * <p>
 * 在车间房间基础上扩展了设备、温湿度、压差、洁净检测和消毒记录列表，
 * 用于展示完整的车间环境详情，支持GMP合规管理
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RoomInfoWithDetailsDto extends RoomInfo {

    /**
     * 该房间的设备列表
     */
    private List<Equipment> equipment;

    /**
     * 该房间的温湿度记录列表
     */
    private List<TemperatureHumidityRecord> temperatureHumidityRecords;

    /**
     * 该房间的压差记录列表
     */
    private List<PressureDifferenceRecord> pressureDifferenceRecords;

    /**
     * 该房间的洁净检测记录列表
     */
    private List<CleanInspectionRecord> cleanInspectionRecords;

    /**
     * 该房间的消毒记录列表
     */
    private List<DisinfectionRecord> disinfectionRecords;
}
