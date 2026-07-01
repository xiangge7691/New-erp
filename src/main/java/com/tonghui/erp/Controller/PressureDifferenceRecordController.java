package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.PressureDifferenceRecord;
import com.tonghui.erp.Data.Entity.RoomInfo;
import com.tonghui.erp.Service.PressureDifferenceRecordService;
import com.tonghui.erp.Service.RoomInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 压差记录控制器
 * 提供压差记录的CRUD操作
 */
@RestController
@RequestMapping("/api/room/{roomId}/pressure")
public class PressureDifferenceRecordController extends BaseController {

    @Autowired
    private PressureDifferenceRecordService pressureDifferenceRecordService;

    @Autowired
    private RoomInfoService roomInfoService;

    /**
     * 分页查询压差记录列表
     */
    @GetMapping
    public ApiResponse<PagedResult<PressureDifferenceRecord>> getAll(
            @PathVariable Integer roomId,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            Page<PressureDifferenceRecord> page = new Page<>(pageIndex + 1, pageSize);
            QueryWrapper<PressureDifferenceRecord> wrapper = new QueryWrapper<>();
            wrapper.eq("room_id", roomId).eq("is_deleted", 0).orderByDesc("record_date");
            Page<PressureDifferenceRecord> pageResult = pressureDifferenceRecordService.page(page, wrapper);

            RoomInfo room = roomInfoService.getById(roomId);
            if (room != null) {
                pageResult.getRecords().forEach(r -> r.setRoomName(room.getRoomName()));
            }

            PagedResult<PressureDifferenceRecord> result = new PagedResult<>();
            result.setItems(pageResult.getRecords());
            result.setTotalCount(pageResult.getTotal());
            result.setPageIndex(pageIndex);
            result.setPageSize(pageSize);
            return success(result);
        } catch (Exception e) {
            return exception(e, "查询压差记录");
        }
    }

    /**
     * 查询压差记录列表（不分页）
     */
    @GetMapping("/list")
    public ApiResponse<List<PressureDifferenceRecord>> getList(@PathVariable Integer roomId) {
        try {
            List<PressureDifferenceRecord> list = pressureDifferenceRecordService.findByRoomId(roomId);
            return success(list);
        } catch (Exception e) {
            return exception(e, "查询压差记录");
        }
    }

    /**
     * 新增压差记录
     */
    @PostMapping
    public ApiResponse<PressureDifferenceRecord> create(@PathVariable Integer roomId, @RequestBody PressureDifferenceRecord record) {
        try {
            record.setRoomId(roomId);
            record.setCreatedTime(LocalDateTime.now());
            record.setIsDeleted(0);
            pressureDifferenceRecordService.save(record);
            return success(record, "新增成功");
        } catch (Exception e) {
            return exception(e, "新增压差记录");
        }
    }

    /**
     * 修改压差记录
     */
    @PutMapping("/{id}")
    public ApiResponse<PressureDifferenceRecord> update(@PathVariable Integer roomId, @PathVariable Long id, @RequestBody PressureDifferenceRecord record) {
        try {
            PressureDifferenceRecord existing = pressureDifferenceRecordService.getById(id);
            if (existing == null || !existing.getRoomId().equals(roomId)) {
                return error("记录不存在");
            }
            record.setId(id);
            pressureDifferenceRecordService.updateById(record);
            return success(record, "修改成功");
        } catch (Exception e) {
            return exception(e, "修改压差记录");
        }
    }

    /**
     * 删除压差记录（软删除）
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Integer roomId, @PathVariable Long id) {
        try {
            PressureDifferenceRecord existing = pressureDifferenceRecordService.getById(id);
            if (existing == null || !existing.getRoomId().equals(roomId)) {
                return error("记录不存在");
            }
            existing.setIsDeleted(1);
            pressureDifferenceRecordService.updateById(existing);
            return success(null, "删除成功");
        } catch (Exception e) {
            return exception(e, "删除压差记录");
        }
    }
}
