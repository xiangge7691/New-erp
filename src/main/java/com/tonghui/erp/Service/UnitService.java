package com.tonghui.erp.Service;

import com.tonghui.erp.Data.Entity.Unit;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;

/**
 * 计量单位服务接口
 * <p>
 * 针对表【unit(计量单位表)】的数据库操作Service接口，提供计量单位的增删改查等业务逻辑接口
 * </p>
 */
public interface UnitService extends IService<Unit> {
    
    // region 计量单位查询方法
    // ===================================
    // 计量单位查询方法
    // ===================================
    
    /**
     * 根据计量单位名称模糊查询（分页）
     * 
     * @param unitName 计量单位名称（模糊匹配），为空时查询所有
     * @param pageRequest 分页参数，包含页码和每页数量等信息
     * @return 分页结果，包含查询到的计量单位列表和分页信息
     */
    PagedResult<Unit> searchByName(String unitName, PageRequestDto pageRequest);

    /**
     * 清理指定单位名称下已被软删除的记录（释放唯一键约束）
     *
     * @param unitName 单位名称
     * @return 清理的记录数
     */
    int cleanSoftDeletedByUnitName(String unitName);

    /**
     * 根据计量单位符号查询未删除的计量单位
     * <p>注意：数据库唯一索引建在 symbol 列上（不是 unit_name）</p>
     *
     * @param symbol 计量单位符号（如 kg、L）
     * @return 计量单位实体，不存在返回null
     */
    Unit getBySymbol(String symbol);

    /**
     * 清理指定单位符号下已被软删除的记录（物理删除，释放唯一索引 uk symbol）
     *
     * @param symbol 计量单位符号
     * @return 清理的记录数
     */
    int cleanSoftDeletedBySymbol(String symbol);

    // endregion
}
