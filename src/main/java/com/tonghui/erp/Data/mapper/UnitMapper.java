package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Data.Entity.Unit;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 单位数据访问Mapper接口
 */
public interface UnitMapper extends BaseMapper<Unit> {

    /**
     * 根据单位名称物理删除已软删除的记录（释放唯一键约束）
     *
     * @param unitName 单位名称
     * @return 删除的记录数
     */
    @Delete("DELETE FROM unit WHERE unit_name = #{unitName} AND is_deleted = 1")
    int physicalDeleteByUnitName(@Param("unitName") String unitName);
}




