package com.tonghui.erp.Data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import com.tonghui.erp.Data.Entity.ProductionShift;

/**
 * 生产班次数据访问Mapper接口
 */
public interface ProductionShiftMapper extends BaseMapper<ProductionShift> {
    /**
     * 查询班组适用的启用班次
     *
     * @param teamId 班组ID
     * @return 班次列表
     */
    @Select("SELECT s.* FROM production_shift s"
            + " JOIN production_shift_team st ON st.shift_id = s.id AND st.is_deleted = 0"
            + " WHERE st.team_id = #{teamId} AND s.is_deleted = 0 AND s.status = 1"
            + " ORDER BY s.start_time ASC")
    List<com.tonghui.erp.Data.Entity.ProductionShift> selectActiveByTeamId(@Param("teamId") Long teamId);
}
