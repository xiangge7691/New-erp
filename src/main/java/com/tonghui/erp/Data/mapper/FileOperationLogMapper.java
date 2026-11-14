package com.tonghui.erp.Data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tonghui.erp.Common.Dto.Log.BusinessHistoryScope;
import com.tonghui.erp.Data.Entity.FileOperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件操作日志Mapper接口
 */
@Mapper
public interface FileOperationLogMapper extends BaseMapper<FileOperationLog> {

    /**
     * 查询业务对象的附件上传 / 删除记录
     * <p>
     * 业务附件日志在 detail 中记录 bizType、bizId 与槽位，按该标记匹配；
     * 只取 UPLOAD / DELETE，下载与预览不进入变更历史
     * </p>
     *
     * @param fileScopes 附件业务范围
     * @param startTime  起始时间（可为空）
     * @param endTime    截止时间（可为空）
     * @param limit      最大返回条数
     * @return 文件操作日志（按时间倒序）
     */
    @Select("<script>"
            + "SELECT id, file_id, file_name, file_path, operation_type, root_type, business_type,"
            + " user_id, user_name, detail, created_time"
            + " FROM file_operation_log"
            + " WHERE root_type = 'business'"
            + " AND operation_type IN ('UPLOAD','DELETE')"
            + " <if test='startTime != null'> AND created_time &gt;= #{startTime}</if>"
            + " <if test='endTime != null'> AND created_time &lt;= #{endTime}</if>"
            + " AND ("
            + " <foreach collection='fileScopes' item='scope' separator=' OR '>"
            + " detail LIKE CONCAT('%bizType=', #{scope.bizType}, ', bizId=', #{scope.bizId}, ',%')"
            + " </foreach>"
            + " )"
            + " ORDER BY created_time DESC, id DESC"
            + " LIMIT #{limit}"
            + "</script>")
    List<FileOperationLog> selectBusinessFileHistory(@Param("fileScopes") List<BusinessHistoryScope.FileScope> fileScopes,
                                                     @Param("startTime") LocalDateTime startTime,
                                                     @Param("endTime") LocalDateTime endTime,
                                                     @Param("limit") int limit);

    /**
     * 根据文件名查询指定操作类型的最近一条操作日志
     * <p>
     * 用于回收站文件夹条目补充删除人信息：文件夹删除时 file_id 为 null，
     * 操作日志中 file_name 记录的是文件夹名，file_path 记录的是删除前相对路径
     * </p>
     *
     * @param fileName      文件/文件夹名称
     * @param operationType 操作类型（如 DELETE）
     * @return 最近一条操作日志，无匹配时返回 null
     */
    @Select("SELECT * FROM file_operation_log WHERE file_name = #{fileName} AND operation_type = #{operationType} ORDER BY created_time DESC LIMIT 1")
    FileOperationLog selectLatestByNameAndType(@Param("fileName") String fileName, @Param("operationType") String operationType);
}
