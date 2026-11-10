package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Dto.Log.DataChangeLogDetailDto;
import com.tonghui.erp.Common.Dto.Log.DataChangeLogDto;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据变更日志服务接口
 */
public interface DataChangeLogService {

    // region 查询
    // ===================================
    // 查询
    // ===================================

    /**
     * 查询指定操作日志下的全部数据变更
     *
     * @param operationLogId 操作日志ID
     * @return 变更列表（按执行顺序）
     */
    List<DataChangeLogDto> listByOperationLogId(Long operationLogId);

    /**
     * 分页查询数据变更日志
     *
     * @param operationLogId 操作日志ID（可为空）
     * @param traceId        变更TraceID（可为空）
     * @param tableName      表名（精确匹配，可为空）
     * @param businessId     业务记录主键（精确匹配，可为空）
     * @param changeType     变更类型（可为空）
     * @param userId         操作人ID（可为空）
     * @param userAccount    操作人账号（精确匹配，可为空）
     * @param startTime      起始时间（含，可为空）
     * @param endTime        截止时间（含，可为空）
     * @param pageRequest    分页参数
     * @return 分页变更日志
     */
    PagedResult<DataChangeLogDto> search(Long operationLogId, String traceId, String tableName,
                                         String businessId, String changeType, Long userId,
                                         String userAccount, LocalDateTime startTime, LocalDateTime endTime,
                                         PageRequestDto pageRequest);

    /**
     * 查询数据变更详情
     *
     * @param id 变更日志ID
     * @return 变更详情；不存在返回 null
     */
    DataChangeLogDetailDto detail(Long id);

    // endregion

    // region 清理
    // ===================================
    // 清理
    // ===================================

    /**
     * 物理清理指定天数之前的变更日志
     *
     * @param retentionDays 保留天数
     * @param batchSize     单批删除条数
     * @return 实际删除条数
     */
    int cleanup(int retentionDays, int batchSize);

    // endregion
}
