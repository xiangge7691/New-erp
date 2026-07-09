package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.Equipment;
import com.tonghui.erp.Data.Entity.EquipmentMaintenance;
import com.tonghui.erp.Data.Entity.FileInfo;
import com.tonghui.erp.Data.Entity.PersonnelFile;
import com.tonghui.erp.Data.Entity.RoomInfo;
import com.tonghui.erp.Service.EquipmentMaintenanceService;
import com.tonghui.erp.Service.EquipmentService;
import com.tonghui.erp.Service.FileInfoService;
import com.tonghui.erp.Service.PersonnelFileService;
import com.tonghui.erp.Service.RoomInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 设备维保记录控制器
 * <p>
 * 提供设备维保记录的CRUD操作、维保提醒查询及附件管理功能，用于设备维护保养的全流程管理
 * </p>
 *
 * 接口清单：
 * ┌────┬────────────────────────────────────────┬────────┬─────────────────────────────────────┐
 * │ #  │ 接口                                   │ 方法   │ 说明                                │
 * ├────┼────────────────────────────────────────┼────────┼─────────────────────────────────────┤
 * │ 1  │ /api/equipmentMaintenance              │ GET   │ 分页查询维保记录列表                │
 * │ 2  │ /api/equipmentMaintenance/{id}         │ GET   │ 根据ID查询维保记录详情              │
 * │ 3  │ /api/equipmentMaintenance              │ POST  │ 新增维保记录                        │
 * │ 4  │ /api/equipmentMaintenance/{id}         │ PUT   │ 修改维保记录                        │
 * │ 5  │ /api/equipmentMaintenance/{id}         │ DELETE│ 删除维保记录（同时删除关联附件）     │
 * │ 6  │ /api/equipmentMaintenance/reminder     │ GET   │ 维保提醒查询                        │
 * │ 7  │ /api/equipmentMaintenance/byEquipment/{equipmentId} │ GET │ 根据设备ID查询维保记录 │
 * │ 8  │ /api/equipmentMaintenance/{id}/attachments │ POST │ 上传维保附件                       │
 * │ 9  │ /api/equipmentMaintenance/{id}/attachments │ GET │ 查询维保记录的附件列表             │
 * │ 10 │ /api/equipmentMaintenance/{id}/attachments/{fileId} │ DELETE │ 删除维保附件           │
 * └────┴────────────────────────────────────────┴────────┴─────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/equipmentMaintenance")
public class EquipmentMaintenanceController extends BaseController {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 设备维保记录服务
     */
    @Autowired
    private EquipmentMaintenanceService equipmentMaintenanceService;

    /**
     * 文件信息服务
     */
    @Autowired
    private FileInfoService fileInfoService;

    /**
     * 设备服务
     */
    @Autowired
    private EquipmentService equipmentService;

    /**
     * 人员档案服务
     */
    @Autowired
    private PersonnelFileService personnelFileService;

    /**
     * 房间信息服务
     */
    @Autowired
    private RoomInfoService roomInfoService;

    // endregion

    // region 维保记录CRUD接口
    // ===================================
    // 维保记录CRUD接口
    // ===================================

    /**
     * 分页查询维保记录列表
     * <p>
     * 支持按设备ID、维保类型、关键词、维保人、维保公司、维保日期范围等条件筛选，按维保日期倒序排列
     * </p>
     *
     * 示例请求：
     * GET /api/equipmentMaintenance?equipmentId=1&maintenanceType=保养&keyword=检查&maintainer=张三&maintenanceCompany=维修公司&maintenanceDateStart=2026-01-01&maintenanceDateEnd=2026-12-31&pageIndex=0&pageSize=10
     *
     * @param equipmentId 设备ID（可选）
     * @param maintenanceType 维保类型（可选），如"保养"、"维修"
     * @param keyword 关键词（可选），模糊匹配维保内容
     * @param maintainer 维保人（可选）
     * @param maintenanceCompany 维保公司（可选）
     * @param maintenanceDateStart 维保日期起始（可选）
     * @param maintenanceDateEnd 维保日期结束（可选）
     * @param pageIndex 页码，从0开始（默认0）
     * @param pageSize 每页大小（默认10）
     * @return ApiResponse&lt;PagedResult&lt;EquipmentMaintenance&gt;&gt; 分页结果，包含维保记录列表和分页信息
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
        fillEquipmentInfo(pageResult.getRecords());
        PagedResult<EquipmentMaintenance> pagedResult = new PagedResult<>();
        pagedResult.setItems(pageResult.getRecords());
        pagedResult.setTotalCount(pageResult.getTotal());
        pagedResult.setPageIndex(pageIndex);
        pagedResult.setPageSize(pageSize);
        
        return success(pagedResult);
    }

    /**
     * 根据ID查询维保记录详情
     *
     * 示例请求：
     * GET /api/equipmentMaintenance/1
     *
     * @param id 维保记录ID（路径参数）
     * @return ApiResponse&lt;EquipmentMaintenance&gt; 维保记录详情，包含所有字段信息
     */
    @GetMapping("/{id}")
    public ApiResponse<EquipmentMaintenance> getById(@PathVariable Long id) {
        EquipmentMaintenance maintenance = equipmentMaintenanceService.getById(id);
        if (maintenance == null) {
            return error("维保记录不存在");
        }
        fillEquipmentInfo(Collections.singletonList(maintenance));
        return success(maintenance);
    }

    /**
     * 新增维保记录
     * <p>
     * 保养类型：自动按维保周期计算下次维保时间；维修类型：不自动计算，需手工填写
     * </p>
     *
     * 示例请求：
     * POST /api/equipmentMaintenance
     * Content-Type: application/json
     * {
     *   "equipmentId": 1,
     *   "maintenanceType": "保养",
     *   "maintenanceContent": "定期检查设备运行状态",
     *   "maintenanceDate": "2026-06-30",
     *   "nextMaintenanceDate": "2026-07-30",
     *   "maintainer": "张三",
     *   "maintenanceCompany": "设备维修公司"
     * }
     *
     * @param maintenance 维保记录实体对象
     * @return ApiResponse&lt;EquipmentMaintenance&gt; 新增的维保记录，包含自动生成的ID
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
     *
     * 示例请求：
     * PUT /api/equipmentMaintenance/1
     * Content-Type: application/json
     * {
     *   "maintenanceContent": "设备检修完成",
     *   "maintenanceCost": 500.00
     * }
     *
     * @param id 维保记录ID（路径参数）
     * @param maintenance 维保记录实体对象（包含需要更新的字段）
     * @return ApiResponse&lt;EquipmentMaintenance&gt; 修改后的维保记录
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
     *
     * 示例请求：
     * DELETE /api/equipmentMaintenance/1
     *
     * @param id 维保记录ID（路径参数）
     * @return ApiResponse&lt;Void&gt; 删除结果
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

    // endregion

    // region 维保提醒接口
    // ===================================
    // 维保提醒接口
    // ===================================

    /**
     * 维保提醒查询
     * <p>
     * 查询在未来指定天数内需要进行维保的记录，用于到期提醒
     * </p>
     *
     * 示例请求：
     * GET /api/equipmentMaintenance/reminder?days=7
     *
     * @param days 提前天数（默认7天），查询未来N天内即将到期的维保记录
     * @return ApiResponse&lt;List&lt;EquipmentMaintenance&gt;&gt; 即将到期的维保记录列表
     */
    @GetMapping("/reminder")
    public ApiResponse<List<EquipmentMaintenance>> reminder(
            @RequestParam(defaultValue = "7") int days) {
        List<EquipmentMaintenance> list = equipmentMaintenanceService.findUpcomingMaintenance(days);
        fillEquipmentInfo(list);
        return success(list);
    }

    /**
     * 根据设备ID查询维保记录
     *
     * 示例请求：
     * GET /api/equipmentMaintenance/byEquipment/1?maintenanceType=保养&maintenanceDateStart=2026-01-01&maintenanceDateEnd=2026-12-31
     *
     * @param equipmentId 设备ID（路径参数）
     * @param maintenanceType 维保类型（可选）
     * @param maintenanceDateStart 维保日期起始（可选）
     * @param maintenanceDateEnd 维保日期结束（可选）
     * @return ApiResponse&lt;List&lt;EquipmentMaintenance&gt;&gt; 维保记录列表
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
        fillEquipmentInfo(list);
        return success(list);
    }

    // endregion

    // region 附件管理接口
    // ===================================
    // 附件管理接口
    // ===================================

    /**
     * 上传维保附件
     *
     * 示例请求：
     * POST /api/equipmentMaintenance/1/attachments
     * Content-Type: multipart/form-data
     * body:
     *   file: [选择文件]
     *   description: 维修照片
     *
     * @param id 维保记录ID（路径参数）
     * @param file 文件对象（multipart/form-data，字段名 file）
     * @param description 文件描述（可选）
     * @return ApiResponse&lt;FileInfo&gt; 上传的文件信息
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
     *
     * 示例请求：
     * GET /api/equipmentMaintenance/1/attachments
     *
     * @param id 维保记录ID（路径参数）
     * @return ApiResponse&lt;List&lt;FileInfo&gt;&gt; 附件文件列表
     */
    @GetMapping("/{id}/attachments")
    public ApiResponse<List<FileInfo>> getAttachments(@PathVariable Long id) {
        List<FileInfo> files = fileInfoService.getFilesByBusiness(id, "EQUIPMENT_MAINTENANCE", null);
        return success(files);
    }

    /**
     * 删除维保附件
     *
     * 示例请求：
     * DELETE /api/equipmentMaintenance/1/attachments/101
     *
     * @param id 维保记录ID（路径参数）
     * @param fileId 文件ID（路径参数）
     * @return ApiResponse&lt;Void&gt; 删除结果
     */
    @DeleteMapping("/{id}/attachments/{fileId}")
    public ApiResponse<Void> deleteAttachment(@PathVariable Long id, @PathVariable Long fileId) {
        fileInfoService.deleteFile(fileId);
        return success(null, "附件删除成功");
    }

    // endregion

    // region 私有辅助方法
    // ===================================
    // 私有辅助方法
    // ===================================

    /**
     * 根据关联ID批量查询设备信息和记录人姓名，填充关联字段
     *
     * @param maintenanceList 维保记录列表
     */
    private void fillEquipmentInfo(List<EquipmentMaintenance> maintenanceList) {
        if (maintenanceList == null || maintenanceList.isEmpty()) {
            return;
        }

        // 批量查询设备信息
        List<Long> equipmentIds = maintenanceList.stream()
                .map(EquipmentMaintenance::getEquipmentId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Integer, Equipment> equipmentMap = equipmentIds.isEmpty()
                ? java.util.Collections.emptyMap()
                : equipmentService.listByIds(equipmentIds.stream().map(Long::intValue).collect(Collectors.toList()))
                        .stream()
                        .collect(Collectors.toMap(Equipment::getEquipmentId, e -> e));
        for (EquipmentMaintenance maintenance : maintenanceList) {
            if (maintenance.getEquipmentId() != null) {
                Equipment equipment = equipmentMap.get(maintenance.getEquipmentId().intValue());
                if (equipment != null) {
                    maintenance.setEquipment(equipment);
                }
            }
        }

        // 批量查询房间名称填充到设备信息
        List<Integer> roomIds = equipmentMap.values().stream()
                .map(Equipment::getRoomId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (!roomIds.isEmpty()) {
            Map<Integer, RoomInfo> roomMap = roomInfoService.listByIds(roomIds).stream()
                    .collect(Collectors.toMap(RoomInfo::getRoomId, r -> r));
            for (EquipmentMaintenance maintenance : maintenanceList) {
                Equipment equipment = maintenance.getEquipment();
                if (equipment != null && equipment.getRoomId() != null) {
                    RoomInfo room = roomMap.get(equipment.getRoomId());
                    if (room != null) {
                        equipment.setRoomName(room.getRoomName());
                    }
                }
            }
        }

        // 批量查询记录人姓名
        List<Long> recorderIds = maintenanceList.stream()
                .map(EquipmentMaintenance::getRecorderId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (!recorderIds.isEmpty()) {
            Map<Long, PersonnelFile> personnelMap = personnelFileService.listByIds(recorderIds)
                    .stream()
                    .collect(Collectors.toMap(PersonnelFile::getPersonnelFileId, p -> p));
            for (EquipmentMaintenance maintenance : maintenanceList) {
                if (maintenance.getRecorderId() != null) {
                    PersonnelFile personnel = personnelMap.get(maintenance.getRecorderId());
                    if (personnel != null) {
                        maintenance.setRecorderName(personnel.getName());
                    }
                }
            }
        }
    }

    // endregion
}
