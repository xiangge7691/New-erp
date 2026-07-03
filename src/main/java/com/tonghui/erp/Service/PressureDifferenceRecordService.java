package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Data.Entity.PressureDifferenceRecord;
import java.util.List;

/**
 * 压差记录服务接口
 * <p>
 * 提供压差记录的CRUD操作及业务查询功能，包括按房间查询历史记录
 * </p>
 */
public interface PressureDifferenceRecordService extends IService<PressureDifferenceRecord> {

    /**
     * 根据房间ID查询压差记录列表
     * <p>
     * 查询指定房间的所有未删除压差记录，按记录日期倒序排列
     * </p>
     *
     * @param roomId 房间ID
     * @return 该房间的压差记录列表，按记录日期倒序
     */
    List<PressureDifferenceRecord> findByRoomId(Integer roomId);
}
