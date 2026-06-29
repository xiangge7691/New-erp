package com.tonghui.erp.Common.Dto.Stock;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 有效期预警统计 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpiryWarningStatsDTO {
    
    /**
     * 紧急预警数量（≤7天）
     */
    private Integer urgentCount = 0;
    
    /**
     * 警告预警数量（≤30天）
     */
    private Integer warningCount = 0;
    
    /**
     * 提示信息数量（≤90天）
     */
    private Integer infoCount = 0;
    
    /**
     * 总预警数量
     */
    private Integer totalCount = 0;
}
