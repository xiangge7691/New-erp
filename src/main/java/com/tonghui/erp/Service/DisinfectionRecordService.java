package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Data.Entity.DisinfectionRecord;
import java.util.List;

public interface DisinfectionRecordService extends IService<DisinfectionRecord> {

    List<DisinfectionRecord> findByRoomId(Integer roomId);

    List<DisinfectionRecord> findUpcomingDisinfection(int days);
}
