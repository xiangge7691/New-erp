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
 */
@RestController
@RequestMapping("/api/approval/record")
public class ApprovalRecordController extends BaseController {

    @Autowired
    private ApprovalRecordService approvalRecordService;

    /**
     * 获取审批记录列表
     */
    @GetMapping
    public ApiResponse<PagedResult<ApprovalRecord>> listRecords(@ModelAttribute PageRequestDto pageRequest) {
        try {
            pageRequest = processPageRequest(pageRequest);
            // 使用MyBatis Plus分页查询
            PagedResult<ApprovalRecord> result = new PagedResult<>();
            // TODO: 实现具体的分页查询逻辑
            return success(result);
        } catch (Exception e) {
            return exception(e, "获取审批记录列表");
        }
    }

    /**
     * 根据实例ID获取审批记录列表
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

    /**
     * 创建审批记录
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
}
