package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Equipment.EquipmentWithDetailsDto;
import com.tonghui.erp.Common.utils.EntityUtils;
import com.tonghui.erp.Data.Entity.Equipment;
import com.tonghui.erp.Service.EquipmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 设备管理控制器
 * <p>
 * 提供设备的CRUD操作、高级搜索、维保设置管理及房间关联查询等功能，用于生产设备的全生命周期管理
 * </p>
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────────┬────────┬─────────────────────────────────────┐
 * │ #  │ 接口                                 │ 方法   │ 说明                                │
 * ├────┼──────────────────────────────────────┼────────┼─────────────────────────────────────┤
 * │ 1  │ /api/equipment                       │ GET   │ 获取所有设备列表（分页）            │
 * │ 2  │ /api/equipment/{id}                  │ GET   │ 根据ID获取设备详情                  │
 * │ 3  │ /api/equipment                       │ POST  │ 新增设备                            │
 * │ 4  │ /api/equipment/{id}                  │ PUT   │ 修改设备                            │
 * │ 5  │ /api/equipment/{id}                  │ DELETE│ 删除设备                            │
 * │ 6  │ /api/equipment/search                │ GET   │ 搜索设备（支持多条件组合查询）      │
 * │ 7  │ /api/equipment/search-with-details   │ GET   │ 搜索设备（带子表：维保记录）        │
 * │ 8  │ /api/equipment/search/manufacturer   │ GET   │ 按生产厂家搜索设备                  │
 * │ 9  │ /api/equipment/room/{roomId}         │ GET   │ 根据房间ID获取设备列表              │
 * │ 10 │ /api/equipment/active                │ GET   │ 获取所有启用的设备列表              │
 * │ 11 │ /api/equipment/{id}/maintenance      │ PUT   │ 更新设备维护日期                    │
 * │ 12 │ /api/equipment/{id}/maintenanceSettings │ PUT │ 保存设备维保设置                  │
 * └────┴──────────────────────────────────────┴────────┴─────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/equipment")
public class EquipmentController extends BaseCrudController<Equipment, Equipment, Integer> {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 设备服务
     */
    private final EquipmentService equipmentService;

    @Autowired
    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    // endregion

    // region CRUD操作实现
    // ===================================
    // CRUD操作实现
    // ===================================

    /**
     * 获取所有设备列表（分页）
     *
     * 示例请求：
     * GET /api/equipment?pageIndex=0&pageSize=10
     *
     * @param pageIndex 页码，从0开始
     * @param pageSize 每页大小
     * @return PagedResult&lt;Equipment&gt; 分页结果，包含设备列表
     */
    @Override
    protected PagedResult<Equipment> getAllData(int pageIndex, int pageSize) {
        PageRequestDto pageRequest = new PageRequestDto();
        pageRequest.setPageIndex(pageIndex);
        pageRequest.setPageSize(pageSize);
        return equipmentService.searchByName(null, pageRequest);
    }

    /**
     * 根据ID获取设备详情
     *
     * 示例请求：
     * GET /api/equipment/1
     *
     * @param id 设备ID
     * @return Equipment 设备详情
     */
    @Override
    protected Equipment getDataById(Integer id) {
        return equipmentService.getById(id);
    }

    /**
     * 新增设备
     * <p>
     * 新增前会检查固定资产编号是否已存在
     * </p>
     *
     * 示例请求：
     * POST /api/equipment
     * Content-Type: application/json
     * {
     *   "equipmentName": "热风循环烘箱",
     *   "fixedAssetCode": "EQ001",
     *   "model": "HX-100",
     *   "manufacturer": "某设备厂",
     *   "roomId": 1,
     *   "status": 1
     * }
     *
     * @param equipment 设备实体对象
     * @return Equipment 新增的设备
     */
    @Override
    protected Equipment doCreate(Equipment equipment) {
        // 检查固定资产编号是否已存在
        if (equipmentService.getByFixedAssetCode(equipment.getFixedAssetCode()) != null) {
            throw new RuntimeException("固定资产编号已存在");
        }

        // 清理已软删除的相同固定资产编号记录（避免唯一键冲突）
        if (equipment.getFixedAssetCode() != null && !equipment.getFixedAssetCode().isEmpty()) {
            equipmentService.cleanSoftDeletedByFixedAssetCode(equipment.getFixedAssetCode());
        }

        equipmentService.save(equipment);
        return equipment;
    }

    /**
     * 修改设备
     * <p>
     * 修改前会检查设备是否存在以及固定资产编号是否被其他记录使用
     * </p>
     *
     * 示例请求：
     * PUT /api/equipment/1
     * Content-Type: application/json
     * {
     *   "equipmentName": "热风循环烘箱（更新）",
     *   "model": "HX-200"
     * }
     *
     * @param id 设备ID
     * @param equipment 设备实体对象
     * @return Equipment 修改后的设备
     */
    @Override
    protected Equipment doUpdate(Integer id, Equipment equipment) {
        Equipment existing = equipmentService.getById(id);
        if (existing == null) {
            throw new RuntimeException("设备不存在");
        }

        // 检查固定资产编号是否被其他记录使用
        Equipment byCode = equipmentService.getByFixedAssetCode(equipment.getFixedAssetCode());
        if (byCode != null && !byCode.getEquipmentId().equals(id)) {
            throw new RuntimeException("固定资产编号已存在");
        }

        equipment.setEquipmentId(id);
        equipmentService.updateById(equipment);
        return equipment;
    }

    /**
     * 删除设备
     *
     * 示例请求：
     * DELETE /api/equipment/1
     *
     * @param id 设备ID
     * @return boolean 删除结果
     */
    @Override
    protected boolean doDelete(Integer id) {
        return equipmentService.removeById(id);
    }

    // endregion

    // region 高级查询接口
    // ===================================
    // 高级查询接口
    // ===================================

    /**
     * 搜索设备（支持多条件组合查询）
     * <p>
     * 支持按设备名称、型号、厂家、创建时间、更新时间等条件进行组合查询，当页码和页面大小都为-1时返回全部结果
     * </p>
     *
     * 示例请求：
     * GET /api/equipment/search?keyword=烘箱&model=HX&manufacturer=某厂&createdTimeStart=2026-01-01T00:00:00&createdTimeEnd=2026-12-31T23:59:59&pageIndex=0&pageSize=10
     *
     * @param equipment 设备查询条件对象（可包含设备名称、型号等）
     * @param keyword 关键字（对固定资产编号、设备名称进行模糊匹配，可选）
     * @param createdTimeStart 创建时间起始（可选）
     * @param createdTimeEnd 创建时间结束（可选）
     * @param updatedTimeStart 更新时间起始（可选）
     * @param updatedTimeEnd 更新时间结束（可选）
     * @param pageRequest 分页请求参数（页码、页面大小，设为 -1 可获取全部数据）
     * @return ApiResponse&lt;PagedResult&lt;Equipment&gt;&gt; 设备列表（分页）
     */
  @GetMapping("/search")
  public ApiResponse<PagedResult<Equipment>> searchEquipments(
            Equipment equipment,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) java.time.LocalDateTime createdTimeStart,
            @RequestParam(required = false) java.time.LocalDateTime createdTimeEnd,
            @RequestParam(required = false) java.time.LocalDateTime updatedTimeStart,
            @RequestParam(required = false) java.time.LocalDateTime updatedTimeEnd,
            @ModelAttribute PageRequestDto pageRequest) {
        pageRequest = processPageRequest(pageRequest);
        
        // 当页码和页面大小都为 -1 时，返回所有结果
        if (pageRequest.getPageIndex() == -1 && pageRequest.getPageSize() == -1) {
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<Equipment> pageResult = equipmentService.queryEquipments(
                  equipment, 
                    keyword,
                    createdTimeStart,
                    createdTimeEnd,
                    updatedTimeStart,
                    updatedTimeEnd,
                    0,
                    Integer.MAX_VALUE);
            
            // 转换为 PagedResult 格式
            PagedResult<Equipment> result = new PagedResult<>();
            result.setItems(pageResult.getRecords());
            result.setTotalCount((int) pageResult.getTotal());
            result.setPageIndex(0);
            result.setPageSize((int) pageResult.getSize());
            return success(result);
        }
        
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Equipment> pageResult = equipmentService.queryEquipments(
              equipment,
                keyword,
                createdTimeStart,
                createdTimeEnd,
                updatedTimeStart,
                updatedTimeEnd,
                pageRequest.getPageIndex(),
                pageRequest.getPageSize());

        // 转换为 PagedResult 格式
        PagedResult<Equipment> result = new PagedResult<>();
        result.setItems(pageResult.getRecords());
        result.setTotalCount((int) pageResult.getTotal());
        result.setPageIndex(pageRequest.getPageIndex());
        result.setPageSize(pageRequest.getPageSize());
        return success(result);
    }

    /**
     * 搜索设备（带子表：维保记录）
     *
     * 示例请求：
     * GET /api/equipment/search-with-details?keyword=烘箱&pageIndex=0&pageSize=10
     *
     * @param equipment 设备查询条件对象
     * @param keyword 关键字（对固定资产编号、设备名称进行模糊匹配，可选）
     * @param createdTimeStart 创建时间起始（可选）
     * @param createdTimeEnd 创建时间结束（可选）
     * @param updatedTimeStart 更新时间起始（可选）
     * @param updatedTimeEnd 更新时间结束（可选）
     * @param pageRequest 分页请求参数
     * @return ApiResponse&lt;PagedResult&lt;EquipmentWithDetailsDto&gt;&gt; 设备列表（含维保记录）
     */
    @GetMapping("/search-with-details")
    public ApiResponse<PagedResult<EquipmentWithDetailsDto>> searchWithDetails(
            Equipment equipment,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) java.time.LocalDateTime createdTimeStart,
            @RequestParam(required = false) java.time.LocalDateTime createdTimeEnd,
            @RequestParam(required = false) java.time.LocalDateTime updatedTimeStart,
            @RequestParam(required = false) java.time.LocalDateTime updatedTimeEnd,
            @ModelAttribute PageRequestDto pageRequest) {
        try {
            pageRequest = processPageRequest(pageRequest);
            PagedResult<EquipmentWithDetailsDto> result = equipmentService.searchWithDetails(
                equipment, keyword, createdTimeStart, createdTimeEnd, updatedTimeStart, updatedTimeEnd,
                pageRequest.getPageIndex(), pageRequest.getPageSize());
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "searchWithDetails");
        }
    }

    /**
     * 按生产厂家搜索设备
     *
     * 示例请求：
     * GET /api/equipment/search/manufacturer?manufacturer=某厂&pageIndex=0&pageSize=10
     *
     * @param manufacturer 生产厂家名称（可选，支持模糊搜索）
     * @param pageRequest 分页请求参数（页码、页面大小）
     * @return ApiResponse&lt;PagedResult&lt;Equipment&gt;&gt; 设备列表（分页）
     */
    @GetMapping("/search/manufacturer")
    public ApiResponse<PagedResult<Equipment>> searchByManufacturer(
            @RequestParam(required = false) String manufacturer,
            @ModelAttribute PageRequestDto pageRequest) {
        pageRequest = processPageRequest(pageRequest);
        PagedResult<Equipment> result = equipmentService.searchByManufacturer(manufacturer, pageRequest);
        return success(result);
    }

    /**
     * 根据房间ID获取设备列表
     *
     * 示例请求：
     * GET /api/equipment/room/1
     *
     * @param roomId 房间ID（路径参数）
     * @return ApiResponse&lt;List&lt;Equipment&gt;&gt; 该房间下的所有设备列表
     */
    @GetMapping("/room/{roomId}")
    public ApiResponse<List<Equipment>> getByRoomId(@PathVariable Integer roomId) {
        List<Equipment> result = equipmentService.listByRoomId(roomId);
        return success(result);
    }

    /**
     * 获取所有启用的设备列表
     *
     * 示例请求：
     * GET /api/equipment/active
     *
     * @return ApiResponse&lt;List&lt;Equipment&gt;&gt; 启用的设备列表
     */
    @GetMapping("/active")
    public ApiResponse<List<Equipment>> listActiveEquipments() {
        List<Equipment> result = equipmentService.listActive();
        return success(result);
    }

    // endregion

    // region 设备维护管理接口
    // ===================================
    // 设备维护管理接口
    // ===================================

    /**
     * 更新设备维护日期
     *
     * 示例请求：
     * PUT /api/equipment/1/maintenance?maintenanceDate=2026-06-30&updaterId=1
     *
     * @param id 设备ID（路径参数）
     * @param maintenanceDate 维护日期
     * @param updaterId 更新人ID（可选，不传则使用当前登录用户）
     * @return ApiResponse&lt;Boolean&gt; 更新结果
     */
    @PutMapping("/{id}/maintenance")
    public ApiResponse<Boolean> updateMaintenanceDate(
            @PathVariable Integer id,
            @RequestParam LocalDate maintenanceDate,
            @RequestParam(required = false) Long updaterId) {
        // 如果没有提供更新人 ID，则使用当前登录用户 ID
        if (updaterId == null) {
            updaterId = EntityUtils.getCurrentUserId();
        }
        
        boolean result = equipmentService.updateMaintenanceDate(id, maintenanceDate, updaterId);
        if (result) {
            return success(true, "更新维护日期成功");
        } else {
            return error("更新维护日期失败");
        }
    }

    /**
     * 保存设备维保设置
     *
     * 示例请求：
     * PUT /api/equipment/1/maintenanceSettings?maintenanceCycle=6&reminderDays=7
     *
     * @param id 设备ID（路径参数）
     * @param maintenanceCycle 维保周期（月）
     * @param reminderDays 到期提醒天数
     * @return ApiResponse&lt;Boolean&gt; 更新结果
     */
    @PutMapping("/{id}/maintenanceSettings")
    public ApiResponse<Boolean> saveMaintenanceSettings(
            @PathVariable Integer id,
            @RequestParam Integer maintenanceCycle,
            @RequestParam Integer reminderDays) {
        Equipment existing = equipmentService.getById(id);
        if (existing == null) {
            return error("设备不存在");
        }
        
        Equipment update = new Equipment();
        update.setEquipmentId(id);
        update.setMaintenanceCycle(maintenanceCycle);
        update.setReminderDays(reminderDays);
        
        equipmentService.updateById(update);
        return success(true, "维保设置保存成功");
    }

    // endregion
}
