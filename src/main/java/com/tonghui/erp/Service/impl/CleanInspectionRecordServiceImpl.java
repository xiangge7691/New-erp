package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.CleanInspectionRecord;
import com.tonghui.erp.Data.mapper.CleanInspectionRecordMapper;
import com.tonghui.erp.Service.CleanInspectionRecordService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CleanInspectionRecordServiceImpl extends ServiceImpl<CleanInspectionRecordMapper, CleanInspectionRecord> implements CleanInspectionRecordService {

    @Override
    public List<CleanInspectionRecord> findByRoomId(Integer roomId) {
        QueryWrapper<CleanInspectionRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("room_id", roomId)
               .eq("is_deleted", 0)
               .orderByDesc("inspection_date");
        return list(wrapper);
    }
}
