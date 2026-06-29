package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.EquipmentMaintenance;
import com.tonghui.erp.Data.mapper.EquipmentMaintenanceMapper;
import com.tonghui.erp.Service.EquipmentMaintenanceService;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

/**
 * 设备维保记录服务实现类
 */
@Service
public class EquipmentMaintenanceServiceImpl extends ServiceImpl<EquipmentMaintenanceMapper, EquipmentMaintenance> implements EquipmentMaintenanceService {

    /**
     * 查询即将到期的维保提醒
     * 查询条件：下次维保日期在今天到指定天数之间
     */
    @Override
    public List<EquipmentMaintenance> findUpcomingMaintenance(int days) {
        LocalDate today = LocalDate.now();
        LocalDate deadline = today.plusDays(days);
        
        QueryWrapper<EquipmentMaintenance> wrapper = new QueryWrapper<>();
        wrapper.isNotNull("next_maintenance_date")  // 下次维保日期不为空
               .ge("next_maintenance_date", today)  // 大于等于今天
               .le("next_maintenance_date", deadline)  // 小于等于截止日期
               .orderByAsc("next_maintenance_date");  // 按下次维保日期升序
        
        return list(wrapper);
    }

    /**
     * 根据设备ID查询维保记录
     */
    @Override
    public List<EquipmentMaintenance> findByEquipmentId(Long equipmentId) {
        QueryWrapper<EquipmentMaintenance> wrapper = new QueryWrapper<>();
        wrapper.eq("equipment_id", equipmentId)
               .orderByDesc("maintenance_date");  // 按维保日期降序
        return list(wrapper);
    }
}
