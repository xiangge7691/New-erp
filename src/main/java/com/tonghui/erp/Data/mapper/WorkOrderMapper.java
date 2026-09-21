package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Data.Entity.WorkOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 工单数据访问Mapper接口
 */
public interface WorkOrderMapper extends BaseMapper<WorkOrder> {

    /**
     * 查询当天最大的工单编号（绕过软删除过滤，包含已删除记录）
     * <p>
     * 用于工单编号生成，避免软删除后编号重复
     * </p>
     *
     * @param prefix 编号前缀，如 "GD20260731"
     * @return 当天最大工单编号，无记录时返回null
     */
    @Select("SELECT work_order_code FROM work_order WHERE work_order_code LIKE CONCAT(#{prefix}, '%') ORDER BY work_order_code DESC LIMIT 1")
    String selectMaxCodeByPrefix(@Param("prefix") String prefix);

    /**
     * 查询所有已检验状态的工单（inspection_end不为空）
     * <p>
     * 用于审核放行模块关联生产任务下拉选择，仅展示已完成检验的工单
     * </p>
     *
     * @return 已检验工单列表，按工单编号倒序
     */
    @Select("SELECT * FROM work_order WHERE inspection_end IS NOT NULL AND is_deleted = 0 ORDER BY work_order_code DESC")
    List<WorkOrder> selectInspectedOrders();
}




