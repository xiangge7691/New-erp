package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.CleaningRecord;
import com.tonghui.erp.Data.Entity.RoomInfo;
import com.tonghui.erp.Service.CleaningRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 清洁记录控制器
 * <p>
 * 提供车间环境清洁记录的CRUD操作及到期提醒查询，用于GMP合规管理中的清洁计划跟踪
 * </p>
 */
@RestController
@RequestMapping("/api/cleaningRecord")
public class CleaningRecordController extends BaseRoomRecordController<CleaningRecord> {

    /** 清洁记录服务 */
    @Autowired
    private CleaningRecordService cleaningRecordService;

    @GetMapping
    public ApiResponse<PagedResult<CleaningRecord>> getAll(
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) String roomName,
            @RequestParam(required = false) String roomCode,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int pageIndex,
            @RequestParam(defaultValue = "20") int pageSize) {
        try {
            if (roomName != null || roomCode != null) {
                List<Integer> roomIds = findRoomIds(roomName, roomCode);
                if (roomIds.isEmpty()) {
                    return success(PagedResult.empty());
                }
                if (roomId != null) {
                    if (!roomIds.contains(roomId.intValue())) {
                        return success(PagedResult.empty());
                    }
                    roomIds = List.of(roomId.intValue());
                }
                QueryWrapper<CleaningRecord> wrapper = new QueryWrapper<>();
                wrapper.eq("is_deleted", 0);
                wrapper.in("room_id", roomIds);
                if (startDate != null) wrapper.ge("cleaning_date", startDate);
                if (endDate != null) wrapper.le("cleaning_date", endDate);
                wrapper.orderByDesc("cleaning_date");
                Page<CleaningRecord> page = new Page<>(pageIndex, pageSize);
                cleaningRecordService.page(page, wrapper);
                fillRoomInfo(page.getRecords(), CleaningRecord::getRoomId,
                        CleaningRecord::setRoomName, CleaningRecord::setRoomCode);
                return success(toPagedResult(page));
            }

            QueryWrapper<CleaningRecord> wrapper = new QueryWrapper<>();
            wrapper.eq("is_deleted", 0);
            if (roomId != null) wrapper.eq("room_id", roomId);
            if (startDate != null) wrapper.ge("cleaning_date", startDate);
            if (endDate != null) wrapper.le("cleaning_date", endDate);
            wrapper.orderByDesc("cleaning_date");
            Page<CleaningRecord> page = new Page<>(pageIndex, pageSize);
            cleaningRecordService.page(page, wrapper);
            fillRoomInfo(page.getRecords(), CleaningRecord::getRoomId,
                    CleaningRecord::setRoomName, CleaningRecord::setRoomCode);
            return success(toPagedResult(page));
        } catch (Exception ex) {
            return exception(ex, "查询清洁记录");
        }
    }

    private <T> PagedResult<T> toPagedResult(Page<T> page) {
        PagedResult<T> result = new PagedResult<>();
        result.setItems(page.getRecords());
        result.setTotalCount(page.getTotal());
        result.setPageIndex((int) page.getCurrent());
        result.setPageSize((int) page.getSize());
        return result;
    }

    @GetMapping("/reminder")
    public ApiResponse<List<CleaningRecord>> reminder(
            @RequestParam(defaultValue = "30") int days) {
        List<CleaningRecord> list = cleaningRecordService.findUpcomingCleaning(days);
        return success(list);
    }

    @PostMapping
    public ApiResponse<CleaningRecord> create(@RequestBody CleaningRecord record) {
        if (record.getCleaningDate() != null && record.getCleaningCycle() != null
                && record.getNextCleaningDate() == null) {
            record.setNextCleaningDate(record.getCleaningDate().plusDays(record.getCleaningCycle()));
        }
        cleaningRecordService.save(record);
        return success(record, "新增成功");
    }

    @PutMapping("/{id}")
    public ApiResponse<CleaningRecord> update(@PathVariable Long id, @RequestBody CleaningRecord record) {
        CleaningRecord existing = cleaningRecordService.getById(id);
        if (existing == null) return error("记录不存在");
        record.setId(id);
        cleaningRecordService.updateById(record);
        return success(record, "修改成功");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        CleaningRecord existing = cleaningRecordService.getById(id);
        if (existing == null) return error("记录不存在");
        cleaningRecordService.removeById(id);
        return success(null, "删除成功");
    }
}
