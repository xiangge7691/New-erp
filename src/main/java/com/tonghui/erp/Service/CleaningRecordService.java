package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Data.Entity.CleaningRecord;
import java.util.List;

/**
 * 清洁记录服务接口
 * <p>
 * 提供清洁记录的CRUD操作及业务查询功能，包括按房间查询和查询即将到期的清洁提醒
 * </p>
 */
public interface CleaningRecordService extends IService<CleaningRecord> {

    /**
     * 根据房间ID查询清洁记录列表
     * <p>
     * 查询指定房间的所有未删除清洁记录，按清洁日期倒序排列
     * </p>
     *
     * @param roomId 房间ID
     * @return 该房间的清洁记录列表，按清洁日期倒序
     */
    List<CleaningRecord> findByRoomId(Integer roomId);

    /**
     * 查询即将到期的清洁提醒
     * <p>
     * 查询在未来指定天数内需要进行清洁的记录，用于到期提醒
     * </p>
     *
     * @param days 提前天数，查询从今天起至指定天数内的待清洁记录
     * @return 即将到期的清洁记录列表，按下次清洁日期升序
     */
    List<CleaningRecord> findUpcomingCleaning(int days);
}
