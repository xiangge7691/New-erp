package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Constants.OperationType;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.annotation.OperationLog;
import com.tonghui.erp.Data.Entity.ProductionShift;
import com.tonghui.erp.Service.ProductionShiftService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 生产班次控制器
 * <p>
 * 维护班次时段、适用班组与启停状态；排产按班组班次校验人员可用时间。
 * </p>
 */
@RestController
@RequestMapping("/api/production-shifts")
public class ProductionShiftController extends BaseController {

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    /** 生产班次服务 */
    private final ProductionShiftService productionShiftService;

    public ProductionShiftController(ProductionShiftService productionShiftService) {
        this.productionShiftService = productionShiftService;
    }

    // endregion

    // region 查询
    // ===================================
    // 查询
    // ===================================

    /**
     * 查询班次列表
     *
     * 示例请求：
     * GET /api/production-shifts?keyword=早班&status=1
     *
     * @param keyword 班次名称关键字
     * @param status  状态：0停用/1启用，为空查全部
     * @return 班次列表
     */
    @GetMapping
    public ApiResponse<List<ProductionShift>> list(@RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) Integer status) {
        try {
            return success(productionShiftService.listShifts(keyword, status));
        } catch (Exception ex) {
            return exception(ex, "查询班次列表");
        }
    }

    /**
     * 查询启用班次
     *
     * 示例请求：
     * GET /api/production-shifts/active
     *
     * @return 启用班次列表
     */
    @GetMapping("/active")
    public ApiResponse<List<ProductionShift>> listActive() {
        try {
            return success(productionShiftService.listActive());
        } catch (Exception ex) {
            return exception(ex, "查询启用班次");
        }
    }

    /**
     * 查询班组适用班次
     *
     * 示例请求：
     * GET /api/production-shifts/by-team/1
     *
     * @param teamId 班组ID
     * @return 班次列表
     */
    @GetMapping("/by-team/{teamId}")
    public ApiResponse<List<ProductionShift>> byTeam(@PathVariable Long teamId) {
        try {
            return success(productionShiftService.listShiftsForTeam(teamId));
        } catch (Exception ex) {
            return exception(ex, "查询班组班次");
        }
    }

    /**
     * 查询班次详情
     *
     * 示例请求：
     * GET /api/production-shifts/1
     *
     * @param id 班次ID
     * @return 班次详情
     */
    @GetMapping("/{id}")
    public ApiResponse<ProductionShift> detail(@PathVariable Long id) {
        try {
            ProductionShift shift = productionShiftService.getShiftDetail(id);
            return shift == null ? error("班次不存在") : success(shift);
        } catch (Exception ex) {
            return exception(ex, "查询班次详情");
        }
    }

    // endregion

    // region 维护
    // ===================================
    // 维护
    // ===================================

    /**
     * 新增班次
     *
     * 示例请求：
     * POST /api/production-shifts
     * {
     *   "shiftName": "早班",
     *   "startTime": "08:00:00",
     *   "endTime": "17:00:00",
     *   "teamIds": [1, 2]
     * }
     *
     * @param shift 班次
     * @return 新增后的班次
     */
    @OperationLog(module = "生产班次", type = OperationType.CREATE, desc = "新增班次",
            businessType = "PRODUCTION_SHIFT", businessId = "#shift.id")
    @PostMapping
    public ApiResponse<ProductionShift> create(@RequestBody ProductionShift shift) {
        try {
            return success(productionShiftService.createShift(shift), "创建成功");
        } catch (Exception ex) {
            return exception(ex, "新增班次");
        }
    }

    /**
     * 修改班次
     *
     * 示例请求：
     * PUT /api/production-shifts/1
     * {
     *   "shiftName": "早班",
     *   "startTime": "08:00:00",
     *   "endTime": "17:00:00",
     *   "teamIds": [1, 2]
     * }
     *
     * @param id    班次ID
     * @param shift 班次
     * @return 修改后的班次
     */
    @OperationLog(module = "生产班次", type = OperationType.UPDATE, desc = "修改班次",
            businessType = "PRODUCTION_SHIFT", businessId = "#id")
    @PutMapping("/{id}")
    public ApiResponse<ProductionShift> update(@PathVariable Long id, @RequestBody ProductionShift shift) {
        try {
            return success(productionShiftService.updateShift(id, shift), "更新成功");
        } catch (Exception ex) {
            return exception(ex, "修改班次");
        }
    }

    /**
     * 启用/停用班次
     *
     * 示例请求：
     * PUT /api/production-shifts/1/status?status=0
     *
     * @param id     班次ID
     * @param status 状态：0停用/1启用
     * @return 修改后的班次
     */
    @OperationLog(module = "生产班次", type = OperationType.UPDATE, desc = "启停班次",
            businessType = "PRODUCTION_SHIFT", businessId = "#id")
    @PutMapping("/{id}/status")
    public ApiResponse<ProductionShift> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        try {
            return success(productionShiftService.changeStatus(id, status), "状态已更新");
        } catch (Exception ex) {
            return exception(ex, "启停班次");
        }
    }

    // endregion
}
