package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Data.Entity.WorkOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
}




