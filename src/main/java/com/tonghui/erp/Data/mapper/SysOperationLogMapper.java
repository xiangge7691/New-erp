package com.tonghui.erp.Data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tonghui.erp.Data.Entity.SysOperationLog;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 操作日志Mapper接口
 */
@Mapper
public interface SysOperationLogMapper extends BaseMapper<SysOperationLog> {

    /**
     * 按ID批量查询操作日志摘要
     * <p>
     * 只返回业务历史时间线需要展示的字段，不加载请求参数等大字段
     * </p>
     *
     * @param ids 操作日志ID集合
     * @return 操作日志摘要
     */
    @Select("<script>"
            + "SELECT id, user_id, user_account, user_name, module, operation_type, operation_desc,"
            + " business_type, business_id, client_ip, ip_location, mac_address, created_time"
            + " FROM sys_operation_log WHERE id IN"
            + " <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + "</script>")
    List<SysOperationLog> selectBriefByIds(@Param("ids") Collection<Long> ids);

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
