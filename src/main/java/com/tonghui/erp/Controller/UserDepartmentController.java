package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.UserDepartment;
import com.tonghui.erp.Service.UserDepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户部门关联管理控制器
 * <p>
 * 处理用户与部门关联关系的HTTP请求，提供RESTful API接口，包括用户部门关联的增删改查操作
 * </p>
 */
@RestController
@RequestMapping("/api/UserDepartment")
public class UserDepartmentController extends BaseCrudController<UserDepartment, UserDepartment, Long> {

    //#region 字段和构造方法
    // ===================================
    // 字段和构造方法
    // ===================================
    
    private final UserDepartmentService userDepartmentService;

    @Autowired
    public UserDepartmentController(UserDepartmentService userDepartmentService) {
        this.userDepartmentService = userDepartmentService;
    }
    
    //#endregion

    //#region CRUD操作实现方法
    // ===================================
    // CRUD操作实现方法
    // ===================================
    
    @Override
    protected PagedResult<UserDepartment> getAllData(int pageIndex, int pageSize) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<UserDepartment> page = 
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageIndex + 1, pageSize);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<UserDepartment> resultPage = 
            userDepartmentService.page(page);

        PagedResult<UserDepartment> pagedResult = new PagedResult<>();
        pagedResult.setItems(resultPage.getRecords());
        pagedResult.setTotalCount(resultPage.getTotal());
        pagedResult.setPageIndex(pageIndex);
        pagedResult.setPageSize((int) resultPage.getSize());
        
        return pagedResult;
    }

    @Override
    protected UserDepartment getDataById(Long id) {
        return userDepartmentService.getById(id);
    }

    @Override
    protected UserDepartment doCreate(UserDepartment userDepartment) {
        boolean result = userDepartmentService.save(userDepartment);
        
        if (!result) {
            throw new RuntimeException("创建用户部门关联失败");
        }
        
        return userDepartment;
    }

    @Override
    protected UserDepartment doUpdate(Long id, UserDepartment userDepartment) {
        // 获取现有用户部门关联
        UserDepartment existingUserDepartment = userDepartmentService.getById(id);
        if (existingUserDepartment == null) {
            throw new RuntimeException("用户部门关联不存在");
        }

        userDepartment.setDepartmentId(id);
        boolean result = userDepartmentService.updateById(userDepartment);
        
        if (!result) {
            throw new RuntimeException("更新用户部门关联失败");
        }
        
        return userDepartment;
    }

    @Override
    protected boolean doDelete(Long id) {
        // 获取现有用户部门关联
        UserDepartment existingUserDepartment = userDepartmentService.getById(id);
        if (existingUserDepartment == null) {
            throw new RuntimeException("用户部门关联不存在");
        }

        return userDepartmentService.removeById(id);
    }
    
    //#endregion
}
