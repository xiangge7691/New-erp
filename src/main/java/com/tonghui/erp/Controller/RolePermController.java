package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.RolePerm;
import com.tonghui.erp.Service.RolePermService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 角色权限关联控制器
 * <p>
 * 处理角色权限关联相关的HTTP请求，提供RESTful API接口，包括角色权限关联的增删改查操作
 * </p>
 */
@RestController
@RequestMapping("/api/RolePermission")
public class RolePermController extends BaseCrudController<RolePerm, RolePerm, Long> {

    //#region 字段和构造方法
    // ===================================
    // 字段和构造方法
    // ===================================
    
    private final RolePermService rolePermService;

    @Autowired
    public RolePermController(RolePermService rolePermService) {
        this.rolePermService = rolePermService;
    }
    
    //#endregion

    //#region CRUD操作实现方法
    // ===================================
    // CRUD操作实现方法
    // ===================================
    
    @Override
    protected PagedResult<RolePerm> getAllData(int pageIndex, int pageSize) {
        return rolePermService.getPaged(pageIndex, pageSize);
    }

    @Override
    protected RolePerm getDataById(Long id) {
        return rolePermService.getById(id);
    }

    @Override
    protected RolePerm doCreate(RolePerm rolePerm) {
        boolean result = rolePermService.save(rolePerm);
        
        if (!result) {
            throw new RuntimeException("创建角色权限关联失败");
        }
        
        return rolePerm;
    }

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

    @Override
    protected boolean doDelete(Long id) {
        // 获取现有角色权限关联
        RolePerm existingRolePerm = rolePermService.getById(id);
        if (existingRolePerm == null) {
            throw new RuntimeException("角色权限关联不存在");
        }

        return rolePermService.removeById(id);
    }
    
    //#endregion

    //#region 角色权限关联查询接口方法
    // ===================================
    // 角色权限关联查询接口方法
    // ===================================
    
    /**
     * 查询角色权限关联（支持分页）
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
    
    //#endregion
}
