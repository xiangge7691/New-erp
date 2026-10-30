package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Data.Entity.EquipmentMaintenance;
import java.util.List;

/**
 * 设备维保记录服务接口
 */
public interface EquipmentMaintenanceService extends IService<EquipmentMaintenance> {

    // region 查询操作
    // ===================================
    // 查询操作
    // ===================================

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

    // endregion

    // region 保存操作
    // ===================================
    // 保存操作
    // ===================================

    /**
     * 新增维保记录（含自动计算逻辑）
     * <p>
     * 保养类型：自动按设备维保周期计算下次维保时间（以维保开始时间所在日期为基准）；
     * 维修类型：不自动计算，需手工填写。
     * 保存前校验维保时间是否与设备使用时间冲突。
     * </p>
     *
     * @param maintenance 维保记录
     * @return 保存后的记录
     * @throws IllegalArgumentException 维保时间与设备使用时间冲突或参数不合法
     */
    EquipmentMaintenance saveWithAutoCalc(EquipmentMaintenance maintenance);

    /**
     * 修改维保记录（含冲突校验）
     * <p>
     * 仅在入参涉及设备或维保时间时，将入参与原记录合并后校验维保时间是否与设备使用时间冲突；
     * 仅修改内容、费用等字段时不触发冲突校验。
     * </p>
     *
     * @param maintenance 维保记录（须包含 maintenanceId）
     * @return 修改后的记录
     * @throws IllegalArgumentException 维保记录不存在或维保时间与设备使用时间冲突
     */
    EquipmentMaintenance updateWithConflictCheck(EquipmentMaintenance maintenance);

    // endregion
}
