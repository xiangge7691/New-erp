package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.System.DepartmentDto;
import com.tonghui.erp.Common.Dto.System.DepartmentWithDetailsDto;
import com.tonghui.erp.Data.Entity.Department;
import com.tonghui.erp.Service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 部门控制器
 * <p>
 * 处理部门相关的HTTP请求，提供RESTful API接口，包括部门的增删改查操作
 * </p>
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────┬────────┬──────────────────────────────┐
 * │ #  │ 接口                             │ 方法   │ 说明                         │
 * ├────┼──────────────────────────────────┼────────┼──────────────────────────────┤
 * │ 1  │ /api/Department                  │ GET    │ 分页查询部门列表             │
 * │ 2  │ /api/Department/{id}             │ GET    │ 获取部门详情                 │
 * │ 3  │ /api/Department                  │ POST   │ 新增部门                     │
 * │ 4  │ /api/Department/{id}             │ PUT    │ 修改部门                     │
 * │ 5  │ /api/Department/{id}             │ DELETE │ 删除部门                     │
 * │ 6  │ /api/Department/search           │ GET    │ 按名称模糊搜索部门           │
 * │ 7  │ /api/Department/search-with-details │ GET │ 查询部门（带子表：岗位、用户）│
 * └────┴──────────────────────────────────┴────────┴──────────────────────────────┘
 */
@RestController
@RequestMapping("/api/Department")
public class DepartmentController extends BaseCrudController<Department, DepartmentDto, Long> {
    
    // region 字段和构造方法
    // ===================================
    // 字段和构造方法
    // ===================================
    
    private DepartmentService departmentService;

    @Autowired
    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }
    
    // endregion

    // region CRUD操作实现方法
    // ===================================
    // CRUD操作实现方法
    // ===================================
    
    @Override
    protected PagedResult<DepartmentDto> getAllData(int pageIndex, int pageSize) {
        return departmentService.advancedSearchDepartments(null, null, pageIndex, pageSize);
    }

    @Override
    protected DepartmentDto getDataById(Long id) {
        return departmentService.getDepartmentDetails(id);
    }

    @Override
    protected DepartmentDto doCreate(Department department) {
        // 检查部门名是否已存在
        Department existingDepartment = departmentService.getByDepartmentName(department.getDepartmentName());
        if (existingDepartment != null) {
            throw new RuntimeException("部门名已存在");
        }

        // 添加部门到数据库
        boolean result = departmentService.save(department);
        
        if (!result) {
            throw new RuntimeException("创建部门失败");
        }
        
        // 获取完整的部门信息
        return departmentService.getDepartmentDetails(department.getDepartmentId());
    }

    @Override
    protected DepartmentDto doUpdate(Long id, Department department) {
        // 检查部门名是否被其他部门使用
        Department departmentWithSameName = departmentService.getByDepartmentName(department.getDepartmentName());
        if (departmentWithSameName != null && !departmentWithSameName.getDepartmentId().equals(id)) {
            throw new RuntimeException("部门名已存在");
        }

        // 更新部门信息
        department.setDepartmentId(id);

        // 更新部门
        boolean result = departmentService.updateById(department);
        
        if (!result) {
            throw new RuntimeException("更新部门失败");
        }
        
        // 获取完整的部门信息
        return departmentService.getDepartmentDetails(id);
    }

    @Override
    protected boolean doDelete(Long id) {
        // 检查是否有用户关联到此部门
        boolean hasUserDepartments = departmentService.hasUserDepartments(id);
        if (hasUserDepartments) {
            throw new RuntimeException("该部门下有关联用户，无法删除");
        }

        // 删除部门
        return departmentService.removeById(id);
    }
    
    // endregion

    // region 搜索与查询
    // ===================================
    // 搜索与查询
    // ===================================
    
    /**
     * 根据部门名称模糊查询部门
     * <p>
     * 支持按部门名称进行模糊查询和分页，当pageSize为-1时返回所有部门
     * </p>
     *
     * 示例请求：
     * GET /api/Department/search?pageIndex=0&pageSize=20&departmentName=生产
     *
     * @param departmentName 部门名称关键词（可选）
     * @param pageRequest 分页请求参数
     * @return ApiResponse&lt;PagedResult&lt;DepartmentDto&gt;&gt; 部门分页列表
     */
    @GetMapping("/search")
    public ApiResponse<PagedResult<DepartmentDto>> searchDepartments(
            @RequestParam(required = false) String departmentName,
            @ModelAttribute PageRequestDto pageRequest) {
        try {
            pageRequest = processPageRequest(pageRequest);
            
            // 如果pageSize为-1，则获取所有部门
            if (pageRequest.getPageSize() == -1) {
                PagedResult<DepartmentDto> allDepartments = departmentService.advancedSearchDepartments(
                        departmentName,
                        null,
                        0,
                        -1);
                return success(processAllDataResult(allDepartments));
            }
            
            PagedResult<DepartmentDto> result = departmentService.advancedSearchDepartments(
                    departmentName,
                    null,
                    pageRequest.getPageIndex(),
                    pageRequest.getPageSize());
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "搜索部门");
        }
    }
    
    // endregion

    // region 带子表查询
    // ===================================
    // 带子表查询
    // ===================================

    /**
     * 查询部门（带子表：岗位、用户部门关联）
     * <p>
     * 返回部门信息及其关联的岗位和用户部门关联数据
     * </p>
     *
     * 示例请求：
     * GET /api/Department/search-with-details?pageIndex=0&pageSize=20
     *
     * @param department 部门查询条件
     * @param pageRequest 分页请求参数
     * @return ApiResponse&lt;PagedResult&lt;DepartmentWithDetailsDto&gt;&gt; 部门详情分页列表
     */
    @GetMapping("/search-with-details")
    public ApiResponse<PagedResult<DepartmentWithDetailsDto>> searchWithDetails(
            Department department,
            @ModelAttribute PageRequestDto pageRequest) {
        try {
            pageRequest = processPageRequest(pageRequest);
            PagedResult<DepartmentWithDetailsDto> result = departmentService.searchWithDetails(
                department, pageRequest.getPageIndex(), pageRequest.getPageSize());
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "searchWithDetails");
        }
    }
    // endregion
}
