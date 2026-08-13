package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.Energy.EnergyRecordPageResult;
import com.tonghui.erp.Data.Entity.EnergyRecord;
import com.tonghui.erp.Data.Entity.FileInfo;
import com.tonghui.erp.Service.EnergyRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 能耗记录控制器
 * <p>
 * 按月记录院内制剂室水、电、气等能耗费用，支持按月份/类型筛选、汇总、CRUD及凭证附件关联
 * </p>
 *
 * 接口清单：
 * ┌────┬─────────────────────────────────────┬────────┬──────────────────────────────┐
 * │ #  │ 接口                                │ 方法   │ 说明                         │
 * ├────┼─────────────────────────────────────┼────────┼──────────────────────────────┤
 * │ 1  │ /api/energy/list                    │ GET    │ 分页查询能耗记录（含汇总）   │
 * │ 2  │ /api/energy                         │ POST   │ 新增能耗记录                 │
 * │ 3  │ /api/energy/{id}                    │ PUT    │ 编辑能耗记录                 │
 * │ 4  │ /api/energy/{id}                    │ DELETE │ 删除能耗记录（软删除）       │
 * │ 5  │ /api/energy/{id}/attachments        │ GET    │ 查询凭证附件列表             │
 * └────┴─────────────────────────────────────┴────────┴──────────────────────────────┘
 */
@RestController
@RequestMapping("/api/energy")
public class EnergyRecordController extends BaseController {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 能耗记录服务
     */
    @Autowired
    private EnergyRecordService energyRecordService;

    // endregion

    // region 查询接口
    // ===================================
    // 查询接口
    // ===================================

    /**
     * 分页查询能耗记录列表（支持按月份、类型筛选，随列表返回费用汇总）
     *
     * 示例请求：
     * GET /api/energy/list?pageIndex=0&pageSize=20
     * GET /api/energy/list?month=2026-08&energyType=电&pageIndex=0&pageSize=20
     *
     * @param month      月份（可选，格式 YYYY-MM）
     * @param energyType 能耗类型（可选：自来水/电/燃气）
     * @param pageIndex  页码（可选，默认0）
     * @param pageSize   每页数量（可选，默认20，最大100）
     * @return 分页结果（含 summary 汇总：总金额 + 各类型金额）
     */
    @GetMapping("/list")
    public ApiResponse<EnergyRecordPageResult> pageQuery(
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String energyType,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "20") int pageSize) {
        try {
            return success(energyRecordService.pageQuery(month, energyType, pageIndex, pageSize));
        } catch (Exception e) {
            return exception(e, "查询能耗记录");
        }
    }

    /**
     * 新增能耗记录
     * <p>
     * 计量单位自动映射（电→度，自来水/燃气→立方米）；
     * 实用量未传时自动计算（本月表底-上月表底）；
     * 总价未传时自动计算（实用量×单价）
     * </p>
     *
     * 示例请求：
     * POST /api/energy
     * Content-Type: application/json
     * {
     *   "month": "2026-08",
     *   "energyType": "电",
     *   "lastMeterReading": 4010.000,
     *   "currentMeterReading": 4510.000,
     *   "unitPrice": 4.84,
     *   "remark": "八月电费",
     *   "attachmentFileId": 1024
     * }
     *
     * @param record          能耗记录（month/energyType 必填）
     * @param attachmentFileId 凭证附件文件ID（可选，前端先调 /api/file-manager/upload 获取）
     * @return 保存后的能耗记录
     */
    @PostMapping
    public ApiResponse<EnergyRecord> create(@RequestBody EnergyRecord record,
                                            @RequestParam(required = false) Long attachmentFileId) {
        try {
            return success(energyRecordService.create(record, attachmentFileId), "新增成功");
        } catch (Exception e) {
            return exception(e, "新增能耗记录");
        }
    }

    // endregion

    // region 编辑与删除接口
    // ===================================
    // 编辑与删除接口
    // ===================================

    /**
     * 编辑能耗记录（规则同新增，仅更新提供的字段）
     *
     * 示例请求：
     * PUT /api/energy/10
     * Content-Type: application/json
     * {
     *   "month": "2026-08",
     *   "energyType": "电",
     *   "lastMeterReading": 4010.000,
     *   "currentMeterReading": 4550.000,
     *   "unitPrice": 4.84,
     *   "remark": "八月电费（补录）"
     * }
     *
     * @param id              能耗记录ID
     * @param record          更新内容
     * @param attachmentFileId 凭证附件文件ID（可选）
     * @return 更新后的能耗记录
     */
    @PutMapping("/{id}")
    public ApiResponse<EnergyRecord> update(@PathVariable Long id,
                                            @RequestBody EnergyRecord record,
                                            @RequestParam(required = false) Long attachmentFileId) {
        try {
            return success(energyRecordService.update(id, record, attachmentFileId), "更新成功");
        } catch (Exception e) {
            return exception(e, "编辑能耗记录");
        }
    }

    /**
     * 删除能耗记录（软删除）
     *
     * 示例请求：DELETE /api/energy/10
     *
     * @param id 能耗记录ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        try {
            energyRecordService.delete(id);
            return success(null, "删除成功");
        } catch (Exception e) {
            return exception(e, "删除能耗记录");
        }
    }

    // endregion

    // region 附件接口
    // ===================================
    // 附件接口
    // ===================================

    /**
     * 查询能耗记录的凭证附件列表
     * <p>
     * 附件由前端先调用 /api/file-manager/upload（root=business, dirPath=能耗管理/能耗记录）上传，
     * 再将返回的 fileId 传入新增/编辑接口完成关联
     * </p>
     *
     * 示例请求：GET /api/energy/10/attachments
     *
     * @param id 能耗记录ID
     * @return 附件文件列表（含 fileId、originalName、fileUrl 等）
     */
    @GetMapping("/{id}/attachments")
    public ApiResponse<List<FileInfo>> getAttachments(@PathVariable Long id) {
        try {
            return success(energyRecordService.getAttachments(id));
        } catch (Exception e) {
            return exception(e, "查询能耗记录附件");
        }
    }

    // endregion
}
