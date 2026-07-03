package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Data.Entity.TemperatureHumidityRecord;
import java.util.List;

/**
 * 温湿度记录服务接口
 * <p>
 * 提供温湿度记录的CRUD操作及业务查询功能，包括按房间查询历史记录
 * </p>
 */
public interface TemperatureHumidityRecordService extends IService<TemperatureHumidityRecord> {

    /**
     * 根据房间ID查询温湿度记录列表
     * <p>
     * 查询指定房间的所有未删除温湿度记录，按记录日期倒序排列
     * </p>
     *
     * @param roomId 房间ID
     * @return 该房间的温湿度记录列表，按记录日期倒序
     */
    List<TemperatureHumidityRecord> findByRoomId(Integer roomId);
}
