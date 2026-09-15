package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.InspectionRecord;
import com.tonghui.erp.Data.Entity.ReleaseReview;
import com.tonghui.erp.Data.mapper.InspectionRecordMapper;
import com.tonghui.erp.Service.ReleaseReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    /**
     * 检验记录数据访问层
     */
    @Autowired
    private InspectionRecordMapper inspectionRecordMapper;

    /**
     * 请检记录数据访问层（用于关联链路查找工单ID）
     */
    @Autowired
    private com.tonghui.erp.Data.mapper.InspectionRequestMapper inspectionRequestMapper;

    /**
     * 工单服务（用于时间回填联动）
     */
    @Autowired
    private com.tonghui.erp.Service.WorkOrderService workOrderService;

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
     * @param createdTimeStart   创建时间起始（可选，格式：yyyy-MM-dd HH:mm:ss）
     * @param createdTimeEnd     创建时间结束（可选，格式：yyyy-MM-dd HH:mm:ss）
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
            @RequestParam(required = false) String createdTimeStart,
            @RequestParam(required = false) String createdTimeEnd,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            Page<ReleaseReview> page = new Page<>(pageIndex + 1, pageSize);
            QueryWrapper<ReleaseReview> wrapper = buildQueryWrapper(releaseCode, relatedInspectionCode, objectName,
                    batchNo, releaseConclusion, reviewer, startTime, endTime, createdTimeStart, createdTimeEnd);
            Page<ReleaseReview> pageResult = releaseReviewService.page(page, wrapper);

            // 填充关联的生产任务编号和制剂信息
            fillWorkOrderInfo(pageResult.getRecords());

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
                    batchNo, releaseConclusion, reviewer, startTime, endTime, null, null);
            wrapper.orderByDesc("review_time");
            List<ReleaseReview> list = releaseReviewService.list(wrapper);
            fillWorkOrderInfo(list);
            return success(list);
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
            fillWorkOrderInfo(List.of(record));
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
     * 必填字段：
     *   objectName        被检对象名称（必填）
     *   batchNo           批号（必填）
     *   releaseConclusion 放行结论（必填）
     *   reviewer          审核人（必填）
     *   reviewTime        审核时间（必填）
     * 条件必填：
     *   reviewOpinion     审核意见（放行结论=拒绝放行时必填）
     * 其他说明：
     *   releaseCode 为空时系统自动生成（格式FX-YYYYMMDD-NNN），亦可手动传入，须唯一
     *   以下字段选填：relatedInspectionCode、spec、remark
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
                    // 回填工单审核放行时间（auditReleaseTime），仅放行结论时触发
                    if ("放行".equals(record.getReleaseConclusion())) {
                        backfillWorkOrderAuditReleaseTime(record);
                    }
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
     * 必填字段说明：同新增接口（releaseCode 编号除外），修改时仅传需变更字段
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
            // 获取更新后的完整记录并触发回填（结论改为放行时回填工单审核放行时间）
            ReleaseReview updated = releaseReviewService.getById(id);
            if (updated != null && "放行".equals(updated.getReleaseConclusion())) {
                backfillWorkOrderAuditReleaseTime(updated);
            }
            return success(updated != null ? updated : record, "修改成功");
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
     * 批量填充审核放行记录关联的生产任务编号和制剂信息
     * <p>
     * 通过 related_inspection_record_code 关联检验记录，获取工单编号和制剂信息
     * </p>
     *
     * @param records 审核放行记录列表
     */
    private void fillWorkOrderInfo(List<ReleaseReview> records) {
        if (records == null || records.isEmpty()) {
            return;
        }

        // 获取所有关联的检验记录编号
        List<String> inspectionRecordCodes = records.stream()
                .map(ReleaseReview::getRelatedInspectionRecordCode)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());

        if (inspectionRecordCodes.isEmpty()) {
            return;
        }

        // 批量查询检验记录（仅查询真实存在的数据库列）
        QueryWrapper<InspectionRecord> recordWrapper = new QueryWrapper<>();
        recordWrapper.in("inspection_code", inspectionRecordCodes);
        recordWrapper.select("inspection_code", "work_order_id", "work_order_code", "preparation_name");
        Map<String, InspectionRecord> recordMap = inspectionRecordMapper.selectList(recordWrapper).stream()
                .collect(Collectors.toMap(InspectionRecord::getInspectionCode, r -> r, (a, b) -> a));

        // 回填信息（仅填充数据库中为空的字段，兼容旧行数据）
        for (ReleaseReview record : records) {
            if (StringUtils.hasText(record.getRelatedInspectionRecordCode())) {
                InspectionRecord inspectionRecord = recordMap.get(record.getRelatedInspectionRecordCode());
                if (inspectionRecord != null) {
                    if (record.getWorkOrderId() == null) {
                        record.setWorkOrderId(inspectionRecord.getWorkOrderId());
                    }
                    if (!StringUtils.hasText(record.getWorkOrderCode())) {
                        record.setWorkOrderCode(inspectionRecord.getWorkOrderCode());
                    }
                    if (!StringUtils.hasText(record.getPreparationName())) {
                        record.setPreparationName(inspectionRecord.getPreparationName());
                    }
                }
            }
        }
    }

    /**
     * 回填工单审核放行时间
     * <p>
     * 通过审核放行记录的 relatedInspectionRecordCode 关联检验记录，
     * 再通过检验记录的 relatedInspectionRequestCode 关联请检记录，
     * 最后从请检记录中获取工单ID，将审核放行时间回填到工单
     * </p>
     *
     * @param record 审核放行记录（需包含 relatedInspectionRecordCode 和 reviewTime）
     */
    private void backfillWorkOrderAuditReleaseTime(ReleaseReview record) {
        if (record.getReviewTime() == null) {
            return;
        }

        // 优先使用审核放行记录直接关联的工单ID
        Long workOrderId = record.getWorkOrderId();
        String itemCategory = "成品";

        // 未直接关联工单时，通过链路回退查询（兼容旧数据）
        if (workOrderId == null) {
            // 优先用 relatedInspectionRecordCode，其次用 relatedInspectionCode
            String inspectionCodeToUse = StringUtils.hasText(record.getRelatedInspectionRecordCode())
                    ? record.getRelatedInspectionRecordCode()
                    : record.getRelatedInspectionCode();
            if (StringUtils.hasText(inspectionCodeToUse)) {
                // 通过检验记录编号查找检验记录，获取关联请检编号
                QueryWrapper<InspectionRecord> recordWrapper = new QueryWrapper<>();
                recordWrapper.eq("inspection_code", inspectionCodeToUse);
                recordWrapper.select("work_order_id", "related_inspection_request_code", "item_category");
                recordWrapper.last("LIMIT 1");
                InspectionRecord inspectionRecord = inspectionRecordMapper.selectOne(recordWrapper);

                if (inspectionRecord == null) {
                    return;
                }

                // 优先从检验记录直接取工单ID
                if (inspectionRecord.getWorkOrderId() != null) {
                    workOrderId = inspectionRecord.getWorkOrderId();
                    itemCategory = inspectionRecord.getItemCategory();
                } else if (StringUtils.hasText(inspectionRecord.getRelatedInspectionRequestCode())) {
                    // 通过请检编号查找请检记录，获取工单ID和被检品属性
                    QueryWrapper<com.tonghui.erp.Data.Entity.InspectionRequest> requestWrapper = new QueryWrapper<>();
                    requestWrapper.eq("inspection_code", inspectionRecord.getRelatedInspectionRequestCode());
                    requestWrapper.select("work_order_id", "item_category");
                    requestWrapper.last("LIMIT 1");
                    com.tonghui.erp.Data.Entity.InspectionRequest inspectionRequest = inspectionRequestMapper.selectOne(requestWrapper);
                    if (inspectionRequest == null) {
                        return;
                    }
                    workOrderId = inspectionRequest.getWorkOrderId();
                    itemCategory = inspectionRequest.getItemCategory();
                }
            }
        }

        // 仅成品类型回填工单审核放行时间
        if (workOrderId != null && "成品".equals(itemCategory)) {
            workOrderService.syncWorkOrderTime(workOrderId, "auditReleaseTime", record.getReviewTime());
        }
    }

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
     * @param createdTimeStart   创建时间起始
     * @param createdTimeEnd     创建时间结束
     * @return 查询条件Wrapper
     */
    private QueryWrapper<ReleaseReview> buildQueryWrapper(String releaseCode, String relatedInspectionCode,
            String objectName, String batchNo, String releaseConclusion, String reviewer,
            String startTime, String endTime, String createdTimeStart, String createdTimeEnd) {
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
        if (StringUtils.hasText(createdTimeStart)) wrapper.ge("created_time", createdTimeStart);
        if (StringUtils.hasText(createdTimeEnd)) wrapper.le("created_time", createdTimeEnd);
        wrapper.orderByDesc("review_time");
        return wrapper;
    }

    // endregion
}