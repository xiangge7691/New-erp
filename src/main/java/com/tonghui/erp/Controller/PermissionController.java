package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.System.PermissionDto;
import com.tonghui.erp.Data.Entity.Permission;
import com.tonghui.erp.Service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 权限控制器
 * <p>
 * 处理权限相关的HTTP请求，提供RESTful API接口，包括权限的增删改查操作
 * </p>
 *
 * 接口清单：
 * ┌────┬────────────────────────────────┬────────┬──────────────────────────────┐
 * │ #  │ 接口                           │ 方法   │ 说明                         │
 * ├────┼────────────────────────────────┼────────┼──────────────────────────────┤
 * │ 1  │ /api/Permission                │ GET    │ 分页查询权限列表             │
 * │ 2  │ /api/Permission/{id}           │ GET    │ 获取权限详情                 │
 * │ 3  │ /api/Permission                │ POST   │ 新增权限                     │
 * │ 4  │ /api/Permission/{id}           │ PUT    │ 修改权限                     │
 * │ 5  │ /api/Permission/{id}           │ DELETE │ 删除权限                     │
 * │ 6  │ /api/Permission/search         │ GET    │ 按名称模糊搜索权限           │
 * └────┴────────────────────────────────┴────────┴──────────────────────────────┘
 */
@RestController
@RequestMapping("/api/Permission")
public class PermissionController extends BaseCrudController<Permission, PermissionDto, Long> {

    // region 字段和构造方法
    // ===================================
    // 字段和构造方法
    // ===================================
    
    private final PermissionService permissionService;

    @Autowired
    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }
    
    // endregion

    // region CRUD操作实现方法
    // ===================================
    // CRUD操作实现方法
    // ===================================
    
    @Override
    protected PagedResult<PermissionDto> getAllData(int pageIndex, int pageSize) {
        return permissionService.advancedSearchPermissions(null, null, null, null, null, pageIndex, pageSize);
    }

    @Override
    protected PermissionDto getDataById(Long id) {
        PagedResult<PermissionDto> result = permissionService.advancedSearchPermissions(
                null, null, id, null, null, 0, 1);
        java.util.List<PermissionDto> items = result.getItems();
        return items.isEmpty() ? null : items.get(0);
    }

    @Override
    protected PermissionDto doCreate(Permission permission) {
        // 检查权限键是否已存在
        Permission existingPermission = permissionService.getByPermKey(permission.getPermKey());
        if (existingPermission != null) {
            throw new RuntimeException("权限键已存在");
        }

        // 添加权限到数据库
        boolean result = permissionService.save(permission);
        
        if (!result) {
            throw new RuntimeException("创建权限失败");
        }
        
        // 获取完整的权限信息
        return permissionService.getPermissionDetails(permission.getPermId());
    }

    @Override
    protected PermissionDto doUpdate(Long id, Permission permission) {
        // 检查权限键是否被其他权限使用
        Permission permissionWithSameKey = permissionService.getByPermKey(permission.getPermKey());
        if (permissionWithSameKey != null && !permissionWithSameKey.getPermId().equals(id)) {
            throw new RuntimeException("权限键已存在");
        }

        // 获取database中的原始权限信息
        Permission originalPermission = permissionService.getById(id);
        if (originalPermission == null) {
            throw new RuntimeException("权限不存在");
        }

        // 更新权限信息
        permission.setPermId(id);
        
        // 特别处理parentId字段，当传入0时，应该将其设为空
        if (permission.getParentId() != null && permission.getParentId() == 0) {
            permission.setParentId(null);
        }
        
        // 使用UpdateWrapper确保所有字段正确更新
        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Permission> updateWrapper = 
            new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
        updateWrapper.eq("perm_id", id);
        
        updateWrapper.set("perm_key", permission.getPermKey() != null ? permission.getPermKey() : originalPermission.getPermKey());
        updateWrapper.set("perm_name",permission.getPermName() != null ?permission.getPermName() : originalPermission.getPermName());
        updateWrapper.set("perm_type",permission.getPermType() != null ?permission.getPermType() : originalPermission.getPermType());
        updateWrapper.set("parent_id",permission.getParentId()); // 直接使用permission.getParentId()
        updateWrapper.set("display_order",permission.getDisplayOrder() != null ?permission.getDisplayOrder() : originalPermission.getDisplayOrder());
        updateWrapper.set("perm_status",permission.getPermStatus() != null ?permission.getPermStatus() : originalPermission.getPermStatus());
        updateWrapper.set("updated_time", java.time.LocalDateTime.now());

        // 更新权限
        boolean result = permissionService.update(updateWrapper);
        
        if (!result) {
            throw new RuntimeException("更新权限失败");
        }
        
        // 获取完整的权限信息
        return permissionService.getPermissionDetails(id);
    }

    @Override
    protected boolean doDelete(Long id) {
        // 注意：PermissionService中没有提供hasRolePerms方法，暂时跳过检查
        // 如果需要实现，应该通过RolePermService来检查

        // 删除权限
        return permissionService.removeById(id);
    }
    
    // endregion

    // region 搜索与查询
    // ===================================
    // 搜索与查询
    // ===================================
    
    /**
     * 根据权限名称模糊查询权限
     * <p>
     * 支持按权限名称进行模糊查询和分页，当pageSize为-1时返回所有权限
     * </p>
     *
     * 示例请求：
     * GET /api/Permission/search?pageIndex=0&pageSize=20&permissionName=查看
     *
     * @param permissionName 权限名称关键词（可选）
     * @param pageRequest 分页请求参数
     * @return ApiResponse&lt;PagedResult&lt;PermissionDto&gt;&gt; 权限分页列表
     */
    @GetMapping("/search")
    public ApiResponse<PagedResult<PermissionDto>> searchPermissions(
            @RequestParam(required = false) String permissionName,
            @ModelAttribute PageRequestDto pageRequest) {
        try {
            pageRequest = processPageRequest(pageRequest);
            
            // 如果pageSize为-1，则获取所有权限
            if (pageRequest.getPageSize() == -1) {
                PagedResult<PermissionDto> allPermissions = permissionService.advancedSearchPermissions(
                        permissionName, null, null, null, null,
                        0,
                        -1);
                return success(processAllDataResult(allPermissions));
            }
            
            PagedResult<PermissionDto> result = permissionService.advancedSearchPermissions(
                    permissionName, null, null, null, null,
                    pageRequest.getPageIndex(),
                    pageRequest.getPageSize());
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "搜索权限");
        }
    }
    
    // endregion
}
