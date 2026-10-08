package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Data.Entity.UserRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 用户角色数据访问Mapper接口
 */
public interface UserRoleMapper extends BaseMapper<UserRole> {

    /**
     * 根据用户ID物理删除所有用户角色关联（绕过逻辑删除，释放唯一键约束）
     *
     * @param userId 用户ID
     * @return 删除的行数
     */
    @Delete("DELETE FROM user_role WHERE user_id = #{userId}")
    int physicalDeleteByUserId(@Param("userId") Long userId);
}