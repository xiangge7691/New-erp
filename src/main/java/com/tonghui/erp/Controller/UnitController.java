package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Data.Entity.Unit;
import com.tonghui.erp.Service.UnitService;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 计量单位管理控制器
 * <p>
 * 提供计量单位的增删改查和模糊查询功能
 * </p>
 */
@RestController
@RequestMapping("/api/Unit")
public class UnitController extends BaseCrudController<Unit, Unit, Long> {
    
    //#region 字段和构造方法
    // ===================================
    // 字段和构造方法
    // ===================================
    
    private final UnitService unitService;

    @Autowired
    public UnitController(UnitService unitService) {
        this.unitService = unitService;
    }
    
    //#endregion

    //#region CRUD操作实现方法
    // ===================================
    // CRUD操作实现方法
    // ===================================
    
    @Override
    protected PagedResult<Unit> getAllData(int pageIndex, int pageSize) {
        PageRequestDto pageRequest = new PageRequestDto();
        pageRequest.setPageIndex(pageIndex);
        pageRequest.setPageSize(pageSize);
        return unitService.searchByName(null, pageRequest);
    }

    @Override
    protected Unit getDataById(Long id) {
        return unitService.getById(id);
    }

    @Override
    protected Unit doCreate(Unit unit) {
        // 检查计量单位名称是否已存在
        LambdaQueryWrapper<Unit> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Unit::getUnitName, unit.getUnitName());
        if (unitService.getOne(queryWrapper) != null) {
            throw new RuntimeException("计量单位名称已存在");
        }

        // 添加计量单位到数据库
        boolean result = unitService.save(unit);
        
        if (!result) {
            throw new RuntimeException("创建计量单位失败");
        }
        
        return unit;
    }

    @Override
    protected Unit doUpdate(Long id, Unit unit) {
        // 检查计量单位名称是否被其他计量单位使用
        LambdaQueryWrapper<Unit> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Unit::getUnitName, unit.getUnitName());
        Unit unitWithSameName = unitService.getOne(queryWrapper);
        if (unitWithSameName != null && !unitWithSameName.getUnitId().equals(id)) {
            throw new RuntimeException("计量单位名称已存在");
        }

        // 更新计量单位信息
        unit.setUnitId(id);

        // 更新计量单位
        boolean result = unitService.updateById(unit);
        
        if (!result) {
            throw new RuntimeException("更新计量单位失败");
        }
        
        return unit;
    }

    @Override
    protected boolean doDelete(Long id) {
        // 删除计量单位
        return unitService.removeById(id);
    }
    
    //#endregion

    //#region 计量单位查询接口方法
    // ===================================
    // 计量单位查询接口方法
    // ===================================
    
    /**
     * 根据计量单位名称模糊查询计量单位
     * 支持按计量单位名称进行模糊查询和分页
     *
     * @param unitName 计量单位名称关键词
     * @param pageRequest 分页请求参数
     * @return 计量单位列表
     */
    @GetMapping("/search")
    public ApiResponse<PagedResult<Unit>> searchUnits(
            @RequestParam(required = false) String unitName,
            @ModelAttribute PageRequestDto pageRequest) {
        try {
            pageRequest = processPageRequest(pageRequest);
            PagedResult<Unit> result = unitService.searchByName(unitName, pageRequest);
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "搜索计量单位");
        }
    }
    
    //#endregion
}
