package com.tonghui.erp.Common.Dto;

import com.tonghui.erp.Common.Dto.Production.ProductionDurationSummaryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 带生产用时汇总的分页结果
 * <p>
 * 在原分页结果基础上增加 summary 字段，原有 items/totalCount/pageIndex/pageSize
 * 保持不变，前端可在同一响应中获取列表与表格下方的平均生产时间
 * </p>
 *
 * @param <T> 数据类型
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PagedResultWithSummary<T> extends PagedResult<T> {

    /**
     * 当前筛选条件下的生产用时汇总（未请求汇总时为 null）
     */
    private ProductionDurationSummaryDto summary;
}
