package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.UserRole;
import com.tonghui.erp.Service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户角色关联管理控制器
 * <p>
 * 处理用户与角色关联关系的HTTP请求，提供RESTful API接口，包括用户角色关联的增删改查操作
 * </p>
 */
@RestController
@RequestMapping("/api/UserRole")
public class UserRoleController extends BaseCrudController<UserRole, UserRole, Long> {

    //#region 字段和构造方法
    // ===================================
    // 字段和构造方法
    // ===================================
    
    private final UserRoleService userRoleService;

    @Autowired
    public UserRoleController(UserRoleService userRoleService) {
        this.userRoleService = userRoleService;
    }
    
    //#endregion

    //#region CRUD操作实现方法
    // ===================================
    // CRUD操作实现方法
    // ===================================
    
    @Override
    protected PagedResult<UserRole> getAllData(int pageIndex, int pageSize) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<UserRole> page = 
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageIndex + 1, pageSize);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<UserRole> resultPage = 
            userRoleService.page(page);

        PagedResult<UserRole> pagedResult = new PagedResult<>();
        pagedResult.setItems(resultPage.getRecords());
        pagedResult.setTotalCount(resultPage.getTotal());
        pagedResult.setPageIndex(pageIndex);
        pagedResult.setPageSize((int) resultPage.getSize());
        
        return pagedResult;
    }

    @Override
    protected UserRole getDataById(Long id) {
        return userRoleService.getById(id);
    }

    @Override
    protected UserRole doCreate(UserRole userRole) {
        boolean result = userRoleService.save(userRole);
        
        if (!result) {
            throw new RuntimeException("创建用户角色关联失败");
        }
        
        return userRole;
    }

    @Override
    protected UserRole doUpdate(Long id, UserRole userRole) {
        // 获取现有用户角色关联
        UserRole existingUserRole = userRoleService.getById(id);
        if (existingUserRole == null) {
            throw new RuntimeException("用户角色关联不存在");
        }

        userRole.setRoleId(id);
        boolean result = userRoleService.updateById(userRole);
        
        if (!result) {
            throw new RuntimeException("更新用户角色关联失败");
        }
        
        return userRole;
    }

    @Override
    protected boolean doDelete(Long id) {
        // 获取现有用户角色关联
        UserRole existingUserRole = userRoleService.getById(id);
        if (existingUserRole == null) {
            throw new RuntimeException("用户角色关联不存在");
        }

        return userRoleService.removeById(id);
    }
    
    //#endregion
}
