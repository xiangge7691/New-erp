package com.tonghui.erp.Data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tonghui.erp.Data.Entity.Organization;
import org.apache.ibatis.annotations.Mapper;

/**
 * 机构信息Mapper接口
 */
@Mapper
public interface OrganizationMapper extends BaseMapper<Organization> {
}
