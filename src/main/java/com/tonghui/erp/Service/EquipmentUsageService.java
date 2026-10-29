package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Dto.Equipment.EquipmentUsageRecordDto;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 设备使用记录服务接口
 * <p>
 * 设备使用记录来源于工单工序执行记录，提供分页查询与按设备导出 Excel 的能力。
 * </p>
 */
public interface EquipmentUsageService {

    /**
     * 分页查询设备使用记录
     *
     * @param equipmentId   设备ID（可选）
     * @param equipmentType 设备类型（可选）
     * @param keyword       关键字（可选，模糊匹配工单编号/批号/制剂名称/工序名称/设备名称/固定资产编号）
     * @param status        执行状态（可选）
     * @param startDate     使用开始日期（可选，格式 yyyy-MM-dd）
     * @param endDate       使用结束日期（可选，格式 yyyy-MM-dd）
     * @param pageRequest   分页参数
     * @return 分页结果
     */
    PagedResult<EquipmentUsageRecordDto> search(Integer equipmentId, String equipmentType, String keyword,
                                                String status, String startDate, String endDate,
                                                PageRequestDto pageRequest);

    /**
     * 导出设备使用记录 Excel
     * <p>
     * 指定设备ID时导出单个设备的单 sheet；未指定时按设备分 sheet 导出全部设备，并附加汇总 sheet。
     * </p>
     *
     * @param equipmentId   设备ID（可选）
     * @param equipmentType 设备类型（可选）
     * @param keyword       关键字（可选）
     * @param status        执行状态（可选）
     * @param startDate     使用开始日期（可选，格式 yyyy-MM-dd）
     * @param endDate       使用结束日期（可选，格式 yyyy-MM-dd）
     * @param out           输出流
     * @throws IOException              写流失败
     * @throws IllegalArgumentException 日期格式非法、开始日期晚于结束日期或导出行数超限
     */
    void exportToExcel(Integer equipmentId, String equipmentType, String keyword, String status,
                       String startDate, String endDate, OutputStream out) throws IOException;
}
