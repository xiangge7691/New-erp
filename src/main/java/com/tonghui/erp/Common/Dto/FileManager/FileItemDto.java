package com.tonghui.erp.Common.Dto.FileManager;

import lombok.Data;

/**
 * 文件/文件夹项DTO
 */
@Data
public class FileItemDto {
    /**
     * 文件/文件夹名称
     */
    private String name;

    /**
     * 相对路径（相对于 uploaded-files）
     */
    private String path;

    /**
     * 是否为文件夹
     */
    private boolean directory;

    /**
     * 文件大小（字节），文件夹为0
     */
    private Long size;

    /**
     * 文件扩展名
     */
    private String extension;

    /**
     * 修改时间
     */
    private String modifiedTime;

    /**
     * 文件类型图标类型（folder/image/document/video/audio/archive/other）
     */
    private String iconType;
}
