package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.ApprovalRecord;
import com.tonghui.erp.Service.ApprovalRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 审批记录控制器
 * <p>
 * 提供审批记录的查询和创建操作，支持按实例ID、节点ID等维度查询
 * </p>
 *
 * 接口清单：
 * ┌────┬────────────────────────────────────┬────────┬──────────────────────────────┐
 * │ #  │ 接口                               │ 方法   │ 说明                         │
 * ├────┼────────────────────────────────────┼────────┼──────────────────────────────┤
 * │ 1  │ /api/approval/record               │ GET    │ 获取审批记录列表（分页）      │
 * │ 2  │ /api/approval/record/instance/{id} │ GET    │ 根据实例ID获取审批记录列表    │
 * │ 3  │ /api/approval/record/node/{id}     │ GET    │ 根据节点ID获取审批记录列表    │
 * │ 4  │ /api/approval/record/{id}          │ GET    │ 根据ID获取审批记录详情        │
 * │ 5  │ /api/approval/record               │ POST   │ 创建审批记录                 │
 * └────┴────────────────────────────────────┴────────┴──────────────────────────────┘
 */
@RestController
@RequestMapping("/api/approval/record")
public class ApprovalRecordController extends BaseController {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 审批记录服务
     */
    @Autowired
    private ApprovalRecordService approvalRecordService;

    // endregion

    // region 查询接口
    // ===================================
    // 查询接口
    // ===================================

    /**
     * 获取审批记录列表（分页）
     *
     * 示例请求：
     * GET /api/approval/record?pageIndex=0&pageSize=20
     *
     * @param pageRequest 分页请求参数
     * @return ApiResponse&lt;PagedResult&lt;ApprovalRecord&gt;&gt; 审批记录分页结果
     */
    @GetMapping
    public ApiResponse<PagedResult<ApprovalRecord>> listRecords(@ModelAttribute PageRequestDto pageRequest) {
        try {
            pageRequest = processPageRequest(pageRequest);
            PagedResult<ApprovalRecord> result = new PagedResult<>();
            // TODO: 实现具体的分页查询逻辑
            return success(result);
        } catch (Exception e) {
            return exception(e, "获取审批记录列表");
        }
    }

    /**
     * 根据实例ID获取审批记录列表
     *
     * 示例请求：
     * GET /api/approval/record/instance/1
     *
     * @param instanceId 审批实例ID（路径参数）
     * @return ApiResponse&lt;List&lt;ApprovalRecord&gt;&gt; 审批记录列表
     */
    @GetMapping("/instance/{instanceId}")
    public ApiResponse<List<ApprovalRecord>> getRecordsByInstanceId(@PathVariable Long instanceId) {
        try {
            List<ApprovalRecord> records = approvalRecordService.getRecordsByInstanceId(instanceId);
            return success(records);
        } catch (Exception e) {
            return exception(e, "获取审批记录列表");
        }
    }

    /**
     * 根据节点ID获取审批记录列表
     *
     * 示例请求：
     * GET /api/approval/record/node/1
     *
     * @param nodeId 审批节点ID（路径参数）
     * @return ApiResponse&lt;List&lt;ApprovalRecord&gt;&gt; 审批记录列表
     */
    @GetMapping("/node/{nodeId}")
    public ApiResponse<List<ApprovalRecord>> getRecordsByNodeId(@PathVariable Long nodeId) {
        try {
            List<ApprovalRecord> records = approvalRecordService.getRecordsByNodeId(nodeId);
            return success(records);
        } catch (Exception e) {
            return exception(e, "获取审批记录列表");
        }
    }

    /**
     * 根据ID获取审批记录详情
     *
     * 示例请求：
     * GET /api/approval/record/1
     *
     * @param id 审批记录ID（路径参数）
     * @return ApiResponse&lt;ApprovalRecord&gt; 审批记录详情
     */
    @GetMapping("/{id}")
    public ApiResponse<ApprovalRecord> getRecordById(@PathVariable Long id) {
        try {
            ApprovalRecord record = approvalRecordService.getById(id);
            if (record == null) {
                return error("未找到指定的审批记录");
            }
            return success(record);
        } catch (Exception e) {
            return exception(e, "获取审批记录详情");
        }
    }

    // endregion

    // region 新增接口
    // ===================================
    // 新增接口
    // ===================================

    /**
     * 创建审批记录
     *
     * 示例请求：
     * POST /api/approval/record
     * Content-Type: application/json
     * {
     *   "instanceId": 1,
     *   "nodeId": 1,
     *   "approverId": 10,
     *   "action": "AGREE",
     *   "comment": "同意"
     * }
     *
     * @param record 审批记录信息
     * @return ApiResponse&lt;ApprovalRecord&gt; 创建的审批记录
     */
    @PostMapping
    public ApiResponse<ApprovalRecord> createRecord(@RequestBody ApprovalRecord record) {
        try {
            boolean saved = approvalRecordService.save(record);
            if (saved) {
                return success(record, "审批记录创建成功");
            } else {
                return error("审批记录创建失败");
            }
        } catch (Exception e) {
            return exception(e, "创建审批记录");
        }
    }

    // endregion
}
