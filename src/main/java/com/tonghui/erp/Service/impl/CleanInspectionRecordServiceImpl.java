package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.CleanInspectionRecord;
import com.tonghui.erp.Data.mapper.CleanInspectionRecordMapper;
import com.tonghui.erp.Service.CleanInspectionRecordService;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * 洁净检测记录服务实现类
 * <p>
 * 实现CleanInspectionRecordService接口，提供洁净检测记录的业务逻辑处理，
 * 包括按房间查询历史检测记录
 * </p>
 */
@Service
public class CleanInspectionRecordServiceImpl extends ServiceImpl<CleanInspectionRecordMapper, CleanInspectionRecord> implements CleanInspectionRecordService {

    // region 业务查询方法
    // ===================================
    // 业务查询方法
    // ===================================

    /**
     * 根据房间ID查询洁净检测记录列表
     *
     * @param roomId 房间ID
     * @return 该房间的检测记录列表，按检测日期倒序
     */
    @Override
    public List<CleanInspectionRecord> findByRoomId(Integer roomId) {
        QueryWrapper<CleanInspectionRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("room_id", roomId)
               .eq("is_deleted", 0)
               .orderByDesc("inspection_date");
        return list(wrapper);
    }

    // endregion
}
