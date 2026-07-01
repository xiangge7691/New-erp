package com.tonghui.erp.Common.Dto.FileManager;

import lombok.Data;
import java.util.List;

/**
 * 目录列表结果DTO
 */
@Data
public class DirectoryListingDto {
    /**
     * 当前路径
     */
    private String currentPath;

    /**
     * 父目录路径（根目录时为null）
     */
    private String parentPath;

    /**
     * 文件夹列表
     */
    private List<FileItemDto> folders;

    /**
     * 文件列表
     */
    private List<FileItemDto> files;
}
