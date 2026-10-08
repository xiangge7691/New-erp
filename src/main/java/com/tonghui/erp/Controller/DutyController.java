package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.Duty;
import com.tonghui.erp.Service.DutyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 职务信息控制器
 * <p>
 * 提供职务信息的CRUD操作与全量列表查询，用于系统组织架构中的职务管理；
 * 职务文件上传对接统一文件模块（businessType=ORGANIZATION_POSITION，目录：机构管理/岗位）
 * </p>
 *
 * 接口清单：
 * ┌────┬──────────────────────┬────────┬─────────────────────────────────────┐
 * │ #  │ 接口                 │ 方法   │ 说明                                │
 * ├────┼──────────────────────┼────────┼─────────────────────────────────────┤
 * │ 1  │ /api/duty            │ GET   │ 分页查询职务列表                    │
 * │ 2  │ /api/duty/{id}       │ GET   │ 根据ID查询职务详情                  │
 * │ 3  │ /api/duty            │ POST  │ 新增职务                            │
 * │ 4  │ /api/duty/{id}       │ PUT   │ 修改职务                            │
 * │ 5  │ /api/duty/{id}       │ DELETE│ 删除职务（软删除，保留已上传文件）  │
 * │ 6  │ /api/duty/list       │ GET   │ 获取全量职务列表（用于下拉选择）    │
 * └────┴──────────────────────┴────────┴─────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/duty")
public class DutyController extends BaseController {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 职务服务
     */
    @Autowired
    private DutyService dutyService;

    // endregion

    // region 职务CRUD接口
    // ===================================
    // 职务CRUD接口
    // ===================================

    /**
     * 分页查询职务列表
     * <p>
     * 支持按关键词模糊匹配职务编码/名称/描述，支持状态筛选，按排序号升序排列
     * </p>
     *
     * 示例请求：
     * GET /api/duty?keyword=主管&status=1&pageIndex=0&pageSize=10
     *
     * @param keyword   关键词（模糊匹配职务编码/名称/描述）
     * @param status    状态筛选（1-启用，0-停用）
     * @param pageIndex 页码，从0开始（默认0）
     * @param pageSize  每页数量（默认10）
     * @return ApiResponse&lt;PagedResult&lt;Duty&gt;&gt; 分页结果，包含职务列表和分页信息
     */
    @GetMapping
    public ApiResponse<PagedResult<Duty>> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "10") int pageSize) {
        Page<Duty> pageResult = dutyService.queryDuties(keyword, status, pageIndex, pageSize);
        PagedResult<Duty> pagedResult = new PagedResult<>();
        pagedResult.setItems(pageResult.getRecords());
        pagedResult.setTotalCount(pageResult.getTotal());
        pagedResult.setPageIndex(pageIndex);
        pagedResult.setPageSize(pageSize);
        return success(pagedResult);
    }

    /**
     * 根据ID查询职务详情
     *
     * 示例请求：
     * GET /api/duty/1
     *
     * @param id 职务ID（路径参数）
     * @return ApiResponse&lt;Duty&gt; 职务详情
     */
    @GetMapping("/{id}")
    public ApiResponse<Duty> getById(@PathVariable Long id) {
        Duty duty = dutyService.getById(id);
        if (duty == null) {
            return error("职务不存在");
        }
        return success(duty);
    }

    /**
     * 新增职务
     * <p>
     * 职务编码必须唯一，重复编码返回错误提示
     * </p>
     *
     * 示例请求：
     * POST /api/duty
     * Content-Type: application/json
     * {
     *   "dutyCode": "ZJ001",
     *   "dutyName": "车间主任",
     *   "dutyDesc": "负责生产车间日常管理",
     *   "status": 1,
     *   "sortOrder": 1
     * }
     *
     * @param duty 职务实体对象
     * @return ApiResponse&lt;Duty&gt; 新增的职务
     */
    @PostMapping
    public ApiResponse<Duty> create(@RequestBody Duty duty) {
        // 职务编码唯一性校验（绕过软删除）
        if (StringUtils.hasText(duty.getDutyCode()) && !dutyService.isCodeUnique(duty.getDutyCode(), null)) {
            return error("职务编码已存在: " + duty.getDutyCode());
        }
        duty.setIsDeleted(0);
        duty.setVersion(0);
        dutyService.save(duty);
        return success(duty, "新增成功");
    }

    /**
     * 修改职务
     * <p>
     * 修改职务编码时校验唯一性（排除自身）
     * </p>
     *
     * 示例请求：
     * PUT /api/duty/1
     * Content-Type: application/json
     * {
     *   "dutyName": "车间主任（更新）",
     *   "dutyDesc": "负责生产车间日常管理更新"
     * }
     *
     * @param id   职务ID（路径参数）
     * @param duty 职务实体对象
     * @return ApiResponse&lt;Duty&gt; 修改后的职务
     */
    @PutMapping("/{id}")
    public ApiResponse<Duty> update(@PathVariable Long id, @RequestBody Duty duty) {
        Duty existing = dutyService.getById(id);
        if (existing == null) {
            return error("职务不存在");
        }
        // 修改编码时校验唯一性（排除自身）
        if (StringUtils.hasText(duty.getDutyCode()) && !dutyService.isCodeUnique(duty.getDutyCode(), id)) {
            return error("职务编码已存在: " + duty.getDutyCode());
        }
        duty.setDutyId(id);
        dutyService.updateById(duty);
        return success(duty, "修改成功");
    }

    /**
     * 删除职务
     * <p>
     * 软删除（is_deleted置1），已上传的附件文件保留在文件模块，供档案留痕
     * </p>
     *
     * 示例请求：
     * DELETE /api/duty/1
     *
     * @param id 职务ID（路径参数）
     * @return ApiResponse&lt;Void&gt; 删除结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        dutyService.removeById(id);
        return success(null, "删除成功");
    }

    /**
     * 获取全量职务列表（用于下拉选择）
     * <p>
     * 仅返回启用状态的职务，按排序号升序排列
     * </p>
     *
     * 示例请求：
     * GET /api/duty/list
     *
     * @return ApiResponse&lt;List&lt;Duty&gt;&gt; 启用状态的职务列表
     */
    @GetMapping("/list")
    public ApiResponse<List<Duty>> list() {
        QueryWrapper<Duty> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1)
               .orderByAsc("sort_order");
        return success(dutyService.list(wrapper));
    }

    // endregion
}