package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.utils.EntityUtils;
import com.tonghui.erp.Data.Entity.Position;
import com.tonghui.erp.Service.PositionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
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
        try {
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
        } catch (Exception e) {
            return exception(e, "操作");
        }
    }

    /**
     * 根据ID查询岗位详情
     */
    @GetMapping("/{id}")
    public ApiResponse<Position> getById(@PathVariable Long id) {
        try {
            Position position = positionService.getById(id);
            if (position == null) {
                return error("岗位不存在");
            }
            return success(position);
        } catch (Exception e) {
            return exception(e, "操作");
        }
    }

    /**
     * 新增岗位
     */
    @PostMapping
    public ApiResponse<Position> create(@RequestBody Position position) {
        try {
            position.setCreatedBy(EntityUtils.getCurrentUserId());
            position.setCreatedAt(LocalDateTime.now());
            position.setIsDeleted(0);
            position.setVersion(0);
            positionService.save(position);
            return success(position, "新增成功");
        } catch (Exception e) {
            return exception(e, "操作");
        }
    }

    /**
     * 修改岗位
     */
    @PutMapping("/{id}")
    public ApiResponse<Position> update(@PathVariable Long id, @RequestBody Position position) {
        try {
            Position existing = positionService.getById(id);
            if (existing == null) {
                return error("岗位不存在");
            }
            position.setPositionId(id);
            position.setUpdatedBy(EntityUtils.getCurrentUserId());
            position.setUpdatedAt(LocalDateTime.now());
            positionService.updateById(position);
            return success(position, "修改成功");
        } catch (Exception e) {
            return exception(e, "操作");
        }
    }

    /**
     * 删除岗位
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        try {
            positionService.removeById(id);
            return success(null, "删除成功");
        } catch (Exception e) {
            return exception(e, "操作");
        }
    }

    /**
     * 获取全量岗位列表（用于下拉选择）
     */
    @GetMapping("/list")
    public ApiResponse<List<Position>> list() {
        try {
            QueryWrapper<Position> wrapper = new QueryWrapper<>();
            wrapper.eq("status", 1)
                   .orderByAsc("sort_order");
            List<Position> list = positionService.list(wrapper);
            return success(list);
        } catch (Exception e) {
            return exception(e, "操作");
        }
    }
}
