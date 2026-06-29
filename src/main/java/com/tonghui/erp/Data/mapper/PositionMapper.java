package com.tonghui.erp.Data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tonghui.erp.Data.Entity.Position;
import org.apache.ibatis.annotations.Mapper;

/**
 * 岗位信息Mapper接口
 */
@Mapper
public interface PositionMapper extends BaseMapper<Position> {
}
