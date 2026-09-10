package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.MaterialRequisition;
import com.tonghui.erp.Data.Entity.MaterialRequisitionDetail;
import com.tonghui.erp.Service.MaterialRequisitionService;
import com.tonghui.erp.Service.MaterialRequisitionDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 领料申请控制器
 */
@RestController
@RequestMapping("/api/material-requisition")
public class MaterialRequisitionController extends BaseController {

    @Autowired
    private MaterialRequisitionService materialRequisitionService;

    @Autowired
    private MaterialRequisitionDetailService materialRequisitionDetailService;

    /**
     * 新增领料申请
     */
    @PostMapping
    public ApiResponse<MaterialRequisition> create(@RequestBody MaterialRequisition requisition) {
        try {
            boolean result = materialRequisitionService.addRequisition(requisition);
            if (result) {
                return success(requisition, "创建成功");
            }
            return error("创建失败");
        } catch (Exception ex) {
            return exception(ex, "创建领料申请失败");
        }
    }

    /**
     * 根据ID获取领料申请详情
     */
    @GetMapping("/{id}")
    public ApiResponse<MaterialRequisition> getById(@PathVariable Long id) {
        try {
            MaterialRequisition requisition = materialRequisitionService.getById(id);
            return success(requisition);
        } catch (Exception ex) {
            return exception(ex, "查询失败");
        }
    }

    /**
     * 根据工单ID查询领料申请列表
     */
    @GetMapping("/by-work-order/{workOrderId}")
    public ApiResponse<List<MaterialRequisition>> getByWorkOrderId(@PathVariable Long workOrderId) {
        try {
            List<MaterialRequisition> list = materialRequisitionService.getByWorkOrderId(workOrderId);
            return success(list);
        } catch (Exception ex) {
            return exception(ex, "查询失败");
        }
    }

    /**
     * 删除领料申请
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        try {
            boolean result = materialRequisitionService.removeById(id);
            return success(result, "删除成功");
        } catch (Exception ex) {
            return exception(ex, "删除失败");
        }
    }

    /**
     * 根据领料申请ID查询领料明细
     */
    @GetMapping("/{id}/details")
    public ApiResponse<List<MaterialRequisitionDetail>> getDetails(@PathVariable Long id) {
        try {
            List<MaterialRequisitionDetail> list = materialRequisitionDetailService.getByRequisitionId(id);
            return success(list);
        } catch (Exception ex) {
            return exception(ex, "查询明细失败");
        }
    }

    /**
     * 批量保存领料明细
     */
    @PostMapping("/{id}/details/batch")
    public ApiResponse<Boolean> batchSaveDetails(@PathVariable Long id,
                                                  @RequestBody List<MaterialRequisitionDetail> details) {
        try {
            materialRequisitionDetailService.batchSave(id, details);
            return success(true, "保存成功");
        } catch (Exception ex) {
            return exception(ex, "保存明细失败");
        }
    }
}
