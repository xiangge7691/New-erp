package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.DisinfectionRecord;
import com.tonghui.erp.Data.mapper.DisinfectionRecordMapper;
import com.tonghui.erp.Service.DisinfectionRecordService;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
public class DisinfectionRecordServiceImpl extends ServiceImpl<DisinfectionRecordMapper, DisinfectionRecord> implements DisinfectionRecordService {

    @Override
    public List<DisinfectionRecord> findByRoomId(Integer roomId) {
        QueryWrapper<DisinfectionRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("room_id", roomId)
               .eq("is_deleted", 0)
               .orderByDesc("disinfection_date");
        return list(wrapper);
    }

    @Override
    public List<DisinfectionRecord> findUpcomingDisinfection(int days) {
        LocalDate today = LocalDate.now();
        LocalDate deadline = today.plusDays(days);

        QueryWrapper<DisinfectionRecord> wrapper = new QueryWrapper<>();
        wrapper.isNotNull("next_disinfection_date")
               .ge("next_disinfection_date", today)
               .le("next_disinfection_date", deadline)
               .eq("is_deleted", 0)
               .orderByAsc("next_disinfection_date");
        return list(wrapper);
    }
}
