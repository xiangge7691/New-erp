package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.RetainedSample;
import com.tonghui.erp.Service.RetainedSampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 留样记录控制器
 * <p>
 * 提供留样记录的CRUD操作、条件分页查询及留样编号生成，
 * 用于质量检验模块的留样管理与定期观察记录
 * </p>
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────┬────────┬──────────────────────────────┐
 * │ #  │ 接口                             │ 方法   │ 说明                         │
 * ├────┼──────────────────────────────────┼────────┼──────────────────────────────┤
 * │ 1  │ /api/retainedSample              │ GET    │ 分页查询留样记录列表          │
 * │ 2  │ /api/retainedSample/list         │ GET    │ 查询留样记录列表（不分页）    │
 * │ 3  │ /api/retainedSample/generateCode │ GET    │ 获取自动生成的留样编号        │
 * │ 4  │ /api/retainedSample/{id}         │ GET    │ 查询留样记录详情              │
 * │ 5  │ /api/retainedSample              │ POST   │ 新增留样记录                  │
 * │ 6  │ /api/retainedSample/{id}         │ PUT    │ 修改留样记录                  │
 * │ 7  │ /api/retainedSample/{id}         │ DELETE │ 删除留样记录（软删除）        │
 * └────┴──────────────────────────────────┴────────┴──────────────────────────────┘
 */
@RestController
@RequestMapping("/api/retainedSample")
public class RetainedSampleController extends BaseController {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 留样记录服务
     */
    @Autowired
    private RetainedSampleService retainedSampleService;

    // endregion

    // region 查询接口
    // ===================================
    // 查询接口
    // ===================================

    /**
     * 分页查询留样记录列表
     *
     * 示例请求：
     * GET /api/retainedSample?retainedCode=LY-20260720001&materialName=维生素C片&status=留样中&startDate=2026-07-01&endDate=2026-07-31&pageIndex=0&pageSize=10
     *
     * @param retainedCode    留样编号（可选，模糊匹配）
     * @param relatedInspectionCode 关联检验编号（可选，模糊匹配）
     * @param materialName    物料名称（可选，模糊匹配）
     * @param batchNo         批号（可选，模糊匹配）
     * @param status          状态（可选，精确匹配）
     * @param startDate       留样开始日期（可选，格式：yyyy-MM-dd）
     * @param endDate         留样结束日期（可选，格式：yyyy-MM-dd）
     * @param createdTimeStart 创建时间起始（可选，格式：yyyy-MM-dd HH:mm:ss）
     * @param createdTimeEnd   创建时间结束（可选，格式：yyyy-MM-dd HH:mm:ss）
     * @param pageIndex       页码索引，从0开始（默认0）
     * @param pageSize        每页数量（默认10）
     * @return ApiResponse&lt;PagedResult&lt;RetainedSample&gt;&gt; 分页结果
     */
    @GetMapping
    public ApiResponse<PagedResult<RetainedSample>> getAll(
            @RequestParam(required = false) String retainedCode,
            @RequestParam(required = false) String relatedInspectionCode,
            @RequestParam(required = false) String materialName,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String createdTimeStart,
            @RequestParam(required = false) String createdTimeEnd,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            Page<RetainedSample> page = new Page<>(pageIndex + 1, pageSize);
            QueryWrapper<RetainedSample> wrapper = buildQueryWrapper(retainedCode, relatedInspectionCode,
                    materialName, batchNo, status, startDate, endDate, createdTimeStart, createdTimeEnd);
            Page<RetainedSample> pageResult = retainedSampleService.page(page, wrapper);

            PagedResult<RetainedSample> result = new PagedResult<>();
            result.setItems(pageResult.getRecords());
            result.setTotalCount(pageResult.getTotal());
            result.setPageIndex(pageIndex);
            result.setPageSize(pageSize);
            return success(result);
        } catch (Exception e) {
            return exception(e, "查询留样记录");
        }
    }

    /**
     * 查询留样记录列表（不分页）
     *
     * 示例请求：
     * GET /api/retainedSample/list?status=留样中
     *
     * @param retainedCode    留样编号（可选，模糊匹配）
     * @param relatedInspectionCode 关联检验编号（可选，模糊匹配）
     * @param materialName    物料名称（可选，模糊匹配）
     * @param batchNo         批号（可选，模糊匹配）
     * @param status          状态（可选，精确匹配）
     * @param startDate       留样开始日期（可选，格式：yyyy-MM-dd）
     * @param endDate         留样结束日期（可选，格式：yyyy-MM-dd）
     * @return ApiResponse&lt;List&lt;RetainedSample&gt;&gt; 留样记录列表
     */
    @GetMapping("/list")
    public ApiResponse<List<RetainedSample>> getList(
            @RequestParam(required = false) String retainedCode,
            @RequestParam(required = false) String relatedInspectionCode,
            @RequestParam(required = false) String materialName,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            QueryWrapper<RetainedSample> wrapper = buildQueryWrapper(retainedCode, relatedInspectionCode,
                    materialName, batchNo, status, startDate, endDate, null, null);
            wrapper.orderByDesc("retained_date");
            return success(retainedSampleService.list(wrapper));
        } catch (Exception e) {
            return exception(e, "查询留样记录");
        }
    }

    /**
     * 查询留样记录详情
     *
     * 示例请求：
     * GET /api/retainedSample/1
     *
     * @param id 留样记录ID（路径参数）
     * @return ApiResponse&lt;RetainedSample&gt; 留样记录详情
     */
    @GetMapping("/{id}")
    public ApiResponse<RetainedSample> getById(@PathVariable Long id) {
        try {
            RetainedSample record = retainedSampleService.getById(id);
            if (record == null) {
                return error("留样记录不存在");
            }
            return success(record);
        } catch (Exception e) {
            return exception(e, "查询留样记录详情");
        }
    }

    /**
     * 获取自动生成的留样编号
     *
     * 示例请求：
     * GET /api/retainedSample/generateCode
     *
     * @return ApiResponse&lt;String&gt; 自动生成的留样编号（格式LY-YYYYMMDD-NNN）
     */
    @GetMapping("/generateCode")
    public ApiResponse<String> generateCode() {
        try {
            return success(retainedSampleService.generateCode());
        } catch (Exception e) {
            return exception(e, "生成留样编号");
        }
    }

    // endregion

    // region 新增接口
    // ===================================
    // 新增接口
    // ===================================

    /**
     * 新增留样记录
     *
     * 示例请求：
     * POST /api/retainedSample
     * Content-Type: application/json
     * {
     *   "retainedCode": "LY-20260720-001",
     *   "relatedInspectionCode": "JY-20260720-001",
     *   "materialName": "维生素C片",
     *   "batchNo": "20260701",
     *   "spec": "100mg",
     *   "retainedQuantity": "100g × 2份",
     *   "retainedDate": "2026-07-20",
     *   "expiryDate": "2027-07-20",
     *   "storageLocation": "留样室A架1",
     *   "observationRecord": "",
     *   "status": "留样中",
     *   "remark": "留样观察"
     * }
     *
     * 必填字段：
     *   materialName      物料名称（必填）
     *   batchNo           批号（必填）
     *   retainedQuantity  留样数量（必填）
     *   retainedDate      留样日期（必填）
     *   expiryDate        留样期限（必填）
     *   storageLocation   存放位置（必填）
     *   status            状态（必填）
     * 条件必填：
     *   destroyDate       销毁日期（状态=已销毁时必填）
     * 其他说明：
     *   retainedCode 为空时系统自动生成（格式LY-YYYYMMDD-NNN），亦可手动传入，须唯一
     *   以下字段选填：relatedInspectionCode、spec、observationRecord、remark
     *
     * @param record 留样记录信息（编号为空时系统自动生成）
     * @return ApiResponse&lt;RetainedSample&gt; 新增的留样记录
     */
    @PostMapping
    public ApiResponse<RetainedSample> create(@RequestBody RetainedSample record) {
        try {
            if ("已销毁".equals(record.getStatus()) && record.getDestroyDate() == null) {
                return error("状态为已销毁时销毁日期必填");
            }
            // 是否由系统自动生成编号（用于唯一索引冲突时的重试决策）
            boolean autoGenerated = !StringUtils.hasText(record.getRetainedCode());
            if (!autoGenerated) {
                // 手动传入编号时校验唯一性（含软删除记录，避免唯一索引冲突）
                if (!retainedSampleService.isCodeUnique(record.getRetainedCode(), null)) {
                    return error("留样编号已存在：" + record.getRetainedCode());
                }
            } else {
                record.setRetainedCode(retainedSampleService.generateCode());
            }
            record.setCreatedTime(LocalDateTime.now());
            record.setIsDeleted(0);
            // 自动生成的编号若因并发冲突触发唯一索引，重新生成并重试（最多3次）
            int attempts = 0;
            while (true) {
                try {
                    retainedSampleService.save(record);
                    break;
                } catch (DuplicateKeyException e) {
                    if (!autoGenerated || ++attempts >= 3) {
                        throw e;
                    }
                    record.setRetainedCode(retainedSampleService.generateCode());
                }
            }
            return success(record, "新增成功");
        } catch (Exception e) {
            return exception(e, "新增留样记录");
        }
    }

    // endregion

    // region 修改与删除接口
    // ===================================
    // 修改与删除接口
    // ===================================

    /**
     * 修改留样记录
     *
     * 示例请求：
     * PUT /api/retainedSample/1
     * Content-Type: application/json
     * {
     *   "status": "已销毁",
     *   "destroyDate": "2027-07-21",
     *   "observationRecord": "留样期内性状稳定，无异常变化"
     * }
     *
     * 必填字段说明：同新增接口（retainedCode 编号除外），修改时仅传需变更字段
     *
     * @param id     留样记录ID（路径参数）
     * @param record 更新的留样记录信息
     * @return ApiResponse&lt;RetainedSample&gt; 修改后的留样记录
     */
    @PutMapping("/{id}")
    public ApiResponse<RetainedSample> update(
            @PathVariable Long id,
            @RequestBody RetainedSample record) {
        try {
            RetainedSample existing = retainedSampleService.getById(id);
            if (existing == null) {
                return error("留样记录不存在");
            }
            if ("已销毁".equals(record.getStatus()) && record.getDestroyDate() == null) {
                return error("状态为已销毁时销毁日期必填");
            }
            if (StringUtils.hasText(record.getRetainedCode())
                    && !record.getRetainedCode().equals(existing.getRetainedCode())
                    && !retainedSampleService.isCodeUnique(record.getRetainedCode(), id)) {
                return error("留样编号已存在：" + record.getRetainedCode());
            }
            record.setId(id);
            record.setUpdatedTime(LocalDateTime.now());
            retainedSampleService.updateById(record);
            return success(record, "修改成功");
        } catch (Exception e) {
            return exception(e, "修改留样记录");
        }
    }

    /**
     * 删除留样记录（软删除）
     *
     * 示例请求：
     * DELETE /api/retainedSample/1
     *
     * @param id 留样记录ID（路径参数）
     * @return ApiResponse&lt;Void&gt; 删除结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        try {
            RetainedSample existing = retainedSampleService.getById(id);
            if (existing == null) {
                return error("留样记录不存在");
            }
            retainedSampleService.removeById(id);
            return success(null, "删除成功");
        } catch (Exception e) {
            return exception(e, "删除留样记录");
        }
    }

    // endregion

    // region 私有工具方法
    // ===================================
    // 私有工具方法
    // ===================================

    /**
     * 构建留样记录查询条件
     *
     * @param retainedCode    留样编号（模糊匹配）
     * @param relatedInspectionCode 关联检验编号（模糊匹配）
     * @param materialName    物料名称（模糊匹配）
     * @param batchNo         批号（模糊匹配）
     * @param status          状态（精确匹配）
     * @param startDate       留样开始日期
     * @param endDate         留样结束日期
     * @param createdTimeStart 创建时间起始
     * @param createdTimeEnd   创建时间结束
     * @return 查询条件Wrapper
     */
    private QueryWrapper<RetainedSample> buildQueryWrapper(String retainedCode, String relatedInspectionCode,
            String materialName, String batchNo, String status, String startDate, String endDate,
            String createdTimeStart, String createdTimeEnd) {
        QueryWrapper<RetainedSample> wrapper = new QueryWrapper<>();
        wrapper.eq("is_deleted", 0);
        if (StringUtils.hasText(retainedCode)) wrapper.like("retained_code", retainedCode);
        if (StringUtils.hasText(relatedInspectionCode)) wrapper.like("related_inspection_code", relatedInspectionCode);
        if (StringUtils.hasText(materialName)) wrapper.like("material_name", materialName);
        if (StringUtils.hasText(batchNo)) wrapper.like("batch_no", batchNo);
        if (StringUtils.hasText(status)) wrapper.eq("status", status);
        if (StringUtils.hasText(startDate)) wrapper.ge("retained_date", startDate);
        if (StringUtils.hasText(endDate)) wrapper.le("retained_date", endDate);
        if (StringUtils.hasText(createdTimeStart)) wrapper.ge("created_time", createdTimeStart);
        if (StringUtils.hasText(createdTimeEnd)) wrapper.le("created_time", createdTimeEnd);
        wrapper.orderByDesc("retained_date");
        return wrapper;
    }

    // endregion
}