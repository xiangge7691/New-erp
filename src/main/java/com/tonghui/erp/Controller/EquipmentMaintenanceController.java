package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.Equipment;
import com.tonghui.erp.Data.Entity.EquipmentMaintenance;
import com.tonghui.erp.Data.Entity.FileInfo;
import com.tonghui.erp.Service.EquipmentMaintenanceService;
import com.tonghui.erp.Service.EquipmentService;
import com.tonghui.erp.Service.FileInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import java.util.List;

/**
 * 设备维保记录控制器
 * 提供设备维保记录的CRUD操作及维保提醒查询
 */
@RestController
@RequestMapping("/api/equipmentMaintenance")
public class EquipmentMaintenanceController extends BaseController {

    @Autowired
    private EquipmentMaintenanceService equipmentMaintenanceService;

    @Autowired
    private FileInfoService fileInfoService;

    @Autowired
    private EquipmentService equipmentService;

    /**
     * 分页查询维保记录列表
     */
    @GetMapping
    public ApiResponse<PagedResult<EquipmentMaintenance>> getAll(
            @RequestParam(required = false) Long equipmentId,
            @RequestParam(required = false) String maintenanceType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String maintainer,
            @RequestParam(required = false) String maintenanceCompany,
            @RequestParam(required = false) LocalDate maintenanceDateStart,
            @RequestParam(required = false) LocalDate maintenanceDateEnd,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "10") int pageSize) {
        Page<EquipmentMaintenance> page = new Page<>(pageIndex + 1, pageSize);
        QueryWrapper<EquipmentMaintenance> wrapper = new QueryWrapper<>();
        
        if (equipmentId != null) {
            wrapper.eq("equipment_id", equipmentId);
        }
        if (StringUtils.hasText(maintenanceType)) {
            wrapper.eq("maintenance_type", maintenanceType);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like("maintenance_content", keyword);
        }
        if (StringUtils.hasText(maintainer)) {
            wrapper.like("maintainer", maintainer);
        }
        if (StringUtils.hasText(maintenanceCompany)) {
            wrapper.like("maintenance_company", maintenanceCompany);
        }
        if (maintenanceDateStart != null) {
            wrapper.ge("maintenance_date", maintenanceDateStart);
        }
        if (maintenanceDateEnd != null) {
            wrapper.le("maintenance_date", maintenanceDateEnd);
        }
        wrapper.orderByDesc("maintenance_date");
        
        Page<EquipmentMaintenance> pageResult = equipmentMaintenanceService.page(page, wrapper);
        PagedResult<EquipmentMaintenance> pagedResult = new PagedResult<>();
        pagedResult.setItems(pageResult.getRecords());
        pagedResult.setTotalCount(pageResult.getTotal());
        pagedResult.setPageIndex(pageIndex);
        pagedResult.setPageSize(pageSize);
        
        return success(pagedResult);
    }

    /**
     * 根据ID查询维保记录详情
     */
    @GetMapping("/{id}")
    public ApiResponse<EquipmentMaintenance> getById(@PathVariable Long id) {
        EquipmentMaintenance maintenance = equipmentMaintenanceService.getById(id);
        if (maintenance == null) {
            return error("维保记录不存在");
        }
        return success(maintenance);
    }

    /**
     * 新增维保记录
     * 保养类型：自动按维保周期计算下次维保时间
     * 维修类型：不自动计算，需手工填写
     */
    @PostMapping
    public ApiResponse<EquipmentMaintenance> create(@RequestBody EquipmentMaintenance maintenance) {
        maintenance.setIsDeleted(0);
        maintenance.setVersion(0);
        equipmentMaintenanceService.saveWithAutoCalc(maintenance);
        return success(maintenance, "新增成功");
    }

    /**
     * 修改维保记录
     */
    @PutMapping("/{id}")
    public ApiResponse<EquipmentMaintenance> update(@PathVariable Long id, @RequestBody EquipmentMaintenance maintenance) {
        EquipmentMaintenance existing = equipmentMaintenanceService.getById(id);
        if (existing == null) {
            return error("维保记录不存在");
        }
        maintenance.setMaintenanceId(id);
        equipmentMaintenanceService.updateById(maintenance);
        return success(maintenance, "修改成功");
    }

    /**
     * 删除维保记录（同时删除关联附件）
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        List<FileInfo> attachments = fileInfoService.getFilesByBusiness(id, "EQUIPMENT_MAINTENANCE", null);
        for (FileInfo file : attachments) {
            fileInfoService.deleteFile(file.getFileId());
        }
        equipmentMaintenanceService.removeById(id);
        return success(null, "删除成功");
    }

    /**
     * 维保提醒查询
     * @param days 提前天数，默认7天
     * @return 即将到期的维保记录列表
     */
    @GetMapping("/reminder")
    public ApiResponse<List<EquipmentMaintenance>> reminder(
            @RequestParam(defaultValue = "7") int days) {
        List<EquipmentMaintenance> list = equipmentMaintenanceService.findUpcomingMaintenance(days);
        return success(list);
    }

    /**
     * 根据设备ID查询维保记录
     */
    @GetMapping("/byEquipment/{equipmentId}")
    public ApiResponse<List<EquipmentMaintenance>> getByEquipmentId(
            @PathVariable Long equipmentId,
            @RequestParam(required = false) String maintenanceType,
            @RequestParam(required = false) LocalDate maintenanceDateStart,
            @RequestParam(required = false) LocalDate maintenanceDateEnd) {
        QueryWrapper<EquipmentMaintenance> wrapper = new QueryWrapper<>();
        wrapper.eq("equipment_id", equipmentId);
        if (StringUtils.hasText(maintenanceType)) {
            wrapper.eq("maintenance_type", maintenanceType);
        }
        if (maintenanceDateStart != null) {
            wrapper.ge("maintenance_date", maintenanceDateStart);
        }
        if (maintenanceDateEnd != null) {
            wrapper.le("maintenance_date", maintenanceDateEnd);
        }
        wrapper.orderByDesc("maintenance_date");
        List<EquipmentMaintenance> list = equipmentMaintenanceService.list(wrapper);
        return success(list);
    }

    // #region 附件管理

    /**
     * 上传维保附件
     * 路径格式：uploaded-files/设备维保/{年}/{月}/{设备名}/{uuid.ext}
     */
    @PostMapping("/{id}/attachments")
    public ApiResponse<FileInfo> uploadAttachment(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String description) throws Exception {
        EquipmentMaintenance maintenance = equipmentMaintenanceService.getById(id);
        if (maintenance == null) {
            return error("维保记录不存在");
        }

        String entityName = "维保记录" + id;
        if (maintenance.getEquipmentId() != null) {
            Equipment equipment = equipmentService.getById(maintenance.getEquipmentId());
            if (equipment != null) {
                entityName = equipment.getEquipmentName();
            }
        }

        FileInfo fileInfo = fileInfoService.uploadFileWithBusinessPath(
                file, "EQUIPMENT_MAINTENANCE", id, entityName, description);
        return success(fileInfo, "附件上传成功");
    }

    /**
     * 查询维保记录的附件列表
     */
    @GetMapping("/{id}/attachments")
    public ApiResponse<List<FileInfo>> getAttachments(@PathVariable Long id) {
        List<FileInfo> files = fileInfoService.getFilesByBusiness(id, "EQUIPMENT_MAINTENANCE", null);
        return success(files);
    }

    /**
     * 删除维保附件
     */
    @DeleteMapping("/{id}/attachments/{fileId}")
    public ApiResponse<Void> deleteAttachment(@PathVariable Long id, @PathVariable Long fileId) {
        fileInfoService.deleteFile(fileId);
        return success(null, "附件删除成功");
    }

    // #endregion
}
