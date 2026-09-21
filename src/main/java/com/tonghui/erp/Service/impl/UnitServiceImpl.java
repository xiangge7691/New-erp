package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.Unit;
import com.tonghui.erp.Service.UnitService;
import com.tonghui.erp.Data.mapper.UnitMapper;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.utils.SoftDeleteCleanHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 计量单位服务实现类
 * <p>
 * 针对表【unit(计量单位表)】的数据库操作Service实现，提供计量单位的增删改查等业务逻辑的具体实现
 * </p>
 */
@Service
public class UnitServiceImpl extends ServiceImpl<UnitMapper, Unit>
    implements UnitService{

    /** 软删除统一清理工具 */
    @Autowired
    private SoftDeleteCleanHelper softDeleteCleanHelper;

    // region 数据清理接口
    // ===================================
    // 数据清理接口
    // ===================================

    /**
     * 清理指定单位名称下已被软删除的记录（释放唯一键约束）
     *
     * @param unitName 单位名称
     * @return 清理的记录数
     */
    public int cleanSoftDeletedByUnitName(String unitName) {
        return baseMapper.physicalDeleteByUnitName(unitName);
    }

    // endregion

    // region 计量单位查询实现方法
    // ===================================
    // 计量单位查询实现方法
    // ===================================
    
    /**
     * 根据计量单位名称模糊查询（分页）
     * 
     * @param unitName 计量单位名称（模糊匹配），为空时查询所有
     * @param pageRequest 分页参数，包含页码和每页数量等信息
     * @return 分页结果，包含查询到的计量单位列表和分页信息
     */
    @Override
    public PagedResult<Unit> searchByName(String unitName, PageRequestDto pageRequest) {
        Page<Unit> page = PagedResult.toMybatisPage(pageRequest);
        
        var query = this.lambdaQuery();
        
        if (unitName != null && !unitName.isEmpty()) {
            query.like(Unit::getUnitName, unitName);
        }
        
        Page<Unit> resultPage = query.page(page);
        return PagedResult.fromPage(resultPage, pageRequest);
    }
    
    // endregion
}




