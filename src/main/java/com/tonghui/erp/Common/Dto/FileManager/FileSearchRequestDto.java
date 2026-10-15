package com.tonghui.erp.Common.Dto.FileManager;

import lombok.Data;

/**
 * 文件搜索请求DTO
 * <p>
 * 封装文件搜索和目录列表筛选的参数
 * </p>
 */
@Data
public class FileSearchRequestDto {

    // region 搜索参数
    // ===================================
    // 搜索参数
    // ===================================

/**
      * 搜索关键词（原始文件名模糊匹配，无记录时回退磁盘名）
      */
    private String keyword;

    /**
     * 路径模糊匹配
     */
    private String path;

    /**
     * 根目录类型："business"（业务文件）或 "custom"（自定义文件，默认）
     */
    private String root;

    // endregion

    // region 筛选参数
    // ===================================
    // 筛选参数
    // ===================================

    /**
     * 最小文件大小（字节）
     */
    private Long minSize;

    /**
     * 最大文件大小（字节）
     */
    private Long maxSize;

    /**
     * 修改时间起始（格式：yyyy-MM-dd HH:mm:ss）
     */
    private String modifiedAfter;

    /**
     * 修改时间截止（格式：yyyy-MM-dd HH:mm:ss）
     */
    private String modifiedBefore;

    // endregion

    // region 分页参数（已废弃）
    // ===================================
    // 分页参数（已废弃）
    // ===================================

    /**
     * 页码，从0开始
     * <p>
     * 已废弃：搜索接口已改为返回目录列表格式（folders/files），不再分页，此参数将被忽略
     * </p>
     */
    private Integer pageIndex;

    /**
     * 每页大小
     * <p>
     * 已废弃：搜索接口已改为返回目录列表格式（folders/files），不再分页，此参数将被忽略
     * </p>
     */
    private Integer pageSize;

    // endregion

    // region 工具方法
    // ===================================
    // 工具方法
    // ===================================

    /**
     * 获取页码，默认0
     * <p>
     * 已废弃：搜索接口不再分页，保留仅为兼容旧调用方
     * </p>
     */
    public int getPageIndex() {
        return pageIndex != null && pageIndex >= 0 ? pageIndex : 0;
    }

    /**
     * 获取每页大小，默认20
     * <p>
     * 已废弃：搜索接口不再分页，保留仅为兼容旧调用方
     * </p>
     */
    public int getPageSize() {
        return pageSize != null && pageSize > 0 ? pageSize : 20;
    }

    /**
     * 获取根目录类型，默认 custom
     */
    public String getRoot() {
        return root != null && !root.isEmpty() ? root : "custom";
    }

    // endregion
}
