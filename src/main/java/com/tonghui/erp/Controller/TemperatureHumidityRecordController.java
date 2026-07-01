package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.TemperatureHumidityRecord;
import com.tonghui.erp.Data.Entity.RoomInfo;
import com.tonghui.erp.Service.TemperatureHumidityRecordService;
import com.tonghui.erp.Service.RoomInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 温湿度记录控制器
 * 提供温湿度记录的CRUD操作
 */
@RestController
@RequestMapping("/api/room/{roomId}/temperature-humidity")
public class TemperatureHumidityRecordController extends BaseController {

    @Autowired
    private TemperatureHumidityRecordService temperatureHumidityRecordService;

    @Autowired
    private RoomInfoService roomInfoService;

    /**
     * 分页查询温湿度记录列表
     */
    @GetMapping
    public ApiResponse<PagedResult<TemperatureHumidityRecord>> getAll(
            @PathVariable Integer roomId,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            Page<TemperatureHumidityRecord> page = new Page<>(pageIndex + 1, pageSize);
            QueryWrapper<TemperatureHumidityRecord> wrapper = new QueryWrapper<>();
            wrapper.eq("room_id", roomId).eq("is_deleted", 0).orderByDesc("record_date");
            Page<TemperatureHumidityRecord> pageResult = temperatureHumidityRecordService.page(page, wrapper);

            RoomInfo room = roomInfoService.getById(roomId);
            if (room != null) {
                pageResult.getRecords().forEach(r -> r.setRoomName(room.getRoomName()));
            }

            PagedResult<TemperatureHumidityRecord> result = new PagedResult<>();
            result.setItems(pageResult.getRecords());
            result.setTotalCount(pageResult.getTotal());
            result.setPageIndex(pageIndex);
            result.setPageSize(pageSize);
            return success(result);
        } catch (Exception e) {
            return exception(e, "查询温湿度记录");
        }
    }

    /**
     * 查询温湿度记录列表（不分页）
     */
    @GetMapping("/list")
    public ApiResponse<List<TemperatureHumidityRecord>> getList(@PathVariable Integer roomId) {
        try {
            List<TemperatureHumidityRecord> list = temperatureHumidityRecordService.findByRoomId(roomId);
            return success(list);
        } catch (Exception e) {
            return exception(e, "查询温湿度记录");
        }
    }

    /**
     * 新增温湿度记录
     */
    @PostMapping
    public ApiResponse<TemperatureHumidityRecord> create(@PathVariable Integer roomId, @RequestBody TemperatureHumidityRecord record) {
        try {
            record.setRoomId(roomId);
            record.setCreatedAt(LocalDateTime.now());
            record.setIsDeleted(0);
            temperatureHumidityRecordService.save(record);
            return success(record, "新增成功");
        } catch (Exception e) {
            return exception(e, "新增温湿度记录");
        }
    }

    /**
     * 修改温湿度记录
     */
    @PutMapping("/{id}")
    public ApiResponse<TemperatureHumidityRecord> update(@PathVariable Integer roomId, @PathVariable Long id, @RequestBody TemperatureHumidityRecord record) {
        try {
            TemperatureHumidityRecord existing = temperatureHumidityRecordService.getById(id);
            if (existing == null || !existing.getRoomId().equals(roomId)) {
                return error("记录不存在");
            }
            record.setId(id);
            temperatureHumidityRecordService.updateById(record);
            return success(record, "修改成功");
        } catch (Exception e) {
            return exception(e, "修改温湿度记录");
        }
    }

    /**
     * 删除温湿度记录（软删除）
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Integer roomId, @PathVariable Long id) {
        try {
            TemperatureHumidityRecord existing = temperatureHumidityRecordService.getById(id);
            if (existing == null || !existing.getRoomId().equals(roomId)) {
                return error("记录不存在");
            }
            existing.setIsDeleted(1);
            temperatureHumidityRecordService.updateById(existing);
            return success(null, "删除成功");
        } catch (Exception e) {
            return exception(e, "删除温湿度记录");
        }
    }
}
