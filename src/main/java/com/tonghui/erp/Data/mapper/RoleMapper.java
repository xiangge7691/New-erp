package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Data.Entity.Role;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 角色数据访问Mapper接口
 */
public interface RoleMapper extends BaseMapper<Role> {

    /**
     * 根据角色名称物理删除已软删除的记录（释放唯一键约束）
     *
     * @param roleName 角色名称
     * @return 删除的记录数
     */
    @Delete("DELETE FROM role WHERE role_name = #{roleName} AND is_deleted = 1")
    int physicalDeleteByRoleName(@Param("roleName") String roleName);
}




