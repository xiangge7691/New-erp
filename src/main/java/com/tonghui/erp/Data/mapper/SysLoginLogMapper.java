package com.tonghui.erp.Data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tonghui.erp.Data.Entity.SysLoginLog;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * 登录日志Mapper接口
 */
@Mapper
public interface SysLoginLogMapper extends BaseMapper<SysLoginLog> {

    /**
     * 物理删除指定时间之前的登录日志（分批清理）
     * <p>
     * 日志表没有 is_deleted 字段，仍使用原生 DELETE，避免全局逻辑删除配置带来歧义
     * </p>
     *
     * @param before 截止时间（早于该时间的日志被删除）
     * @param limit  单批最大删除条数
     * @return 实际删除条数
     */
    @Delete("DELETE FROM sys_login_log WHERE login_time < #{before} LIMIT #{limit}")
    int deleteBefore(@Param("before") LocalDateTime before, @Param("limit") int limit);

    /**
     * 统计指定时间范围内的登录总次数
     *
     * @param startTime 起始时间（含）
     * @param endTime   截止时间（含）
     * @return 登录总次数
     */
    @Select("SELECT COUNT(*) FROM sys_login_log WHERE login_time >= #{startTime} AND login_time <= #{endTime}")
    Long countByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 统计指定时间范围内的登录成功次数
     *
     * @param startTime 起始时间（含）
     * @param endTime   截止时间（含）
     * @return 登录成功次数
     */
    @Select("SELECT COUNT(*) FROM sys_login_log WHERE login_time >= #{startTime} AND login_time <= #{endTime} AND login_result = 1")
    Long countSuccessByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 统计指定时间范围内的登录失败次数
     *
     * @param startTime 起始时间（含）
     * @param endTime   截止时间（含）
     * @return 登录失败次数
     */
    @Select("SELECT COUNT(*) FROM sys_login_log WHERE login_time >= #{startTime} AND login_time <= #{endTime} AND login_result = 0")
    Long countFailByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 统计指定时间范围内的去重登录账号数
     *
     * @param startTime 起始时间（含）
     * @param endTime   截止时间（含）
     * @return 去重账号数
     */
    @Select("SELECT COUNT(DISTINCT user_account) FROM sys_login_log WHERE login_time >= #{startTime} AND login_time <= #{endTime}")
    Long countDistinctAccountByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}
