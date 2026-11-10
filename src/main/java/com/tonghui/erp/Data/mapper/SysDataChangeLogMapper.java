package com.tonghui.erp.Data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tonghui.erp.Data.Entity.SysDataChangeLog;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 数据变更日志Mapper接口
 */
@Mapper
public interface SysDataChangeLogMapper extends BaseMapper<SysDataChangeLog> {

    /**
     * 物理删除指定时间之前的变更日志（分批清理）
     *
     * @param before 截止时间（早于该时间的日志被删除）
     * @param limit  单批最大删除条数
     * @return 实际删除条数
     */
    @Delete("DELETE FROM sys_data_change_log WHERE created_time < #{before} LIMIT #{limit}")
    int deleteBefore(@Param("before") LocalDateTime before, @Param("limit") int limit);
}
