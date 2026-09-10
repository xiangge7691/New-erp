package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Data.Entity.CleanInspectionRecord;
import java.util.List;

/**
 * 洁净检测记录服务接口
 * <p>
 * 提供洁净检测记录的CRUD操作及业务查询功能，包括按房间查询历史检测记录
 * </p>
 */
public interface CleanInspectionRecordService extends IService<CleanInspectionRecord> {

    /**
     * 根据房间ID查询洁净检测记录列表
     * <p>
     * 查询指定房间的所有未删除检测记录，按检测日期倒序排列
     * </p>
     *
     * @param roomId 房间ID
     * @return 该房间的检测记录列表，按检测日期倒序
     */
    List<CleanInspectionRecord> findByRoomId(Integer roomId);
}
