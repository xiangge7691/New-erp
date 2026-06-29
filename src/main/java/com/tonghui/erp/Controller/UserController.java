package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.System.UserCreateDto;
import com.tonghui.erp.Common.Dto.System.UserDto;
import com.tonghui.erp.Common.Dto.System.UserUpdateDto;
import com.tonghui.erp.Data.Entity.User;
import com.tonghui.erp.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理控制器
 * <p>
 * 提供用户相关的RESTful API接口，包括用户的增删改查、角色和部门分配等操作
 * </p>
 */
@RestController
@RequestMapping("/api/User")
public class UserController extends BaseCrudController<UserCreateDto, UserDto, Long> {

    //#region 字段和构造方法
    // ===================================
    // 字段和构造方法
    // ===================================
    
    private UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    //#endregion

    //#region CRUD操作实现方法
    // ===================================
    // CRUD操作实现方法
    // ===================================
    
    @Override
    protected PagedResult<UserDto> getAllData(int pageIndex, int pageSize) {
        return userService.advancedSearchUsers(null, null, null, null, null, pageIndex, pageSize);
    }

    @Override
    protected UserDto getDataById(Long id) {
        PagedResult<UserDto> searchResult = userService.advancedSearchUsers(
            null, null, null, null, id, 0, 1);
        List<UserDto> items = searchResult.getItems();
        return items.isEmpty() ? null : items.get(0);
    }

    @Override
    protected UserDto doCreate(UserCreateDto userCreateDto) {
        // 检查用户名是否已存在（使用精确查询）
        // 创建一个QueryWrapper来进行精确查询
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_account", userCreateDto.getUserName());
        if (userService.count(queryWrapper) > 0) {
            throw new RuntimeException("用户名已存在");
        }

        // 创建用户实体
        User user = new User();
        user.setUserAccount(userCreateDto.getUserName());
        user.setUserName(userCreateDto.getName());
        user.setPassword(userCreateDto.getPassword());
        user.setPhone(userCreateDto.getPhone());
        user.setGender(userCreateDto.getGender());
        // 处理status字段类型转换
        if (userCreateDto.getStatus() instanceof Boolean) {
            user.setUserStatus((Boolean) userCreateDto.getStatus() ? 1 : 0);
        } else if (userCreateDto.getStatus() instanceof Integer) {
            user.setUserStatus((Integer) userCreateDto.getStatus());
        } else {
            user.setUserStatus(1); // 默认启用
        }
        user.setUserNotes(userCreateDto.getNotes());

        // 添加用户到数据库
        boolean result = userService.saveWithHashedPassword(user);
        
        if (!result) {
            throw new RuntimeException("创建用户失败");
        }

        // 创建用户角色关联
        if (userCreateDto.getRoleIds() != null && !userCreateDto.getRoleIds().isEmpty()) {
            userService.assignRolesToUser(user.getUserId(), userCreateDto.getRoleIds());
        }

        // 创建用户部门关联
        if (userCreateDto.getDepartmentIds() != null && !userCreateDto.getDepartmentIds().isEmpty()) {
            userService.assignDepartmentsToUser(user.getUserId(), userCreateDto.getDepartmentIds());
        }

        // 获取完整的用户信息（包括角色和部门）
        return userService.getDtoById(user.getUserId());
    }

    @Override
    protected UserDto doUpdate(Long id, UserCreateDto userUpdateDto) {
        // 获取现有用户
        User existingUser = userService.getById(id);
        if (existingUser == null) {
            throw new RuntimeException("用户不存在");
        }

        // 检查用户名是否被其他用户使用（使用精确查询）
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_account", userUpdateDto.getUserName());
        queryWrapper.ne("user_id", id);  // 排除当前用户自身
        if (userService.count(queryWrapper) > 0) {
            throw new RuntimeException("用户名已存在");
        }

        // 创建更新包装器，只更新明确提供的字段
        User userToUpdate = new User();
        userToUpdate.setUserId(id);
        userToUpdate.setUserAccount(userUpdateDto.getUserName());
        userToUpdate.setUserName(userUpdateDto.getName());
        userToUpdate.setPhone(userUpdateDto.getPhone());
        userToUpdate.setGender(userUpdateDto.getGender());
        // 处理status字段类型转换
        if (userUpdateDto.getStatus() instanceof Boolean) {
            userToUpdate.setUserStatus((Boolean) userUpdateDto.getStatus() ? 1 : 0);
        } else if (userUpdateDto.getStatus() instanceof Integer) {
            userToUpdate.setUserStatus((Integer) userUpdateDto.getStatus());
        } else {
            userToUpdate.setUserStatus(existingUser.getUserStatus() != null ? existingUser.getUserStatus() : 1);
        }
        userToUpdate.setUserNotes(userUpdateDto.getNotes());

        // 只有当密码字段明确提供且非空时才更新密码
        if (userUpdateDto.getPassword() != null) {
            if (!userUpdateDto.getPassword().isEmpty()) {
                // 提供了非空密码，更新密码
                userToUpdate.setPassword(userUpdateDto.getPassword());
            } else {
                // 提供了空密码，拒绝更新
                throw new RuntimeException("密码不能为空");
            }
        }
        // 如果password为null，表示没有提供密码字段，不设置password字段，这样就不会更新密码

        // 更新用户
        boolean result = userService.updateWithHashedPasswordSelective(userToUpdate);
        if (!result) {
            throw new RuntimeException("更新用户失败");
        }

        // 更新用户角色关联
        // 当RoleIds为null时不处理，当为[]空数组时清除所有角色关联
        if (userUpdateDto.getRoleIds() != null) {
            userService.updateUserRoles(id, userUpdateDto.getRoleIds());
        }

        // 更新用户部门关联
        // 当DepartmentIds为null时不处理，当为[]空数组时清除所有部门关联
        if (userUpdateDto.getDepartmentIds() != null) {
            userService.updateUserDepartments(id, userUpdateDto.getDepartmentIds());
        }

        // 获取完整的用户信息（包括角色和部门）
        return userService.getDtoById(id);
    }

    @Override
    protected boolean doDelete(Long id) {
        // 获取现有用户
        User existingUser = userService.getById(id);
        if (existingUser == null) {
            throw new RuntimeException("用户不存在");
        }

        // 删除用户相关的所有关联信息
        userService.deleteUserAssociations(id);

        // 删除用户
        return userService.removeById(id);
    }
    
    //#endregion

    //#region 用户查询接口方法
    // ===================================
    // 用户查询接口方法
    // ===================================
    
    /**
     * 查询用户（支持按部门、角色、状态和用户名进行模糊查询）
     * 支持多条件组合查询和分页
     * 
     * @param userName 用户名关键词
     * @param departmentId 部门ID
     * @param roleId 角色ID
     * @param status 用户状态
     * @param pageRequest 分页请求参数
     * @return 符合条件的用户列表
     */
    @GetMapping("/search")
    public ApiResponse<PagedResult<UserDto>> searchUsers(
        @RequestParam(required = false) String userName,
        @RequestParam(required = false) Long departmentId,
        @RequestParam(required = false) Long roleId,
        @RequestParam(required = false) Integer status,
        @ModelAttribute PageRequestDto pageRequest) {
        
        try {
            pageRequest = processPageRequest(pageRequest);
            
            // 如果pageSize为-1，则获取所有用户
            if (pageRequest.getPageSize() == -1) {
                PagedResult<UserDto> allUsers = userService.advancedSearchUsers(
                    userName, departmentId, roleId, status, null,
                    0, 
                    -1);
                return success(processAllDataResult(allUsers));
            }
            
            PagedResult<UserDto> result = userService.advancedSearchUsers(
                userName, departmentId, roleId, status, null,
                pageRequest.getPageIndex(), 
                pageRequest.getPageSize());
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "搜索用户");
        }
    }
    
    //#endregion
}
