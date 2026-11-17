package com.tonghui.erp.Data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tonghui.erp.Data.Entity.ProductionTeamMember;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import com.tonghui.erp.Data.Entity.ProductionTeamMember;

/**
 * 班组成员数据访问Mapper接口
 */
public interface ProductionTeamMemberMapper extends BaseMapper<ProductionTeamMember> {

    /**
     * 查询班组成员（含人员姓名、工号）
     *
     * @param teamId 班组ID
     * @return 成员列表
     */
    @Select("SELECT m.*, p.name AS personnelName, p.employee_no AS employeeNo"
            + " FROM production_team_member m"
            + " LEFT JOIN personnel_file p ON p.personnel_file_id = m.personnel_id AND p.is_deleted = 0"
            + " WHERE m.team_id = #{teamId} AND m.is_deleted = 0"
            + " ORDER BY m.is_primary DESC, m.id ASC")
    List<ProductionTeamMember> selectByTeamIdWithPersonnel(@Param("teamId") Long teamId);

    /**
     * 物理删除班组下全部成员（重建成员关系时使用）
     *
     * @param teamId 班组ID
     * @return 删除行数
     */
    @Delete("DELETE FROM production_team_member WHERE team_id = #{teamId}")
    int physicalDeleteByTeamId(@Param("teamId") Long teamId);

    /**
     * 查询班组成员的人员档案ID
     *
     * @param teamId 班组ID
     * @return 人员档案ID列表
     */
    @Select("SELECT personnel_id FROM production_team_member WHERE team_id = #{teamId} AND is_deleted = 0")
    List<Long> selectPersonnelIdsByTeamId(@Param("teamId") Long teamId);
}
