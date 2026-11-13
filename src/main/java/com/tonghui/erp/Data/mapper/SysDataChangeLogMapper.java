package com.tonghui.erp.Data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tonghui.erp.Data.Entity.SysDataChangeLog;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据变更日志Mapper接口
 */
@Mapper
public interface SysDataChangeLogMapper extends BaseMapper<SysDataChangeLog> {

    /**
     * 查询指定表与业务主键集合的变更历史
     * <p>
     * 只查询时间线展示需要的字段，不加载 before_json / after_json 大字段
     * </p>
     *
     * @param tableName   表名
     * @param businessIds 业务主键集合
     * @param startTime   起始时间（可为空）
     * @param endTime     截止时间（可为空）
     * @param limit       最大返回条数
     * @return 变更日志列表（按时间倒序）
     */
    @Select("<script>"
            + "SELECT id, trace_id, operation_log_id, table_name, entity_class, business_id,"
            + " change_type, change_source, changed_fields, field_count,"
            + " user_id, user_account, user_name, created_time"
            + " FROM sys_data_change_log"
            + " WHERE table_name = #{tableName}"
            + " AND business_id IN"
            + " <foreach collection='businessIds' item='businessId' open='(' separator=',' close=')'>#{businessId}</foreach>"
            + " <if test='startTime != null'> AND created_time &gt;= #{startTime}</if>"
            + " <if test='endTime != null'> AND created_time &lt;= #{endTime}</if>"
            + " ORDER BY created_time DESC, id DESC"
            + " LIMIT #{limit}"
            + "</script>")
    List<SysDataChangeLog> selectHistory(@Param("tableName") String tableName,
                                         @Param("businessIds") List<String> businessIds,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime,
                                         @Param("limit") int limit);

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
