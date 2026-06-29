package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Data.Entity.EquipmentMaintenance;
import java.util.List;

/**
 * 设备维保记录服务接口
 */
public interface EquipmentMaintenanceService extends IService<EquipmentMaintenance> {

    /**
     * 查询即将到期的维保提醒
     * @param days 提前天数
     * @return 即将到期的维保记录列表
     */
    List<EquipmentMaintenance> findUpcomingMaintenance(int days);

    /**
     * 根据设备ID查询维保记录
     * @param equipmentId 设备ID
     * @return 维保记录列表
     */
    List<EquipmentMaintenance> findByEquipmentId(Long equipmentId);

    /**
     * 新增维保记录（含自动计算逻辑）
     * 保养类型：自动按维保周期计算下次维保时间
     * 维修类型：不自动计算，需手工填写
     * @param maintenance 维保记录
     * @return 保存后的记录
     */
    EquipmentMaintenance saveWithAutoCalc(EquipmentMaintenance maintenance);
}
