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
 */
@RestController
@RequestMapping("/api/cleaningRecord")
public class CleaningRecordController extends BaseController {

    @Autowired
    private CleaningRecordService cleaningRecordService;

    /**
     * 分页查询清洁记录列表
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
     */
    @GetMapping("/reminder")
    public ApiResponse<List<CleaningRecord>> reminder(
            @RequestParam(defaultValue = "30") int days) {
        List<CleaningRecord> list = cleaningRecordService.findUpcomingCleaning(days);
        return success(list);
    }

    /**
     * 新增清洁记录
     */
    @PostMapping
    public ApiResponse<CleaningRecord> create(@RequestBody CleaningRecord record) {
        if (record.getCleaningArea() == null || record.getCleaningArea().isEmpty()) {
            return error("清洁区域不能为空");
        }
        if (record.getCleaningDate() != null && record.getCleaningCycle() != null
                && record.getNextCleaningDate() == null) {
            record.setNextCleaningDate(record.getCleaningDate().plusDays(record.getCleaningCycle()));
        }
        cleaningRecordService.save(record);
        return success(record, "新增成功");
    }

    /**
     * 修改清洁记录
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
}
