package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Data.Entity.WorkOrderProcessExecution;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tonghui.erp.Common.Dto.Equipment.EquipmentUsageConflictDto;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 工单工序执行记录数据访问Mapper接口
 */
public interface WorkOrderProcessExecutionMapper extends BaseMapper<WorkOrderProcessExecution> {

    /**
     * 按记录ID集合物理删除（批量保存时删除被移除的行）
     * <p>绕过全局软删除配置，直接执行物理删除，保留未被移除行的ID</p>
     *
     * @param ids 记录ID集合
     * @return 删除的行数
     */
    @Delete("<script>DELETE FROM work_order_process_execution WHERE id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    int physicalDeleteByIds(@Param("ids") List<Long> ids);

    /**
     * 根据工单ID查询工序执行记录（含关联表详情）
     * <p>
     * 关联 process_type、room_info、equipment 表获取显示字段
     * </p>
     *
     * @param workOrderId 工单ID
     * @return 工序执行记录列表（含工序类型名称、配置室名称、设备名称等）
     */
    List<WorkOrderProcessExecution> selectByWorkOrderIdWithDetails(@Param("workOrderId") Long workOrderId);

    /**
     * 查询指定工单当前未删除的工序执行记录ID
     * <p>
     * 批量保存采用"提交全量覆盖"语义：未出现在入参中的既有行将被删除，
     * 因此这些行的设备占用不应参与本次冲突校验，需要排除。
     * </p>
     *
     * @param workOrderId 工单ID
     * @return 记录ID集合
     */
    List<Long> selectIdsByWorkOrderId(@Param("workOrderId") Long workOrderId);

    /**
     * 查询指定设备在时间窗内已被占用的工序执行记录（冲突粗筛）
     * <p>
     * SQL 先按"设备集合 + 全局时间窗"粗筛，调用方再按每行的时间区间做精确的半开区间重叠判断，
     * 避免逐行查询数据库。
     * </p>
     *
     * @param equipmentIds 设备ID集合
     * @param minStart     本批记录的最早开始时间
     * @param maxEnd       本批记录的最晚结束时间
     * @param excludeIds   需要排除的记录ID（本工单既有行）
     * @return 冲突明细列表
     */
    List<EquipmentUsageConflictDto> selectConflictRecords(@Param("equipmentIds") Collection<Integer> equipmentIds,
                                                          @Param("minStart") LocalDateTime minStart,
                                                          @Param("maxEnd") LocalDateTime maxEnd,
                                                          @Param("excludeIds") Collection<Long> excludeIds);
}
