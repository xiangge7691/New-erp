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

/**
 * 洁净检测记录控制器
 * <p>
 * 提供车间洁净度检测记录的CRUD操作，用于GMP合规管理中的洁净度跟踪
 * </p>
 *
 * 接口清单：
 * ┌────┬────────────────────────────────────────┬────────┬──────────────────────────────┐
 * │ #  │ 接口                                   │ 方法   │ 说明                         │
 * ├────┼────────────────────────────────────────┼────────┼──────────────────────────────┤
 * │ 1  │ /api/room/{roomId}/inspection          │ GET    │ 分页查询洁净检测记录列表      │
 * │ 2  │ /api/room/{roomId}/inspection/list     │ GET    │ 查询洁净检测记录列表（不分页） │
 * │ 3  │ /api/room/{roomId}/inspection          │ POST   │ 新增洁净检测记录             │
 * │ 4  │ /api/room/{roomId}/inspection/{id}     │ PUT    │ 修改洁净检测记录             │
 * │ 5  │ /api/room/{roomId}/inspection/{id}     │ DELETE │ 删除洁净检测记录（软删除）    │
 * └────┴────────────────────────────────────────┴────────┴──────────────────────────────┘
 */
@RestController
@RequestMapping("/api/room/{roomId}/inspection")
public class CleanInspectionRecordController extends BaseController {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 洁净检测记录服务
     */
    @Autowired
    private CleanInspectionRecordService cleanInspectionRecordService;

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
     * 分页查询洁净检测记录列表
     * <p>
     * 查询指定房间的洁净度检测记录，支持分页，自动填充房间名称
     * </p>
     *
     * 示例请求：
     * GET /api/room/1/inspection?pageIndex=0&pageSize=10
     * GET /api/room/1/inspection
     *
     * @param roomId    房间ID（路径参数）
     * @param pageIndex 页码索引，从0开始（默认0）
     * @param pageSize  每页数量（默认10）
     * @return ApiResponse&lt;PagedResult&lt;CleanInspectionRecord&gt;&gt; 分页结果
     */
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

    /**
     * 查询洁净检测记录列表（不分页）
     * <p>
     * 查询指定房间的所有洁净度检测记录，按检测日期倒序排列
     * </p>
     *
     * 示例请求：
     * GET /api/room/1/inspection/list
     *
     * @param roomId 房间ID（路径参数）
     * @return ApiResponse&lt;List&lt;CleanInspectionRecord&gt;&gt; 洁净检测记录列表
     */
    @GetMapping("/list")
    public ApiResponse<List<CleanInspectionRecord>> getList(@PathVariable Integer roomId) {
        try {
            List<CleanInspectionRecord> list = cleanInspectionRecordService.findByRoomId(roomId);
            return success(list);
        } catch (Exception e) {
            return exception(e, "查询洁净检测记录");
        }
    }

    // endregion

    // region 新增接口
    // ===================================
    // 新增接口
    // ===================================

    /**
     * 新增洁净检测记录
     *
     * 示例请求：
     * POST /api/room/1/inspection
     * Content-Type: application/json
     * {
     *   "inspectionDate": "2026-07-01",
     *   "inspectionArea": "配制间",
     *   "inspectionItem": "悬浮粒子",
     *   "inspectionResult": "合格",
     *   "inspector": "王五",
     *   "nextInspectionDate": "2026-08-01",
     *   "remark": "检测正常"
     * }
     *
     * @param roomId 房间ID（路径参数）
     * @param record 洁净检测记录信息
     * @return ApiResponse&lt;CleanInspectionRecord&gt; 新增的检测记录
     */
    @PostMapping
    public ApiResponse<CleanInspectionRecord> create(@PathVariable Integer roomId, @RequestBody CleanInspectionRecord record) {
        try {
            record.setRoomId(roomId);
            record.setCreatedTime(LocalDateTime.now());
            record.setIsDeleted(0);
            cleanInspectionRecordService.save(record);
            return success(record, "新增成功");
        } catch (Exception e) {
            return exception(e, "新增洁净检测记录");
        }
    }

    // endregion

    // region 修改与删除接口
    // ===================================
    // 修改与删除接口
    // ===================================

    /**
     * 修改洁净检测记录
     *
     * 示例请求：
     * PUT /api/room/1/inspection/10
     * Content-Type: application/json
     * {
     *   "inspectionResult": "合格（已更新）",
     *   "remark": "复检通过"
     * }
     *
     * @param roomId 房间ID（路径参数）
     * @param id     记录ID（路径参数）
     * @param record 更新的检测记录信息
     * @return ApiResponse&lt;CleanInspectionRecord&gt; 修改后的检测记录
     */
    @PutMapping("/{id}")
    public ApiResponse<CleanInspectionRecord> update(@PathVariable Integer roomId, @PathVariable Long id, @RequestBody CleanInspectionRecord record) {
        try {
            CleanInspectionRecord existing = cleanInspectionRecordService.getById(id);
            if (existing == null || !existing.getRoomId().equals(roomId)) {
                return error("记录不存在");
            }
            record.setId(id);
            record.setUpdatedTime(LocalDateTime.now());
            cleanInspectionRecordService.updateById(record);
            return success(record, "修改成功");
        } catch (Exception e) {
            return exception(e, "修改洁净检测记录");
        }
    }

    /**
     * 删除洁净检测记录（软删除）
     *
     * 示例请求：
     * DELETE /api/room/1/inspection/10
     *
     * @param roomId 房间ID（路径参数）
     * @param id     记录ID（路径参数）
     * @return ApiResponse&lt;Void&gt; 删除结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Integer roomId, @PathVariable Long id) {
        try {
            CleanInspectionRecord existing = cleanInspectionRecordService.getById(id);
            if (existing == null || !existing.getRoomId().equals(roomId)) {
                return error("记录不存在");
            }
            cleanInspectionRecordService.removeById(id);
            return success(null, "删除成功");
        } catch (Exception e) {
            return exception(e, "删除洁净检测记录");
        }
    }

    // endregion
}
