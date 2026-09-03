package com.tonghui.erp.Data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tonghui.erp.Data.Entity.FileOperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 文件操作日志Mapper接口
 */
@Mapper
public interface FileOperationLogMapper extends BaseMapper<FileOperationLog> {

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
