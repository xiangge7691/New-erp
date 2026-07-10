package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Dto.FileManager.DirectoryListingDto;
import com.tonghui.erp.Common.Dto.FileManager.FileItemDto;
import com.tonghui.erp.Data.Entity.FileInfo;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 文件管理器服务接口
 */
public interface FileManagerService {

    // region 目录操作
    // ===================================
    // 目录操作
    // ===================================

    /**
     * 列出目录内容
     *
     * @param relativePath 相对路径，为空时列出根目录
     * @param root         根目录类型："business"（业务文件）或 "custom"（自定义文件）
     * @return 目录列表结果
     */
    DirectoryListingDto listDirectory(String relativePath, String root);

    /**
     * 新建文件夹
     *
     * @param parentPath 父目录相对路径
     * @param folderName 文件夹名称
     * @param root       根目录类型
     */
    void createFolder(String parentPath, String folderName, String root);

    /**
     * 重命名文件或文件夹（仅自定义文件允许）
     *
     * @param relativePath 文件/文件夹的相对路径
     * @param newName      新名称
     * @param root         根目录类型
     */
    void rename(String relativePath, String newName, String root);

    /**
     * 移动文件或文件夹（仅自定义文件允许）
     *
     * @param sourcePath      源相对路径
     * @param targetDirectory 目标目录相对路径
     * @param root            根目录类型
     */
    void move(String sourcePath, String targetDirectory, String root);

    /**
     * 复制文件或文件夹（仅自定义文件允许）
     *
     * @param sourcePath      源相对路径
     * @param targetDirectory 目标目录相对路径
     * @param root            根目录类型
     */
    void copy(String sourcePath, String targetDirectory, String root);

    /**
     * 删除文件或文件夹（仅自定义文件允许，移入回收站）
     *
     * @param relativePath 相对路径
     * @param root         根目录类型
     */
    void delete(String relativePath, String root);

    /**
     * 从回收站恢复文件或文件夹
     *
     * @param relativePath 相对路径
     * @param root         根目录类型
     */
    void restore(String relativePath, String root);

    /**
     * 永久删除文件或文件夹（从回收站彻底删除）
     *
     * @param relativePath 相对路径
     * @param root         根目录类型
     */
    void permanentDelete(String relativePath, String root);

    /**
     * 列出回收站内容（扫描 .recycle-bin 目录）
     *
     * @return 已删除的文件列表
     */
    List<FileItemDto> listRecycleBin();

    // endregion

    // region 文件操作
    // ===================================
    // 文件操作
    // ===================================

    /**
     * 上传文件到指定目录
     *
     * @param file        文件对象
     * @param relativeDir 目标目录相对路径
     * @param root        根目录类型
     * @return 文件信息
     */
    FileInfo uploadFile(MultipartFile file, String relativeDir, String root) throws IOException;

    /**
     * 获取文件输入流（用于下载）
     *
     * @param relativePath 文件相对路径
     * @param root         根目录类型
     * @return 文件输入流
     */
    InputStream downloadFile(String relativePath, String root) throws IOException;

    /**
     * 获取文件输入流（用于预览）
     *
     * @param relativePath 文件相对路径
     * @param root         根目录类型
     * @return 文件输入流
     */
    InputStream previewFile(String relativePath, String root) throws IOException;

    /**
     * 搜索文件
     *
     * @param keyword      搜索关键词
     * @param relativePath 搜索范围的相对路径，为空则搜索全部
     * @param root         根目录类型
     * @return 匹配的文件列表
     */
    List<FileItemDto> searchFiles(String keyword, String relativePath, String root);

    // endregion
}
