package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.utils.EntityUtils;
import com.tonghui.erp.Data.Entity.DisinfectionRecord;
import com.tonghui.erp.Data.Entity.RoomInfo;
import com.tonghui.erp.Service.DisinfectionRecordService;
import com.tonghui.erp.Service.RoomInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/room/{roomId}/disinfection")
public class DisinfectionRecordController extends BaseController {

    @Autowired
    private DisinfectionRecordService disinfectionRecordService;

    @Autowired
    private RoomInfoService roomInfoService;

    @GetMapping
    public ApiResponse<PagedResult<DisinfectionRecord>> getAll(
            @PathVariable Integer roomId,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            Page<DisinfectionRecord> page = new Page<>(pageIndex + 1, pageSize);
            QueryWrapper<DisinfectionRecord> wrapper = new QueryWrapper<>();
            wrapper.eq("room_id", roomId).eq("is_deleted", 0).orderByDesc("disinfection_date");
            Page<DisinfectionRecord> pageResult = disinfectionRecordService.page(page, wrapper);

            RoomInfo room = roomInfoService.getById(roomId);
            if (room != null) {
                pageResult.getRecords().forEach(r -> r.setRoomName(room.getRoomName()));
            }

            PagedResult<DisinfectionRecord> result = new PagedResult<>();
            result.setItems(pageResult.getRecords());
            result.setTotalCount(pageResult.getTotal());
            result.setPageIndex(pageIndex);
            result.setPageSize(pageSize);
            return success(result);
        } catch (Exception e) {
            return exception(e, "查询消毒记录");
        }
    }

    @GetMapping("/list")
    public ApiResponse<List<DisinfectionRecord>> getList(@PathVariable Integer roomId) {
        try {
            List<DisinfectionRecord> list = disinfectionRecordService.findByRoomId(roomId);
            return success(list);
        } catch (Exception e) {
            return exception(e, "查询消毒记录");
        }
    }

    @PostMapping
    public ApiResponse<DisinfectionRecord> create(@PathVariable Integer roomId, @RequestBody DisinfectionRecord record) {
        try {
            record.setRoomId(roomId);
            record.setCreatedAt(LocalDateTime.now());
            record.setIsDeleted(0);

            if (record.getDisinfectionDate() != null && record.getDisinfectionCycle() != null
                    && record.getNextDisinfectionDate() == null) {
                record.setNextDisinfectionDate(record.getDisinfectionDate().plusDays(record.getDisinfectionCycle()));
            }

            disinfectionRecordService.save(record);
            return success(record, "新增成功");
        } catch (Exception e) {
            return exception(e, "新增消毒记录");
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<DisinfectionRecord> update(@PathVariable Integer roomId, @PathVariable Long id, @RequestBody DisinfectionRecord record) {
        try {
            DisinfectionRecord existing = disinfectionRecordService.getById(id);
            if (existing == null || !existing.getRoomId().equals(roomId)) {
                return error("记录不存在");
            }
            record.setId(id);
            record.setUpdatedAt(LocalDateTime.now());
            disinfectionRecordService.updateById(record);
            return success(record, "修改成功");
        } catch (Exception e) {
            return exception(e, "修改消毒记录");
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Integer roomId, @PathVariable Long id) {
        try {
            DisinfectionRecord existing = disinfectionRecordService.getById(id);
            if (existing == null || !existing.getRoomId().equals(roomId)) {
                return error("记录不存在");
            }
            existing.setIsDeleted(1);
            disinfectionRecordService.updateById(existing);
            return success(null, "删除成功");
        } catch (Exception e) {
            return exception(e, "删除消毒记录");
        }
    }

    @GetMapping("/reminder")
    public ApiResponse<List<DisinfectionRecord>> reminder(
            @PathVariable Integer roomId,
            @RequestParam(defaultValue = "30") int days) {
        try {
            List<DisinfectionRecord> list = disinfectionRecordService.findUpcomingDisinfection(days);
            return success(list);
        } catch (Exception e) {
            return exception(e, "查询消毒提醒");
        }
    }
}
