package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.ReleaseReview;
import com.tonghui.erp.Service.ReleaseReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审核放行控制器
 * <p>
 * 提供审核放行的CRUD操作、条件分页查询及放行编号生成，
 * 用于质量检验模块的放行决策记录管理
 * </p>
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────┬────────┬──────────────────────────────┐
 * │ #  │ 接口                             │ 方法   │ 说明                         │
 * ├────┼──────────────────────────────────┼────────┼──────────────────────────────┤
 * │ 1  │ /api/releaseReview               │ GET    │ 分页查询审核放行列表          │
 * │ 2  │ /api/releaseReview/list          │ GET    │ 查询审核放行列表（不分页）    │
 * │ 3  │ /api/releaseReview/generateCode  │ GET    │ 获取自动生成的放行编号        │
 * │ 4  │ /api/releaseReview/{id}          │ GET    │ 查询审核放行详情              │
 * │ 5  │ /api/releaseReview               │ POST   │ 新增审核放行                  │
 * │ 6  │ /api/releaseReview/{id}          │ PUT    │ 修改审核放行                  │
 * │ 7  │ /api/releaseReview/{id}          │ DELETE │ 删除审核放行（软删除）        │
 * └────┴──────────────────────────────────┴────────┴──────────────────────────────┘
 */
@RestController
@RequestMapping("/api/releaseReview")
public class ReleaseReviewController extends BaseController {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 审核放行服务
     */
    @Autowired
    private ReleaseReviewService releaseReviewService;

    // endregion

    // region 查询接口
    // ===================================
    // 查询接口
    // ===================================

    /**
     * 分页查询审核放行列表
     *
     * 示例请求：
     * GET /api/releaseReview?releaseCode=FX-20260720001&objectName=维生素C片&releaseConclusion=放行&reviewer=审核员甲&startTime=2026-07-01 00:00:00&endTime=2026-07-31 23:59:59&pageIndex=0&pageSize=10
     *
     * @param releaseCode        放行编号（可选，模糊匹配）
     * @param relatedInspectionCode 关联检验编号（可选，模糊匹配）
     * @param objectName         被检对象名称（可选，模糊匹配）
     * @param batchNo            批号（可选，模糊匹配）
     * @param releaseConclusion  放行结论（可选，精确匹配）
     * @param reviewer           审核人（可选，模糊匹配）
     * @param startTime          审核开始时间（可选，格式：yyyy-MM-dd HH:mm:ss）
     * @param endTime            审核结束时间（可选，格式：yyyy-MM-dd HH:mm:ss）
     * @param pageIndex          页码索引，从0开始（默认0）
     * @param pageSize           每页数量（默认10）
     * @return ApiResponse&lt;PagedResult&lt;ReleaseReview&gt;&gt; 分页结果
     */
    @GetMapping
    public ApiResponse<PagedResult<ReleaseReview>> getAll(
            @RequestParam(required = false) String releaseCode,
            @RequestParam(required = false) String relatedInspectionCode,
            @RequestParam(required = false) String objectName,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) String releaseConclusion,
            @RequestParam(required = false) String reviewer,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            Page<ReleaseReview> page = new Page<>(pageIndex + 1, pageSize);
            QueryWrapper<ReleaseReview> wrapper = buildQueryWrapper(releaseCode, relatedInspectionCode, objectName,
                    batchNo, releaseConclusion, reviewer, startTime, endTime);
            Page<ReleaseReview> pageResult = releaseReviewService.page(page, wrapper);

            PagedResult<ReleaseReview> result = new PagedResult<>();
            result.setItems(pageResult.getRecords());
            result.setTotalCount(pageResult.getTotal());
            result.setPageIndex(pageIndex);
            result.setPageSize(pageSize);
            return success(result);
        } catch (Exception e) {
            return exception(e, "查询审核放行");
        }
    }

    /**
     * 查询审核放行列表（不分页）
     *
     * 示例请求：
     * GET /api/releaseReview/list?releaseConclusion=放行
     *
     * @param releaseCode        放行编号（可选，模糊匹配）
     * @param relatedInspectionCode 关联检验编号（可选，模糊匹配）
     * @param objectName         被检对象名称（可选，模糊匹配）
     * @param batchNo            批号（可选，模糊匹配）
     * @param releaseConclusion  放行结论（可选，精确匹配）
     * @param reviewer           审核人（可选，模糊匹配）
     * @param startTime          审核开始时间（可选，格式：yyyy-MM-dd HH:mm:ss）
     * @param endTime            审核结束时间（可选，格式：yyyy-MM-dd HH:mm:ss）
     * @return ApiResponse&lt;List&lt;ReleaseReview&gt;&gt; 审核放行列表
     */
    @GetMapping("/list")
    public ApiResponse<List<ReleaseReview>> getList(
            @RequestParam(required = false) String releaseCode,
            @RequestParam(required = false) String relatedInspectionCode,
            @RequestParam(required = false) String objectName,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) String releaseConclusion,
            @RequestParam(required = false) String reviewer,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        try {
            QueryWrapper<ReleaseReview> wrapper = buildQueryWrapper(releaseCode, relatedInspectionCode, objectName,
                    batchNo, releaseConclusion, reviewer, startTime, endTime);
            wrapper.orderByDesc("review_time");
            return success(releaseReviewService.list(wrapper));
        } catch (Exception e) {
            return exception(e, "查询审核放行");
        }
    }

    /**
     * 查询审核放行详情
     *
     * 示例请求：
     * GET /api/releaseReview/1
     *
     * @param id 审核放行ID（路径参数）
     * @return ApiResponse&lt;ReleaseReview&gt; 审核放行详情
     */
    @GetMapping("/{id}")
    public ApiResponse<ReleaseReview> getById(@PathVariable Long id) {
        try {
            ReleaseReview record = releaseReviewService.getById(id);
            if (record == null) {
                return error("审核放行不存在");
            }
            return success(record);
        } catch (Exception e) {
            return exception(e, "查询审核放行详情");
        }
    }

    /**
     * 获取自动生成的放行编号
     *
     * 示例请求：
     * GET /api/releaseReview/generateCode
     *
     * @return ApiResponse&lt;String&gt; 自动生成的放行编号（格式FX-YYYYMMDD-NNN）
     */
    @GetMapping("/generateCode")
    public ApiResponse<String> generateCode() {
        try {
            return success(releaseReviewService.generateCode());
        } catch (Exception e) {
            return exception(e, "生成放行编号");
        }
    }

    // endregion

    // region 新增接口
    // ===================================
    // 新增接口
    // ===================================

    /**
     * 新增审核放行
     *
     * 示例请求：
     * POST /api/releaseReview
     * Content-Type: application/json
     * {
     *   "releaseCode": "FX-20260720-001",
     *   "relatedInspectionCode": "JY-20260720-001",
     *   "objectName": "维生素C片",
     *   "batchNo": "20260701",
     *   "spec": "100mg",
     *   "releaseConclusion": "放行",
     *   "reviewOpinion": "检验合格，同意放行",
     *   "reviewer": "审核员甲",
     *   "reviewTime": "2026-07-20 14:00:00",
     *   "remark": ""
     * }
     *
     * @param record 审核放行信息（编号为空时系统自动生成，拒绝放行时审核意见必填）
     * @return ApiResponse&lt;ReleaseReview&gt; 新增的审核放行
     */
    @PostMapping
    public ApiResponse<ReleaseReview> create(@RequestBody ReleaseReview record) {
        try {
            if ("拒绝放行".equals(record.getReleaseConclusion()) && !StringUtils.hasText(record.getReviewOpinion())) {
                return error("拒绝放行时审核意见必填");
            }
            // 是否由系统自动生成编号（用于唯一索引冲突时的重试决策）
            boolean autoGenerated = !StringUtils.hasText(record.getReleaseCode());
            if (!autoGenerated) {
                // 手动传入编号时校验唯一性（含软删除记录，避免唯一索引冲突）
                if (!releaseReviewService.isCodeUnique(record.getReleaseCode(), null)) {
                    return error("放行编号已存在：" + record.getReleaseCode());
                }
            } else {
                record.setReleaseCode(releaseReviewService.generateCode());
            }
            record.setCreatedTime(LocalDateTime.now());
            record.setIsDeleted(0);
            // 自动生成的编号若因并发冲突触发唯一索引，重新生成并重试（最多3次）
            int attempts = 0;
            while (true) {
                try {
                    releaseReviewService.save(record);
                    break;
                } catch (DuplicateKeyException e) {
                    if (!autoGenerated || ++attempts >= 3) {
                        throw e;
                    }
                    record.setReleaseCode(releaseReviewService.generateCode());
                }
            }
            return success(record, "新增成功");
        } catch (Exception e) {
            return exception(e, "新增审核放行");
        }
    }

    // endregion

    // region 修改与删除接口
    // ===================================
    // 修改与删除接口
    // ===================================

    /**
     * 修改审核放行
     *
     * 示例请求：
     * PUT /api/releaseReview/1
     * Content-Type: application/json
     * {
     *   "releaseConclusion": "拒绝放行",
     *   "reviewOpinion": "外观性状不符合标准"
     * }
     *
     * @param id     审核放行ID（路径参数）
     * @param record 更新的审核放行信息
     * @return ApiResponse&lt;ReleaseReview&gt; 修改后的审核放行
     */
    @PutMapping("/{id}")
    public ApiResponse<ReleaseReview> update(
            @PathVariable Long id,
            @RequestBody ReleaseReview record) {
        try {
            ReleaseReview existing = releaseReviewService.getById(id);
            if (existing == null) {
                return error("审核放行不存在");
            }
            if ("拒绝放行".equals(record.getReleaseConclusion()) && !StringUtils.hasText(record.getReviewOpinion())) {
                return error("拒绝放行时审核意见必填");
            }
            if (StringUtils.hasText(record.getReleaseCode())
                    && !record.getReleaseCode().equals(existing.getReleaseCode())
                    && !releaseReviewService.isCodeUnique(record.getReleaseCode(), id)) {
                return error("放行编号已存在：" + record.getReleaseCode());
            }
            record.setId(id);
            record.setUpdatedTime(LocalDateTime.now());
            releaseReviewService.updateById(record);
            return success(record, "修改成功");
        } catch (Exception e) {
            return exception(e, "修改审核放行");
        }
    }

    /**
     * 删除审核放行（软删除）
     *
     * 示例请求：
     * DELETE /api/releaseReview/1
     *
     * @param id 审核放行ID（路径参数）
     * @return ApiResponse&lt;Void&gt; 删除结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        try {
            ReleaseReview existing = releaseReviewService.getById(id);
            if (existing == null) {
                return error("审核放行不存在");
            }
            releaseReviewService.removeById(id);
            return success(null, "删除成功");
        } catch (Exception e) {
            return exception(e, "删除审核放行");
        }
    }

    // endregion

    // region 私有工具方法
    // ===================================
    // 私有工具方法
    // ===================================

    /**
     * 构建审核放行查询条件
     *
     * @param releaseCode        放行编号（模糊匹配）
     * @param relatedInspectionCode 关联检验编号（模糊匹配）
     * @param objectName         被检对象名称（模糊匹配）
     * @param batchNo            批号（模糊匹配）
     * @param releaseConclusion  放行结论（精确匹配）
     * @param reviewer           审核人（模糊匹配）
     * @param startTime          审核开始时间
     * @param endTime            审核结束时间
     * @return 查询条件Wrapper
     */
    private QueryWrapper<ReleaseReview> buildQueryWrapper(String releaseCode, String relatedInspectionCode,
            String objectName, String batchNo, String releaseConclusion, String reviewer,
            String startTime, String endTime) {
        QueryWrapper<ReleaseReview> wrapper = new QueryWrapper<>();
        wrapper.eq("is_deleted", 0);
        if (StringUtils.hasText(releaseCode)) wrapper.like("release_code", releaseCode);
        if (StringUtils.hasText(relatedInspectionCode)) wrapper.like("related_inspection_code", relatedInspectionCode);
        if (StringUtils.hasText(objectName)) wrapper.like("object_name", objectName);
        if (StringUtils.hasText(batchNo)) wrapper.like("batch_no", batchNo);
        if (StringUtils.hasText(releaseConclusion)) wrapper.eq("release_conclusion", releaseConclusion);
        if (StringUtils.hasText(reviewer)) wrapper.like("reviewer", reviewer);
        if (StringUtils.hasText(startTime)) wrapper.ge("review_time", startTime);
        if (StringUtils.hasText(endTime)) wrapper.le("review_time", endTime);
        wrapper.orderByDesc("review_time");
        return wrapper;
    }

    // endregion
}