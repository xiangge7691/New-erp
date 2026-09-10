package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Data.Entity.Permission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 权限数据访问Mapper接口
 */
public interface PermissionMapper extends BaseMapper<Permission> {

    /**
     * 根据权限键物理删除已软删除的记录（释放唯一键约束）
     *
     * @param permKey 权限键
     * @return 删除的记录数
     */
    @Delete("DELETE FROM permission WHERE perm_key = #{permKey} AND is_deleted = 1")
    int physicalDeleteByPermKey(@Param("permKey") String permKey);
}




