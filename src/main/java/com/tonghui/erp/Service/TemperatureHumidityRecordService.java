package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Data.Entity.TemperatureHumidityRecord;
import java.util.List;

public interface TemperatureHumidityRecordService extends IService<TemperatureHumidityRecord> {

    List<TemperatureHumidityRecord> findByRoomId(Integer roomId);
}
