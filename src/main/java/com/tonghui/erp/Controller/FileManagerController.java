package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.FileManager.DirectoryListingDto;
import com.tonghui.erp.Common.Dto.FileManager.FileItemDto;
import com.tonghui.erp.Data.Entity.FileInfo;
import com.tonghui.erp.Service.FileManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
 * │ 8  │ /api/file-manager/download   │ GET    │ 下载文件         │
 * │ 9  │ /api/file-manager/preview    │ GET    │ 预览文件         │
 * │ 10 │ /api/file-manager/search     │ GET    │ 搜索文件         │
 * └────┴──────────────────────────────┴────────┴──────────────────┘
 *
 * 安全说明：
 * - 所有路径操作限制在 uploaded-files 目录内
 * - 禁止使用 .. 访问上级目录
 * - 文件类型复用 FileStorageConfig.allowedTypes
 */
@RestController
@RequestMapping("/api/file-manager")
public class FileManagerController extends BaseController {

    @Autowired
    private FileManagerService fileManagerService;

    /**
     * 列出目录内容
     *
     * @param path 目录相对路径（为空时列出根目录）
     * @return 目录列表（文件夹在前，文件在后）
     *
     * 请求示例：GET /api/file-manager/list?path=维保记录/2026/06
     */
    @GetMapping("/list")
    public ApiResponse<DirectoryListingDto> listDirectory(
            @RequestParam(required = false, defaultValue = "") String path) {
        try {
            DirectoryListingDto result = fileManagerService.listDirectory(path);
            return success(result);
        } catch (Exception e) {
            return exception(e, "列出目录");
        }
    }

    /**
     * 新建文件夹
     *
     * @param parentPath 父目录相对路径
     * @param folderName 文件夹名称
     *
     * 请求示例：POST /api/file-manager/mkdir?parentPath=维保记录/2026/06&folderName=热风循环烘箱
     */
    @PostMapping("/mkdir")
    public ApiResponse<Void> createFolder(
            @RequestParam String parentPath,
            @RequestParam String folderName) {
        try {
            fileManagerService.createFolder(parentPath, folderName);
            return success(null, "文件夹创建成功");
        } catch (Exception e) {
            return exception(e, "创建文件夹");
        }
    }

    /**
     * 重命名文件或文件夹
     *
     * @param path    文件/文件夹的相对路径
     * @param newName 新名称
     *
     * 请求示例：PUT /api/file-manager/rename?path=维保记录/2026/06/old_name&newName=new_name
     */
    @PutMapping("/rename")
    public ApiResponse<Void> rename(
            @RequestParam String path,
            @RequestParam String newName) {
        try {
            fileManagerService.rename(path, newName);
            return success(null, "重命名成功");
        } catch (Exception e) {
            return exception(e, "重命名");
        }
    }

    /**
     * 移动文件或文件夹
     *
     * @param sourcePath      源相对路径
     * @param targetDirectory 目标目录相对路径
     *
     * 请求示例：PUT /api/file-manager/move?sourcePath=维保记录/file.jpg&targetDirectory=设备照片/
     */
    @PutMapping("/move")
    public ApiResponse<Void> move(
            @RequestParam String sourcePath,
            @RequestParam String targetDirectory) {
        try {
            fileManagerService.move(sourcePath, targetDirectory);
            return success(null, "移动成功");
        } catch (Exception e) {
            return exception(e, "移动");
        }
    }

    /**
     * 复制文件或文件夹
     *
     * @param sourcePath      源相对路径
     * @param targetDirectory 目标目录相对路径
     *
     * 请求示例：POST /api/file-manager/copy?sourcePath=维保记录/file.jpg&targetDirectory=备份/
     */
    @PostMapping("/copy")
    public ApiResponse<Void> copy(
            @RequestParam String sourcePath,
            @RequestParam String targetDirectory) {
        try {
            fileManagerService.copy(sourcePath, targetDirectory);
            return success(null, "复制成功");
        } catch (Exception e) {
            return exception(e, "复制");
        }
    }

    /**
     * 删除文件或文件夹
     *
     * @param path 文件/文件夹的相对路径
     *
     * 请求示例：DELETE /api/file-manager/delete?path=维保记录/file.jpg
     */
    @DeleteMapping("/delete")
    public ApiResponse<Void> delete(@RequestParam String path) {
        try {
            fileManagerService.delete(path);
            return success(null, "删除成功");
        } catch (Exception e) {
            return exception(e, "删除");
        }
    }

    /**
     * 上传文件到指定目录
     *
     * @param file    文件对象
     * @param dirPath 目标目录相对路径
     * @return 上传的文件信息
     *
     * 请求示例：POST /api/file-manager/upload (multipart/form-data)
     */
    @PostMapping("/upload")
    public ApiResponse<FileInfo> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "") String dirPath) {
        try {
            FileInfo fileInfo = fileManagerService.uploadFile(file, dirPath);
            return success(fileInfo, "上传成功");
        } catch (IOException e) {
            return exception(e, "上传文件");
        }
    }

    /**
     * 下载文件
     *
     * @param path 文件相对路径
     * @return 文件流
     *
     * 请求示例：GET /api/file-manager/download?path=维保记录/2026/06/uuid.jpg
     */
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam String path) {
        try {
            FileInfo fileInfo = null;
            String filename = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;

            InputStream inputStream = fileManagerService.downloadFile(path);
            InputStreamResource resource = new InputStreamResource(inputStream);

            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 预览文件
     *
     * @param path 文件相对路径
     * @return 文件流（用于前端预览）
     *
     * 请求示例：GET /api/file-manager/preview?path=维保记录/2026/06/uuid.jpg
     */
    @GetMapping("/preview")
    public ResponseEntity<Resource> previewFile(@RequestParam String path) {
        try {
            InputStream inputStream = fileManagerService.previewFile(path);
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
     * @param keyword 搜索关键词（文件名模糊匹配）
     * @param path    搜索范围的相对路径（为空则搜索全部）
     * @return 匹配的文件列表
     *
     * 请求示例：GET /api/file-manager/search?keyword=维修单&path=维保记录
     */
    @GetMapping("/search")
    public ApiResponse<List<FileItemDto>> searchFiles(
            @RequestParam String keyword,
            @RequestParam(required = false, defaultValue = "") String path) {
        try {
            List<FileItemDto> results = fileManagerService.searchFiles(keyword, path);
            return success(results);
        } catch (Exception e) {
            return exception(e, "搜索文件");
        }
    }
}
