package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.TemperatureHumidityRecord;
import com.tonghui.erp.Data.mapper.TemperatureHumidityRecordMapper;
import com.tonghui.erp.Service.TemperatureHumidityRecordService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TemperatureHumidityRecordServiceImpl extends ServiceImpl<TemperatureHumidityRecordMapper, TemperatureHumidityRecord> implements TemperatureHumidityRecordService {

    @Override
    public List<TemperatureHumidityRecord> findByRoomId(Integer roomId) {
        QueryWrapper<TemperatureHumidityRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("room_id", roomId)
               .eq("is_deleted", 0)
               .orderByDesc("record_date");
        return list(wrapper);
    }
}
