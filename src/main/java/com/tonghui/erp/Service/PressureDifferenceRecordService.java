package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Data.Entity.PressureDifferenceRecord;
import java.util.List;

public interface PressureDifferenceRecordService extends IService<PressureDifferenceRecord> {

    List<PressureDifferenceRecord> findByRoomId(Integer roomId);
}
