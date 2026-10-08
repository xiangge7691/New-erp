package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Data.Entity.UserDepartment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 用户部门数据访问Mapper接口
 */
public interface UserDepartmentMapper extends BaseMapper<UserDepartment> {

    /**
     * 根据用户ID物理删除所有用户部门关联（绕过逻辑删除，释放唯一键约束）
     *
     * @param userId 用户ID
     * @return 删除的行数
     */
    @Delete("DELETE FROM user_department WHERE user_id = #{userId}")
    int physicalDeleteByUserId(@Param("userId") Long userId);
}