package com.tonghui.erp.Data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tonghui.erp.Data.Entity.SysOperationLog;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 操作日志Mapper接口
 */
@Mapper
public interface SysOperationLogMapper extends BaseMapper<SysOperationLog> {

    /**
     * 物理删除指定时间之前的操作日志（分批清理）
     * <p>
     * 日志表没有 is_deleted 字段，仍使用原生 DELETE，避免全局逻辑删除配置带来歧义
     * </p>
     *
     * @param before 截止时间（早于该时间的日志被删除）
     * @param limit  单批最大删除条数
     * @return 实际删除条数
     */
    @Delete("DELETE FROM sys_operation_log WHERE created_time < #{before} LIMIT #{limit}")
    int deleteBefore(@Param("before") LocalDateTime before, @Param("limit") int limit);
}
