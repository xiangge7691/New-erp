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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 温湿度记录控制器
 * <p>
 * 提供车间温湿度检测记录的CRUD操作，用于GMP合规管理中的环境监控
 * </p>
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────────────┬────────┬──────────────────────────────┐
 * │ #  │ 接口                                     │ 方法   │ 说明                         │
 * ├────┼──────────────────────────────────────────┼────────┼──────────────────────────────┤
 * │ 1  │ /api/temperatureHumidityRecord           │ GET    │ 分页查询温湿度记录列表        │
 * │ 2  │ /api/temperatureHumidityRecord/list      │ GET    │ 查询温湿度记录列表（不分页）   │
 * │ 3  │ /api/temperatureHumidityRecord           │ POST   │ 新增温湿度记录               │
 * │ 4  │ /api/temperatureHumidityRecord/{id}      │ PUT    │ 修改温湿度记录               │
 * │ 5  │ /api/temperatureHumidityRecord/{id}      │ DELETE │ 删除温湿度记录（软删除）      │
 * └────┴──────────────────────────────────────────┴────────┴──────────────────────────────┘
 */
@RestController
@RequestMapping("/api/temperatureHumidityRecord")
public class TemperatureHumidityRecordController extends BaseController {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 温湿度记录服务
     */
    @Autowired
    private TemperatureHumidityRecordService temperatureHumidityRecordService;

    /**
     * 房间信息服务
     */
    @Autowired
    private RoomInfoService roomInfoService;

    // endregion

    // region 查询接口
    // ===================================
    // 查询接口
    // ===================================

    /**
     * 分页查询温湿度记录列表
     * <p>
     * 查询温湿度检测记录，支持分页和按房间筛选。roomId为可选参数：
     * 传入时按房间过滤，不传入时返回所有房间的记录
     * </p>
     *
     * 示例请求：
     * GET /api/temperatureHumidityRecord?pageIndex=0&pageSize=10
     * GET /api/temperatureHumidityRecord?roomId=1&pageIndex=0&pageSize=10
     *
     * @param roomId    房间ID（可选），不传则返回所有记录
     * @param pageIndex 页码索引，从0开始（默认0）
     * @param pageSize  每页数量（默认10）
     * @return ApiResponse&lt;PagedResult&lt;TemperatureHumidityRecord&gt;&gt; 分页结果
     */
    @GetMapping
    public ApiResponse<PagedResult<TemperatureHumidityRecord>> getAll(
            @RequestParam(required = false) Integer roomId,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            Page<TemperatureHumidityRecord> page = new Page<>(pageIndex + 1, pageSize);
            QueryWrapper<TemperatureHumidityRecord> wrapper = new QueryWrapper<>();
            wrapper.eq("is_deleted", 0);
            if (roomId != null) {
                wrapper.eq("room_id", roomId);
            }
            wrapper.orderByDesc("record_date");
            Page<TemperatureHumidityRecord> pageResult = temperatureHumidityRecordService.page(page, wrapper);

            // 批量填充房间名称
            Set<Integer> roomIds = pageResult.getRecords().stream()
                    .map(TemperatureHumidityRecord::getRoomId)
                    .collect(Collectors.toSet());
            if (!roomIds.isEmpty()) {
                Map<Integer, RoomInfo> roomMap = roomInfoService.listByIds(roomIds).stream()
                        .collect(Collectors.toMap(RoomInfo::getRoomId, r -> r));
                pageResult.getRecords().forEach(r -> {
                    RoomInfo room = roomMap.get(r.getRoomId());
                    if (room != null) r.setRoomName(room.getRoomName());
                });
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
     * <p>
     * 查询所有温湿度检测记录，按记录日期倒序排列。roomId为可选参数：
     * 传入时按房间过滤，不传入时返回所有房间的记录
     * </p>
     *
     * 示例请求：
     * GET /api/temperatureHumidityRecord/list
     * GET /api/temperatureHumidityRecord/list?roomId=1
     *
     * @param roomId 房间ID（可选），不传则返回所有记录
     * @return ApiResponse&lt;List&lt;TemperatureHumidityRecord&gt;&gt; 温湿度记录列表
     */
    @GetMapping("/list")
    public ApiResponse<List<TemperatureHumidityRecord>> getList(
            @RequestParam(required = false) Integer roomId) {
        try {
            QueryWrapper<TemperatureHumidityRecord> wrapper = new QueryWrapper<>();
            wrapper.eq("is_deleted", 0);
            if (roomId != null) {
                wrapper.eq("room_id", roomId);
            }
            wrapper.orderByDesc("record_date");
            List<TemperatureHumidityRecord> list = temperatureHumidityRecordService.list(wrapper);

            // 批量填充房间名称
            Set<Integer> roomIds = list.stream()
                    .map(TemperatureHumidityRecord::getRoomId)
                    .collect(Collectors.toSet());
            if (!roomIds.isEmpty()) {
                Map<Integer, RoomInfo> roomMap = roomInfoService.listByIds(roomIds).stream()
                        .collect(Collectors.toMap(RoomInfo::getRoomId, r -> r));
                list.forEach(r -> {
                    RoomInfo room = roomMap.get(r.getRoomId());
                    if (room != null) r.setRoomName(room.getRoomName());
                });
            }

            return success(list);
        } catch (Exception e) {
            return exception(e, "查询温湿度记录");
        }
    }

    // endregion

    // region 新增接口
    // ===================================
    // 新增接口
    // ===================================

    /**
     * 新增温湿度记录
     *
     * 示例请求：
     * POST /api/temperatureHumidityRecord?roomId=1
     * Content-Type: application/json
     * {
     *   "recordDate": "2026-07-01",
     *   "inspectionArea": "配制间",
     *   "temperature": 22.5,
     *   "humidity": 45.0,
     *   "recorder": "李四",
     *   "remark": "环境正常"
     * }
     *
     * @param roomId 房间ID（必填）
     * @param record 温湿度记录信息
     * @return ApiResponse&lt;TemperatureHumidityRecord&gt; 新增的温湿度记录
     */
    @PostMapping
    public ApiResponse<TemperatureHumidityRecord> create(
            @RequestParam Integer roomId,
            @RequestBody TemperatureHumidityRecord record) {
        try {
            record.setRoomId(roomId);
            record.setCreatedTime(LocalDateTime.now());
            record.setIsDeleted(0);
            temperatureHumidityRecordService.save(record);
            return success(record, "新增成功");
        } catch (Exception e) {
            return exception(e, "新增温湿度记录");
        }
    }

    // endregion

    // region 修改与删除接口
    // ===================================
    // 修改与删除接口
    // ===================================

    /**
     * 修改温湿度记录
     *
     * 示例请求：
     * PUT /api/temperatureHumidityRecord/10?roomId=1
     * Content-Type: application/json
     * {
     *   "temperature": 23.0,
     *   "humidity": 46.5,
     *   "remark": "温度略有升高"
     * }
     *
     * @param roomId 房间ID（必填）
     * @param id     记录ID（路径参数）
     * @param record 更新的温湿度记录信息
     * @return ApiResponse&lt;TemperatureHumidityRecord&gt; 修改后的温湿度记录
     */
    @PutMapping("/{id}")
    public ApiResponse<TemperatureHumidityRecord> update(
            @RequestParam Integer roomId,
            @PathVariable Long id,
            @RequestBody TemperatureHumidityRecord record) {
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
     *
     * 示例请求：
     * DELETE /api/temperatureHumidityRecord/10?roomId=1
     *
     * @param roomId 房间ID（必填）
     * @param id     记录ID（路径参数）
     * @return ApiResponse&lt;Void&gt; 删除结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @RequestParam Integer roomId,
            @PathVariable Long id) {
        try {
            TemperatureHumidityRecord existing = temperatureHumidityRecordService.getById(id);
            if (existing == null || !existing.getRoomId().equals(roomId)) {
                return error("记录不存在");
            }
            temperatureHumidityRecordService.removeById(id);
            return success(null, "删除成功");
        } catch (Exception e) {
            return exception(e, "删除温湿度记录");
        }
    }

    // endregion
}
