package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.Position;
import com.tonghui.erp.Data.mapper.PositionMapper;
import com.tonghui.erp.Service.PositionService;
import org.springframework.stereotype.Service;

/**
 * 岗位信息服务实现类
 */
@Service
public class PositionServiceImpl extends ServiceImpl<PositionMapper, Position> implements PositionService {
}
