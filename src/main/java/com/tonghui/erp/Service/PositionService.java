package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.System.PositionWithDetailsDto;
import com.tonghui.erp.Data.Entity.Position;

/**
 * 岗位信息服务接口
 */
public interface PositionService extends IService<Position> {

    Page<Position> queryPositions(Position position, int pageNum, int pageSize);

    PagedResult<PositionWithDetailsDto> searchWithDetails(Position position, int pageNum, int pageSize);
}
