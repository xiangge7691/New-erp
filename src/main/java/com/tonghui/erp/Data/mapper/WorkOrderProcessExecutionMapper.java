package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Data.Entity.WorkOrderProcessExecution;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

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
}
