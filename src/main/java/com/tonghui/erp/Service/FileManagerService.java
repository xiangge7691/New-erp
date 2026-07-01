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

    /**
     * 列出目录内容
     *
     * @param relativePath 相对路径（相对于uploaded-files），为空时列出根目录
     * @return 目录列表结果
     */
    DirectoryListingDto listDirectory(String relativePath);

    /**
     * 新建文件夹
     *
     * @param parentPath 父目录相对路径
     * @param folderName 文件夹名称
     */
    void createFolder(String parentPath, String folderName);

    /**
     * 重命名文件或文件夹
     *
     * @param relativePath 文件/文件夹的相对路径
     * @param newName      新名称
     */
    void rename(String relativePath, String newName);

    /**
     * 移动文件或文件夹
     *
     * @param sourcePath      源相对路径
     * @param targetDirectory 目标目录相对路径
     */
    void move(String sourcePath, String targetDirectory);

    /**
     * 复制文件或文件夹
     *
     * @param sourcePath      源相对路径
     * @param targetDirectory 目标目录相对路径
     */
    void copy(String sourcePath, String targetDirectory);

    /**
     * 删除文件或文件夹
     *
     * @param relativePath 相对路径
     */
    void delete(String relativePath);

    /**
     * 上传文件到指定目录
     *
     * @param file         文件对象
     * @param relativeDir  目标目录相对路径
     * @return 文件信息
     */
    FileInfo uploadFile(MultipartFile file, String relativeDir) throws IOException;

    /**
     * 获取文件输入流（用于下载）
     *
     * @param relativePath 文件相对路径
     * @return 文件输入流
     */
    InputStream downloadFile(String relativePath) throws IOException;

    /**
     * 获取文件输入流（用于预览）
     *
     * @param relativePath 文件相对路径
     * @return 文件输入流
     */
    InputStream previewFile(String relativePath) throws IOException;

    /**
     * 搜索文件
     *
     * @param keyword      搜索关键词
     * @param relativePath 搜索范围的相对路径，为空则搜索全部
     * @return 匹配的文件列表
     */
    List<FileItemDto> searchFiles(String keyword, String relativePath);
}
