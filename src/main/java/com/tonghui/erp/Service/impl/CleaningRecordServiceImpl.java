package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.CleaningRecord;
import com.tonghui.erp.Data.mapper.CleaningRecordMapper;
import com.tonghui.erp.Service.CleaningRecordService;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

/**
 * 清洁记录服务实现类
 * <p>
 * 实现CleaningRecordService接口，提供清洁记录的业务逻辑处理，
 * 包括按房间查询和即将到期的清洁提醒查询
 * </p>
 */
@Service
public class CleaningRecordServiceImpl extends ServiceImpl<CleaningRecordMapper, CleaningRecord> implements CleaningRecordService {

    // region 业务查询方法
    // ===================================
    // 业务查询方法
    // ===================================

    /**
     * 根据房间ID查询清洁记录列表
     *
     * @param roomId 房间ID
     * @return 该房间的清洁记录列表，按清洁日期倒序
     */
    @Override
    public List<CleaningRecord> findByRoomId(Integer roomId) {
        QueryWrapper<CleaningRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("room_id", roomId)
               .eq("is_deleted", 0)
               .orderByDesc("cleaning_date");
        return list(wrapper);
    }

    /**
     * 查询即将到期的清洁提醒
     * <p>
     * 筛选条件：下次清洁日期不为空、在今天至指定天数范围内、未删除
     * </p>
     *
     * @param days 提前天数
     * @return 即将到期的清洁记录列表，按下次清洁日期升序
     */
    @Override
    public List<CleaningRecord> findUpcomingCleaning(int days) {
        LocalDate today = LocalDate.now();
        LocalDate deadline = today.plusDays(days);

        QueryWrapper<CleaningRecord> wrapper = new QueryWrapper<>();
        wrapper.isNotNull("next_cleaning_date")
               .ge("next_cleaning_date", today)
               .le("next_cleaning_date", deadline)
               .eq("is_deleted", 0)
               .orderByAsc("next_cleaning_date");
        return list(wrapper);
    }

    // endregion
}
