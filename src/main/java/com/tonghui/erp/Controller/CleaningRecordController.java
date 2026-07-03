package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.CleaningRecord;
import com.tonghui.erp.Service.CleaningRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 清洁记录控制器
 * <p>
 * 提供车间环境清洁记录的CRUD操作及到期提醒查询，用于GMP合规管理中的清洁计划跟踪
 * </p>
 *
 * 接口清单：
 * ┌────┬──────────────────────────────┬────────┬──────────────────────────────┐
 * │ #  │ 接口                         │ 方法   │ 说明                         │
 * ├────┼──────────────────────────────┼────────┼──────────────────────────────┤
 * │ 1  │ /api/cleaningRecord          │ GET    │ 分页/按房间查询清洁记录列表   │
 * │ 2  │ /api/cleaningRecord/reminder │ GET    │ 查询即将到期的清洁提醒        │
 * │ 3  │ /api/cleaningRecord          │ POST   │ 新增清洁记录                 │
 * │ 4  │ /api/cleaningRecord/{id}     │ PUT    │ 修改清洁记录                 │
 * │ 5  │ /api/cleaningRecord/{id}     │ DELETE │ 删除清洁记录                 │
 * └────┴──────────────────────────────┴────────┴──────────────────────────────┘
 */
@RestController
@RequestMapping("/api/cleaningRecord")
public class CleaningRecordController extends BaseController {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 清洁记录服务
     */
    @Autowired
    private CleaningRecordService cleaningRecordService;

    // endregion

    // region 查询接口
    // ===================================
    // 查询接口
    // ===================================

    /**
     * 查询清洁记录列表
     * <p>
     * 支持按房间ID筛选，返回所有未删除的清洁记录，按清洁日期倒序排列
     * </p>
     *
     * 示例请求：
     * GET /api/cleaningRecord?roomId=1
     * GET /api/cleaningRecord
     *
     * @param roomId 房间ID（可选），按房间筛选清洁记录
     * @return ApiResponse&lt;List&lt;CleaningRecord&gt;&gt; 清洁记录列表
     */
    @GetMapping
    public ApiResponse<List<CleaningRecord>> getAll(
            @RequestParam(required = false) Long roomId) {
        QueryWrapper<CleaningRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("is_deleted", 0);
        if (roomId != null) {
            wrapper.eq("room_id", roomId);
        }
        wrapper.orderByDesc("cleaning_date");
        List<CleaningRecord> list = cleaningRecordService.list(wrapper);
        return success(list);
    }

    /**
     * 查询即将到期的清洁提醒
     * <p>
     * 查询在未来指定天数内需要进行清洁的记录，用于到期提醒
     * </p>
     *
     * 示例请求：
     * GET /api/cleaningRecord/reminder?days=30
     * GET /api/cleaningRecord/reminder
     *
     * @param days 提前天数（默认30天），查询从今天起至指定天数内的待清洁记录
     * @return ApiResponse&lt;List&lt;CleaningRecord&gt;&gt; 即将到期的清洁记录列表
     */
    @GetMapping("/reminder")
    public ApiResponse<List<CleaningRecord>> reminder(
            @RequestParam(defaultValue = "30") int days) {
        List<CleaningRecord> list = cleaningRecordService.findUpcomingCleaning(days);
        return success(list);
    }

    // endregion

    // region 新增接口
    // ===================================
    // 新增接口
    // ===================================

    /**
     * 新增清洁记录
     * <p>
     * 如果提供了清洁日期和清洁周期但未提供下次清洁日期，系统将自动计算下次清洁日期
     * </p>
     *
     * 示例请求：
     * POST /api/cleaningRecord
     * Content-Type: application/json
     * {
     *   "roomId": 1,
     *   "cleaningDate": "2026-07-01",
     *   "cleaningArea": "配制间操作台",
     *   "cleaningMethod": "擦拭消毒",
     *   "cleaningPerson": "张三",
     *   "cleaningCycle": 7,
     *   "remark": "日常清洁"
     * }
     *
     * @param record 清洁记录信息
     * @return ApiResponse&lt;CleaningRecord&gt; 新增的清洁记录
     */
    @PostMapping
    public ApiResponse<CleaningRecord> create(@RequestBody CleaningRecord record) {
        if (record.getCleaningDate() != null && record.getCleaningCycle() != null
                && record.getNextCleaningDate() == null) {
            record.setNextCleaningDate(record.getCleaningDate().plusDays(record.getCleaningCycle()));
        }
        cleaningRecordService.save(record);
        return success(record, "新增成功");
    }

    // endregion

    // region 修改与删除接口
    // ===================================
    // 修改与删除接口
    // ===================================

    /**
     * 修改清洁记录
     *
     * 示例请求：
     * PUT /api/cleaningRecord/1
     * Content-Type: application/json
     * {
     *   "cleaningArea": "配制间操作台（已更新）",
     *   "cleaningMethod": "喷洒消毒",
     *   "remark": "已修改"
     * }
     *
     * @param id     记录ID（路径参数）
     * @param record 更新的清洁记录信息
     * @return ApiResponse&lt;CleaningRecord&gt; 修改后的清洁记录
     */
    @PutMapping("/{id}")
    public ApiResponse<CleaningRecord> update(@PathVariable Long id, @RequestBody CleaningRecord record) {
        CleaningRecord existing = cleaningRecordService.getById(id);
        if (existing == null) {
            return error("记录不存在");
        }
        record.setId(id);
        cleaningRecordService.updateById(record);
        return success(record, "修改成功");
    }

    /**
     * 删除清洁记录
     *
     * 示例请求：
     * DELETE /api/cleaningRecord/1
     *
     * @param id 记录ID（路径参数）
     * @return ApiResponse&lt;Void&gt; 删除结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        CleaningRecord existing = cleaningRecordService.getById(id);
        if (existing == null) {
            return error("记录不存在");
        }
        cleaningRecordService.removeById(id);
        return success(null, "删除成功");
    }

    // endregion
}
