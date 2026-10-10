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

    // region 查询操作
    // ===================================
    // 查询操作
    // ===================================

    Page<Position> queryPositions(Position position, int pageNum, int pageSize);

    PagedResult<PositionWithDetailsDto> searchWithDetails(Position position, int pageNum, int pageSize);

    // endregion

    // region 唯一性校验与软删除清理
    // ===================================
    // 唯一性校验与软删除清理
    // ===================================

    /**
     * 按岗位编码查询未删除的岗位
     *
     * @param positionCode 岗位编码
     * @return 岗位实体，不存在返回null
     */
    Position getByPositionCode(String positionCode);

    /**
     * 清理指定岗位编码下已被软删除的记录（物理删除，释放唯一键约束）
     *
     * @param positionCode 岗位编码
     * @return 清理的记录数
     */
    int cleanSoftDeletedByPositionCode(String positionCode);

    // endregion
}
