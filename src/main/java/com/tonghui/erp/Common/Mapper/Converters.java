package com.tonghui.erp.Common.Mapper;

import com.tonghui.erp.Common.Dto.System.DepartmentDto;
import com.tonghui.erp.Common.Dto.System.PermissionDto;
import com.tonghui.erp.Common.Dto.System.RoleDto;
import com.tonghui.erp.Common.Dto.System.UserDto;
import com.tonghui.erp.Common.Dto.System.UserDepartmentDto;
import com.tonghui.erp.Common.Dto.System.UserRoleDto;
import com.tonghui.erp.Data.Entity.Department;
import com.tonghui.erp.Data.Entity.Permission;
import com.tonghui.erp.Data.Entity.Role;
import com.tonghui.erp.Data.Entity.User;
import com.tonghui.erp.Data.Entity.UserDepartment;
import com.tonghui.erp.Data.Entity.UserRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

/**
 * MapStruct 实体-DTO转换器
 * <p>
 * 统一管理所有实体到DTO的转换逻辑
 * </p>
 */
@Mapper(componentModel = "spring")
public interface Converters {

    Converters INSTANCE = Mappers.getMapper(Converters.class);

    //#region 用户转换
    // ===================================
    // 用户转换
    // ===================================

    /**
     * User -> UserDto（基础字段转换）
     */
    @Mapping(source = "userId", target = "id")
    @Mapping(source = "userName", target = "name")
    @Mapping(source = "userStatus", target = "status")
    @Mapping(source = "userNotes", target = "notes")
    @Mapping(target = "departments", ignore = true)
    @Mapping(target = "primaryDepartment", ignore = true)
    @Mapping(target = "roles", ignore = true)
    UserDto toUserDto(User user);

    /**
     * User -> UserDto（完整转换，含关联数据）
     */
    @Mapping(source = "userId", target = "id")
    @Mapping(source = "userName", target = "name")
    @Mapping(source = "userStatus", target = "status")
    @Mapping(source = "userNotes", target = "notes")
    UserDto toUserDtoWithAssociations(User user);

    //#endregion

    //#region 角色转换
    // ===================================
    // 角色转换
    // ===================================

    /**
     * Role -> RoleDto（基础字段转换）
     */
    @Mapping(source = "roleStatus", target = "status")
    @Mapping(target = "users", ignore = true)
    @Mapping(target = "permissions", ignore = true)
    RoleDto toRoleDto(Role role);

    /**
     * Role -> RoleDto（完整转换，含关联数据）
     */
    @Mapping(source = "roleStatus", target = "status")
    RoleDto toRoleDtoWithAssociations(Role role);

    //#endregion

    //#region 权限转换
    // ===================================
    // 权限转换
    // ===================================

    /**
     * Permission -> PermissionDto（基础字段转换）
     */
    @Mapping(source = "permId", target = "id")
    @Mapping(source = "permStatus", target = "status")
    @Mapping(source = "permType", target = "permType", qualifiedByName = "objectToString")
    @Mapping(target = "children", ignore = true)
    @Mapping(target = "roles", ignore = true)
    PermissionDto toPermissionDto(Permission permission);

    /**
     * Permission -> PermissionDto（完整转换，含关联数据）
     */
    @Mapping(source = "permId", target = "id")
    @Mapping(source = "permStatus", target = "status")
    @Mapping(source = "permType", target = "permType", qualifiedByName = "objectToString")
    PermissionDto toPermissionDtoWithAssociations(Permission permission);

    //#endregion

    //#region 部门转换
    // ===================================
    // 部门转换
    // ===================================

    /**
     * Department -> DepartmentDto
     */
    @Mapping(source = "departmentId", target = "id")
    DepartmentDto toDepartmentDto(Department department);

    //#endregion

    //#region 关联关系转换
    // ===================================
    // 关联关系转换
    // ===================================

    /**
     * UserRole -> UserRoleDto
     */
    UserRoleDto toUserRoleDto(UserRole userRole);

    /**
     * UserDepartment -> UserDepartmentDto
     */
    UserDepartmentDto toUserDepartmentDto(UserDepartment userDepartment);

    //#endregion

    //#region 类型转换方法
    // ===================================
    // 类型转换方法
    // ===================================

    /**
     * Object -> String 类型转换
     * 用于 permType 字段（数据库中可能存储为 Object 类型）
     */
    @Named("objectToString")
    default String objectToString(Object value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    //#endregion
}
