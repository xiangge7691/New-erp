package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.PressureDifferenceRecord;
import com.tonghui.erp.Data.mapper.PressureDifferenceRecordMapper;
import com.tonghui.erp.Service.PressureDifferenceRecordService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PressureDifferenceRecordServiceImpl extends ServiceImpl<PressureDifferenceRecordMapper, PressureDifferenceRecord> implements PressureDifferenceRecordService {

    @Override
    public List<PressureDifferenceRecord> findByRoomId(Integer roomId) {
        QueryWrapper<PressureDifferenceRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("room_id", roomId)
               .eq("is_deleted", 0)
               .orderByDesc("record_date");
        return list(wrapper);
    }
}
