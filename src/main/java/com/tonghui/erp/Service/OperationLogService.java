package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Dto.Log.OperationLogContext;
import com.tonghui.erp.Common.Dto.Log.OperationLogDetailDto;
import com.tonghui.erp.Common.Dto.Log.OperationLogDto;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;

import java.time.LocalDateTime;

/**
 * 操作日志服务接口
 */
public interface OperationLogService {

    // region 日志记录
    // ===================================
    // 日志记录
    // ===================================

    /**
     * 异步写入操作日志
     *
     * @param context 操作日志上下文
     */
    void recordAsync(OperationLogContext context);

    // endregion

    // region 日志查询
    // ===================================
    // 日志查询
    // ===================================

    /**
     * 分页查询操作日志
     *
     * @param userAccount   操作人账号（精确匹配，可为空）
     * @param userId        操作人ID（精确匹配，可为空）
     * @param userName      操作人姓名（模糊匹配，可为空）
     * @param module        业务模块（模糊匹配，可为空）
     * @param operationType 操作类型（精确匹配，可为空）
     * @param resultStatus  结果：1成功/0失败（可为空）
     * @param businessType  业务对象类型（精确匹配，可为空）
     * @param businessId    业务对象ID（精确匹配，可为空）
     * @param keyword       关键字（模糊匹配操作描述、请求地址、账号、姓名，可为空）
     * @param startTime     起始时间（含，可为空）
     * @param endTime       截止时间（含，可为空）
     * @param pageRequest   分页参数
     * @return 分页操作日志
     */
    PagedResult<OperationLogDto> search(String userAccount, Long userId, String userName, String module,
                                        String operationType, Integer resultStatus, String businessType,
                                        String businessId, String keyword, LocalDateTime startTime,
                                        LocalDateTime endTime, PageRequestDto pageRequest);

    /**
     * 查询操作日志详情
     *
     * @param id 日志ID
     * @return 日志详情；不存在返回 null
     */
    OperationLogDetailDto detail(Long id);

    // endregion

    // region 日志清理
    // ===================================
    // 日志清理
    // ===================================

    /**
     * 物理清理指定天数之前的操作日志
     *
     * @param retentionDays 保留天数
     * @param batchSize     单批删除条数
     * @return 实际删除条数
     */
    int cleanup(int retentionDays, int batchSize);

    // endregion
}
