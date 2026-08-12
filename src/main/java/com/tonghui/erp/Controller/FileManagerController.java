package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.FileManager.DirectoryListingDto;
import com.tonghui.erp.Common.Dto.FileManager.FileItemDto;
import com.tonghui.erp.Common.Dto.FileManager.FileSearchRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.FileInfo;
import com.tonghui.erp.Data.Entity.FileOperationLog;
import com.tonghui.erp.Service.FileManagerService;
import com.tonghui.erp.Service.FileOperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;

/**
 * 文件管理器控制器
 * 提供类似Windows文件管理器的文件操作接口
 *
 * 接口清单：
 * ┌────┬──────────────────────────────┬────────┬──────────────────┐
 * │ #  │ 接口                         │ 方法   │ 说明             │
 * ├────┼──────────────────────────────┼────────┼──────────────────┤
 * │ 1  │ /api/file-manager/list       │ GET    │ 列出目录内容     │
 * │ 2  │ /api/file-manager/mkdir      │ POST   │ 新建文件夹       │
 * │ 3  │ /api/file-manager/rename     │ PUT    │ 重命名           │
 * │ 4  │ /api/file-manager/move       │ PUT    │ 移动             │
 * │ 5  │ /api/file-manager/copy       │ POST   │ 复制             │
 * │ 6  │ /api/file-manager/delete     │ DELETE │ 删除             │
 * │ 7  │ /api/file-manager/upload     │ POST   │ 上传文件         │
 * │ 8  │ /api/file-manager/download   │ GET    │ 下载文件/导出文件夹（ZIP） │
 * │ 9  │ /api/file-manager/preview    │ GET    │ 预览文件         │
 * │ 10 │ /api/file-manager/search     │ GET    │ 搜索文件         │
 * └────┴──────────────────────────────┴────────┴──────────────────┘
 *
 * root 参数说明：
 * - "business"：业务文件（uploaded-files），仅允许 list/download/preview
 * - "custom"：自定义文件（custom-files），允许所有操作（默认值）
 */
@RestController
@RequestMapping("/api/file-manager")
public class FileManagerController extends BaseController {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    @Autowired
    private FileManagerService fileManagerService;

    @Autowired
    private FileOperationLogService fileOperationLogService;

    // endregion

    // region 目录操作接口
    // ===================================
    // 目录操作接口
    // ===================================

    /**
     * 列出目录内容
     *
     * 示例请求：GET /api/file-manager/list?path=文档&root=custom
     *
     * @param path 目录相对路径（为空时列出根目录）
     * @param root 根目录类型："business"（业务文件）或 "custom"（自定义文件，默认）
     * @return 目录列表（文件夹在前，文件在后）
     */
    @GetMapping("/list")
    public ApiResponse<DirectoryListingDto> listDirectory(
            @RequestParam(required = false, defaultValue = "") String path,
            @RequestParam(defaultValue = "custom") String root) {
        try {
            DirectoryListingDto result = fileManagerService.listDirectory(path, root);
            return success(result);
        } catch (Exception e) {
            return exception(e, "列出目录");
        }
    }

    /**
     * 新建文件夹
     *
     * 示例请求：POST /api/file-manager/mkdir?parentPath=文档&folderName=新建文件夹&root=custom
     *
     * @param parentPath 父目录相对路径
     * @param folderName 文件夹名称
     * @param root       根目录类型
     */
    @PostMapping("/mkdir")
    public ApiResponse<Void> createFolder(
            @RequestParam String parentPath,
            @RequestParam String folderName,
            @RequestParam(defaultValue = "custom") String root) {
        try {
            fileManagerService.createFolder(parentPath, folderName, root);
            return success(null, "文件夹创建成功");
        } catch (Exception e) {
            return exception(e, "创建文件夹");
        }
    }

    /**
     * 重命名文件或文件夹（仅自定义文件允许）
     *
     * 示例请求：PUT /api/file-manager/rename?path=文档/old_name&newName=new_name&root=custom
     *
     * @param path    文件/文件夹的相对路径
     * @param newName 新名称
     * @param root    根目录类型
     */
    @PutMapping("/rename")
    public ApiResponse<Void> rename(
            @RequestParam String path,
            @RequestParam String newName,
            @RequestParam(defaultValue = "custom") String root) {
        try {
            fileManagerService.rename(path, newName, root);
            return success(null, "重命名成功");
        } catch (Exception e) {
            return exception(e, "重命名");
        }
    }

    /**
     * 移动文件或文件夹（仅自定义文件允许）
     *
     * 示例请求：PUT /api/file-manager/move?sourcePath=文档/file.jpg&targetDirectory=备份&root=custom
     *
     * @param sourcePath      源相对路径
     * @param targetDirectory 目标目录相对路径
     * @param root            根目录类型
     */
    @PutMapping("/move")
    public ApiResponse<Void> move(
            @RequestParam String sourcePath,
            @RequestParam String targetDirectory,
            @RequestParam(defaultValue = "custom") String root) {
        try {
            fileManagerService.move(sourcePath, targetDirectory, root);
            return success(null, "移动成功");
        } catch (Exception e) {
            return exception(e, "移动");
        }
    }

    /**
     * 复制文件或文件夹（仅自定义文件允许）
     *
     * 示例请求：POST /api/file-manager/copy?sourcePath=文档/file.jpg&targetDirectory=备份&root=custom
     *
     * @param sourcePath      源相对路径
     * @param targetDirectory 目标目录相对路径
     * @param root            根目录类型
     */
    @PostMapping("/copy")
    public ApiResponse<Void> copy(
            @RequestParam String sourcePath,
            @RequestParam String targetDirectory,
            @RequestParam(defaultValue = "custom") String root) {
        try {
            fileManagerService.copy(sourcePath, targetDirectory, root);
            return success(null, "复制成功");
        } catch (Exception e) {
            return exception(e, "复制");
        }
    }

    /**
     * 删除文件或文件夹（仅自定义文件允许）
     *
     * 示例请求：DELETE /api/file-manager/delete?path=文档/file.jpg&root=custom
     *
     * @param path 文件/文件夹的相对路径
     * @param root 根目录类型
     */
    @DeleteMapping("/delete")
    public ApiResponse<Void> delete(
            @RequestParam String path,
            @RequestParam(defaultValue = "custom") String root) {
        try {
            fileManagerService.delete(path, root);
            return success(null, "删除成功");
        } catch (Exception e) {
            return exception(e, "删除");
        }
    }

    // endregion

    // region 回收站接口
    // ===================================
    // 回收站接口
    // ===================================

    /**
     * 查看回收站（支持按删除人、删除时间段筛选）
     *
     * 示例请求：
     * GET /api/file-manager/recycle-bin
     * GET /api/file-manager/recycle-bin?deletedBy=1&startTime=2026-01-01&endTime=2026-06-30 23:59:59
     *
     * @param deletedBy 删除人ID（可选）
     * @param startTime 删除时间起始（可选，格式：yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss，含边界）
     * @param endTime   删除时间截止（可选，格式：yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss，含边界）
     * @return 已删除的文件列表（含删除人、删除时间）
     */
    @GetMapping("/recycle-bin")
    public ApiResponse<List<FileItemDto>> listRecycleBin(
            @RequestParam(required = false) Long deletedBy,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        try {
            List<FileItemDto> result = fileManagerService.listRecycleBin(
                    deletedBy, parseTimeParam(startTime, true), parseTimeParam(endTime, false));
            return success(result);
        } catch (Exception e) {
            return exception(e, "查看回收站");
        }
    }

    /**
     * 恢复文件（从回收站恢复）
     *
     * 示例请求：POST /api/file-manager/restore?path=文档/file.jpg&root=custom
     *
     * @param path 文件/文件夹的相对路径
     * @param root 根目录类型
     */
    @PostMapping("/restore")
    public ApiResponse<Void> restore(
            @RequestParam String path,
            @RequestParam(defaultValue = "custom") String root) {
        try {
            fileManagerService.restore(path, root);
            return success(null, "恢复成功");
        } catch (Exception e) {
            return exception(e, "恢复文件");
        }
    }

    /**
     * 永久删除（从回收站彻底删除）
     *
     * 示例请求：DELETE /api/file-manager/permanent-delete?path=文档/file.jpg&root=custom
     *
     * @param path 文件/文件夹的相对路径
     * @param root 根目录类型
     */
    @DeleteMapping("/permanent-delete")
    public ApiResponse<Void> permanentDelete(
            @RequestParam String path,
            @RequestParam(defaultValue = "custom") String root) {
        try {
            fileManagerService.permanentDelete(path, root);
            return success(null, "永久删除成功");
        } catch (Exception e) {
            return exception(e, "永久删除");
        }
    }

    // endregion

    // region 操作日志接口
    // ===================================
    // 操作日志接口
    // ===================================

    /**
     * 查询文件操作日志
     *
     * 示例请求：
     * GET /api/file-manager/operation-log?operationType=UPLOAD&pageIndex=0&pageSize=20
     * GET /api/file-manager/operation-log?startTime=2026-01-01&endTime=2026-06-30 12:59:59&userId=1&pageIndex=0&pageSize=20
     *
     * @param operationType 操作类型（可选）：UPLOAD/DOWNLOAD/PREVIEW/CREATE_FOLDER/DELETE/RESTORE/RENAME/MOVE/COPY
     * @param userId        操作人ID（可选）
     * @param startTime     操作时间起始（可选，格式：yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss，含边界）
     * @param endTime       操作时间截止（可选，格式：yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss，含边界）
     * @param pageIndex     页码
     * @param pageSize      每页大小
     * @return 操作日志列表
     */
    @GetMapping("/operation-log")
    public ApiResponse<Page<FileOperationLog>> queryOperationLog(
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "20") int pageSize) {
        try {
            Page<FileOperationLog> result = fileOperationLogService.queryLogs(
                    operationType, userId, parseTimeParam(startTime, true), parseTimeParam(endTime, false), pageIndex, pageSize);
            return success(result);
        } catch (Exception e) {
            return exception(e, "查询操作日志");
        }
    }

    /**
     * 解析时间范围参数
     * <p>
     * 支持 yyyy-MM-dd（起始日 00:00:00，截止日 23:59:59.999）与 yyyy-MM-dd HH:mm:ss 两种格式
     * 格式错误或无内容时返回 null（不参与筛选）
     * </p>
     *
     * @param timeStr 时间字符串（可为空）
     * @param isStart 是否为起始时间（true=起始，false=截止）
     * @return 解析后的 LocalDateTime，解析失败或无内容返回 null
     */
    private LocalDateTime parseTimeParam(String timeStr, boolean isStart) {
        if (!StringUtils.hasText(timeStr)) {
            return null;
        }
        String trimmed = timeStr.trim();
        // 完整时间格式：yyyy-MM-dd HH:mm:ss
        try {
            return LocalDateTime.parse(trimmed, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (DateTimeParseException ignored) {
        }
        // 仅日期格式：yyyy-MM-dd
        try {
            LocalDate date = LocalDate.parse(trimmed, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return isStart ? date.atStartOfDay() : date.atTime(LocalTime.MAX);
        } catch (DateTimeParseException ignored) {
        }
        return null;
    }

    // endregion

    // region 文件操作接口
    // ===================================
    // 文件操作接口
    // ===================================

    /**
     * 上传文件到指定目录
     *
     * 示例请求：POST /api/file-manager/upload?dirPath=文档&root=custom (multipart/form-data)
     *
     * @param file    文件对象
     * @param dirPath 目标目录相对路径
     * @param root    根目录类型
     * @return 上传的文件信息
     */
    @PostMapping("/upload")
    public ApiResponse<FileInfo> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "") String dirPath,
            @RequestParam(defaultValue = "custom") String root) {
        try {
            FileInfo fileInfo = fileManagerService.uploadFile(file, dirPath, root);
            return success(fileInfo, "上传成功");
        } catch (IOException e) {
            return exception(e, "上传文件");
        }
    }

    /**
     * 下载文件或导出文件夹（合并接口）
     * <p>
     * 根据 path 指向的对象自动区分处理方式：
     * - path 为空或 "/" 或指向文件夹：递归打包为 ZIP 下载（保留目录层级结构，文件名还原为原始文件名）
     * - path 指向单个文件：直接下载该文件
     * </p>
     *
     * 示例请求：
     * GET /api/file-manager/download?path=文档/uuid.jpg&root=custom
     * GET /api/file-manager/download?path=文档&root=custom
     * GET /api/file-manager/download?root=custom
     *
     * @param path 文件或文件夹相对路径（为空或 "/" 时导出整个根目录为 ZIP）
     * @param root 根目录类型："business"（业务文件）或 "custom"（自定义文件，默认）
     * @return 文件流（单文件：原文件；文件夹：{文件夹名}.zip）
     */
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(
            @RequestParam(required = false, defaultValue = "") String path,
            @RequestParam(defaultValue = "custom") String root) {
        try {
            // 空路径 / 根目录 / 文件夹：打包为 ZIP 下载
            if (!StringUtils.hasText(path) || "/".equals(path.trim())
                    || fileManagerService.isDirectory(path, root)) {
                byte[] zipBytes = fileManagerService.exportFolder(path, root);
                String folderName = getExportFolderName(path);
                String filename = folderName + ".zip";
                InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(zipBytes));
                String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8);
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                        .body(resource);
            }
            // 单文件下载
            String filename = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
            InputStream inputStream = fileManagerService.downloadFile(path, root);
            InputStreamResource resource = new InputStreamResource(inputStream);
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 计算导出 ZIP 的文件名（根目录导出时使用 root）
     *
     * @param path 文件夹相对路径
     * @return 文件夹名称
     */
    private String getExportFolderName(String path) {
        if (!StringUtils.hasText(path) || "/".equals(path.trim())) {
            return "root";
        }
        String trimmed = path.trim();
        String name = trimmed.substring(trimmed.lastIndexOf('/') + 1);
        return name.isEmpty() ? "root" : name;
    }

    /**
     * 预览文件
     *
     * 示例请求：GET /api/file-manager/preview?path=文档/uuid.jpg&root=custom
     *
     * @param path 文件相对路径
     * @param root 根目录类型
     * @return 文件流（用于前端预览）
     */
    @GetMapping("/preview")
    public ResponseEntity<Resource> previewFile(
            @RequestParam String path,
            @RequestParam(defaultValue = "custom") String root) {
        try {
            InputStream inputStream = fileManagerService.previewFile(path, root);
            InputStreamResource resource = new InputStreamResource(inputStream);

            String filename = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
            String ext = filename.contains(".") ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase() : "";

            MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
            if (Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp").contains(ext)) {
                mediaType = MediaType.parseMediaType("image/" + ext);
            } else if ("pdf".equals(ext)) {
                mediaType = MediaType.APPLICATION_PDF;
            } else if ("txt".equals(ext)) {
                mediaType = MediaType.TEXT_PLAIN;
            }

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8))
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 搜索文件
     *
     * 支持文件名关键词搜索、路径模糊匹配、文件大小筛选、修改日期筛选、分页
     *
     * 示例请求：
     * GET /api/file-manager/search?keyword=维修单&path=文档&root=custom&minSize=1024&maxSize=1048576&modifiedAfter=2026-01-01 00:00:00&pageIndex=0&pageSize=20
     *
     * @param request 搜索请求参数
     * @return 分页搜索结果
     */
    @GetMapping("/search")
    public ApiResponse<PagedResult<FileItemDto>> searchFiles(FileSearchRequestDto request) {
        try {
            if (request.getRoot() == null) {
                request.setRoot("custom");
            }
            PagedResult<FileItemDto> results = fileManagerService.searchFiles(request);
            return success(results);
        } catch (Exception e) {
            return exception(e, "搜索文件");
        }
    }

    // endregion
}
