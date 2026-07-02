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
 * 提供岗位信息的CRUD操作
 */
@RestController
@RequestMapping("/api/position")
public class PositionController extends BaseController {

    @Autowired
    private PositionService positionService;

    /**
     * 分页查询岗位列表
     * @param keyword 关键词（模糊匹配岗位名称）
     * @param status 状态筛选
     * @param pageIndex 页码（从0开始）
     * @param pageSize 每页数量
     * @return 分页结果
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
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        positionService.removeById(id);
        return success(null, "删除成功");
    }

    /**
     * 获取全量岗位列表（用于下拉选择）
     */
    @GetMapping("/list")
    public ApiResponse<List<Position>> list() {
        QueryWrapper<Position> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1)
               .orderByAsc("sort_order");
        List<Position> list = positionService.list(wrapper);
        return success(list);
    }

    // #region 带子表查询

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

    // #endregion
}
