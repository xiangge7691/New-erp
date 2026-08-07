package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.VerificationRecord;
import com.tonghui.erp.Service.VerificationRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 验证记录控制器
 * <p>
 * 提供验证记录的CRUD操作、高级查询及到期提醒等功能
 * 支持六种验证类别：设备确认、厂房验证、工艺验证、清洁验证、设施验证、检验方法验证
 * </p>
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────────────────┬────────┬──────────────────────────────┐
 * │ #  │ 接口                                         │ 方法   │ 说明                         │
 * ├────┼──────────────────────────────────────────────┼────────┼──────────────────────────────┤
 * │ 1  │ /api/verification-record                     │ GET    │ 分页查询验证记录列表         │
 * │ 2  │ /api/verification-record/{id}                │ GET    │ 获取验证记录详情             │
 * │ 3  │ /api/verification-record                     │ POST   │ 新增验证记录                 │
 * │ 4  │ /api/verification-record/{id}                │ PUT    │ 修改验证记录                 │
 * │ 5  │ /api/verification-record/{id}                │ DELETE │ 删除验证记录                 │
 * │ 6  │ /api/verification-record/search              │ GET    │ 高级查询验证记录             │
 * │ 7  │ /api/verification-record/warning             │ GET    │ 到期提醒查询                 │
 * │ 8  │ /api/verification-record/{id}/reminder-status│ PUT    │ 更新预警状态                 │
 * └────┴──────────────────────────────────────────────┴────────┴──────────────────────────────┘
 */
@RestController
@RequestMapping("/api/verification-record")
public class VerificationRecordController extends BaseCrudController<VerificationRecord, VerificationRecord, Long> {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 验证记录服务
     */
    @Autowired
    private VerificationRecordService verificationRecordService;

    // endregion

    // region CRUD操作实现方法
    // ===================================
    // CRUD操作实现方法
    // ===================================

    /**
     * 获取所有验证记录数据（分页）
     *
     * @param pageIndex 页码
     * @param pageSize  每页大小
     * @return 分页结果
     */
    @Override
    protected PagedResult<VerificationRecord> getAllData(int pageIndex, int pageSize) {
        int safePageIndex = Math.max(0, pageIndex);
        int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);

        Page<VerificationRecord> pageResult = verificationRecordService.getVerificationRecordList(safePageIndex, safePageSize);

        PagedResult<VerificationRecord> pagedResult = new PagedResult<>();
        pagedResult.setItems(pageResult.getRecords());
        pagedResult.setTotalCount(pageResult.getTotal());
        pagedResult.setPageIndex(safePageIndex);
        pagedResult.setPageSize((int) pageResult.getSize());

        return pagedResult;
    }

    /**
     * 根据ID获取验证记录
     *
     * @param id 验证记录ID
     * @return 验证记录实体
     */
    @Override
    protected VerificationRecord getDataById(Long id) {
        return verificationRecordService.getVerificationRecordById(id);
    }

    /**
     * 创建验证记录
     *
     * @param verificationRecord 验证记录信息
     * @return 创建后的验证记录
     */
    @Override
    protected VerificationRecord doCreate(VerificationRecord verificationRecord) {
        verificationRecordService.addVerificationRecord(verificationRecord);
        return verificationRecord;
    }

    /**
     * 更新验证记录
     *
     * @param id                  验证记录ID
     * @param verificationRecord  验证记录信息
     * @return 更新后的验证记录
     */
    @Override
    protected VerificationRecord doUpdate(Long id, VerificationRecord verificationRecord) {
        verificationRecord.setId(id);
        verificationRecordService.updateVerificationRecord(verificationRecord);
        return verificationRecord;
    }

    /**
     * 删除验证记录
     *
     * @param id 验证记录ID
     * @return 是否删除成功
     */
    @Override
    protected boolean doDelete(Long id) {
        return verificationRecordService.deleteVerificationRecord(id);
    }

    // endregion

    // region 高级查询
    // ===================================
    // 高级查询
    // ===================================

    /**
     * 高级查询验证记录（支持多条件 + 分页）
     *
     * 可选查询条件：
     * - category：验证类别（equipment/building/process/cleaning/facility/method）
     * - verificationNo：验证编号（模糊匹配）
     * - verificationName：验证名称（模糊匹配）
     * - relatedObject：关联对象（模糊匹配）
     * - executor：执行人（模糊匹配）
     * - auditor：审核人（模糊匹配）
     * - executeDateStart/End：执行日期范围
     * - nextVerifyDateStart/End：下次验证日期范围
     * - createdTimeStart/End：创建时间范围
     * - updatedTimeStart/End：更新时间范围
     *
     * 示例请求：
     * GET /api/verification-record/search?pageIndex=0&pageSize=20&keyword=验证&category=equipment&executeDateStart=2026-01-01T00:00:00&executeDateEnd=2026-12-31T23:59:59
     *
     * @param verificationRecord 查询条件（自动从query参数映射）
     * @param keyword            关键字（对验证编号、验证名称、关联对象进行模糊匹配，可选）
     * @param executeDateStart   执行日期开始（可选）
     * @param executeDateEnd     执行日期结束（可选）
     * @param nextVerifyDateStart 下次验证日期开始（可选）
     * @param nextVerifyDateEnd   下次验证日期结束（可选）
     * @param createdTimeStart   创建时间开始（可选）
     * @param createdTimeEnd     创建时间结束（可选）
     * @param updatedTimeStart   更新时间开始（可选）
     * @param updatedTimeEnd     更新时间结束（可选）
     * @param pageIndex          页码
     * @param pageSize           每页大小
     * @return 分页结果
     */
    @GetMapping("/search")
    public ApiResponse<PagedResult<VerificationRecord>> queryVerificationRecords(VerificationRecord verificationRecord,
                                                                                   @RequestParam(required = false) String keyword,
                                                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime executeDateStart,
                                                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime executeDateEnd,
                                                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime nextVerifyDateStart,
                                                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime nextVerifyDateEnd,
                                                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTimeStart,
                                                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTimeEnd,
                                                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime updatedTimeStart,
                                                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime updatedTimeEnd,
                                                                                   @RequestParam int pageIndex,
                                                                                   @RequestParam int pageSize) {
        try {
            int safePageIndex = Math.max(0, pageIndex);
            int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);

            Page<VerificationRecord> pageResult = verificationRecordService.queryVerificationRecords(
                    verificationRecord, keyword, executeDateStart, executeDateEnd,
                    nextVerifyDateStart, nextVerifyDateEnd,
                    createdTimeStart, createdTimeEnd, updatedTimeStart, updatedTimeEnd,
                    safePageIndex, safePageSize);

            PagedResult<VerificationRecord> pagedResult = new PagedResult<>();
            pagedResult.setItems(pageResult.getRecords());
            pagedResult.setTotalCount(pageResult.getTotal());
            pagedResult.setPageIndex(safePageIndex);
            pagedResult.setPageSize((int) pageResult.getSize());

            return success(pagedResult);
        } catch (Exception ex) {
            return exception(ex, "查询验证记录");
        }
    }

    // endregion

    // region 到期提醒
    // ===================================
    // 到期提醒
    // ===================================

    /**
     * 查询即将到期的验证记录
     *
     * 示例请求：
     * GET /api/verification-record/warning?days=30
     *
     * @param days 提前天数，默认30天
     * @return 到期的验证记录列表
     */
    @GetMapping("/warning")
    public ApiResponse<List<VerificationRecord>> getExpiringVerifications(
            @RequestParam(value = "days", defaultValue = "30") int days) {
        try {
            List<VerificationRecord> expiringList = verificationRecordService.getExpiringVerifications(days);
            return success(expiringList);
        } catch (Exception ex) {
            return exception(ex, "查询到期验证");
        }
    }

    /**
     * 更新验证记录预警状态
     *
     * 示例请求：
     * PUT /api/verification-record/1/reminder-status?status=1
     *
     * @param id     验证记录ID（路径参数）
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
            boolean result = verificationRecordService.updateReminderStatus(id, status);
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
