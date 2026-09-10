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
 *
 * 接口清单：
 * ┌────┬──────────────────────────────┬────────┬──────────────────────────────┐
 * │ #  │ 接口                         │ 方法   │ 说明                         │
 * ├────┼──────────────────────────────┼────────┼──────────────────────────────┤
 * │ 1  │ /api/UserRole                │ GET    │ 获取所有用户角色关联（分页） │
 * │ 2  │ /api/UserRole/{id}           │ GET    │ 根据ID获取用户角色关联详情   │
 * │ 3  │ /api/UserRole                │ POST   │ 新增用户角色关联             │
 * │ 4  │ /api/UserRole/{id}           │ PUT    │ 修改用户角色关联             │
 * │ 5  │ /api/UserRole/{id}           │ DELETE │ 删除用户角色关联             │
 * └────┴──────────────────────────────┴────────┴──────────────────────────────┘
 */
@RestController
@RequestMapping("/api/UserRole")
public class UserRoleController extends BaseCrudController<UserRole, UserRole, Long> {

    // region 字段和构造方法
    // ===================================
    // 字段和构造方法
    // ===================================
    
    /**
     * 用户角色关联服务
     */
    private final UserRoleService userRoleService;

    /**
     * 构造方法，注入用户角色关联服务
     *
     * @param userRoleService 用户角色关联服务
     */
    @Autowired
    public UserRoleController(UserRoleService userRoleService) {
        this.userRoleService = userRoleService;
    }
    
    // endregion

    // region CRUD操作实现方法
    // ===================================
    // CRUD操作实现方法
    // ===================================
    
    /**
     * 获取所有用户角色关联（分页）
     *
     * 示例请求：
     * GET /api/UserRole?pageIndex=0&pageSize=10
     *
     * @param pageIndex 页码，从0开始
     * @param pageSize 每页大小
     * @return 分页结果，包含用户角色关联列表
     */
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

    /**
     * 根据ID获取用户角色关联详情
     *
     * 示例请求：
     * GET /api/UserRole/1
     *
     * @param id 用户角色关联ID
     * @return 用户角色关联详情
     */
    @Override
    protected UserRole getDataById(Long id) {
        return userRoleService.getById(id);
    }

    /**
     * 新增用户角色关联
     *
     * 示例请求：
     * POST /api/UserRole
     * Content-Type: application/json
     * {
     *   "userId": 1,
     *   "roleId": 2
     * }
     *
     * @param userRole 用户角色关联实体对象
     * @return 新增的用户角色关联
     */
    @Override
    protected UserRole doCreate(UserRole userRole) {
        boolean result = userRoleService.save(userRole);
        
        if (!result) {
            throw new RuntimeException("创建用户角色关联失败");
        }
        
        return userRole;
    }

    /**
     * 修改用户角色关联
     *
     * 示例请求：
     * PUT /api/UserRole/1
     * Content-Type: application/json
     * {
     *   "userId": 1,
     *   "roleId": 3
     * }
     *
     * @param id 用户角色关联ID
     * @param userRole 用户角色关联实体对象
     * @return 修改后的用户角色关联
     */
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

    /**
     * 删除用户角色关联
     *
     * 示例请求：
     * DELETE /api/UserRole/1
     *
     * @param id 用户角色关联ID
     * @return 删除结果
     */
    @Override
    protected boolean doDelete(Long id) {
        // 获取现有用户角色关联
        UserRole existingUserRole = userRoleService.getById(id);
        if (existingUserRole == null) {
            throw new RuntimeException("用户角色关联不存在");
        }

        return userRoleService.removeById(id);
    }
    
    // endregion
}
