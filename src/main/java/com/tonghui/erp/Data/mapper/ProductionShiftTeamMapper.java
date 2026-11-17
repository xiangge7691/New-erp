package com.tonghui.erp.Data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import com.tonghui.erp.Data.Entity.ProductionShiftTeam;

/**
 * 班次适用班组数据访问Mapper接口
 */
public interface ProductionShiftTeamMapper extends BaseMapper<ProductionShiftTeam> {

    /**
     * 查询班次适用的班组ID
     *
     * @param shiftId 班次ID
     * @return 班组ID列表
     */
    @Select("SELECT team_id FROM production_shift_team WHERE shift_id = #{shiftId} AND is_deleted = 0")
    List<Long> selectTeamIdsByShiftId(@Param("shiftId") Long shiftId);

    /**
     * 物理删除班次的适用班组（重建关系时使用）
     *
     * @param shiftId 班次ID
     * @return 删除行数
     */
    @Delete("DELETE FROM production_shift_team WHERE shift_id = #{shiftId}")
    int physicalDeleteByShiftId(@Param("shiftId") Long shiftId);
}
