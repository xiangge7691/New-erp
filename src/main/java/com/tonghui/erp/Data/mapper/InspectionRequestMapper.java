package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Data.Entity.InspectionRequest;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 请检记录数据访问Mapper接口
 */
public interface InspectionRequestMapper extends BaseMapper<InspectionRequest> {

    /**
     * 根据ID物理删除请检记录
     *
     * @param id 请检记录ID
     * @return 删除的记录数
     */
    @Delete("DELETE FROM inspection_request WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);
}
