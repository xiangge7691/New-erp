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
 * <p>
 * 提供车间压差检测记录的CRUD操作，用于GMP合规管理中的洁净度梯度监控
 * </p>
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────────────────┬────────┬──────────────────────────────┐
 * │ #  │ 接口                                         │ 方法   │ 说明                         │
 * ├────┼──────────────────────────────────────────────┼────────┼──────────────────────────────┤
 * │ 1  │ /api/room/{roomId}/pressure                  │ GET    │ 分页查询压差记录列表          │
 * │ 2  │ /api/room/{roomId}/pressure/list             │ GET    │ 查询压差记录列表（不分页）     │
 * │ 3  │ /api/room/{roomId}/pressure                  │ POST   │ 新增压差记录                 │
 * │ 4  │ /api/room/{roomId}/pressure/{id}             │ PUT    │ 修改压差记录                 │
 * │ 5  │ /api/room/{roomId}/pressure/{id}             │ DELETE │ 删除压差记录（软删除）        │
 * └────┴──────────────────────────────────────────────┴────────┴──────────────────────────────┘
 */
@RestController
@RequestMapping("/api/room/{roomId}/pressure")
public class PressureDifferenceRecordController extends BaseController {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 压差记录服务
     */
    @Autowired
    private PressureDifferenceRecordService pressureDifferenceRecordService;

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
     * 分页查询压差记录列表
     * <p>
     * 查询指定房间的压差检测记录，支持分页，自动填充房间名称
     * </p>
     *
     * 示例请求：
     * GET /api/room/1/pressure?pageIndex=0&pageSize=10
     * GET /api/room/1/pressure
     *
     * @param roomId    房间ID（路径参数）
     * @param pageIndex 页码索引，从0开始（默认0）
     * @param pageSize  每页数量（默认10）
     * @return ApiResponse&lt;PagedResult&lt;PressureDifferenceRecord&gt;&gt; 分页结果
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
     * <p>
     * 查询指定房间的所有压差检测记录，按记录日期倒序排列
     * </p>
     *
     * 示例请求：
     * GET /api/room/1/pressure/list
     *
     * @param roomId 房间ID（路径参数）
     * @return ApiResponse&lt;List&lt;PressureDifferenceRecord&gt;&gt; 压差记录列表
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

    // endregion

    // region 新增接口
    // ===================================
    // 新增接口
    // ===================================

    /**
     * 新增压差记录
     *
     * 示例请求：
     * POST /api/room/1/pressure
     * Content-Type: application/json
     * {
     *   "recordDate": "2026-07-01",
     *   "inspectionArea": "配制间与缓冲间之间",
     *   "pressureValue": 15.5,
     *   "recorder": "赵六",
     *   "remark": "压差正常"
     * }
     *
     * @param roomId 房间ID（路径参数）
     * @param record 压差记录信息
     * @return ApiResponse&lt;PressureDifferenceRecord&gt; 新增的压差记录
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

    // endregion

    // region 修改与删除接口
    // ===================================
    // 修改与删除接口
    // ===================================

    /**
     * 修改压差记录
     *
     * 示例请求：
     * PUT /api/room/1/pressure/10
     * Content-Type: application/json
     * {
     *   "pressureValue": 16.0,
     *   "remark": "压差略有升高"
     * }
     *
     * @param roomId 房间ID（路径参数）
     * @param id     记录ID（路径参数）
     * @param record 更新的压差记录信息
     * @return ApiResponse&lt;PressureDifferenceRecord&gt; 修改后的压差记录
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
     *
     * 示例请求：
     * DELETE /api/room/1/pressure/10
     *
     * @param roomId 房间ID（路径参数）
     * @param id     记录ID（路径参数）
     * @return ApiResponse&lt;Void&gt; 删除结果
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

    // endregion
}
