package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Data.Entity.RolePerm;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 角色权限数据访问Mapper接口
 */
public interface RolePermMapper extends BaseMapper<RolePerm> {

    /**
     * 根据角色ID物理删除所有角色权限关联（绕过逻辑删除）
     *
     * @param roleId 角色ID
     * @return 删除的行数
     */
    @Delete("DELETE FROM role_perm WHERE role_id = #{roleId}")
    int physicalDeleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据角色ID清理已被软删除的记录（释放唯一键约束）
     *
     * @param roleId 角色ID
     * @return 删除的行数
     */
    @Delete("DELETE FROM role_perm WHERE role_id = #{roleId} AND is_deleted = 1")
    int cleanSoftDeletedByRoleId(@Param("roleId") Long roleId);
}




