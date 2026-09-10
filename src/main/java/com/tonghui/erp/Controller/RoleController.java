package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.System.RoleCreateDto;
import com.tonghui.erp.Common.Dto.System.RoleDto;
import com.tonghui.erp.Common.Dto.System.RoleWithDetailsDto;
import com.tonghui.erp.Data.Entity.Role;
import com.tonghui.erp.Service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 角色管理控制器
 * <p>
 * 处理角色相关的HTTP请求，提供RESTful API接口，包括角色的增删改查操作、
 * 高级查询、带子表查询等功能
 * </p>
 *
 * 接口清单：
 * ┌────┬────────────────────────────────┬────────┬──────────────────────────────┐
 * │ #  │ 接口                           │ 方法   │ 说明                         │
 * ├────┼────────────────────────────────┼────────┼──────────────────────────────┤
 * │ 1  │ /api/Role                      │ GET    │ 分页查询角色列表             │
 * │ 2  │ /api/Role/{id}                 │ GET    │ 获取角色详情                 │
 * │ 3  │ /api/Role                      │ POST   │ 新增角色                     │
 * │ 4  │ /api/Role/{id}                 │ PUT    │ 修改角色                     │
 * │ 5  │ /api/Role/{id}                 │ DELETE │ 删除角色                     │
 * │ 6  │ /api/Role/search               │ GET    │ 高级查询角色                 │
 * │ 7  │ /api/Role/search-with-details  │ GET    │ 带子表查询角色               │
 * └────┴────────────────────────────────┴────────┴──────────────────────────────┘
 */
@RestController
@RequestMapping("/api/Role")
public class RoleController extends BaseCrudController<RoleCreateDto, RoleDto, Long> {

    // region 字段和构造方法
    // ===================================
    // 字段和构造方法
    // ===================================
    
    private final RoleService roleService;

    @Autowired
    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }
    
    // endregion

    // region CRUD操作实现方法
    // ===================================
    // CRUD操作实现方法
    // ===================================

    /**
     * 获取所有角色数据（分页）
     *
     * @param pageIndex 页码
     * @param pageSize  每页大小
     * @return 分页结果
     */
    @Override
    protected PagedResult<RoleDto> getAllData(int pageIndex, int pageSize) {
        return roleService.advancedSearchRoles(null, null, null, null, null, pageIndex, pageSize);
    }

    /**
     * 根据ID获取角色
     *
     * @param id 角色ID
     * @return 角色详情
     */
    @Override
    protected RoleDto getDataById(Long id) {
        return roleService.getRoleDetails(id);
    }

    /**
     * 创建角色
     *
     * @param roleCreateDto 角色创建信息
     * @return 创建后的角色详情
     */
    @Override
    protected RoleDto doCreate(RoleCreateDto roleCreateDto) {
        // 检查角色名是否已存在
        Role existingRole = roleService.getOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Role>().eq("role_name", roleCreateDto.getRoleName()));
        if (existingRole != null) {
            throw new RuntimeException("角色名已存在");
        }

        // 清理已软删除的相同角色名记录（避免唯一键冲突）
        roleService.cleanSoftDeletedByRoleName(roleCreateDto.getRoleName());

        // 创建角色实体
        Role role = new Role();
        role.setRoleName(roleCreateDto.getRoleName());
        role.setRoleDesc(roleCreateDto.getRoleDesc());
        role.setRoleStatus(roleCreateDto.getStatus());

        // 添加角色到数据库
        boolean result = roleService.save(role);
        
        if (!result) {
            throw new RuntimeException("创建角色失败");
        }

        // 创建角色权限关联
        if (roleCreateDto.getPermissionIds() != null && !roleCreateDto.getPermissionIds().isEmpty()) {
            roleService.assignPermissionsToRole(role.getRoleId(), roleCreateDto.getPermissionIds());
        }

        // 获取完整的角色信息（包括权限）
        return roleService.getRoleDetails(role.getRoleId());
    }

    /**
     * 更新角色
     *
     * @param id              角色ID
     * @param roleUpdateDto   角色更新信息
     * @return 更新后的角色详情
     */
    @Override
    protected RoleDto doUpdate(Long id, RoleCreateDto roleUpdateDto) {
        // 获取现有角色
        Role existingRole = roleService.getById(id);
        if (existingRole == null) {
            throw new RuntimeException("角色不存在");
        }

        // 检查角色名是否被其他角色使用
        Role roleWithSameName = roleService.getOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Role>().eq("role_name", roleUpdateDto.getRoleName()));
        if (roleWithSameName != null && !roleWithSameName.getRoleId().equals(id)) {
            throw new RuntimeException("角色名已存在");
        }

        // 更新角色信息
        existingRole.setRoleName(roleUpdateDto.getRoleName());
        existingRole.setRoleDesc(roleUpdateDto.getRoleDesc());
        existingRole.setRoleStatus(roleUpdateDto.getStatus());

        // 更新角色
        boolean result = roleService.updateById(existingRole);
        
        if (!result) {
            throw new RuntimeException("更新角色失败");
        }

        // 更新角色权限关联
        // 当PermissionIds为null时不处理，当为[]空数组时清除所有权限关联
        if (roleUpdateDto.getPermissionIds() != null) {
            roleService.assignPermissionsToRole(id, roleUpdateDto.getPermissionIds());
        }

        // 获取完整的角色信息（包括权限）
        return roleService.getRoleDetails(id);
    }

    /**
     * 删除角色
     *
     * @param id 角色ID
     * @return 是否删除成功
     */
    @Override
    protected boolean doDelete(Long id) {
        // 获取现有角色
        Role existingRole = roleService.getById(id);
        if (existingRole == null) {
            throw new RuntimeException("角色不存在");
        }

        // 检查是否有用户关联到此角色
        // 注意：这个检查在实际的service中没有提供，我们暂时跳过这个检查
        // 如果需要实现，应该通过UserRoleService来检查

        // 删除角色相关的所有关联信息
        // 注意：这个功能在实际的service中没有提供，我们暂时跳过
        // 如果需要实现，应该通过RolePermService和UserRoleService来删除关联

        // 删除角色
        return roleService.removeById(id);
    }
    
    // endregion

    // region 角色查询接口方法
    // ===================================
    // 角色查询接口方法
    // ===================================
    
    /**
     * 查询角色（支持按名称、状态等条件查询）
     * <p>
     * 支持多条件组合查询和分页
     * </p>
     *
     * 示例请求：
     * GET /api/Role/search?pageIndex=1&pageSize=20&roleName=管理员&roleStatus=1
     *
     * @param roleName   角色名称关键词
     * @param roleStatus 角色状态
     * @param pageRequest 分页请求参数
     * @return 符合条件的角色列表
     */
    @GetMapping("/search")
    public ApiResponse<PagedResult<RoleDto>> searchRoles(
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) Integer roleStatus,
            @ModelAttribute PageRequestDto pageRequest) {
        try {
            pageRequest = processPageRequest(pageRequest);
            
            // 如果pageSize为-1，则获取所有角色
            if (pageRequest.getPageSize() == -1) {
                PagedResult<RoleDto> allRoles = roleService.advancedSearchRoles(
                    roleName, null, roleStatus, null, null,
                    0, 
                    -1);
                return success(processAllDataResult(allRoles));
            }
            
            PagedResult<RoleDto> result = roleService.advancedSearchRoles(
                roleName, null, roleStatus, null, null,
                pageRequest.getPageIndex(), 
                pageRequest.getPageSize());
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "搜索角色");
        }
    }
    
    // endregion

    // region 带子表查询接口
    // ===================================
    // 带子表查询接口
    // ===================================

    /**
     * 查询角色（带子表：权限、用户关联）
     *
     * 示例请求：
     * GET /api/Role/search-with-details?pageIndex=1&pageSize=20
     *
     * @param role        查询条件
     * @param pageRequest 分页请求参数
     * @return 分页结果（包含权限和用户关联信息）
     */
// @GetMapping("/search-with-details")
    public ApiResponse<PagedResult<RoleWithDetailsDto>> searchWithDetails(
            Role role,
            @ModelAttribute PageRequestDto pageRequest) {
        try {
            pageRequest = processPageRequest(pageRequest);
            PagedResult<RoleWithDetailsDto> result = roleService.searchWithDetails(
                role, pageRequest.getPageIndex(), pageRequest.getPageSize());
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "searchWithDetails");
        }
    }
    // endregion
}
