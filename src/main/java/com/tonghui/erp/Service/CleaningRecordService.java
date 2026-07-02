package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Data.Entity.CleaningRecord;
import java.util.List;

public interface CleaningRecordService extends IService<CleaningRecord> {

    List<CleaningRecord> findByRoomId(Integer roomId);

    List<CleaningRecord> findUpcomingCleaning(int days);
}
