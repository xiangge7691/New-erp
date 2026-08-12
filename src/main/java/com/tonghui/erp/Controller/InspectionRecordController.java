package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.InspectionRecord;
import com.tonghui.erp.Service.InspectionRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 检验记录控制器
 * <p>
 * 提供检验记录的CRUD操作、条件分页查询及检验编号生成，
 * 用于质量检验模块的检验数据录入管理（承载报告书附件）
 * </p>
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────┬────────┬──────────────────────────────┐
 * │ #  │ 接口                             │ 方法   │ 说明                         │
 * ├────┼──────────────────────────────────┼────────┼──────────────────────────────┤
 * │ 1  │ /api/inspectionRecord            │ GET    │ 分页查询检验记录列表          │
 * │ 2  │ /api/inspectionRecord/list       │ GET    │ 查询检验记录列表（不分页）    │
 * │ 3  │ /api/inspectionRecord/generateCode │ GET  │ 获取自动生成的检验编号        │
 * │ 4  │ /api/inspectionRecord/{id}       │ GET    │ 查询检验记录详情              │
 * │ 5  │ /api/inspectionRecord            │ POST   │ 新增检验记录                  │
 * │ 6  │ /api/inspectionRecord/{id}       │ PUT    │ 修改检验记录                  │
 * │ 7  │ /api/inspectionRecord/{id}       │ DELETE │ 删除检验记录（软删除）        │
 * └────┴──────────────────────────────────┴────────┴──────────────────────────────┘
 *
 * 报告书说明：检验记录的报告书通过全局文件接口上传关联，见 FileController 业务类型 QUALITY_INSPECTION_REPORT
 */
@RestController
@RequestMapping("/api/inspectionRecord")
public class InspectionRecordController extends BaseController {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 检验记录服务
     */
    @Autowired
    private InspectionRecordService inspectionRecordService;

    // endregion

    // region 查询接口
    // ===================================
    // 查询接口
    // ===================================

    /**
     * 分页查询检验记录列表
     *
     * 示例请求：
     * GET /api/inspectionRecord?inspectionCode=JY-20260720001&objectName=维生素C片&conclusion=合格&startTime=2026-07-01 00:00:00&endTime=2026-07-31 23:59:59&pageIndex=0&pageSize=10
     *
     * @param inspectionCode        检验编号（可选，模糊匹配）
     * @param relatedSamplingCode   关联取样编号（可选，模糊匹配）
     * @param objectName            被检对象名称（可选，模糊匹配）
     * @param batchNo               批号（可选，模糊匹配）
     * @param inspectionBasis       检验依据（可选，模糊匹配）
     * @param inspector             检验人（可选，模糊匹配）
     * @param conclusion            总体结论（可选，精确匹配）
     * @param startTime             检验开始开始时间（可选，格式：yyyy-MM-dd HH:mm:ss）
     * @param endTime               检验开始结束时间（可选，格式：yyyy-MM-dd HH:mm:ss）
     * @param pageIndex             页码索引，从0开始（默认0）
     * @param pageSize              每页数量（默认10）
     * @return ApiResponse&lt;PagedResult&lt;InspectionRecord&gt;&gt; 分页结果
     */
    @GetMapping
    public ApiResponse<PagedResult<InspectionRecord>> getAll(
            @RequestParam(required = false) String inspectionCode,
            @RequestParam(required = false) String relatedSamplingCode,
            @RequestParam(required = false) String objectName,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) String inspectionBasis,
            @RequestParam(required = false) String inspector,
            @RequestParam(required = false) String conclusion,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            Page<InspectionRecord> page = new Page<>(pageIndex + 1, pageSize);
            QueryWrapper<InspectionRecord> wrapper = buildQueryWrapper(inspectionCode, relatedSamplingCode,
                    objectName, batchNo, inspectionBasis, inspector, conclusion, startTime, endTime);
            Page<InspectionRecord> pageResult = inspectionRecordService.page(page, wrapper);

            PagedResult<InspectionRecord> result = new PagedResult<>();
            result.setItems(pageResult.getRecords());
            result.setTotalCount(pageResult.getTotal());
            result.setPageIndex(pageIndex);
            result.setPageSize(pageSize);
            return success(result);
        } catch (Exception e) {
            return exception(e, "查询检验记录");
        }
    }

    /**
     * 查询检验记录列表（不分页）
     *
     * 示例请求：
     * GET /api/inspectionRecord/list?conclusion=合格
     *
     * @param inspectionCode        检验编号（可选，模糊匹配）
     * @param relatedSamplingCode   关联取样编号（可选，模糊匹配）
     * @param objectName            被检对象名称（可选，模糊匹配）
     * @param batchNo               批号（可选，模糊匹配）
     * @param inspectionBasis       检验依据（可选，模糊匹配）
     * @param inspector             检验人（可选，模糊匹配）
     * @param conclusion            总体结论（可选，精确匹配）
     * @param startTime             检验开始开始时间（可选，格式：yyyy-MM-dd HH:mm:ss）
     * @param endTime               检验开始结束时间（可选，格式：yyyy-MM-dd HH:mm:ss）
     * @return ApiResponse&lt;List&lt;InspectionRecord&gt;&gt; 检验记录列表
     */
    @GetMapping("/list")
    public ApiResponse<List<InspectionRecord>> getList(
            @RequestParam(required = false) String inspectionCode,
            @RequestParam(required = false) String relatedSamplingCode,
            @RequestParam(required = false) String objectName,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) String inspectionBasis,
            @RequestParam(required = false) String inspector,
            @RequestParam(required = false) String conclusion,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        try {
            QueryWrapper<InspectionRecord> wrapper = buildQueryWrapper(inspectionCode, relatedSamplingCode,
                    objectName, batchNo, inspectionBasis, inspector, conclusion, startTime, endTime);
            wrapper.orderByDesc("start_time");
            return success(inspectionRecordService.list(wrapper));
        } catch (Exception e) {
            return exception(e, "查询检验记录");
        }
    }

    /**
     * 查询检验记录详情
     *
     * 示例请求：
     * GET /api/inspectionRecord/1
     *
     * @param id 检验记录ID（路径参数）
     * @return ApiResponse&lt;InspectionRecord&gt; 检验记录详情
     */
    @GetMapping("/{id}")
    public ApiResponse<InspectionRecord> getById(@PathVariable Long id) {
        try {
            InspectionRecord record = inspectionRecordService.getById(id);
            if (record == null) {
                return error("检验记录不存在");
            }
            return success(record);
        } catch (Exception e) {
            return exception(e, "查询检验记录详情");
        }
    }

    /**
     * 获取自动生成的检验编号
     *
     * 示例请求：
     * GET /api/inspectionRecord/generateCode
     *
     * @return ApiResponse&lt;String&gt; 自动生成的检验编号（格式JY-YYYYMMDD-NNN）
     */
    @GetMapping("/generateCode")
    public ApiResponse<String> generateCode() {
        try {
            return success(inspectionRecordService.generateCode());
        } catch (Exception e) {
            return exception(e, "生成检验编号");
        }
    }

    // endregion

    // region 新增接口
    // ===================================
    // 新增接口
    // ===================================

    /**
     * 新增检验记录
     *
     * 示例请求：
     * POST /api/inspectionRecord
     * Content-Type: application/json
     * {
     *   "inspectionCode": "JY-20260720-001",
     *   "relatedSamplingCode": "QY-20260720-001",
     *   "objectName": "维生素C片",
     *   "batchNo": "20260701",
     *   "spec": "100mg",
     *   "inspectionBasis": "中国药典2020版",
     *   "inspectionItem": "含量测定、崩解时限、外观性状",
     *   "inspector": "王五",
     *   "reviewer": "赵六",
     *   "startTime": "2026-07-20 10:00:00",
     *   "endTime": "2026-07-20 11:30:00",
     *   "conclusion": "合格",
     *   "remark": "检验通过"
     * }
     *
     * @param record 检验记录信息（编号为空时系统自动生成）
     * @return ApiResponse&lt;InspectionRecord&gt; 新增的检验记录
     */
    @PostMapping
    public ApiResponse<InspectionRecord> create(@RequestBody InspectionRecord record) {
        try {
            // 是否由系统自动生成编号（用于唯一索引冲突时的重试决策）
            boolean autoGenerated = !StringUtils.hasText(record.getInspectionCode());
            if (!autoGenerated) {
                // 手动传入编号时校验唯一性（含软删除记录，避免唯一索引冲突）
                if (!inspectionRecordService.isCodeUnique(record.getInspectionCode(), null)) {
                    return error("检验编号已存在：" + record.getInspectionCode());
                }
            } else {
                record.setInspectionCode(inspectionRecordService.generateCode());
            }
            record.setCreatedTime(LocalDateTime.now());
            record.setIsDeleted(0);
            // 自动生成的编号若因并发冲突触发唯一索引，重新生成并重试（最多3次）
            int attempts = 0;
            while (true) {
                try {
                    inspectionRecordService.save(record);
                    break;
                } catch (DuplicateKeyException e) {
                    if (!autoGenerated || ++attempts >= 3) {
                        throw e;
                    }
                    record.setInspectionCode(inspectionRecordService.generateCode());
                }
            }
            return success(record, "新增成功");
        } catch (Exception e) {
            return exception(e, "新增检验记录");
        }
    }

    // endregion

    // region 修改与删除接口
    // ===================================
    // 修改与删除接口
    // ===================================

    /**
     * 修改检验记录
     *
     * 示例请求：
     * PUT /api/inspectionRecord/1
     * Content-Type: application/json
     * {
     *   "conclusion": "不合格",
     *   "remark": "含量超出标准范围"
     * }
     *
     * @param id     检验记录ID（路径参数）
     * @param record 更新的检验记录信息
     * @return ApiResponse&lt;InspectionRecord&gt; 修改后的检验记录
     */
    @PutMapping("/{id}")
    public ApiResponse<InspectionRecord> update(
            @PathVariable Long id,
            @RequestBody InspectionRecord record) {
        try {
            InspectionRecord existing = inspectionRecordService.getById(id);
            if (existing == null) {
                return error("检验记录不存在");
            }
            if (StringUtils.hasText(record.getInspectionCode())
                    && !record.getInspectionCode().equals(existing.getInspectionCode())
                    && !inspectionRecordService.isCodeUnique(record.getInspectionCode(), id)) {
                return error("检验编号已存在：" + record.getInspectionCode());
            }
            record.setId(id);
            record.setUpdatedTime(LocalDateTime.now());
            inspectionRecordService.updateById(record);
            return success(record, "修改成功");
        } catch (Exception e) {
            return exception(e, "修改检验记录");
        }
    }

    /**
     * 删除检验记录（软删除）
     *
     * 示例请求：
     * DELETE /api/inspectionRecord/1
     *
     * @param id 检验记录ID（路径参数）
     * @return ApiResponse&lt;Void&gt; 删除结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        try {
            InspectionRecord existing = inspectionRecordService.getById(id);
            if (existing == null) {
                return error("检验记录不存在");
            }
            inspectionRecordService.removeById(id);
            return success(null, "删除成功");
        } catch (Exception e) {
            return exception(e, "删除检验记录");
        }
    }

    // endregion

    // region 私有工具方法
    // ===================================
    // 私有工具方法
    // ===================================

    /**
     * 构建检验记录查询条件
     *
     * @param inspectionCode      检验编号（模糊匹配）
     * @param relatedSamplingCode 关联取样编号（模糊匹配）
     * @param objectName          被检对象名称（模糊匹配）
     * @param batchNo             批号（模糊匹配）
     * @param inspectionBasis     检验依据（模糊匹配）
     * @param inspector           检验人（模糊匹配）
     * @param conclusion          总体结论（精确匹配）
     * @param startTime           检验开始开始时间
     * @param endTime             检验开始结束时间
     * @return 查询条件Wrapper
     */
    private QueryWrapper<InspectionRecord> buildQueryWrapper(String inspectionCode, String relatedSamplingCode,
            String objectName, String batchNo, String inspectionBasis, String inspector, String conclusion,
            String startTime, String endTime) {
        QueryWrapper<InspectionRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("is_deleted", 0);
        if (StringUtils.hasText(inspectionCode)) wrapper.like("inspection_code", inspectionCode);
        if (StringUtils.hasText(relatedSamplingCode)) wrapper.like("related_sampling_code", relatedSamplingCode);
        if (StringUtils.hasText(objectName)) wrapper.like("object_name", objectName);
        if (StringUtils.hasText(batchNo)) wrapper.like("batch_no", batchNo);
        if (StringUtils.hasText(inspectionBasis)) wrapper.like("inspection_basis", inspectionBasis);
        if (StringUtils.hasText(inspector)) wrapper.like("inspector", inspector);
        if (StringUtils.hasText(conclusion)) wrapper.eq("conclusion", conclusion);
        if (StringUtils.hasText(startTime)) wrapper.ge("start_time", startTime);
        if (StringUtils.hasText(endTime)) wrapper.le("start_time", endTime);
        wrapper.orderByDesc("start_time");
        return wrapper;
    }

    // endregion
}