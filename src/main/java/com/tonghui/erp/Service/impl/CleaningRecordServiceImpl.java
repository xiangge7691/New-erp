package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.CleaningRecord;
import com.tonghui.erp.Data.mapper.CleaningRecordMapper;
import com.tonghui.erp.Service.CleaningRecordService;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
public class CleaningRecordServiceImpl extends ServiceImpl<CleaningRecordMapper, CleaningRecord> implements CleaningRecordService {

    @Override
    public List<CleaningRecord> findByRoomId(Integer roomId) {
        QueryWrapper<CleaningRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("room_id", roomId)
               .eq("is_deleted", 0)
               .orderByDesc("cleaning_date");
        return list(wrapper);
    }

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
}
