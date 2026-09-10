package com.tonghui.erp.Data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tonghui.erp.Data.Entity.CleaningRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 清洁记录数据访问接口
 * <p>
 * 提供清洁记录的MyBatis-Plus基础CRUD操作
 * </p>
 */
@Mapper
public interface CleaningRecordMapper extends BaseMapper<CleaningRecord> {
}
