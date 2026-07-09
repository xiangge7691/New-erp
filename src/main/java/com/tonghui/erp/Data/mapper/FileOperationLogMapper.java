package com.tonghui.erp.Data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tonghui.erp.Data.Entity.FileOperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件操作日志Mapper接口
 */
@Mapper
public interface FileOperationLogMapper extends BaseMapper<FileOperationLog> {
}
