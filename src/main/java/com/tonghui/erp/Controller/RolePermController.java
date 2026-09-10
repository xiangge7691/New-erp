package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.RolePerm;
import com.tonghui.erp.Service.RolePermService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 角色权限关联控制器
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────────┬────────┬─────────────────────────────────────┐
 * │ #  │ 接口                                 │ 方法   │ 说明                                │
 * ├────┼──────────────────────────────────────┼────────┼─────────────────────────────────────┤
 * │ 1  │ /api/RolePermission                  │ GET   │ 获取所有角色权限关联（分页）        │
 * │ 2  │ /api/RolePermission/{id}             │ GET   │ 根据ID获取角色权限关联详情          │
 * │ 3  │ /api/RolePermission                  │ POST  │ 新增角色权限关联                    │
 * │ 4  │ /api/RolePermission/{id}             │ PUT   │ 修改角色权限关联                    │
 * │ 5  │ /api/RolePermission/{id}             │ DELETE│ 删除角色权限关联                    │
 * │ 6  │ /api/RolePermission/search           │ GET   │ 查询角色权限关联（支持分页）        │
 * └────┴──────────────────────────────────────┴────────┴─────────────────────────────────────┘
 *
 * 说明：
 * - 处理角色与权限的关联关系管理
 * - 支持CRUD操作和分页查询
 */
@RestController
@RequestMapping("/api/RolePermission")
public class RolePermController extends BaseCrudController<RolePerm, RolePerm, Long> {

    // region 字段和构造方法
    // ===================================
    // 字段和构造方法
    // ===================================
    
    private final RolePermService rolePermService;

    @Autowired
    public RolePermController(RolePermService rolePermService) {
        this.rolePermService = rolePermService;
    }
    
    // endregion

    // region CRUD操作实现方法
    // ===================================
    // CRUD操作实现方法
    // ===================================
    
    /**
     * 获取所有角色权限关联（分页）
     *
     * 示例请求：
     * GET /api/RolePermission?pageIndex=0&pageSize=10
     *
     * @param pageIndex 页码，从0开始
     * @param pageSize 每页大小
     * @return 分页结果，包含角色权限关联列表
     */
    @Override
    protected PagedResult<RolePerm> getAllData(int pageIndex, int pageSize) {
        return rolePermService.getPaged(pageIndex, pageSize);
    }

    /**
     * 根据ID获取角色权限关联详情
     *
     * 示例请求：
     * GET /api/RolePermission/1
     *
     * @param id 角色权限关联ID
     * @return 角色权限关联详情
     */
    @Override
    protected RolePerm getDataById(Long id) {
        return rolePermService.getById(id);
    }

    /**
     * 新增角色权限关联
     *
     * 示例请求：
     * POST /api/RolePermission
     * Content-Type: application/json
     * {
     *   "roleId": 1,
     *   "permId": 1
     * }
     *
     * @param rolePerm 角色权限关联实体对象
     * @return 新增的角色权限关联
     */
    @Override
    protected RolePerm doCreate(RolePerm rolePerm) {
        boolean result = rolePermService.save(rolePerm);
        
        if (!result) {
            throw new RuntimeException("创建角色权限关联失败");
        }
        
        return rolePerm;
    }

    /**
     * 修改角色权限关联
     *
     * 示例请求：
     * PUT /api/RolePermission/1
     * Content-Type: application/json
     * {
     *   "roleId": 1,
     *   "permId": 2
     * }
     *
     * @param id 角色权限关联ID
     * @param rolePerm 角色权限关联实体对象
     * @return 修改后的角色权限关联
     */
    @Override
    protected RolePerm doUpdate(Long id, RolePerm rolePerm) {
        // 获取现有角色权限关联
        RolePerm existingRolePerm = rolePermService.getById(id);
        if (existingRolePerm == null) {
            throw new RuntimeException("角色权限关联不存在");
        }

        rolePerm.setId(id);
        boolean result = rolePermService.updateById(rolePerm);
        
        if (!result) {
            throw new RuntimeException("更新角色权限关联失败");
        }
        
        return rolePerm;
    }

    /**
     * 删除角色权限关联
     *
     * 示例请求：
     * DELETE /api/RolePermission/1
     *
     * @param id 角色权限关联ID
     * @return 删除结果
     */
    @Override
    protected boolean doDelete(Long id) {
        // 获取现有角色权限关联
        RolePerm existingRolePerm = rolePermService.getById(id);
        if (existingRolePerm == null) {
            throw new RuntimeException("角色权限关联不存在");
        }

        return rolePermService.removeById(id);
    }
    
    // endregion

    // region 角色权限关联查询接口方法
    // ===================================
    // 角色权限关联查询接口方法
    // ===================================
    
    /**
     * 查询角色权限关联（支持分页）
     *
     * 示例请求：
     * GET /api/RolePermission/search?pageIndex=0&pageSize=10
     *
     * @param pageRequest 分页请求参数
     * @return 符合条件的角色权限关联列表
     */
    @GetMapping("/search")
    public PagedResult<RolePerm> searchRolePerms(@ModelAttribute PageRequestDto pageRequest) {
        pageRequest = processPageRequest(pageRequest);
        
        // 如果pageSize为-1，则获取所有角色权限关联
        if (pageRequest.getPageSize() == -1) {
            PagedResult<RolePerm> allRolePerms = rolePermService.getPaged(0, Integer.MAX_VALUE);
            return processAllDataResult(allRolePerms);
        }
        
        return rolePermService.getPaged(
            pageRequest.getPageIndex(), 
            pageRequest.getPageSize());
    }
    
    // endregion
}