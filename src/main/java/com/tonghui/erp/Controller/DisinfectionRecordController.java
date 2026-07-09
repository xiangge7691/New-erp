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

/**
 * 消毒管理记录控制器
 * <p>
 * 提供车间消毒操作记录的CRUD操作及到期提醒查询，用于GMP合规管理中的消毒计划跟踪
 * </p>
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────────────────┬────────┬──────────────────────────────┐
 * │ #  │ 接口                                         │ 方法   │ 说明                         │
 * ├────┼──────────────────────────────────────────────┼────────┼──────────────────────────────┤
 * │ 1  │ /api/room/{roomId}/disinfection              │ GET    │ 分页查询消毒记录列表          │
 * │ 2  │ /api/room/{roomId}/disinfection/list         │ GET    │ 查询消毒记录列表（不分页）     │
 * │ 3  │ /api/room/{roomId}/disinfection              │ POST   │ 新增消毒记录（自动计算下次日期）│
 * │ 4  │ /api/room/{roomId}/disinfection/{id}         │ PUT    │ 修改消毒记录                 │
 * │ 5  │ /api/room/{roomId}/disinfection/{id}         │ DELETE │ 删除消毒记录（软删除）        │
 * │ 6  │ /api/room/{roomId}/disinfection/reminder     │ GET    │ 查询即将到期的消毒提醒        │
 * └────┴──────────────────────────────────────────────┴────────┴──────────────────────────────┘
 */
@RestController
@RequestMapping("/api/room/{roomId}/disinfection")
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
     * <p>
     * 查询指定房间的消毒操作记录，支持分页，自动填充房间名称
     * </p>
     *
     * 示例请求：
     * GET /api/room/1/disinfection?pageIndex=0&pageSize=10
     * GET /api/room/1/disinfection
     *
     * @param roomId    房间ID（路径参数）
     * @param pageIndex 页码索引，从0开始（默认0）
     * @param pageSize  每页数量（默认10）
     * @return ApiResponse&lt;PagedResult&lt;DisinfectionRecord&gt;&gt; 分页结果
     */
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

    /**
     * 查询消毒记录列表（不分页）
     * <p>
     * 查询指定房间的所有消毒操作记录，按消毒日期倒序排列
     * </p>
     *
     * 示例请求：
     * GET /api/room/1/disinfection/list
     *
     * @param roomId 房间ID（路径参数）
     * @return ApiResponse&lt;List&lt;DisinfectionRecord&gt;&gt; 消毒记录列表
     */
    @GetMapping("/list")
    public ApiResponse<List<DisinfectionRecord>> getList(@PathVariable Integer roomId) {
        try {
            List<DisinfectionRecord> list = disinfectionRecordService.findByRoomId(roomId);
            return success(list);
        } catch (Exception e) {
            return exception(e, "查询消毒记录");
        }
    }

    /**
     * 查询即将到期的消毒提醒
     * <p>
     * 查询在未来指定天数内需要进行消毒的记录，用于到期提醒
     * </p>
     *
     * 示例请求：
     * GET /api/room/1/disinfection/reminder?days=30
     * GET /api/room/1/disinfection/reminder
     *
     * @param roomId 房间ID（路径参数）
     * @param days   提前天数（默认30天），查询从今天起至指定天数内的待消毒记录
     * @return ApiResponse&lt;List&lt;DisinfectionRecord&gt;&gt; 即将到期的消毒记录列表
     */
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
     * POST /api/room/1/disinfection
     * Content-Type: application/json
     * {
     *   "disinfectionDate": "2026-07-01",
     *   "disinfectionMethod": "紫外线消毒",
     *   "disinfectionPerson": "孙七",
     *   "disinfectionCycle": 14,
     *   "remark": "定期消毒"
     * }
     *
     * @param roomId 房间ID（路径参数）
     * @param record 消毒记录信息
     * @return ApiResponse&lt;DisinfectionRecord&gt; 新增的消毒记录
     */
    @PostMapping
    public ApiResponse<DisinfectionRecord> create(@PathVariable Integer roomId, @RequestBody DisinfectionRecord record) {
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
     * PUT /api/room/1/disinfection/10
     * Content-Type: application/json
     * {
     *   "disinfectionMethod": "臭氧消毒",
     *   "remark": "已更新消毒方式"
     * }
     *
     * @param roomId 房间ID（路径参数）
     * @param id     记录ID（路径参数）
     * @param record 更新的消毒记录信息
     * @return ApiResponse&lt;DisinfectionRecord&gt; 修改后的消毒记录
     */
    @PutMapping("/{id}")
    public ApiResponse<DisinfectionRecord> update(@PathVariable Integer roomId, @PathVariable Long id, @RequestBody DisinfectionRecord record) {
        try {
            DisinfectionRecord existing = disinfectionRecordService.getById(id);
            if (existing == null || !existing.getRoomId().equals(roomId)) {
                return error("记录不存在");
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
     * DELETE /api/room/1/disinfection/10
     *
     * @param roomId 房间ID（路径参数）
     * @param id     记录ID（路径参数）
     * @return ApiResponse&lt;Void&gt; 删除结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Integer roomId, @PathVariable Long id) {
        try {
            DisinfectionRecord existing = disinfectionRecordService.getById(id);
            if (existing == null || !existing.getRoomId().equals(roomId)) {
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
