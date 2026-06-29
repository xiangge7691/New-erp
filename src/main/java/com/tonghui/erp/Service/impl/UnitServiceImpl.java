package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.Unit;
import com.tonghui.erp.Service.UnitService;
import com.tonghui.erp.Data.mapper.UnitMapper;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
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

    //#region 计量单位查询实现方法
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
        // 创建Page对象，处理全量数据的情况
        Page<Unit> page;
        if (pageRequest.getPageIndex() == -1 || pageRequest.getPageSize() == -1) {
            // 获取所有数据
            page = new Page<>(1, 10000);
        } else {
            // 页码从0开始，但MyBatis Plus的Page页码从1开始，所以需要+1
            page = new Page<>(pageRequest.getPageIndex() + 1, pageRequest.getPageSize());
        }
        
        // 构建查询条件
        var query = this.lambdaQuery();
        
        // 如果unitName不为空，则添加模糊查询条件
        if (unitName != null && !unitName.isEmpty()) {
            query.like(Unit::getUnitName, unitName);
        }
        
        Page<Unit> resultPage = query.page(page);
        
        PagedResult<Unit> pagedResult = new PagedResult<>();
        pagedResult.setItems(resultPage.getRecords());
        pagedResult.setTotalCount(resultPage.getTotal());
        
        // 处理分页信息
        if (pageRequest.getPageIndex() == -1 || pageRequest.getPageSize() == -1) {
            // 全量数据情况
            pagedResult.setPageIndex(0);
            if (resultPage.getTotal() > 0) {
                pagedResult.setPageSize((int) resultPage.getTotal());
            } else {
                pagedResult.setPageSize(0);
            }
        } else {
            // 分页情况，页码从0开始
            pagedResult.setPageIndex((int) resultPage.getCurrent() - 1);
            pagedResult.setPageSize((int) resultPage.getSize());
        }
        
        return pagedResult;
    }
    
    //#endregion
}




