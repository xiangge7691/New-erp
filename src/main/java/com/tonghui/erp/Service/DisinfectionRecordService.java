package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Data.Entity.DisinfectionRecord;
import java.util.List;

/**
 * 消毒记录服务接口
 * <p>
 * 提供消毒记录的CRUD操作及业务查询功能，包括按房间查询和查询即将到期的消毒提醒
 * </p>
 */
public interface DisinfectionRecordService extends IService<DisinfectionRecord> {

    /**
     * 根据房间ID查询消毒记录列表
     * <p>
     * 查询指定房间的所有未删除消毒记录，按消毒日期倒序排列
     * </p>
     *
     * @param roomId 房间ID
     * @return 该房间的消毒记录列表，按消毒日期倒序
     */
    List<DisinfectionRecord> findByRoomId(Integer roomId);

    /**
     * 查询即将到期的消毒提醒
     * <p>
     * 查询在未来指定天数内需要进行消毒的记录，用于到期提醒
     * </p>
     *
     * @param days 提前天数，查询从今天起至指定天数内的待消毒记录
     * @return 即将到期的消毒记录列表，按下次消毒日期升序
     */
    List<DisinfectionRecord> findUpcomingDisinfection(int days);
}
