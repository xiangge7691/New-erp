package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Data.Entity.CleanInspectionRecord;
import java.util.List;

public interface CleanInspectionRecordService extends IService<CleanInspectionRecord> {

    List<CleanInspectionRecord> findByRoomId(Integer roomId);
}
