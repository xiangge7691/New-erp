package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.System.UserDto;
import com.tonghui.erp.Data.Entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 用户服务接口
 * <p>
 * 提供用户相关的业务逻辑接口，包括用户的基本操作、查询、权限分配等功能
 * </p>
 * 
 * @author 87954
 * @description 针对表【user(用户信息表)】的数据库操作Service
 * @createDate 2025-08-27 10:08:57
 */
public interface UserService extends IService<User> {
    
    //#region 基础操作接口
    // ===================================
    // 用户基础操作接口
    // ===================================
    
    /**
     * 获取所有用户（不包含密码）
     * @return 用户DTO列表
     */
    List<UserDto> listDto();

    /**
     * 根据ID获取用户（不包含密码）
     * @param id 用户ID
     * @return 用户DTO对象
     */
    UserDto getDtoById(Long id);

    /**
     * 创建用户并哈希密码
     * @param user 用户信息
     * @return 是否创建成功
     */
    boolean saveWithHashedPassword(User user);

    /**
     * 更新用户并哈希密码（如果提供了新密码）
     * @param user 用户信息
     * @return 是否更新成功
     */
    boolean updateWithHashedPassword(User user);
    
    /**
     * 选择性更新用户信息（只更新非null字段）
     * @param user 用户信息
     * @return 是否更新成功
     */
    boolean updateWithHashedPasswordSelective(User user);
    //#endregion
    
    //#region 查询接口
    // ===================================
    // 用户查询接口
    // ===================================
    
    /**
     * 高级查询用户（支持按部门、角色、状态和用户名进行模糊查询）
     * 当不传递任何参数时，返回所有用户
     * 
     * @param userName 用户名关键词，支持模糊查询用户名称和真实姓名
     * @param departmentId 部门ID，筛选指定部门的用户
     * @param roleId 角色ID，筛选具有指定角色的用户
     * @param status 用户状态，true为启用，false为禁用
     * @param userId 用户ID，用于精确查询单个用户
     * @param pageIndex 页码，从0开始
     * @param pageSize 每页数量，-1表示不分页返回所有结果
     * @return 用户列表的分页结果
     */
    PagedResult<UserDto> advancedSearchUsers(
        String userName, 
        Long departmentId, 
        Long roleId, 
        Integer status, 
        Long userId,
        int pageIndex, 
        int pageSize);
        
    /**
     * 高级查询用户（默认分页参数）
     * @return 用户列表的分页结果
     */
    PagedResult<UserDto> advancedSearchUsers();
    //#endregion
    
    //#region 验证接口
    // ===================================
    // 用户验证接口
    // ===================================
    
    /**
     * 验证用户密码
     * 通过用户名和密码验证用户身份
     * 
     * @param userName 用户名
     * @param password 明文密码
     * @return 验证结果，true表示验证成功，false表示验证失败
     */
    boolean validateUserPassword(String userName, String password);
    //#endregion
    
    //#region 权限分配接口
    // ===================================
    // 用户权限分配接口
    // ===================================
    
    /**
     * 为用户分配角色
     * 创建用户与角色的关联关系
     * 
     * @param userId 用户ID
     * @param roleIds 角色ID列表
     * @return 操作结果，true表示成功，false表示失败
     */
    boolean assignRolesToUser(Long userId, List<Long> roleIds);
    
    /**
     * 为用户分配部门
     * 创建用户与部门的关联关系，第一个部门将被设为主部门
     * 
     * @param userId 用户ID
     * @param departmentIds 部门ID列表
     * @return 操作结果，true表示成功，false表示失败
     */
    boolean assignDepartmentsToUser(Long userId, List<Long> departmentIds);
    
    /**
     * 更新用户角色关联
     * 先删除用户现有的所有角色关联，然后创建新的角色关联
     * 
     * @param userId 用户ID
     * @param roleIds 角色ID列表
     * @return 操作结果，true表示成功，false表示失败
     */
    boolean updateUserRoles(Long userId, List<Long> roleIds);
    
    /**
     * 更新用户部门关联
     * 先删除用户现有的所有部门关联，然后创建新的部门关联
     * 
     * @param userId 用户ID
     * @param departmentIds 部门ID列表
     * @return 操作结果，true表示成功，false表示失败
     */
    boolean updateUserDepartments(Long userId, List<Long> departmentIds);
    //#endregion
    
    //#region 关联信息管理接口
    // ===================================
    // 用户关联信息管理接口
    // ===================================
    
    /**
     * 删除用户相关的所有关联信息
     * 删除用户的角色关联和部门关联信息
     * 
     * @param userId 用户ID
     * @return 操作结果，true表示成功，false表示失败
     */
    boolean deleteUserAssociations(Long userId);
    //#endregion
    
    //#region 数据转换接口
    // ===================================
    // 数据转换接口
    // ===================================
    
    /**
     * 将User实体转换为UserDto
     *
     * @param user User实体
     * @return UserDto对象
     */
    UserDto convertToDto(User user);
    //#endregion
}
