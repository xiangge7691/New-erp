package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Dto.FileManager.DirectoryListingDto;
import com.tonghui.erp.Common.Dto.FileManager.FileItemDto;
import com.tonghui.erp.Common.Dto.FileManager.FileSearchRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.FileInfo;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
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
     * @param deletedBy 删除人ID筛选（可选，为空不过滤）
     * @param startTime 删除时间起始（可选，含边界）
     * @param endTime   删除时间截止（可选，含边界）
     * @return 已删除的文件列表（含删除人、删除时间）
     */
    /**
     * 列出回收站内容（支持按删除人、删除时间段筛选）
     *
     * @param deletedBy     删除人ID（精确匹配，可为空）
     * @param deletedByName 删除人姓名（模糊匹配，可为空）
     * @param startTime     删除时间起始（含边界，可为空）
     * @param endTime       删除时间截止（含边界，可为空）
     * @return 回收站文件列表
     */
    List<FileItemDto> listRecycleBin(Long deletedBy, String deletedByName, LocalDateTime startTime, LocalDateTime endTime);

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
     * <p>
     * 支持文件名关键词搜索、路径模糊匹配、文件大小筛选、修改日期筛选
     * </p>
     *
     * @param request 搜索请求参数
     * @return 分页搜索结果
     */
    PagedResult<FileItemDto> searchFiles(FileSearchRequestDto request);

    /**
     * 导出文件夹为 ZIP 文件
     * <p>
     * 递归打包整个文件夹（保留目录层级结构，文件名还原为原始文件名），跳过回收站目录
     * </p>
     *
     * @param relativePath 文件夹相对路径（为空时导出整个根目录）
     * @param root         根目录类型："business"（业务文件）或 "custom"（自定义文件）
     * @return ZIP 文件内容（字节数组）
     */
    byte[] exportFolder(String relativePath, String root);

    /**
     * 判断相对路径是否为目录
     * <p>
     * 用于下载接口自动区分：路径指向文件夹时打包 ZIP，指向文件时直接下载
     * </p>
     *
     * @param relativePath 相对路径（可为空，空路径视为根目录）
     * @param root         根目录类型："business"（业务文件）或 "custom"（自定义文件）
     * @return 是否为目录（路径不存在或为文件时返回 false）
     */
    boolean isDirectory(String relativePath, String root);

    // endregion
}
