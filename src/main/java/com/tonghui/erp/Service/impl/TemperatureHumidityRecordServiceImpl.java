package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.TemperatureHumidityRecord;
import com.tonghui.erp.Data.mapper.TemperatureHumidityRecordMapper;
import com.tonghui.erp.Service.TemperatureHumidityRecordService;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * 温湿度记录服务实现类
 * <p>
 * 实现TemperatureHumidityRecordService接口，提供温湿度记录的业务逻辑处理，
 * 包括按房间查询历史记录
 * </p>
 */
@Service
public class TemperatureHumidityRecordServiceImpl extends ServiceImpl<TemperatureHumidityRecordMapper, TemperatureHumidityRecord> implements TemperatureHumidityRecordService {

    // region 业务查询方法
    // ===================================
    // 业务查询方法
    // ===================================

    /**
     * 根据房间ID查询温湿度记录列表
     *
     * @param roomId 房间ID
     * @return 该房间的温湿度记录列表，按记录日期倒序
     */
    @Override
    public List<TemperatureHumidityRecord> findByRoomId(Integer roomId) {
        QueryWrapper<TemperatureHumidityRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("room_id", roomId)
               .eq("is_deleted", 0)
               .orderByDesc("record_date");
        return list(wrapper);
    }

    // endregion
}
