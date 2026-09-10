package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Data.Entity.WorkOrderProcessExecution;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 工单工序执行记录数据访问Mapper接口
 */
public interface WorkOrderProcessExecutionMapper extends BaseMapper<WorkOrderProcessExecution> {

    /**
     * 根据工单ID物理删除工序执行记录
     *
     * @param workOrderId 工单ID
     * @return 删除的记录数
     */
    @Delete("DELETE FROM work_order_process_execution WHERE work_order_id = #{workOrderId}")
    int physicalDeleteByWorkOrderId(@Param("workOrderId") Long workOrderId);

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
}
