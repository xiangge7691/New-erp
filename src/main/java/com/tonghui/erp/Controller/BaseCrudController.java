package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import org.springframework.web.bind.annotation.*;

/**
 * CRUD控制器基类
 * <p>
 * 提供标准的CRUD操作模板方法，包括获取列表、根据ID获取详情、创建、更新和删除等操作
 * </p>
 *
 * @param <T> 实体类型
 * @param <D> DTO类型
 * @param <ID> ID类型
 */
public abstract class BaseCrudController<T, D, ID> extends BaseController {

    //#region CRUD操作接口方法
    // ===================================
    // CRUD操作接口方法
    // ===================================
    
    /**
     * 获取所有实体列表（分页）
     * 
     * @param pageRequest 分页请求参数
     * @return 分页结果响应
     */
    @GetMapping
    public ApiResponse<PagedResult<D>> getAll(@ModelAttribute PageRequestDto pageRequest) {
        try {
            pageRequest = processPageRequest(pageRequest);

            // 如果pageSize为-1，则获取所有数据
            if (pageRequest.getPageSize() == -1) {
                PagedResult<D> allData = getAllData(-1, -1);
                return success(processAllDataResult(allData));
            }

            PagedResult<D> pagedData = getAllData(pageRequest.getPageIndex(), pageRequest.getPageSize());
            return success(pagedData);
        } catch (Exception ex) {
            return exception(ex, "获取列表");
        }
    }

    /**
     * 根据ID获取实体详情
     * 
     * @param id 实体ID
     * @return 实体详情响应
     */
    @GetMapping("/{id}")
    public ApiResponse<D> getById(@PathVariable ID id) {
        try {
            D data = getDataById(id);
            if (data == null) {
                return error("数据不存在");
            }
            return success(data);
        } catch (Exception ex) {
            return exception(ex, "获取数据");
        }
    }

    /**
     * 创建实体
     * 
     * @param entity 实体
     * @return 创建结果响应
     */
    @PostMapping
    public ApiResponse<D> create(@RequestBody T entity) {
        try {
            if (entity == null) {
                return error("请求参数不能为空");
            }

            D result = doCreate(entity);
            return success(result, "创建成功");
        } catch (Exception ex) {
            return exception(ex, "创建");
        }
    }

    /**
     * 更新实体
     * 
     * @param id 实体ID
     * @param entity 实体
     * @return 更新结果响应
     */
    @PutMapping("/{id}")
    public ApiResponse<D> update(@PathVariable ID id, @RequestBody T entity) {
        try {
            if (entity == null) {
                return error("请求参数不能为空");
            }

            D existing = getDataById(id);
            if (existing == null) {
                return error("数据不存在");
            }

            D result = doUpdate(id, entity);
            return success(result, "更新成功");
        } catch (Exception ex) {
            return exception(ex, "更新");
        }
    }

    /**
     * 删除实体
     * 
     * @param id 实体ID
     * @return 删除结果响应
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable ID id) {
        try {
            D existing = getDataById(id);
            if (existing == null) {
                return error("数据不存在");
            }

            boolean result = doDelete(id);
            if (result) {
                return success(true, "删除成功");
            } else {
                return error("删除失败");
            }
        } catch (Exception ex) {
            return exception(ex, "删除");
        }
    }
    
    //#endregion

    //#region 抽象方法定义
    // ===================================
    // 抽象方法定义
    // ===================================
    
    /**
     * 获取所有数据
     * 
     * @param pageIndex 页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    protected abstract PagedResult<D> getAllData(int pageIndex, int pageSize);

    /**
     * 根据ID获取数据
     * 
     * @param id ID
     * @return 数据
     */
    protected abstract D getDataById(ID id);

    /**
     * 创建实体
     * 
     * @param entity 实体
     * @return 创建结果
     */
    protected abstract D doCreate(T entity);

    /**
     * 更新实体
     * 
     * @param id ID
     * @param entity 实体
     * @return 更新结果
     */
    protected abstract D doUpdate(ID id, T entity);

    /**
     * 删除实体
     * 
     * @param id ID
     * @return 删除结果
     */
    protected abstract boolean doDelete(ID id);
    
    //#endregion
}
