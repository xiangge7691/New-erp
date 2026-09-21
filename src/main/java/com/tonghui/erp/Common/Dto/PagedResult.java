package com.tonghui.erp.Common.Dto;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

import java.util.Collections;
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
    
    // region 分页数据字段
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
    
    // endregion

    // region 工具方法
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
    
    // endregion

    // region 分页工厂方法
    // ===================================
    // 分页工厂方法
    // ===================================

    /**
     * 根据PageRequestDto创建MyBatis-Plus分页对象
     * <p>
     * 当pageIndex和pageSize均为-1时，创建全量数据查询的Page对象（pageSize=10000）
     * </p>
     *
     * @param <T>           数据类型
     * @param pageRequest   分页请求参数
     * @return MyBatis-Plus Page对象
     */
    public static <T> Page<T> toMybatisPage(PageRequestDto pageRequest) {
        if (pageRequest.getPageIndex() == -1 || pageRequest.getPageSize() == -1) {
            return new Page<>(1, 10000);
        }
        return new Page<>(pageRequest.getPageIndex() + 1, pageRequest.getPageSize());
    }

    /**
     * 将MyBatis-Plus的Page结果转换为PagedResult
     * <p>
     * 统一处理分页索引转换：MyBatis-Plus页码从1开始，PagedResult页码从0开始
     * 全量数据查询时pageIndex固定为0
     * </p>
     *
     * @param <T>           数据类型
     * @param page          MyBatis-Plus分页结果
     * @param pageRequest   分页请求参数
     * @return PagedResult分页结果
     */
    public static <T> PagedResult<T> fromPage(Page<T> page, PageRequestDto pageRequest) {
        PagedResult<T> result = new PagedResult<>();
        result.setItems(page.getRecords());
        result.setTotalCount(page.getTotal());

        if (pageRequest.getPageIndex() == -1 || pageRequest.getPageSize() == -1) {
            result.setPageIndex(0);
            result.setPageSize(page.getTotal() > 0 ? (int) page.getTotal() : 0);
        } else {
            result.setPageIndex((int) page.getCurrent() - 1);
            result.setPageSize((int) page.getSize());
        }
        return result;
    }

    /**
     * 创建空的分页结果
     *
     * @param <T> 数据类型
     * @return 空的PagedResult
     */
    public static <T> PagedResult<T> empty() {
        PagedResult<T> result = new PagedResult<>();
        result.setItems(Collections.emptyList());
        result.setTotalCount(0);
        result.setPageIndex(0);
        result.setPageSize(0);
        return result;
    }

    // endregion
}
