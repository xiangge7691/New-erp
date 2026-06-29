# Service 接口文档

## 目录
- [DepartmentService](#departmentservice)
- [DosageFormService](#dosageformservice)
- [LoginService](#loginservice)
- [PermissionService](#permissionservice)
- [RolePermService](#rolepermservice)
- [RoleService](#roleservice)
- [UnitService](#unitservice)
- [UserDepartmentService](#userdepartmentservice)
- [UserRoleService](#userroleservice)
- [UserService](#userservice)

---

## DepartmentService

**描述**：提供部门相关的业务逻辑接口，包括部门的查询、管理等功能。

### 方法列表

1. **listDto()**
   - **返回值**：`List<DepartmentDto>`
   - **功能**：获取所有部门DTO列表。

2. **getDtoById(Long id)**
   - **参数**：`id` - 部门ID。
   - **返回值**：`DepartmentDto`
   - **功能**：根据ID获取部门DTO。

3. **advancedSearchDepartments(String departmentName, Integer status, int pageIndex, int pageSize)**
   - **参数**：
     - `departmentName` - 部门名称关键词，支持模糊查询。
     - `status` - 部门状态。
     - `pageIndex` - 页码，从0开始。
     - `pageSize` - 每页数量，-1表示不分页返回所有结果。
   - **返回值**：`PagedResult<DepartmentDto>`
   - **功能**：高级查询部门（支持按名称、状态等条件查询）。

4. **convertToDto(Department department)**
   - **参数**：`department` - Department实体。
   - **返回值**：`DepartmentDto`
   - **功能**：将Department实体转换为DepartmentDto。

5. **getByDepartmentName(String departmentName)**
   - **参数**：`departmentName` - 部门名称。
   - **返回值**：`Department`
   - **功能**：根据部门名称获取部门。

6. **hasUserDepartments(Long departmentId)**
   - **参数**：`departmentId` - 部门ID。
   - **返回值**：`boolean`
   - **功能**：检查部门是否有关联的用户。

7. **getDepartmentDetails(Long departmentId)**
   - **参数**：`departmentId` - 部门ID。
   - **返回值**：`DepartmentDto`
   - **功能**：获取部门详细信息。

---

## DosageFormService

**描述**：提供药品剂型的增删改查等业务逻辑接口。

### 方法列表

1. **searchByName(String dosageName, PageRequestDto pageRequest)**
   - **参数**：
     - `dosageName` - 剂型名称（模糊匹配），为空时查询所有。
     - `pageRequest` - 分页参数，包含页码和每页数量等信息。
   - **返回值**：`PagedResult<DosageForm>`
   - **功能**：根据剂型名称模糊查询（分页）。

---

## LoginService

**描述**：定义用户登录相关业务操作的接口规范，提供用户身份验证和登录信息构建功能。

### 方法列表

1. **loginWithFullResponseAsync(String userName, String password)**
   - **参数**：
     - `userName` - 用户名，用于标识用户身份的唯一字符串。
     - `password` - 密码，用户登录凭证的明文形式。
   - **返回值**：`CompletableFuture<LoginResponse>`
   - **功能**：用户登录并构建完整响应（异步方式）。

2. **loginWithFullResponse(String userName, String password)**
   - **参数**：
     - `userName` - 用户名，用于标识用户身份的唯一字符串。
     - `password` - 密码，用户登录凭证的明文形式。
   - **返回值**：`LoginResponse`
   - **功能**：用户登录并构建完整响应（同步方式）。

---

## PermissionService

**描述**：提供权限相关的业务逻辑接口，包括权限的查询、管理、树形结构构建等功能。

### 方法列表

1. **advancedSearchPermissions(String permissionName, String permKey, Long permId, Integer status, Long roleId, int pageIndex, int pageSize)**
   - **参数**：
     - `permissionName` - 权限名称关键词，支持模糊查询。
     - `permKey` - 权限键名。
     - `permId` - 权限ID。
     - `status` - 权限状态。
     - `roleId` - 角色ID。
     - `pageIndex` - 页码，从0开始。
     - `pageSize` - 每页数量，-1表示不分页返回所有结果。
   - **返回值**：`PagedResult<PermissionDto>`
   - **功能**：高级查询权限（支持按名称、键名、状态、角色等条件查询）。

2. **getPermissionDetails(Long permissionId)**
   - **参数**：`permissionId` - 权限ID。
   - **返回值**：`PermissionDto`
   - **功能**：获取权限详细信息。

3. **getByPermKey(String permKey)**
   - **参数**：`permKey` - 权限键名。
   - **返回值**：`Permission`
   - **功能**：根据权限键获取权限。

4. **getByParentId(Long parentId)**
   - **参数**：`parentId` - 父权限ID。
   - **返回值**：`List<Permission>`
   - **功能**：根据父ID获取权限列表。

5. **getPermissionTree()**
   - **返回值**：`List<PermissionDto>`
   - **功能**：获取权限树形结构。

6. **convertToDto(Permission permission)**
   - **参数**：`permission` - Permission实体。
   - **返回值**：`PermissionDto`
   - **功能**：将Permission实体转换为PermissionDto。

---

## RolePermService

**描述**：提供角色权限关联相关的业务逻辑接口，包括角色权限关联的查询、管理等功能。

### 方法列表

1. **getPermissionIdsByRoleId(Long roleId)**
   - **参数**：`roleId` - 角色ID。
   - **返回值**：`List<Long>`
   - **功能**：根据角色ID获取权限列表。

2. **getRoleIdsByPermissionId(Long permId)**
   - **参数**：`permId` - 权限ID。
   - **返回值**：`List<Long>`
   - **功能**：根据权限ID获取角色列表。

3. **assignPermissionsToRole(Long roleId, List<Long> permIds)**
   - **参数**：
     - `roleId` - 角色ID。
     - `permIds` - 权限ID列表。
   - **返回值**：`boolean`
   - **功能**：为角色分配权限。

4. **updateRolePermissions(Long roleId, List<Long> permIds)**
   - **参数**：
     - `roleId` - 角色ID。
     - `permIds` - 权限ID列表。
   - **返回值**：`boolean`
   - **功能**：更新角色权限关联。

5. **getPaged(int pageIndex, int pageSize)**
   - **参数**：
     - `pageIndex` - 页码（从0开始）。
     - `pageSize` - 每页数量。
   - **返回值**：`PagedResult<RolePerm>`
   - **功能**：分页查询角色权限关联。

6. **getByRoleId(Long roleId)**
   - **参数**：`roleId` - 角色ID。
   - **返回值**：`List<RolePerm>`
   - **功能**：根据角色ID获取权限关联。

7. **getByPermId(Long permId)**
   - **参数**：`permId` - 权限ID。
   - **返回值**：`List<RolePerm>`
   - **功能**：根据权限ID获取角色关联。

---

## RoleService

**描述**：提供角色相关的业务逻辑接口，包括角色的查询、权限分配等功能。

### 方法列表

1. **advancedSearchRoles(String roleName, Long roleId, Integer status, Long userId, Long permissionId, int pageIndex, int pageSize)**
   - **参数**：
     - `roleName` - 角色名称关键词，支持模糊查询。
     - `roleId` - 角色ID，用于精确查询单个角色。
     - `status` - 角色状态，1为启用，0为禁用。
     - `userId` - 用户ID，筛选具有指定用户的角色。
     - `permissionId` - 权限ID，筛选具有指定权限的角色。
     - `pageIndex` - 页码，从0开始。
     - `pageSize` - 每页数量，-1表示不分页返回所有结果。
   - **返回值**：`PagedResult<RoleDto>`
   - **功能**：高级查询角色（支持按角色名称进行模糊查询）。

2. **getRoleDetails(Long roleId)**
   - **参数**：`roleId` - 角色ID。
   - **返回值**：`RoleDto`
   - **功能**：获取角色详细信息。

3. **convertToDto(Role role)**
   - **参数**：`role` - Role实体。
   - **返回值**：`RoleDto`
   - **功能**：将Role实体转换为RoleDto。

4. **assignPermissionsToRole(Long roleId, List<Long> permissionIds)**
   - **参数**：
     - `roleId` - 角色ID。
     - `permissionIds` - 权限ID列表。
   - **返回值**：`boolean`
   - **功能**：为角色分配权限。

---

## UnitService

**描述**：提供计量单位的增删改查等业务逻辑接口。

### 方法列表

1. **searchByName(String unitName, PageRequestDto pageRequest)**
   - **参数**：
     - `unitName` - 计量单位名称（模糊匹配），为空时查询所有。
     - `pageRequest` - 分页参数，包含页码和每页数量等信息。
   - **返回值**：`PagedResult<Unit>`
   - **功能**：根据计量单位名称模糊查询（分页）。

---

## UserDepartmentService

**描述**：提供用户部门关联相关的业务逻辑接口，包括用户部门关联的查询、管理等功能。

### 方法列表

1. **getByUserId(Long userId)**
   - **参数**：`userId` - 用户ID。
   - **返回值**：`List<UserDepartment>`
   - **功能**：根据用户ID获取所有部门关联。

2. **getByDepartmentId(Long departmentId)**
   - **参数**：`departmentId` - 部门ID。
   - **返回值**：`List<UserDepartment>`
   - **功能**：根据部门ID获取所有用户关联。

3. **getPrimaryDepartment(Long userId)**
   - **参数**：`userId` - 用户ID。
   - **返回值**：`UserDepartment`
   - **功能**：获取用户的主部门。

4. **getPaged(int pageIndex, int pageSize)**
   - **参数**：
     - `pageIndex` - 页码（从0开始）。
     - `pageSize` - 每页数量。
   - **返回值**：`PagedResult<UserDepartment>`
   - **功能**：分页查询用户部门关联。

5. **searchUserDepartments(Long userId, Long departmentId, Integer isPrimary, String createdStartTime, String createdEndTime, int pageIndex, int pageSize)**
   - **参数**：
     - `userId` - 用户ID。
     - `departmentId` - 部门ID。
     - `isPrimary` - 是否主部门。
     - `createdStartTime` - 创建时间起始。
     - `createdEndTime` - 创建时间结束。
     - `pageIndex` - 页码。
     - `pageSize` - 每页数量。
   - **返回值**：`PagedResult<UserDepartment>`
   - **功能**：根据查询条件获取用户部门关联（分页）。

6. **getPagedDto(int pageIndex, int pageSize)**
   - **参数**：
     - `pageIndex` - 页码。
     - `pageSize` - 每页数量。
   - **返回值**：`PagedResult<UserDepartmentDto>`
   - **功能**：分页获取所有用户部门关联DTO。

7. **getDtosByUserId(Long userId)**
   - **参数**：`userId` - 用户ID。
   - **返回值**：`List<UserDepartmentDto>`
   - **功能**：根据用户ID获取所有部门关联DTO。

8. **getDtosByDepartmentId(Long departmentId)**
   - **参数**：`departmentId` - 部门ID。
   - **返回值**：`List<UserDepartmentDto>`
   - **功能**：根据部门ID获取所有用户关联DTO。

9. **getPrimaryDepartmentDto(Long userId)**
   - **参数**：`userId` - 用户ID。
   - **返回值**：`UserDepartmentDto`
   - **功能**：获取用户的主部门DTO。

10. **convertToDto(UserDepartment entity)**
    - **参数**：`entity` - UserDepartment实体。
    - **返回值**：`UserDepartmentDto`
    - **功能**：将UserDepartment实体转换为UserDepartmentDto。

---

## UserRoleService

**描述**：提供用户角色关联相关的业务逻辑接口，包括用户角色关联的查询、管理等功能。

### 方法列表

1. **getByUserId(Long userId)**
   - **参数**：`userId` - 用户ID。
   - **返回值**：`List<UserRole>`
   - **功能**：根据用户ID获取所有角色关联。

2. **getByRoleId(Long roleId)**
   - **参数**：`roleId` - 角色ID。
   - **返回值**：`List<UserRole>`
   - **功能**：根据角色ID获取所有用户关联。

3. **getDtosByUserId(Long userId)**
   - **参数**：`userId` - 用户ID。
   - **返回值**：`List<UserRoleDto>`
   - **功能**：根据用户ID获取所有角色关联DTO。

4. **getDtosByRoleId(Long roleId)**
   - **参数**：`roleId` - 角色ID。
   - **返回值**：`List<UserRoleDto>`
   - **功能**：根据角色ID获取所有用户关联DTO。

5. **convertToDto(UserRole entity)**
   - **参数**：`entity` - 用户角色实体。
   - **返回值**：`UserRoleDto`
   - **功能**：将用户角色实体转换为DTO。

6. **advancedSearchUserRoles(Long userId, Long roleId, String createdStartTime, String createdEndTime, int pageIndex, int pageSize)**
   - **参数**：
     - `userId` - 用户ID，用于筛选指定用户的关联。
     - `roleId` - 角色ID，用于筛选指定角色的关联。
     - `createdStartTime` - 创建时间起始，用于筛选此时间之后创建的关联。
     - `createdEndTime` - 创建时间结束，用于筛选此时间之前创建的关联。
     - `pageIndex` - 页码，从0开始。
     - `pageSize` - 每页数量，-1表示不分页返回所有结果。
   - **返回值**：`PagedResult<UserRoleDto>`
   - **功能**：高级查询用户角色关联（支持按用户ID、角色ID和创建时间查询）。

7. **searchUserRoles(Long userId, Long roleId, String createdStartTime, String createdEndTime, int pageIndex, int pageSize)**
   - **参数**：
     - `userId` - 用户ID。
     - `roleId` - 角色ID。
     - `createdStartTime` - 创建时间起始。
     - `createdEndTime` - 创建时间结束。
     - `pageIndex` - 页码。
     - `pageSize` - 每页数量。
   - **返回值**：`PagedResult<UserRole>`
   - **功能**：根据查询条件获取用户角色关联（分页）。

8. **getPagedDto(int pageIndex, int pageSize)**
   - **参数**：
     - `pageIndex` - 页码。
     - `pageSize` - 每页数量。
   - **返回值**：`PagedResult<UserRoleDto>`
   - **功能**：分页获取所有用户角色关联DTO。

---

## UserService

**描述**：提供用户相关的业务逻辑接口，包括用户的基本操作、查询、权限分配等功能。

### 方法列表

1. **listDto()**
   - **返回值**：`List<UserDto>`
   - **功能**：获取所有用户（不包含密码）。

2. **getDtoById(Long id)**
   - **参数**：`id` - 用户ID。
   - **返回值**：`UserDto`
   - **功能**：根据ID获取用户（不包含密码）。

3. **saveWithHashedPassword(User user)**
   - **参数**：`user` - 用户信息。
   - **返回值**：`boolean`
   - **功能**：创建用户并哈希密码。

4. **updateWithHashedPassword(User user)**
   - **参数**：`user` - 用户信息。
   - **返回值**：`boolean`
   - **功能**：更新用户并哈希密码（如果提供了新密码）。

5. **advancedSearchUsers(String userName, Long departmentId, Long roleId, Integer status, Long userId, int pageIndex, int pageSize)**
   - **参数**：
     - `userName` - 用户名关键词，支持模糊查询用户名称和真实姓名。
     - `departmentId` - 部门ID，筛选指定部门的用户。
     - `roleId` - 角色ID，筛选具有指定角色的用户。
     - `status` - 用户状态，true为启用，false为禁用。
     - `userId` - 用户ID，用于精确查询单个用户。
     - `pageIndex` - 页码，从0开始。
     - `pageSize` - 每页数量，-1表示不分页返回所有结果。
   - **返回值**：`PagedResult<UserDto>`
   - **功能**：高级查询用户（支持按部门、角色、状态和用户名进行模糊查询）。

6. **validateUserPassword(String userName, String password)**
   - **参数**：
     - `userName` - 用户名。
     - `password` - 明文密码。
   - **返回值**：`boolean`
   - **功能**：验证用户密码。

7. **assignRolesToUser(Long userId, List<Long> roleIds)**
   - **参数**：
     - `userId` - 用户ID。
     - `roleIds` - 角色ID列表。
   - **返回值**：`boolean`
   - **功能**：为用户分配角色。

8. **assignDepartmentsToUser(Long userId, List<Long> departmentIds)**
   - **参数**：
     - `userId` - 用户ID。
     - `departmentIds` - 部门ID列表。
   - **返回值**：`boolean`
   - **功能**：为用户分配部门。

9. **updateUserRoles(Long userId, List<Long> roleIds)**
   - **参数**：
     - `userId` - 用户ID。
     - `roleIds` - 角色ID列表。
   - **返回值**：`boolean`
   - **功能**：更新用户角色关联。

10. **updateUserDepartments(Long userId, List<Long> departmentIds)**
    - **参数**：
      - `userId` - 用户ID。
      - `departmentIds` - 部门ID列表。
    - **返回值**：`boolean`
    - **功能**：更新用户部门关联。

11. **deleteUserAssociations(Long userId)**
    - **参数**：`userId` - 用户ID。
    - **返回值**：`boolean`
    - **功能**：删除用户相关的所有关联信息。

12. **convertToDto(User user)**
    - **参数**：`user` - User实体。
    - **返回值**：`UserDto`
    - **功能**：将User实体转换为UserDto。

---

### 文档生成完成

此文档包含了项目中所有 Service 接口的详细说明，可以作为开发参考文档使用。