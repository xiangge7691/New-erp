package com.tonghui.erp.Data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tonghui.erp.Data.Entity.PerformanceCoefficientVersion;
import org.apache.ibatis.annotations.Select;

/**
 * 绩效系数版本数据访问Mapper接口
 */
public interface PerformanceCoefficientVersionMapper extends BaseMapper<PerformanceCoefficientVersion> {

    /**
     * 查询历史最大版本号（包含软删除记录，避免版本号唯一键冲突）
     */
    @Select("SELECT COALESCE(MAX(CAST(SUBSTRING(version_no, 2) AS UNSIGNED)), 0) "
            + "FROM performance_coefficient_version WHERE version_no LIKE 'V%'")
    Integer maxVersionNumber();
}
