package com.tonghui.erp.Common.Dto;

import lombok.Data;
import java.util.List;

/**
 * 分页结果
 * <p>
 * 用于封装分页查询的结果数据，包含数据列表和分页信息
 * </p>
 * 
 * @param <T> 数据类型
 */
@Data
public class PagedResult<T> {
    
    //#region 分页数据字段
    // ===================================
    // 分页数据字段
    // ===================================
    
    /**
     * 数据列表
     * <p>当前页的数据集合</p>
     */
    private List<T> items;

    /**
     * 总记录数
     * <p>满足查询条件的总记录数，用于计算总页数</p>
     */
    private long totalCount;

    /**
     * 页码
     * <p>当前页的页码，从0开始计数</p>
     */
    private int pageIndex;

    /**
     * 每页数量
     * <p>每页显示的记录数量，用于分页查询</p>
     */
    private int pageSize;
    
    //#endregion

    //#region 工具方法
    // ===================================
    // 工具方法
    // ===================================
    
    /**
     * 计算总页数
     * 
     * @return 总页数
     */
    public long getTotalPages() {
        if (pageSize <= 0) {
            return 0;
        }
        return (totalCount + pageSize - 1) / pageSize;
    }
    
    /**
     * 判断是否有下一页
     * 
     * @return 是否有下一页
     */
    public boolean hasNextPage() {
        return pageIndex < getTotalPages();
    }
    
    /**
     * 判断是否有上一页
     * 
     * @return 是否有上一页
     */
    public boolean hasPreviousPage() {
        return pageIndex > 0;
    }
    
    //#endregion
}
