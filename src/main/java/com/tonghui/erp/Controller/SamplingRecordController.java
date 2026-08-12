package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.SamplingRecord;
import com.tonghui.erp.Service.SamplingRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 取样记录控制器
 * <p>
 * 提供取样记录的CRUD操作、条件分页查询及取样编号生成，
 * 用于质量检验模块的取样过程记录管理
 * </p>
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────┬────────┬──────────────────────────────┐
 * │ #  │ 接口                             │ 方法   │ 说明                         │
 * ├────┼──────────────────────────────────┼────────┼──────────────────────────────┤
 * │ 1  │ /api/samplingRecord              │ GET    │ 分页查询取样记录列表          │
 * │ 2  │ /api/samplingRecord/list         │ GET    │ 查询取样记录列表（不分页）    │
 * │ 3  │ /api/samplingRecord/generateCode │ GET    │ 获取自动生成的取样编号        │
 * │ 4  │ /api/samplingRecord/{id}         │ GET    │ 查询取样记录详情              │
 * │ 5  │ /api/samplingRecord              │ POST   │ 新增取样记录                  │
 * │ 6  │ /api/samplingRecord/{id}         │ PUT    │ 修改取样记录                  │
 * │ 7  │ /api/samplingRecord/{id}         │ DELETE │ 删除取样记录（软删除）        │
 * └────┴──────────────────────────────────┴────────┴──────────────────────────────┘
 */
@RestController
@RequestMapping("/api/samplingRecord")
public class SamplingRecordController extends BaseController {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 取样记录服务
     */
    @Autowired
    private SamplingRecordService samplingRecordService;

    // endregion

    // region 查询接口
    // ===================================
    // 查询接口
    // ===================================

    /**
     * 分页查询取样记录列表
     *
     * 示例请求：
     * GET /api/samplingRecord?samplingCode=QY-20260720001&objectName=维生素C片&samplingLocation=洁净区&samplingMethod=随机&startTime=2026-07-01 00:00:00&endTime=2026-07-31 23:59:59&pageIndex=0&pageSize=10
     *
     * @param samplingCode    取样编号（可选，模糊匹配）
     * @param relatedPlanCode 关联检验计划编号（可选，模糊匹配）
     * @param objectName      被检对象名称（可选，模糊匹配）
     * @param batchNo         批号（可选，模糊匹配）
     * @param samplingLocation 取样地点（可选，模糊匹配）
     * @param samplingMethod  取样方法（可选，精确匹配）
     * @param sampler         取样人（可选，模糊匹配）
     * @param startTime       取样开始时间（可选，格式：yyyy-MM-dd HH:mm:ss）
     * @param endTime         取样结束时间（可选，格式：yyyy-MM-dd HH:mm:ss）
     * @param pageIndex       页码索引，从0开始（默认0）
     * @param pageSize        每页数量（默认10）
     * @return ApiResponse&lt;PagedResult&lt;SamplingRecord&gt;&gt; 分页结果
     */
    @GetMapping
    public ApiResponse<PagedResult<SamplingRecord>> getAll(
            @RequestParam(required = false) String samplingCode,
            @RequestParam(required = false) String relatedPlanCode,
            @RequestParam(required = false) String objectName,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) String samplingLocation,
            @RequestParam(required = false) String samplingMethod,
            @RequestParam(required = false) String sampler,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            Page<SamplingRecord> page = new Page<>(pageIndex + 1, pageSize);
            QueryWrapper<SamplingRecord> wrapper = buildQueryWrapper(samplingCode, relatedPlanCode, objectName,
                    batchNo, samplingLocation, samplingMethod, sampler, startTime, endTime);
            Page<SamplingRecord> pageResult = samplingRecordService.page(page, wrapper);

            PagedResult<SamplingRecord> result = new PagedResult<>();
            result.setItems(pageResult.getRecords());
            result.setTotalCount(pageResult.getTotal());
            result.setPageIndex(pageIndex);
            result.setPageSize(pageSize);
            return success(result);
        } catch (Exception e) {
            return exception(e, "查询取样记录");
        }
    }

    /**
     * 查询取样记录列表（不分页）
     *
     * 示例请求：
     * GET /api/samplingRecord/list?samplingMethod=随机
     *
     * @param samplingCode    取样编号（可选，模糊匹配）
     * @param relatedPlanCode 关联检验计划编号（可选，模糊匹配）
     * @param objectName      被检对象名称（可选，模糊匹配）
     * @param batchNo         批号（可选，模糊匹配）
     * @param samplingLocation 取样地点（可选，模糊匹配）
     * @param samplingMethod  取样方法（可选，精确匹配）
     * @param sampler         取样人（可选，模糊匹配）
     * @param startTime       取样开始时间（可选，格式：yyyy-MM-dd HH:mm:ss）
     * @param endTime         取样结束时间（可选，格式：yyyy-MM-dd HH:mm:ss）
     * @return ApiResponse&lt;List&lt;SamplingRecord&gt;&gt; 取样记录列表
     */
    @GetMapping("/list")
    public ApiResponse<List<SamplingRecord>> getList(
            @RequestParam(required = false) String samplingCode,
            @RequestParam(required = false) String relatedPlanCode,
            @RequestParam(required = false) String objectName,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) String samplingLocation,
            @RequestParam(required = false) String samplingMethod,
            @RequestParam(required = false) String sampler,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        try {
            QueryWrapper<SamplingRecord> wrapper = buildQueryWrapper(samplingCode, relatedPlanCode, objectName,
                    batchNo, samplingLocation, samplingMethod, sampler, startTime, endTime);
            wrapper.orderByDesc("sampling_time");
            return success(samplingRecordService.list(wrapper));
        } catch (Exception e) {
            return exception(e, "查询取样记录");
        }
    }

    /**
     * 查询取样记录详情
     *
     * 示例请求：
     * GET /api/samplingRecord/1
     *
     * @param id 取样记录ID（路径参数）
     * @return ApiResponse&lt;SamplingRecord&gt; 取样记录详情
     */
    @GetMapping("/{id}")
    public ApiResponse<SamplingRecord> getById(@PathVariable Long id) {
        try {
            SamplingRecord record = samplingRecordService.getById(id);
            if (record == null) {
                return error("取样记录不存在");
            }
            return success(record);
        } catch (Exception e) {
            return exception(e, "查询取样记录详情");
        }
    }

    /**
     * 获取自动生成的取样编号
     *
     * 示例请求：
     * GET /api/samplingRecord/generateCode
     *
     * @return ApiResponse&lt;String&gt; 自动生成的取样编号（格式QY-YYYYMMDD-NNN）
     */
    @GetMapping("/generateCode")
    public ApiResponse<String> generateCode() {
        try {
            return success(samplingRecordService.generateCode());
        } catch (Exception e) {
            return exception(e, "生成取样编号");
        }
    }

    // endregion

    // region 新增接口
    // ===================================
    // 新增接口
    // ===================================

    /**
     * 新增取样记录
     *
     * 示例请求：
     * POST /api/samplingRecord
     * Content-Type: application/json
     * {
     *   "samplingCode": "QY-20260720-001",
     *   "relatedPlanCode": "JH-20260720-001",
     *   "objectName": "维生素C片",
     *   "batchNo": "20260701",
     *   "spec": "100mg",
     *   "samplingLocation": "洁净区",
     *   "samplingQuantity": "100g × 3份",
     *   "samplingCount": 3,
     *   "samplingMethod": "随机",
     *   "sampler": "李四",
     *   "samplingTime": "2026-07-20 09:30:00",
     *   "remark": "按检验计划取样"
     * }
     *
     * @param record 取样记录信息（编号为空时系统自动生成）
     * @return ApiResponse&lt;SamplingRecord&gt; 新增的取样记录
     */
    @PostMapping
    public ApiResponse<SamplingRecord> create(@RequestBody SamplingRecord record) {
        try {
            // 是否由系统自动生成编号（用于唯一索引冲突时的重试决策）
            boolean autoGenerated = !StringUtils.hasText(record.getSamplingCode());
            if (!autoGenerated) {
                // 手动传入编号时校验唯一性（含软删除记录，避免唯一索引冲突）
                if (!samplingRecordService.isCodeUnique(record.getSamplingCode(), null)) {
                    return error("取样编号已存在：" + record.getSamplingCode());
                }
            } else {
                record.setSamplingCode(samplingRecordService.generateCode());
            }
            record.setCreatedTime(LocalDateTime.now());
            record.setIsDeleted(0);
            // 自动生成的编号若因并发冲突触发唯一索引，重新生成并重试（最多3次）
            int attempts = 0;
            while (true) {
                try {
                    samplingRecordService.save(record);
                    break;
                } catch (DuplicateKeyException e) {
                    if (!autoGenerated || ++attempts >= 3) {
                        throw e;
                    }
                    record.setSamplingCode(samplingRecordService.generateCode());
                }
            }
            return success(record, "新增成功");
        } catch (Exception e) {
            return exception(e, "新增取样记录");
        }
    }

    // endregion

    // region 修改与删除接口
    // ===================================
    // 修改与删除接口
    // ===================================

    /**
     * 修改取样记录
     *
     * 示例请求：
     * PUT /api/samplingRecord/1
     * Content-Type: application/json
     * {
     *   "samplingQuantity": "100g × 4份",
     *   "samplingCount": 4
     * }
     *
     * @param id     取样记录ID（路径参数）
     * @param record 更新的取样记录信息
     * @return ApiResponse&lt;SamplingRecord&gt; 修改后的取样记录
     */
    @PutMapping("/{id}")
    public ApiResponse<SamplingRecord> update(
            @PathVariable Long id,
            @RequestBody SamplingRecord record) {
        try {
            SamplingRecord existing = samplingRecordService.getById(id);
            if (existing == null) {
                return error("取样记录不存在");
            }
            if (StringUtils.hasText(record.getSamplingCode())
                    && !record.getSamplingCode().equals(existing.getSamplingCode())
                    && !samplingRecordService.isCodeUnique(record.getSamplingCode(), id)) {
                return error("取样编号已存在：" + record.getSamplingCode());
            }
            record.setId(id);
            record.setUpdatedTime(LocalDateTime.now());
            samplingRecordService.updateById(record);
            return success(record, "修改成功");
        } catch (Exception e) {
            return exception(e, "修改取样记录");
        }
    }

    /**
     * 删除取样记录（软删除）
     *
     * 示例请求：
     * DELETE /api/samplingRecord/1
     *
     * @param id 取样记录ID（路径参数）
     * @return ApiResponse&lt;Void&gt; 删除结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        try {
            SamplingRecord existing = samplingRecordService.getById(id);
            if (existing == null) {
                return error("取样记录不存在");
            }
            samplingRecordService.removeById(id);
            return success(null, "删除成功");
        } catch (Exception e) {
            return exception(e, "删除取样记录");
        }
    }

    // endregion

    // region 私有工具方法
    // ===================================
    // 私有工具方法
    // ===================================

    /**
     * 构建取样记录查询条件
     *
     * @param samplingCode    取样编号（模糊匹配）
     * @param relatedPlanCode 关联检验计划编号（模糊匹配）
     * @param objectName      被检对象名称（模糊匹配）
     * @param batchNo         批号（模糊匹配）
     * @param samplingLocation 取样地点（模糊匹配）
     * @param samplingMethod  取样方法（精确匹配）
     * @param sampler         取样人（模糊匹配）
     * @param startTime       取样开始时间
     * @param endTime         取样结束时间
     * @return 查询条件Wrapper
     */
    private QueryWrapper<SamplingRecord> buildQueryWrapper(String samplingCode, String relatedPlanCode, String objectName,
            String batchNo, String samplingLocation, String samplingMethod, String sampler,
            String startTime, String endTime) {
        QueryWrapper<SamplingRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("is_deleted", 0);
        if (StringUtils.hasText(samplingCode)) wrapper.like("sampling_code", samplingCode);
        if (StringUtils.hasText(relatedPlanCode)) wrapper.like("related_plan_code", relatedPlanCode);
        if (StringUtils.hasText(objectName)) wrapper.like("object_name", objectName);
        if (StringUtils.hasText(batchNo)) wrapper.like("batch_no", batchNo);
        if (StringUtils.hasText(samplingLocation)) wrapper.like("sampling_location", samplingLocation);
        if (StringUtils.hasText(samplingMethod)) wrapper.eq("sampling_method", samplingMethod);
        if (StringUtils.hasText(sampler)) wrapper.like("sampler", sampler);
        if (StringUtils.hasText(startTime)) wrapper.ge("sampling_time", startTime);
        if (StringUtils.hasText(endTime)) wrapper.le("sampling_time", endTime);
        wrapper.orderByDesc("sampling_time");
        return wrapper;
    }

    // endregion
}