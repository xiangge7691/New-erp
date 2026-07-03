package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.System.PositionWithDetailsDto;
import com.tonghui.erp.Data.Entity.Position;
import com.tonghui.erp.Service.PositionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 岗位信息控制器
 * <p>
 * 提供岗位信息的CRUD操作、全量列表查询及带子表查询功能，用于系统组织架构中的岗位管理
 * </p>
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────────┬────────┬─────────────────────────────────────┐
 * │ #  │ 接口                                 │ 方法   │ 说明                                │
 * ├────┼──────────────────────────────────────┼────────┼─────────────────────────────────────┤
 * │ 1  │ /api/position                        │ GET   │ 分页查询岗位列表                    │
 * │ 2  │ /api/position/{id}                   │ GET   │ 根据ID查询岗位详情                  │
 * │ 3  │ /api/position                        │ POST  │ 新增岗位                            │
 * │ 4  │ /api/position/{id}                   │ PUT   │ 修改岗位                            │
 * │ 5  │ /api/position/{id}                   │ DELETE│ 删除岗位                            │
 * │ 6  │ /api/position/list                   │ GET   │ 获取全量岗位列表（用于下拉选择）    │
 * │ 7  │ /api/position/search-with-details    │ GET   │ 带子表查询岗位                      │
 * └────┴──────────────────────────────────────┴────────┴─────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/position")
public class PositionController extends BaseController {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 岗位服务
     */
    @Autowired
    private PositionService positionService;

    // endregion

    // region 岗位CRUD接口
    // ===================================
    // 岗位CRUD接口
    // ===================================

    /**
     * 分页查询岗位列表
     * <p>
     * 支持按关键词模糊匹配岗位名称，支持状态筛选，按排序字段升序排列
     * </p>
     *
     * 示例请求：
     * GET /api/position?keyword=主管&status=1&pageIndex=0&pageSize=10
     *
     * @param keyword 关键词（模糊匹配岗位名称）
     * @param status 状态筛选（1-启用，0-禁用）
     * @param pageIndex 页码，从0开始（默认0）
     * @param pageSize 每页数量（默认10）
     * @return ApiResponse&lt;PagedResult&lt;Position&gt;&gt; 分页结果，包含岗位列表和分页信息
     */
    @GetMapping
    public ApiResponse<PagedResult<Position>> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "10") int pageSize) {
        Page<Position> page = new Page<>(pageIndex + 1, pageSize);
        QueryWrapper<Position> wrapper = new QueryWrapper<>();
        
        // 关键词模糊查询
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like("position_name", keyword);
        }
        // 状态筛选
        if (status != null) {
            wrapper.eq("status", status);
        }
        wrapper.orderByAsc("sort_order");
        
        Page<Position> pageResult = positionService.page(page, wrapper);
        PagedResult<Position> pagedResult = new PagedResult<>();
        pagedResult.setItems(pageResult.getRecords());
        pagedResult.setTotalCount(pageResult.getTotal());
        pagedResult.setPageIndex(pageIndex);
        pagedResult.setPageSize(pageSize);
        
        return success(pagedResult);
    }

    /**
     * 根据ID查询岗位详情
     *
     * 示例请求：
     * GET /api/position/1
     *
     * @param id 岗位ID（路径参数）
     * @return ApiResponse&lt;Position&gt; 岗位详情
     */
    @GetMapping("/{id}")
    public ApiResponse<Position> getById(@PathVariable Long id) {
        Position position = positionService.getById(id);
        if (position == null) {
            return error("岗位不存在");
        }
        return success(position);
    }

    /**
     * 新增岗位
     *
     * 示例请求：
     * POST /api/position
     * Content-Type: application/json
     * {
     *   "positionName": "生产主管",
     *   "status": 1,
     *   "sortOrder": 1,
     *   "description": "负责生产管理"
     * }
     *
     * @param position 岗位实体对象
     * @return ApiResponse&lt;Position&gt; 新增的岗位
     */
    @PostMapping
    public ApiResponse<Position> create(@RequestBody Position position) {
        position.setIsDeleted(0);
        position.setVersion(0);
        positionService.save(position);
        return success(position, "新增成功");
    }

    /**
     * 修改岗位
     *
     * 示例请求：
     * PUT /api/position/1
     * Content-Type: application/json
     * {
     *   "positionName": "生产主管（更新）",
     *   "description": "负责生产管理更新"
     * }
     *
     * @param id 岗位ID（路径参数）
     * @param position 岗位实体对象
     * @return ApiResponse&lt;Position&gt; 修改后的岗位
     */
    @PutMapping("/{id}")
    public ApiResponse<Position> update(@PathVariable Long id, @RequestBody Position position) {
        Position existing = positionService.getById(id);
        if (existing == null) {
            return error("岗位不存在");
        }
        position.setPositionId(id);
        positionService.updateById(position);
        return success(position, "修改成功");
    }

    /**
     * 删除岗位
     *
     * 示例请求：
     * DELETE /api/position/1
     *
     * @param id 岗位ID（路径参数）
     * @return ApiResponse&lt;Void&gt; 删除结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        positionService.removeById(id);
        return success(null, "删除成功");
    }

    /**
     * 获取全量岗位列表（用于下拉选择）
     * <p>
     * 仅返回启用状态的岗位，按排序字段升序排列
     * </p>
     *
     * 示例请求：
     * GET /api/position/list
     *
     * @return ApiResponse&lt;List&lt;Position&gt;&gt; 启用状态的岗位列表
     */
    @GetMapping("/list")
    public ApiResponse<List<Position>> list() {
        QueryWrapper<Position> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1)
               .orderByAsc("sort_order");
        List<Position> list = positionService.list(wrapper);
        return success(list);
    }

    // endregion

    // region 带子表查询接口
    // ===================================
    // 带子表查询接口
    // ===================================

    /**
     * 带子表查询岗位
     *
     * 示例请求：
     * GET /api/position/search-with-details?positionName=主管&pageIndex=0&pageSize=10
     *
     * @param position 岗位查询条件对象
     * @param pageIndex 页码，从0开始
     * @param pageSize 每页大小
     * @return ApiResponse&lt;PagedResult&lt;PositionWithDetailsDto&gt;&gt; 岗位列表（含子表信息）
     */
    @GetMapping("/search-with-details")
    public ApiResponse<PagedResult<PositionWithDetailsDto>> searchWithDetails(Position position,
                                                                              @RequestParam int pageIndex,
                                                                              @RequestParam int pageSize) {
        try {
            int safePageIndex = Math.max(0, pageIndex);
            int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);
            PagedResult<PositionWithDetailsDto> result = positionService.searchWithDetails(position, safePageIndex, safePageSize);
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "查询失败");
        }
    }

    // endregion
}
