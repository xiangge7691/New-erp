package com.tonghui.erp.Data.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.Equipment.EquipmentUsageQueryDto;
import com.tonghui.erp.Common.Dto.Equipment.EquipmentUsageRecordDto;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 设备使用记录数据访问Mapper接口
 * <p>
 * 设备使用记录不单独建表，统一来源于工单工序执行记录（work_order_process_execution），
 * 关联设备、工单、工序类型、房间后形成使用台账。
 * </p>
 */
public interface EquipmentUsageMapper {

    /**
     * 分页查询设备使用记录
     *
     * @param page  分页对象
     * @param query 查询条件
     * @return 分页结果
     */
    Page<EquipmentUsageRecordDto> selectUsagePage(Page<EquipmentUsageRecordDto> page,
                                                  @Param("q") EquipmentUsageQueryDto query);

    /**
     * 查询设备使用记录列表（导出用，按设备名称、开始时间排序）
     *
     * @param query 查询条件
     * @param limit 最大返回行数（为空不限制）
     * @return 使用记录列表
     */
    List<EquipmentUsageRecordDto> selectUsageList(@Param("q") EquipmentUsageQueryDto query,
                                                  @Param("limit") Integer limit);
}
