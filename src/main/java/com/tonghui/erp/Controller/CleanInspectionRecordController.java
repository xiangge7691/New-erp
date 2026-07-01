package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.CleanInspectionRecord;
import com.tonghui.erp.Data.Entity.RoomInfo;
import com.tonghui.erp.Service.CleanInspectionRecordService;
import com.tonghui.erp.Service.RoomInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/room/{roomId}/inspection")
public class CleanInspectionRecordController extends BaseController {

    @Autowired
    private CleanInspectionRecordService cleanInspectionRecordService;

    @Autowired
    private RoomInfoService roomInfoService;

    @GetMapping
    public ApiResponse<PagedResult<CleanInspectionRecord>> getAll(
            @PathVariable Integer roomId,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            Page<CleanInspectionRecord> page = new Page<>(pageIndex + 1, pageSize);
            QueryWrapper<CleanInspectionRecord> wrapper = new QueryWrapper<>();
            wrapper.eq("room_id", roomId).eq("is_deleted", 0).orderByDesc("inspection_date");
            Page<CleanInspectionRecord> pageResult = cleanInspectionRecordService.page(page, wrapper);

            RoomInfo room = roomInfoService.getById(roomId);
            if (room != null) {
                pageResult.getRecords().forEach(r -> r.setRoomName(room.getRoomName()));
            }

            PagedResult<CleanInspectionRecord> result = new PagedResult<>();
            result.setItems(pageResult.getRecords());
            result.setTotalCount(pageResult.getTotal());
            result.setPageIndex(pageIndex);
            result.setPageSize(pageSize);
            return success(result);
        } catch (Exception e) {
            return exception(e, "查询洁净检测记录");
        }
    }

    @GetMapping("/list")
    public ApiResponse<List<CleanInspectionRecord>> getList(@PathVariable Integer roomId) {
        try {
            List<CleanInspectionRecord> list = cleanInspectionRecordService.findByRoomId(roomId);
            return success(list);
        } catch (Exception e) {
            return exception(e, "查询洁净检测记录");
        }
    }

    @PostMapping
    public ApiResponse<CleanInspectionRecord> create(@PathVariable Integer roomId, @RequestBody CleanInspectionRecord record) {
        try {
            record.setRoomId(roomId);
            record.setCreatedAt(LocalDateTime.now());
            record.setIsDeleted(0);
            cleanInspectionRecordService.save(record);
            return success(record, "新增成功");
        } catch (Exception e) {
            return exception(e, "新增洁净检测记录");
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<CleanInspectionRecord> update(@PathVariable Integer roomId, @PathVariable Long id, @RequestBody CleanInspectionRecord record) {
        try {
            CleanInspectionRecord existing = cleanInspectionRecordService.getById(id);
            if (existing == null || !existing.getRoomId().equals(roomId)) {
                return error("记录不存在");
            }
            record.setId(id);
            record.setUpdatedAt(LocalDateTime.now());
            cleanInspectionRecordService.updateById(record);
            return success(record, "修改成功");
        } catch (Exception e) {
            return exception(e, "修改洁净检测记录");
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Integer roomId, @PathVariable Long id) {
        try {
            CleanInspectionRecord existing = cleanInspectionRecordService.getById(id);
            if (existing == null || !existing.getRoomId().equals(roomId)) {
                return error("记录不存在");
            }
            existing.setIsDeleted(1);
            cleanInspectionRecordService.updateById(existing);
            return success(null, "删除成功");
        } catch (Exception e) {
            return exception(e, "删除洁净检测记录");
        }
    }
}
