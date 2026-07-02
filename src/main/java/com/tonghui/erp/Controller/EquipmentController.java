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
 * 提供设备的增删改查、搜索及维护管理功能
 */
@RestController
@RequestMapping("/api/equipment")
public class EquipmentController extends BaseCrudController<Equipment, Equipment, Integer> {

    private final EquipmentService equipmentService;

    @Autowired
    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    // CRUD 实现
    @Override
    protected PagedResult<Equipment> getAllData(int pageIndex, int pageSize) {
        PageRequestDto pageRequest = new PageRequestDto();
        pageRequest.setPageIndex(pageIndex);
        pageRequest.setPageSize(pageSize);
        return equipmentService.searchByName(null, pageRequest);
    }

    @Override
    protected Equipment getDataById(Integer id) {
        return equipmentService.getById(id);
    }

    @Override
    protected Equipment doCreate(Equipment equipment) {
        // 检查固定资产编号是否已存在
        if (equipmentService.getByFixedAssetCode(equipment.getFixedAssetCode()) != null) {
            throw new RuntimeException("固定资产编号已存在");
        }

        equipmentService.save(equipment);
        return equipment;
    }

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

    @Override
    protected boolean doDelete(Integer id) {
        return equipmentService.removeById(id);
    }

    /**
     * 搜索设备（支持多条件组合查询）
     * @param equipment 设备查询条件对象（可包含设备名称、型号等）
     * @param createdTimeStart 创建时间起始（可选）
     * @param createdTimeEnd 创建时间结束（可选）
     * @param updatedTimeStart 更新时间起始（可选）
     * @param updatedTimeEnd 更新时间结束（可选）
     * @param pageRequest 分页请求参数（页码、页面大小，设为 -1 可获取全部数据）
     * @return 设备列表（分页）
     */
  @GetMapping("/search")
  public ApiResponse<PagedResult<Equipment>> searchEquipments(
            Equipment equipment,
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
        result.setPageSize((int) pageResult.getSize());
        return success(result);
    }

    /**
     * 搜索设备（带子表：维保记录）
     */
    @GetMapping("/search-with-details")
    public ApiResponse<PagedResult<EquipmentWithDetailsDto>> searchWithDetails(
            Equipment equipment,
            @RequestParam(required = false) java.time.LocalDateTime createdTimeStart,
            @RequestParam(required = false) java.time.LocalDateTime createdTimeEnd,
            @RequestParam(required = false) java.time.LocalDateTime updatedTimeStart,
            @RequestParam(required = false) java.time.LocalDateTime updatedTimeEnd,
            @ModelAttribute PageRequestDto pageRequest) {
        try {
            pageRequest = processPageRequest(pageRequest);
            PagedResult<EquipmentWithDetailsDto> result = equipmentService.searchWithDetails(
                equipment, createdTimeStart, createdTimeEnd, updatedTimeStart, updatedTimeEnd,
                pageRequest.getPageIndex(), pageRequest.getPageSize());
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "searchWithDetails");
        }
    }

    /**
     * 按生产厂家搜索设备
     * @param manufacturer 生产厂家名称（可选，支持模糊搜索）
     * @param pageRequest 分页请求参数（页码、页面大小）
     * @return 设备列表（分页）
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
     * 根据房间 ID 获取设备列表
     * @param roomId 房间 ID
     * @return 该房间下的所有设备列表
     */
    @GetMapping("/room/{roomId}")
    public ApiResponse<List<Equipment>> getByRoomId(@PathVariable Integer roomId) {
        List<Equipment> result = equipmentService.listByRoomId(roomId);
        return success(result);
    }

    /**
     * 获取所有启用的设备列表
     * @return 启用的设备列表
     */
    @GetMapping("/active")
    public ApiResponse<List<Equipment>> listActiveEquipments() {
        List<Equipment> result = equipmentService.listActive();
        return success(result);
    }

    /**
     * 更新设备维护日期
     * @param id 设备 ID
     * @param maintenanceDate 维护日期
     * @param updaterId 更新人 ID（可选，不传则使用当前登录用户）
     * @return 更新结果
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
     * @param id 设备ID
     * @param maintenanceCycle 维保周期（月）
     * @param reminderDays 到期提醒天数
     * @return 更新结果
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
}
