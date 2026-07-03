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
 *
 * 接口清单：
 * ┌────┬──────────────────────────────┬────────┬──────────────────────────────┐
 * │ #  │ 接口                         │ 方法   │ 说明                         │
 * ├────┼──────────────────────────────┼────────┼──────────────────────────────┤
 * │ 1  │ /api/UserDepartment          │ GET    │ 获取所有用户部门关联（分页） │
 * │ 2  │ /api/UserDepartment/{id}     │ GET    │ 根据ID获取用户部门关联详情   │
 * │ 3  │ /api/UserDepartment          │ POST   │ 新增用户部门关联             │
 * │ 4  │ /api/UserDepartment/{id}     │ PUT    │ 修改用户部门关联             │
 * │ 5  │ /api/UserDepartment/{id}     │ DELETE │ 删除用户部门关联             │
 * └────┴──────────────────────────────┴────────┴──────────────────────────────┘
 */
@RestController
@RequestMapping("/api/UserDepartment")
public class UserDepartmentController extends BaseCrudController<UserDepartment, UserDepartment, Long> {

    // region 字段和构造方法
    // ===================================
    // 字段和构造方法
    // ===================================
    
    /**
     * 用户部门关联服务
     */
    private final UserDepartmentService userDepartmentService;

    /**
     * 构造方法，注入用户部门关联服务
     *
     * @param userDepartmentService 用户部门关联服务
     */
    @Autowired
    public UserDepartmentController(UserDepartmentService userDepartmentService) {
        this.userDepartmentService = userDepartmentService;
    }
    
    // endregion

    // region CRUD操作实现方法
    // ===================================
    // CRUD操作实现方法
    // ===================================
    
    /**
     * 获取所有用户部门关联（分页）
     *
     * 示例请求：
     * GET /api/UserDepartment?pageIndex=0&pageSize=10
     *
     * @param pageIndex 页码，从0开始
     * @param pageSize 每页大小
     * @return 分页结果，包含用户部门关联列表
     */
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

    /**
     * 根据ID获取用户部门关联详情
     *
     * 示例请求：
     * GET /api/UserDepartment/1
     *
     * @param id 用户部门关联ID
     * @return 用户部门关联详情
     */
    @Override
    protected UserDepartment getDataById(Long id) {
        return userDepartmentService.getById(id);
    }

    /**
     * 新增用户部门关联
     *
     * 示例请求：
     * POST /api/UserDepartment
     * Content-Type: application/json
     * {
     *   "userId": 1,
     *   "departmentId": 2
     * }
     *
     * @param userDepartment 用户部门关联实体对象
     * @return 新增的用户部门关联
     */
    @Override
    protected UserDepartment doCreate(UserDepartment userDepartment) {
        boolean result = userDepartmentService.save(userDepartment);
        
        if (!result) {
            throw new RuntimeException("创建用户部门关联失败");
        }
        
        return userDepartment;
    }

    /**
     * 修改用户部门关联
     *
     * 示例请求：
     * PUT /api/UserDepartment/1
     * Content-Type: application/json
     * {
     *   "userId": 1,
     *   "departmentId": 3
     * }
     *
     * @param id 用户部门关联ID
     * @param userDepartment 用户部门关联实体对象
     * @return 修改后的用户部门关联
     */
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

    /**
     * 删除用户部门关联
     *
     * 示例请求：
     * DELETE /api/UserDepartment/1
     *
     * @param id 用户部门关联ID
     * @return 删除结果
     */
    @Override
    protected boolean doDelete(Long id) {
        // 获取现有用户部门关联
        UserDepartment existingUserDepartment = userDepartmentService.getById(id);
        if (existingUserDepartment == null) {
            throw new RuntimeException("用户部门关联不存在");
        }

        return userDepartmentService.removeById(id);
    }
    
    // endregion
}
