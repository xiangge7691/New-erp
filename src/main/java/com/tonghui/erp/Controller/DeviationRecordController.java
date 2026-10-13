package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Quality.DeviationRecordSaveDto;
import com.tonghui.erp.Common.Dto.Quality.DeviationRecordWithAttachmentsDto;
import com.tonghui.erp.Common.Dto.Quality.WorkOrderOptionDto;
import com.tonghui.erp.Data.Entity.DeviationRecord;
import com.tonghui.erp.Data.Entity.FileInfo;
import com.tonghui.erp.Service.DeviationRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 偏差记录控制器
 * <p>
 * 提供偏差记录的CRUD操作、条件分页查询、偏差编号生成及生产任务下拉查询，
 * 用于质量检验模块的GMP偏差台账管理
 * </p>
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────────┬────────┬──────────────────────────────┐
 * │ #  │ 接口                                 │ 方法   │ 说明                         │
 * ├────┼──────────────────────────────────────┼────────┼──────────────────────────────┤
 * │ 1  │ /api/deviation                       │ GET    │ 分页查询偏差记录列表          │
 * │ 2  │ /api/deviation/{id}                  │ GET    │ 查询偏差记录详情（含附件）    │
 * │ 3  │ /api/deviation/generateCode          │ GET    │ 获取自动生成的偏差编号        │
 * │ 4  │ /api/deviation/work-orders           │ GET    │ 生产任务下拉查询              │
 * │ 5  │ /api/deviation/{id}/attachments      │ GET    │ 查询附件列表                  │
 * │ 6  │ /api/deviation                       │ POST   │ 新增偏差记录                  │
 * │ 7  │ /api/deviation/{id}                  │ PUT    │ 修改偏差记录                  │
 * │ 8  │ /api/deviation/{id}                  │ DELETE │ 删除偏差记录（软删除）        │
 * └────┴──────────────────────────────────────┴────────┴──────────────────────────────┘
 */
@RestController
@RequestMapping("/api/deviation")
public class DeviationRecordController extends BaseController {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 偏差记录服务
     */
    @Autowired
    private DeviationRecordService deviationRecordService;

    // endregion

    // region 查询接口
    // ===================================
    // 查询接口
    // ===================================

    /**
     * 分页查询偏差记录列表（列表项含创建人姓名createdByName）
     *
     * 示例请求：
     * GET /api/deviation?keyword=偏差&workOrderCode=SC&startDate=2026-10-01&endDate=2026-10-12&pageIndex=0&pageSize=10
     *
     * @param keyword       关键字（可选，模糊匹配偏差编号或偏差标题）
     * @param workOrderCode 生产任务编号（可选，模糊匹配快照字段）
     * @param startDate     实际发生时间起始（可选，格式：yyyy-MM-dd）
     * @param endDate       实际发生时间结束（可选，格式：yyyy-MM-dd）
     * @param pageIndex     页码索引，从0开始（默认0）
     * @param pageSize      每页数量（默认10）
     * @return ApiResponse&lt;PagedResult&lt;DeviationRecordWithAttachmentsDto&gt;&gt; 分页结果（列表项含创建人姓名createdByName，附件列表仅详情返回）
     */
    @GetMapping
    public ApiResponse<PagedResult<DeviationRecordWithAttachmentsDto>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String workOrderCode,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            PageRequestDto pageRequest = new PageRequestDto();
            pageRequest.setPageIndex(pageIndex);
            pageRequest.setPageSize(pageSize);
            return success(deviationRecordService.search(keyword, workOrderCode, startDate, endDate, pageRequest));
        } catch (Exception e) {
            return exception(e, "查询偏差记录");
        }
    }

    /**
     * 查询偏差记录详情（含附件列表与创建人/更新人姓名）
     *
     * 示例请求：
     * GET /api/deviation/1
     *
     * @param id 偏差记录ID（路径参数）
     * @return ApiResponse&lt;DeviationRecordWithAttachmentsDto&gt; 偏差记录详情
     */
    @GetMapping("/{id}")
    public ApiResponse<DeviationRecordWithAttachmentsDto> getDetail(@PathVariable Long id) {
        try {
            DeviationRecordWithAttachmentsDto detail = deviationRecordService.getDetail(id);
            if (detail == null) {
                return error("偏差记录不存在");
            }
            return success(detail);
        } catch (Exception e) {
            return exception(e, "查询偏差记录详情");
        }
    }

    /**
     * 获取自动生成的偏差编号
     *
     * 示例请求：
     * GET /api/deviation/generateCode
     *
     * @return ApiResponse&lt;String&gt; 自动生成的偏差编号（格式PC-YYYYMMDD-NNN）
     */
    @GetMapping("/generateCode")
    public ApiResponse<String> generateCode() {
        try {
            return success(deviationRecordService.generateCode());
        } catch (Exception e) {
            return exception(e, "生成偏差编号");
        }
    }

    /**
     * 生产任务下拉查询（关键字模糊匹配任务编号/制剂名称）
     *
     * 示例请求：
     * GET /api/deviation/work-orders?keyword=VC
     *
     * @param keyword 关键字（可选）
     * @return ApiResponse&lt;List&lt;WorkOrderOptionDto&gt;&gt; 下拉选项列表（最多20条）
     */
    @GetMapping("/work-orders")
    public ApiResponse<List<WorkOrderOptionDto>> searchWorkOrders(
            @RequestParam(required = false) String keyword) {
        try {
            return success(deviationRecordService.searchWorkOrders(keyword));
        } catch (Exception e) {
            return exception(e, "查询生产任务下拉");
        }
    }

    /**
     * 查询偏差记录附件列表
     *
     * 示例请求：
     * GET /api/deviation/1/attachments
     *
     * @param id 偏差记录ID（路径参数）
     * @return ApiResponse&lt;List&lt;FileInfo&gt;&gt; 附件文件列表
     */
    @GetMapping("/{id}/attachments")
    public ApiResponse<List<FileInfo>> getAttachments(@PathVariable Long id) {
        try {
            return success(deviationRecordService.getAttachments(id));
        } catch (Exception e) {
            return exception(e, "查询偏差记录附件");
        }
    }

    // endregion

    // region 新增接口
    // ===================================
    // 新增接口
    // ===================================

    /**
     * 新增偏差记录
     *
     * 示例请求：
     * POST /api/deviation
     * Content-Type: application/json
     * {
     *   "workOrderId": 1,
     *   "deviationTitle": "混合工序装量偏差",
     *   "occurTime": "2026-10-10 14:30:00",
     *   "processName": "混合",
     *   "description": "混合过程中发现装量超出工艺范围",
     *   "actionDesc": "暂停放行，重新取样复测",
     *   "resultDesc": "复测合格，恢复正常生产",
     *   "handlerName": "张三",
     *   "handleTime": "2026-10-10 16:00:00",
     *   "remark": "已记录于批生产记录",
     *   "attachmentFileIds": [101, 102]
     * }
     *
     * 必填字段：
     *   workOrderId      关联生产任务ID（必填）
     *   deviationTitle   偏差标题（必填，最长200字符）
     *   occurTime        实际发生时间（必填）
     *   description      偏差描述（必填，最长5000字符）
     * 其他说明：
     *   deviationCode 由服务端自动生成（格式PC-YYYYMMDD-NNN），不可传入
     *   保存时自动快照生产任务编号/制剂名称/批号（仅作展示，不联动任务状态）
     *   以下字段选填：processName、actionDesc、resultDesc、handlerName、handleTime、remark、attachmentFileIds
     *
     * @param dto 保存请求体
     * @return ApiResponse&lt;DeviationRecord&gt; 新增的偏差记录（含自动生成的编号）
     */
    @PostMapping
    public ApiResponse<DeviationRecord> create(@RequestBody DeviationRecordSaveDto dto) {
        try {
            return success(deviationRecordService.create(dto), "新增成功");
        } catch (Exception e) {
            return exception(e, "新增偏差记录");
        }
    }

    // endregion

    // region 修改与删除接口
    // ===================================
    // 修改与删除接口
    // ===================================

    /**
     * 修改偏差记录
     *
     * 示例请求：
     * PUT /api/deviation/1
     * Content-Type: application/json
     * {
     *   "workOrderId": 1,
     *   "deviationTitle": "混合工序装量偏差（修订）",
     *   "occurTime": "2026-10-10 14:30:00",
     *   "processName": "混合",
     *   "description": "混合过程中发现装量超出工艺范围",
     *   "actionDesc": "暂停放行，重新取样复测",
     *   "resultDesc": "复测合格，恢复正常生产",
     *   "handlerName": "张三",
     *   "handleTime": "2026-10-10 16:00:00",
     *   "attachmentFileIds": [101]
     * }
     *
     * 必填字段说明：同新增接口；偏差编号不可修改，服务端保留原值
     *
     * @param id  偏差记录ID（路径参数）
     * @param dto 更新的偏差记录信息
     * @return ApiResponse&lt;DeviationRecord&gt; 修改后的偏差记录
     */
    @PutMapping("/{id}")
    public ApiResponse<DeviationRecord> update(@PathVariable Long id, @RequestBody DeviationRecordSaveDto dto) {
        try {
            return success(deviationRecordService.update(id, dto), "修改成功");
        } catch (Exception e) {
            return exception(e, "修改偏差记录");
        }
    }

    /**
     * 删除偏差记录（软删除）
     *
     * 示例请求：
     * DELETE /api/deviation/1
     *
     * @param id 偏差记录ID（路径参数）
     * @return ApiResponse&lt;Void&gt; 删除结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        try {
            DeviationRecord existing = deviationRecordService.getById(id);
            if (existing == null) {
                return error("偏差记录不存在");
            }
            deviationRecordService.removeById(id);
            return success(null, "删除成功");
        } catch (Exception e) {
            return exception(e, "删除偏差记录");
        }
    }

    // endregion
}
