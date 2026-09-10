package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.DisinfectionRecord;
import com.tonghui.erp.Data.mapper.DisinfectionRecordMapper;
import com.tonghui.erp.Service.DisinfectionRecordService;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

/**
 * 消毒记录服务实现类
 * <p>
 * 实现DisinfectionRecordService接口，提供消毒记录的业务逻辑处理，
 * 包括按房间查询和即将到期的消毒提醒查询
 * </p>
 */
@Service
public class DisinfectionRecordServiceImpl extends ServiceImpl<DisinfectionRecordMapper, DisinfectionRecord> implements DisinfectionRecordService {

    // region 业务查询方法
    // ===================================
    // 业务查询方法
    // ===================================

    /**
     * 根据房间ID查询消毒记录列表
     *
     * @param roomId 房间ID
     * @return 该房间的消毒记录列表，按消毒日期倒序
     */
    @Override
    public List<DisinfectionRecord> findByRoomId(Integer roomId) {
        QueryWrapper<DisinfectionRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("room_id", roomId)
               .eq("is_deleted", 0)
               .orderByDesc("disinfection_date");
        return list(wrapper);
    }

    /**
     * 查询即将到期的消毒提醒
     * <p>
     * 筛选条件：下次消毒日期不为空、在今天至指定天数范围内、未删除
     * </p>
     *
     * @param days 提前天数
     * @return 即将到期的消毒记录列表，按下次消毒日期升序
     */
    @Override
    public List<DisinfectionRecord> findUpcomingDisinfection(int days) {
        LocalDate today = LocalDate.now();
        LocalDate deadline = today.plusDays(days);
        LocalDate pastDeadline = today.minusDays(days);

        QueryWrapper<DisinfectionRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("is_deleted", 0)
               .isNotNull("next_disinfection_date")
               .ge("next_disinfection_date", pastDeadline)
               .le("next_disinfection_date", deadline)
               .orderByAsc("next_disinfection_date");
        return list(wrapper);
    }

    // endregion
}
