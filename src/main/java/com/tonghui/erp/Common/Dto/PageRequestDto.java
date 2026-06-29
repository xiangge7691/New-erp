package com.tonghui.erp.Common.Dto;

import lombok.Data;

/**
 * 分页请求参数
 * <p>
 * 用于封装分页查询的请求参数，包括页码和每页数量
 * </p>
 */
@Data
public class PageRequestDto {
    
    //#region 分页参数字段
    // ===================================
    // 分页参数字段
    // ===================================
    
    /**
     * 页码，从0开始，-1表示全量数据
     */
    private int pageIndex = -1;

    /**
     * 每页数量，-1表示全量数据
     */
    private int pageSize = -1;
    //#endregion

    //#region 分页参数验证方法
    // ===================================
    // 分页参数验证方法
    // ===================================
    
    /**
     * 验证并修正分页参数
     * <p>
     * 确保分页参数在合理范围内，pageIndex和pageSize为-1时表示获取全量数据
     * </p>
     */
    public void validateAndFix() {
        // 不对pageIndex和pageSize做默认值设置，保留-1表示全量数据
        if (this.pageIndex < 0 && this.pageIndex != -1) {
            this.pageIndex = 0;
        }
        if (this.pageSize < 0 && this.pageSize != -1) {
            this.pageSize = 10;
        }
        if (this.pageSize > 10000) {
            this.pageSize = 10000;
        }
    }
    //#endregion
}
