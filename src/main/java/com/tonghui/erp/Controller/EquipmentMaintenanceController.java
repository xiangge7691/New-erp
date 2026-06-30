package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.utils.EntityUtils;
import com.tonghui.erp.Data.Entity.Equipment;
import com.tonghui.erp.Data.Entity.EquipmentMaintenance;
import com.tonghui.erp.Data.Entity.FileInfo;
import com.tonghui.erp.Service.EquipmentMaintenanceService;
import com.tonghui.erp.Service.EquipmentService;
import com.tonghui.erp.Service.FileInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
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
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            Page<EquipmentMaintenance> page = new Page<>(pageIndex + 1, pageSize);
            QueryWrapper<EquipmentMaintenance> wrapper = new QueryWrapper<>();
            
            if (equipmentId != null) {
                wrapper.eq("equipment_id", equipmentId);
            }
            if (maintenanceType != null && !maintenanceType.isEmpty()) {
                wrapper.eq("maintenance_type", maintenanceType);
            }
            wrapper.orderByDesc("maintenance_date");
            
            Page<EquipmentMaintenance> pageResult = equipmentMaintenanceService.page(page, wrapper);
            PagedResult<EquipmentMaintenance> pagedResult = new PagedResult<>();
            pagedResult.setItems(pageResult.getRecords());
            pagedResult.setTotalCount(pageResult.getTotal());
            pagedResult.setPageIndex(pageIndex);
            pagedResult.setPageSize(pageSize);
            
            return success(pagedResult);
        } catch (Exception e) {
            return exception(e, "操作");
        }
    }

    /**
     * 根据ID查询维保记录详情
     */
    @GetMapping("/{id}")
    public ApiResponse<EquipmentMaintenance> getById(@PathVariable Long id) {
        try {
            EquipmentMaintenance maintenance = equipmentMaintenanceService.getById(id);
            if (maintenance == null) {
                return error("维保记录不存在");
            }
            return success(maintenance);
        } catch (Exception e) {
            return exception(e, "操作");
        }
    }

    /**
     * 新增维保记录
     * 保养类型：自动按维保周期计算下次维保时间
     * 维修类型：不自动计算，需手工填写
     */
    @PostMapping
    public ApiResponse<EquipmentMaintenance> create(@RequestBody EquipmentMaintenance maintenance) {
        try {
            maintenance.setCreatedBy(EntityUtils.getCurrentUserId());
            maintenance.setCreatedAt(LocalDateTime.now());
            maintenance.setIsDeleted(0);
            maintenance.setVersion(0);
            equipmentMaintenanceService.saveWithAutoCalc(maintenance);
            return success(maintenance, "新增成功");
        } catch (Exception e) {
            return exception(e, "新增维保记录");
        }
    }

    /**
     * 修改维保记录
     */
    @PutMapping("/{id}")
    public ApiResponse<EquipmentMaintenance> update(@PathVariable Long id, @RequestBody EquipmentMaintenance maintenance) {
        try {
            EquipmentMaintenance existing = equipmentMaintenanceService.getById(id);
            if (existing == null) {
                return error("维保记录不存在");
            }
            maintenance.setMaintenanceId(id);
            maintenance.setUpdatedBy(EntityUtils.getCurrentUserId());
            maintenance.setUpdatedAt(LocalDateTime.now());
            equipmentMaintenanceService.updateById(maintenance);
            return success(maintenance, "修改成功");
        } catch (Exception e) {
            return exception(e, "操作");
        }
    }

    /**
     * 删除维保记录（同时删除关联附件）
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        try {
            List<FileInfo> attachments = fileInfoService.getFilesByBusiness(id, "EQUIPMENT_MAINTENANCE");
            for (FileInfo file : attachments) {
                fileInfoService.deleteFile(file.getFileId());
            }
            equipmentMaintenanceService.removeById(id);
            return success(null, "删除成功");
        } catch (Exception e) {
            return exception(e, "操作");
        }
    }

    /**
     * 维保提醒查询
     * @param days 提前天数，默认7天
     * @return 即将到期的维保记录列表
     */
    @GetMapping("/reminder")
    public ApiResponse<List<EquipmentMaintenance>> reminder(
            @RequestParam(defaultValue = "7") int days) {
        try {
            List<EquipmentMaintenance> list = equipmentMaintenanceService.findUpcomingMaintenance(days);
            return success(list);
        } catch (Exception e) {
            return exception(e, "操作");
        }
    }

    /**
     * 根据设备ID查询维保记录
     */
    @GetMapping("/byEquipment/{equipmentId}")
    public ApiResponse<List<EquipmentMaintenance>> getByEquipmentId(@PathVariable Long equipmentId) {
        try {
            List<EquipmentMaintenance> list = equipmentMaintenanceService.findByEquipmentId(equipmentId);
            return success(list);
        } catch (Exception e) {
            return exception(e, "操作");
        }
    }

    // #region 附件管理

    /**
     * 上传维保附件
     * 路径格式：uploaded-files/维保记录/{年}/{月}/{设备名}/{uuid.ext}
     */
    @PostMapping("/{id}/attachments")
    public ApiResponse<FileInfo> uploadAttachment(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String description) {
        try {
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
        } catch (Exception e) {
            return exception(e, "上传附件");
        }
    }

    /**
     * 查询维保记录的附件列表
     */
    @GetMapping("/{id}/attachments")
    public ApiResponse<List<FileInfo>> getAttachments(@PathVariable Long id) {
        try {
            List<FileInfo> files = fileInfoService.getFilesByBusiness(id, "EQUIPMENT_MAINTENANCE");
            return success(files);
        } catch (Exception e) {
            return exception(e, "查询附件");
        }
    }

    /**
     * 删除维保附件
     */
    @DeleteMapping("/{id}/attachments/{fileId}")
    public ApiResponse<Void> deleteAttachment(@PathVariable Long id, @PathVariable Long fileId) {
        try {
            fileInfoService.deleteFile(fileId);
            return success(null, "附件删除成功");
        } catch (Exception e) {
            return exception(e, "删除附件");
        }
    }

    // #endregion
}
