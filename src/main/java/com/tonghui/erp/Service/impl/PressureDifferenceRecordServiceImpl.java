package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.PressureDifferenceRecord;
import com.tonghui.erp.Data.mapper.PressureDifferenceRecordMapper;
import com.tonghui.erp.Service.PressureDifferenceRecordService;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * 压差记录服务实现类
 * <p>
 * 实现PressureDifferenceRecordService接口，提供压差记录的业务逻辑处理，
 * 包括按房间查询历史记录
 * </p>
 */
@Service
public class PressureDifferenceRecordServiceImpl extends ServiceImpl<PressureDifferenceRecordMapper, PressureDifferenceRecord> implements PressureDifferenceRecordService {

    // region 业务查询方法
    // ===================================
    // 业务查询方法
    // ===================================

    /**
     * 根据房间ID查询压差记录列表
     *
     * @param roomId 房间ID
     * @return 该房间的压差记录列表，按记录日期倒序
     */
    @Override
    public List<PressureDifferenceRecord> findByRoomId(Integer roomId) {
        QueryWrapper<PressureDifferenceRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("room_id", roomId)
               .eq("is_deleted", 0)
               .orderByDesc("record_date");
        return list(wrapper);
    }

    // endregion
}
