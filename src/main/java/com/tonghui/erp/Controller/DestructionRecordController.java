package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Quality.DestructionRecordSaveDto;
import com.tonghui.erp.Common.Dto.Quality.DestructionRecordWithDetailsDto;
import com.tonghui.erp.Common.Dto.Quality.RetainedSampleOptionDto;
import com.tonghui.erp.Data.Entity.DestructionRecord;
import com.tonghui.erp.Data.Entity.FileInfo;
import com.tonghui.erp.Service.DestructionRecordService;
import org.springframework.beans.factory.annotation.Autowired;
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
 * 销毁记录控制器
 * <p>
 * 提供销毁记录（不合格品销毁/留样及稳定性考察产品销毁）的CRUD操作、
 * 条件分页查询、销毁单号生成、作废及留样下拉查询，
 * 用于质量检验模块的GMP销毁台账管理
 * </p>
 * <p>
 * 作废语义：不提供删除接口，作废通过 PUT /{id}/void 实现，作废后禁止修改
 * </p>
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────────────┬────────┬──────────────────────────────┐
 * │ #  │ 接口                                     │ 方法   │ 说明                         │
 * ├────┼──────────────────────────────────────────┼────────┼──────────────────────────────┤
 * │ 1  │ /api/destruction                         │ GET    │ 分页查询销毁记录列表          │
 * │ 2  │ /api/destruction/{id}                    │ GET    │ 查询销毁记录详情（含明细附件）│
 * │ 3  │ /api/destruction/generateCode            │ GET    │ 获取自动生成的销毁单号        │
 * │ 4  │ /api/destruction/retained-samples        │ GET    │ 留样下拉查询                  │
 * │ 5  │ /api/destruction/{id}/attachments        │ GET    │ 查询附件列表                  │
 * │ 6  │ /api/destruction                         │ POST   │ 新增销毁记录（主表+明细）      │
 * │ 7  │ /api/destruction/{id}                    │ PUT    │ 修改销毁记录（明细整体重建）    │
 * │ 8  │ /api/destruction/{id}/void               │ PUT    │ 作废销毁记录                  │
 * └────┴──────────────────────────────────────────┴────────┴──────────────────────────────┘
 */
@RestController
@RequestMapping("/api/destruction")
public class DestructionRecordController extends BaseController {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 销毁记录服务
     */
    @Autowired
    private DestructionRecordService destructionRecordService;

    // endregion

    // region 查询接口
    // ===================================
    // 查询接口
    // ===================================

    /**
     * 分页查询销毁记录列表
     *
     * 示例请求：
     * GET /api/destruction?recordType=不合格品销毁&keyword=BX&batchNo=20260701&status=正常&startDate=2026-10-01&endDate=2026-10-12&pageIndex=0&pageSize=10
     *
     * @param recordType 记录类型（可选，精确匹配：不合格品销毁/留样及稳定性考察产品销毁）
     * @param keyword    关键字（可选，模糊匹配销毁单号）
     * @param batchNo    批号（可选，模糊匹配明细批号，存在该批号明细的主表命中）
     * @param status     状态（可选，精确匹配：正常/作废）
     * @param startDate  登记时间起始（可选，格式：yyyy-MM-dd）
     * @param endDate    登记时间结束（可选，格式：yyyy-MM-dd）
     * @param pageIndex  页码索引，从0开始（默认0）
     * @param pageSize   每页数量（默认10）
     * @return ApiResponse&lt;PagedResult&lt;DestructionRecord&gt;&gt; 分页结果
     */
    @GetMapping
    public ApiResponse<PagedResult<DestructionRecord>> search(
            @RequestParam(required = false) String recordType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            PageRequestDto pageRequest = new PageRequestDto();
            pageRequest.setPageIndex(pageIndex);
            pageRequest.setPageSize(pageSize);
            return success(destructionRecordService.search(recordType, keyword, batchNo,
                    status, startDate, endDate, pageRequest));
        } catch (Exception e) {
            return exception(e, "查询销毁记录");
        }
    }

    /**
     * 查询销毁记录详情（含明细列表、附件列表与创建人/更新人姓名）
     *
     * 示例请求：
     * GET /api/destruction/1
     *
     * @param id 销毁记录ID（路径参数）
     * @return ApiResponse&lt;DestructionRecordWithDetailsDto&gt; 销毁记录详情
     */
    @GetMapping("/{id}")
    public ApiResponse<DestructionRecordWithDetailsDto> getDetail(@PathVariable Long id) {
        try {
            DestructionRecordWithDetailsDto detail = destructionRecordService.getDetail(id);
            if (detail == null) {
                return error("销毁记录不存在");
            }
            return success(detail);
        } catch (Exception e) {
            return exception(e, "查询销毁记录详情");
        }
    }

    /**
     * 获取自动生成的销毁单号
     * <p>
     * 按记录类型选择前缀：不合格品销毁BPXH/留样及稳定性考察产品销毁LYXH
     * </p>
     *
     * 示例请求：
     * GET /api/destruction/generateCode?recordType=不合格品销毁
     *
     * @param recordType 记录类型（必填：不合格品销毁/留样及稳定性考察产品销毁）
     * @return ApiResponse&lt;String&gt; 自动生成的销毁单号（格式BPXH-YYYYMMDD-NNN或LYXH-YYYYMMDD-NNN）
     */
    @GetMapping("/generateCode")
    public ApiResponse<String> generateCode(@RequestParam String recordType) {
        try {
            return success(destructionRecordService.generateCode(recordType));
        } catch (Exception e) {
            return exception(e, "生成销毁单号");
        }
    }

    /**
     * 留样下拉查询（仅返回状态为留样中的记录，关键字模糊匹配留样编号/物料名称/批号）
     *
     * 示例请求：
     * GET /api/destruction/retained-samples?keyword=VC
     *
     * @param keyword 关键字（可选）
     * @return ApiResponse&lt;List&lt;RetainedSampleOptionDto&gt;&gt; 下拉选项列表（最多20条）
     */
    @GetMapping("/retained-samples")
    public ApiResponse<List<RetainedSampleOptionDto>> searchRetainedSamples(
            @RequestParam(required = false) String keyword) {
        try {
            return success(destructionRecordService.searchRetainedSamples(keyword));
        } catch (Exception e) {
            return exception(e, "查询留样下拉");
        }
    }

    /**
     * 查询销毁记录附件列表
     *
     * 示例请求：
     * GET /api/destruction/1/attachments
     *
     * @param id 销毁记录ID（路径参数）
     * @return ApiResponse&lt;List&lt;FileInfo&gt;&gt; 附件文件列表
     */
    @GetMapping("/{id}/attachments")
    public ApiResponse<List<FileInfo>> getAttachments(@PathVariable Long id) {
        try {
            return success(destructionRecordService.getAttachments(id));
        } catch (Exception e) {
            return exception(e, "查询销毁记录附件");
        }
    }

    // endregion

    // region 新增接口
    // ===================================
    // 新增接口
    // ===================================

    /**
     * 新增销毁记录（主表+明细一次保存）
     *
     * 示例请求：
     * POST /api/destruction
     * Content-Type: application/json
     * {
     *   "recordType": "不合格品销毁",
     *   "destroyReason": "中间体含量不合格，无法继续使用",
     *   "destroyMethod": "交有资质单位高温焚烧",
     *   "destroyLocation": "厂区危废暂存间",
     *   "applicantName": "李四",
     *   "applyDate": "2026-10-10",
     *   "approverName": "王五",
     *   "approveDate": "2026-10-10",
     *   "destroyerName": "赵六",
     *   "destroyDate": "2026-10-11",
     *   "supervisorName": "钱七",
     *   "superviseDate": "2026-10-11",
     *   "remark": "随附检验报告复印件",
     *   "details": [
     *     { "materialName": "维生素C", "spec": "食品级", "batchNo": "20260701", "quantity": 50.5, "retainedSampleId": null },
     *     { "materialName": "淀粉", "spec": "药用", "batchNo": "20260615", "quantity": 30.0 }
     *   ],
     *   "attachmentFileIds": [201]
     * }
     *
     * 必填字段：
     *   recordType  记录类型（必填：不合格品销毁/留样及稳定性考察产品销毁，决定单号前缀）
     *   details     销毁明细列表（必填，至少1行，明细materialName必填）
     * 其他说明：
     *   destructionCode 由服务端自动生成（不合格品销毁BPXH-前缀/留样销毁LYXH-前缀），不可传入
     *   totalQuantity 由服务端按明细quantity求和计算，不可传入
     *   明细行retainedSampleId仅作追溯引用，保存不联动留样状态
     *   以下字段选填：destroyReason、destroyMethod、destroyLocation、remark、四方签字姓名与日期、attachmentFileIds
     *
     * @param dto 保存请求体（主表字段+明细列表）
     * @return ApiResponse&lt;DestructionRecord&gt; 新增的销毁记录（含自动生成的单号与合计数量）
     */
    @PostMapping
    public ApiResponse<DestructionRecord> create(@RequestBody DestructionRecordSaveDto dto) {
        try {
            return success(destructionRecordService.create(dto), "新增成功");
        } catch (Exception e) {
            return exception(e, "新增销毁记录");
        }
    }

    // endregion

    // region 修改与作废接口
    // ===================================
    // 修改与作废接口
    // ===================================

    /**
     * 修改销毁记录（主表+明细一次保存，明细整体重建）
     *
     * 示例请求：
     * PUT /api/destruction/1
     * Content-Type: application/json
     * {
     *   "recordType": "不合格品销毁",
     *   "destroyReason": "中间体含量不合格，无法继续使用（补充说明）",
     *   "destroyMethod": "交有资质单位高温焚烧",
     *   "destroyLocation": "厂区危废暂存间",
     *   "destroyerName": "赵六",
     *   "destroyDate": "2026-10-11",
     *   "supervisorName": "钱七",
     *   "superviseDate": "2026-10-11",
     *   "details": [
     *     { "materialName": "维生素C", "spec": "食品级", "batchNo": "20260701", "quantity": 50.5 }
     *   ],
     *   "attachmentFileIds": [201, 202]
     * }
     *
     * 必填字段说明：同新增接口；销毁单号与状态不可修改，服务端保留原值
     * 其他说明：已作废的销毁记录不能修改；明细列表整体重建（旧行删除后按新列表重插）
     *
     * @param id  销毁记录ID（路径参数）
     * @param dto 更新的销毁记录信息（主表字段+明细列表）
     * @return ApiResponse&lt;DestructionRecord&gt; 修改后的销毁记录
     */
    @PutMapping("/{id}")
    public ApiResponse<DestructionRecord> update(@PathVariable Long id, @RequestBody DestructionRecordSaveDto dto) {
        try {
            return success(destructionRecordService.update(id, dto), "修改成功");
        } catch (Exception e) {
            return exception(e, "修改销毁记录");
        }
    }

    /**
     * 作废销毁记录
     * <p>
     * 不提供删除接口，作废后status置为作废并记录作废时间与原因，已作废记录禁止修改
     * </p>
     *
     * 示例请求：
     * PUT /api/destruction/1/void
     * Content-Type: application/json
     * "单号填写错误，需重新登记"
     *
     * @param id         销毁记录ID（路径参数）
     * @param voidRemark 作废原因（可选，最长500字符；请求体为纯文本字符串）
     * @return ApiResponse&lt;Void&gt; 作废结果
     */
    @PutMapping("/{id}/void")
    public ApiResponse<Void> voidRecord(@PathVariable Long id,
                                        @RequestBody(required = false) String voidRemark) {
        try {
            destructionRecordService.voidRecord(id, voidRemark);
            return success(null, "作废成功");
        } catch (Exception e) {
            return exception(e, "作废销毁记录");
        }
    }

    // endregion
}
