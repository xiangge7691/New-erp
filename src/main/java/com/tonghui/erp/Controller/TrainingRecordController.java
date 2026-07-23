package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.TrainingRecord;
import com.tonghui.erp.Service.TrainingRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 培训记录控制器
 * <p>
 * 提供培训记录的CRUD操作、高级查询、编号生成及到期提醒等功能
 * </p>
 *
 * 接口清单：
 * ┌────┬────────────────────────────────────┬────────┬──────────────────────────────┐
 * │ #  │ 接口                               │ 方法   │ 说明                         │
 * ├────┼────────────────────────────────────┼────────┼──────────────────────────────┤
 * │ 1  │ /api/training-record               │ GET    │ 分页查询培训记录列表         │
 * │ 2  │ /api/training-record/{id}          │ GET    │ 获取培训记录详情             │
 * │ 3  │ /api/training-record               │ POST   │ 新增培训记录                 │
 * │ 4  │ /api/training-record/{id}          │ PUT    │ 修改培训记录                 │
 * │ 5  │ /api/training-record/{id}          │ DELETE │ 删除培训记录                 │
 * │ 6  │ /api/training-record/search        │ GET    │ 高级查询培训记录             │
 * │ 7  │ /api/training-record/warning       │ GET    │ 到期提醒查询                 │
 * └────┴────────────────────────────────────┴────────┴──────────────────────────────┘
 */
@RestController
@RequestMapping("/api/training-record")
public class TrainingRecordController extends BaseCrudController<TrainingRecord, TrainingRecord, Long> {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 培训记录服务
     */
    @Autowired
    private TrainingRecordService trainingRecordService;

    // endregion

    // region CRUD操作实现方法
    // ===================================
    // CRUD操作实现方法
    // ===================================

    /**
     * 获取所有培训记录数据（分页）
     *
     * @param pageIndex 页码
     * @param pageSize  每页大小
     * @return 分页结果
     */
    @Override
    protected PagedResult<TrainingRecord> getAllData(int pageIndex, int pageSize) {
        int safePageIndex = Math.max(0, pageIndex);
        int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);

        Page<TrainingRecord> pageResult = trainingRecordService.getTrainingRecordList(safePageIndex, safePageSize);

        PagedResult<TrainingRecord> pagedResult = new PagedResult<>();
        pagedResult.setItems(pageResult.getRecords());
        pagedResult.setTotalCount(pageResult.getTotal());
        pagedResult.setPageIndex(safePageIndex);
        pagedResult.setPageSize((int) pageResult.getSize());

        return pagedResult;
    }

    /**
     * 根据ID获取培训记录
     *
     * @param id 培训记录ID
     * @return 培训记录实体
     */
    @Override
    protected TrainingRecord getDataById(Long id) {
        return trainingRecordService.getTrainingRecordById(id);
    }

    /**
     * 创建培训记录
     *
     * @param trainingRecord 培训记录信息
     * @return 创建后的培训记录
     */
    @Override
    protected TrainingRecord doCreate(TrainingRecord trainingRecord) {
        trainingRecordService.addTrainingRecord(trainingRecord);
        return trainingRecord;
    }

    /**
     * 更新培训记录
     *
     * @param id              培训记录ID
     * @param trainingRecord  培训记录信息
     * @return 更新后的培训记录
     */
    @Override
    protected TrainingRecord doUpdate(Long id, TrainingRecord trainingRecord) {
        trainingRecord.setId(id);
        trainingRecordService.updateTrainingRecord(trainingRecord);
        return trainingRecord;
    }

    /**
     * 删除培训记录
     *
     * @param id 培训记录ID
     * @return 是否删除成功
     */
    @Override
    protected boolean doDelete(Long id) {
        return trainingRecordService.deleteTrainingRecord(id);
    }

    // endregion

    // region 高级查询
    // ===================================
    // 高级查询
    // ===================================

    /**
     * 高级查询培训记录（支持多条件 + 分页）
     *
     * 可选查询条件：
     * - trainingName：模糊匹配
     * - trainingCategory：精确匹配
     * - trainingForm：精确匹配
     * - trainingDateStart/End：培训日期范围
     *
     * 示例请求：
     * GET /api/training-record/search?pageIndex=0&pageSize=20&trainingName=GMP&trainingDateStart=2026-01-01T00:00:00&trainingDateEnd=2026-12-31T23:59:59
     *
     * @param trainingRecord    查询条件（自动从query参数映射）
     * @param trainingDateStart 培训日期开始（可选）
     * @param trainingDateEnd   培训日期结束（可选）
     * @param pageIndex         页码
     * @param pageSize          每页大小
     * @return 分页结果
     */
    @GetMapping("/search")
    public ApiResponse<PagedResult<TrainingRecord>> queryTrainingRecords(TrainingRecord trainingRecord,
                                                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime trainingDateStart,
                                                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime trainingDateEnd,
                                                                         @RequestParam int pageIndex,
                                                                         @RequestParam int pageSize) {
        try {
            int safePageIndex = Math.max(0, pageIndex);
            int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);

            Page<TrainingRecord> pageResult = trainingRecordService.queryTrainingRecords(trainingRecord, trainingDateStart, trainingDateEnd, safePageIndex, safePageSize);

            PagedResult<TrainingRecord> pagedResult = new PagedResult<>();
            pagedResult.setItems(pageResult.getRecords());
            pagedResult.setTotalCount(pageResult.getTotal());
            pagedResult.setPageIndex(safePageIndex);
            pagedResult.setPageSize((int) pageResult.getSize());

            return success(pagedResult);
        } catch (Exception ex) {
            return exception(ex, "查询培训记录");
        }
    }

    // endregion

    // region 到期提醒
    // ===================================
    // 到期提醒
    // ===================================

    /**
     * 查询即将到期的培训记录
     *
     * 示例请求：
     * GET /api/training-record/warning?days=30
     *
     * @param days 提前天数，默认30天
     * @return 即将到期的培训记录列表
     */
    @GetMapping("/warning")
    public ApiResponse<List<TrainingRecord>> getExpiringTrainings(
            @RequestParam(value = "days", defaultValue = "30") int days) {
        try {
            List<TrainingRecord> expiringList = trainingRecordService.getExpiringTrainings(days);
            return success(expiringList);
        } catch (Exception ex) {
            return exception(ex, "查询到期培训");
        }
    }

    /**
     * 更新培训记录预警状态
     *
     * 示例请求：
     * PUT /api/training-record/1/reminder-status?status=1
     *
     * @param id     培训记录ID（路径参数）
     * @param status 预警状态（0未处理/1已处理/2不再提醒）
     * @return 操作结果
     */
    @PutMapping("/{id}/reminder-status")
    public ApiResponse<Void> updateReminderStatus(@PathVariable Long id,
                                                  @RequestParam Integer status) {
        try {
            if (status == null || status < 0 || status > 2) {
                return error("预警状态值无效，应为0、1或2");
            }
            boolean result = trainingRecordService.updateReminderStatus(id, status);
            if (result) {
                return success(null, "更新成功");
            } else {
                return error("记录不存在");
            }
        } catch (Exception ex) {
            return exception(ex, "更新预警状态");
        }
    }

    // endregion
}
