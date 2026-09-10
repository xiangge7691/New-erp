package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.DisinfectionRecord;
import com.tonghui.erp.Data.Entity.RoomInfo;
import com.tonghui.erp.Service.DisinfectionRecordService;
import com.tonghui.erp.Service.RoomInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 消毒管理记录控制器
 * <p>
 * 提供车间消毒操作记录的CRUD操作及到期提醒查询，用于GMP合规管理中的消毒计划跟踪
 * </p>
 *
 * 接口清单：
 * ┌────┬────────────────────────────────────┬────────┬──────────────────────────────┐
 * │ #  │ 接口                               │ 方法   │ 说明                         │
 * ├────┼────────────────────────────────────┼────────┼──────────────────────────────┤
 * │ 1  │ /api/disinfectionRecord            │ GET    │ 分页查询消毒记录列表          │
 * │ 2  │ /api/disinfectionRecord/list       │ GET    │ 查询消毒记录列表（不分页）     │
 * │ 3  │ /api/disinfectionRecord            │ POST   │ 新增消毒记录（自动计算下次日期）│
 * │ 4  │ /api/disinfectionRecord/{id}       │ PUT    │ 修改消毒记录                 │
 * │ 5  │ /api/disinfectionRecord/{id}       │ DELETE │ 删除消毒记录（软删除）        │
 * │ 6  │ /api/disinfectionRecord/reminder   │ GET    │ 查询即将到期的消毒提醒        │
 * └────┴────────────────────────────────────┴────────┴──────────────────────────────┘
 */
@RestController
@RequestMapping("/api/disinfectionRecord")
public class DisinfectionRecordController extends BaseController {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 消毒记录服务
     */
    @Autowired
    private DisinfectionRecordService disinfectionRecordService;

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
     * 分页查询消毒记录列表
     *
     * 示例请求：
     * GET /api/disinfectionRecord?pageIndex=0&pageSize=10
     * GET /api/disinfectionRecord?pageIndex=0&pageSize=10&roomName=洁净&roomCode=QJ-001&startDate=2026-01-01&endDate=2026-12-31
     *
     * @param roomId    房间ID（可选）
     * @param roomName  房间名称（可选，模糊匹配）
     * @param roomCode  房间编码（可选，精确匹配）
     * @param startDate 开始日期（可选，格式：yyyy-MM-dd）
     * @param endDate   结束日期（可选，格式：yyyy-MM-dd）
     * @param pageIndex 页码索引，从0开始（默认0）
     * @param pageSize  每页数量（默认10）
     * @return ApiResponse&lt;PagedResult&lt;DisinfectionRecord&gt;&gt; 分页结果
     */
    @GetMapping
    public ApiResponse<PagedResult<DisinfectionRecord>> getAll(
            @RequestParam(required = false) Integer roomId,
            @RequestParam(required = false) String roomName,
            @RequestParam(required = false) String roomCode,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            // 如果有房间名称或房间编码筛选，先查出符合条件的房间ID
            if (roomName != null || roomCode != null) {
                QueryWrapper<RoomInfo> roomWrapper = new QueryWrapper<>();
                roomWrapper.eq("is_deleted", 0);
                if (roomName != null) {
                    roomWrapper.like("room_name", roomName);
                }
                if (roomCode != null) {
                    roomWrapper.eq("room_code", roomCode);
                }
                List<RoomInfo> rooms = roomInfoService.list(roomWrapper);
                if (rooms.isEmpty()) {
                    return success(new PagedResult<>());
                }
                List<Integer> roomIds = rooms.stream().map(RoomInfo::getRoomId).collect(Collectors.toList());
                if (roomId != null) {
                    if (!roomIds.contains(roomId)) {
                        return success(new PagedResult<>());
                    }
                    roomIds = List.of(roomId);
                }
                QueryWrapper<DisinfectionRecord> wrapper = new QueryWrapper<>();
                wrapper.eq("is_deleted", 0);
                wrapper.in("room_id", roomIds);
                if (startDate != null) {
                    wrapper.ge("disinfection_date", startDate);
                }
                if (endDate != null) {
                    wrapper.le("disinfection_date", endDate);
                }
                wrapper.orderByDesc("disinfection_date");
                Page<DisinfectionRecord> page = new Page<>(pageIndex + 1, pageSize);
                disinfectionRecordService.page(page, wrapper);
                fillRoomInfo(page.getRecords());
                PagedResult<DisinfectionRecord> result = new PagedResult<>();
                result.setItems(page.getRecords());
                result.setTotalCount(page.getTotal());
                result.setPageIndex(pageIndex);
                result.setPageSize(pageSize);
                return success(result);
            }

            Page<DisinfectionRecord> page = new Page<>(pageIndex + 1, pageSize);
            QueryWrapper<DisinfectionRecord> wrapper = new QueryWrapper<>();
            wrapper.eq("is_deleted", 0);
            if (roomId != null) {
                wrapper.eq("room_id", roomId);
            }
            if (startDate != null) {
                wrapper.ge("disinfection_date", startDate);
            }
            if (endDate != null) {
                wrapper.le("disinfection_date", endDate);
            }
            wrapper.orderByDesc("disinfection_date");
            Page<DisinfectionRecord> pageResult = disinfectionRecordService.page(page, wrapper);
            fillRoomInfo(pageResult.getRecords());

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

    private void fillRoomInfo(List<DisinfectionRecord> list) {
        Set<Integer> roomIds = list.stream()
                .map(DisinfectionRecord::getRoomId)
                .collect(Collectors.toSet());
        if (!roomIds.isEmpty()) {
            Map<Integer, RoomInfo> roomMap = roomInfoService.listByIds(roomIds).stream()
                    .collect(Collectors.toMap(RoomInfo::getRoomId, r -> r));
            list.forEach(r -> {
                RoomInfo room = roomMap.get(r.getRoomId());
                if (room != null) {
                    r.setRoomName(room.getRoomName());
                    r.setRoomCode(room.getRoomCode());
                }
            });
        }
    }

    /**
     * 查询消毒记录列表（不分页）
     *
     * @param roomId 房间ID（可选）
     * @return ApiResponse&lt;List&lt;DisinfectionRecord&gt;&gt; 消毒记录列表
     */
// @GetMapping("/list")
    public ApiResponse<List<DisinfectionRecord>> getList(
            @RequestParam(required = false) Integer roomId) {
        try {
            QueryWrapper<DisinfectionRecord> wrapper = new QueryWrapper<>();
            wrapper.eq("is_deleted", 0);
            if (roomId != null) {
                wrapper.eq("room_id", roomId);
            }
            wrapper.orderByDesc("disinfection_date");
            List<DisinfectionRecord> list = disinfectionRecordService.list(wrapper);
            fillRoomInfo(list);
            return success(list);
        } catch (Exception e) {
            return exception(e, "查询消毒记录");
        }
    }

    /**
     * 查询即将到期的消毒提醒
     *
     * @param days 提前天数（默认30天）
     * @return ApiResponse&lt;List&lt;DisinfectionRecord&gt;&gt; 即将到期的消毒记录列表
     */
// @GetMapping("/reminder")
    public ApiResponse<List<DisinfectionRecord>> reminder(
            @RequestParam(defaultValue = "30") int days) {
        try {
            List<DisinfectionRecord> list = disinfectionRecordService.findUpcomingDisinfection(days);
            fillRoomInfo(list);
            return success(list);
        } catch (Exception e) {
            return exception(e, "查询消毒提醒");
        }
    }

    // endregion

    // region 新增接口
    // ===================================
    // 新增接口
    // ===================================

    /**
     * 新增消毒记录
     * <p>
     * 如果提供了消毒日期和消毒周期但未提供下次消毒日期，系统将自动计算下次消毒日期
     * </p>
     *
     * 示例请求：
     * POST /api/disinfectionRecord?roomId=1
     * Content-Type: application/json
     * {
     *   "disinfectionDate": "2026-07-01",
     *   "disinfectionMethod": "紫外线消毒",
     *   "disinfectionPerson": "孙七",
     *   "disinfectionCycle": 14,
     *   "remark": "定期消毒"
     * }
     *
     * @param roomId 房间ID（必填）
     * @param record 消毒记录信息
     * @return ApiResponse&lt;DisinfectionRecord&gt; 新增的消毒记录
     */
    @PostMapping
    public ApiResponse<DisinfectionRecord> create(
            @RequestParam Integer roomId,
            @RequestBody DisinfectionRecord record) {
        try {
            record.setRoomId(roomId);
            record.setCreatedTime(LocalDateTime.now());
            record.setIsDeleted(0);

            // 自动计算下次消毒日期
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

    // endregion

    // region 修改与删除接口
    // ===================================
    // 修改与删除接口
    // ===================================

    /**
     * 修改消毒记录
     *
     * 示例请求：
     * PUT /api/disinfectionRecord/10?roomId=1
     * Content-Type: application/json
     * {
     *   "disinfectionMethod": "臭氧消毒",
     *   "remark": "已更新消毒方式"
     * }
     *
     * @param roomId 房间ID（可选，当请求体未携带roomId时作为兜底，用于修改房间）
     * @param id     记录ID（路径参数）
     * @param record 更新的消毒记录信息
     * @return ApiResponse&lt;DisinfectionRecord&gt; 修改后的消毒记录
     */
    @PutMapping("/{id}")
    public ApiResponse<DisinfectionRecord> update(
            @RequestParam(required = false) Integer roomId,
            @PathVariable Long id,
            @RequestBody DisinfectionRecord record) {
        try {
            DisinfectionRecord existing = disinfectionRecordService.getById(id);
            if (existing == null) {
                return error("记录不存在");
            }
            // 房间ID优先取请求体，其次取URL参数：兼容两种传参方式，并允许修改房间
            Integer targetRoomId = record.getRoomId() != null ? record.getRoomId() : roomId;
            if (targetRoomId != null) {
                record.setRoomId(targetRoomId);
            }
            record.setId(id);
            record.setUpdatedTime(LocalDateTime.now());
            disinfectionRecordService.updateById(record);
            return success(record, "修改成功");
        } catch (Exception e) {
            return exception(e, "修改消毒记录");
        }
    }

    /**
     * 删除消毒记录（软删除）
     *
     * 示例请求：
     * DELETE /api/disinfectionRecord/10?roomId=1
     *
     * @param roomId 房间ID（可选，兼容旧传参）
     * @param id     记录ID（路径参数）
     * @return ApiResponse&lt;Void&gt; 删除结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @RequestParam(required = false) Integer roomId,
            @PathVariable Long id) {
        try {
            DisinfectionRecord existing = disinfectionRecordService.getById(id);
            if (existing == null) {
                return error("记录不存在");
            }
            disinfectionRecordService.removeById(id);
            return success(null, "删除成功");
        } catch (Exception e) {
            return exception(e, "删除消毒记录");
        }
    }

    // endregion
}
