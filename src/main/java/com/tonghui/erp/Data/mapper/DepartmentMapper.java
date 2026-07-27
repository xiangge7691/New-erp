package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Data.Entity.Department;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 部门数据访问Mapper接口
 */
public interface DepartmentMapper extends BaseMapper<Department> {

    /**
     * 根据部门名称物理删除已软删除的记录（释放唯一键约束）
     *
     * @param departmentName 部门名称
     * @return 删除的记录数
     */
    @Delete("DELETE FROM department WHERE department_name = #{departmentName} AND is_deleted = 1")
    int physicalDeleteByDepartmentName(@Param("departmentName") String departmentName);
}




