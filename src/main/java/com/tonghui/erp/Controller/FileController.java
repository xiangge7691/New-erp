package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.FileInfo;
import com.tonghui.erp.Service.FileInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;  // 修改为jakarta.servlet
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 文件管理控制器
 */
@RestController
@RequestMapping("/api/files")
public class FileController extends BaseController {

    @Autowired
    private FileInfoService fileInfoService;

    /**
     * 上传文件
     *
     * @param file 文件对象
     * @param category 文件分类（可选）
     * @param description 文件描述（可选）
     * @param businessId 业务ID（可选）
     * @param businessType 业务类型（可选）
     * @return 上传结果
     */
    @PostMapping("/upload")
    public ApiResponse<FileInfo> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "businessId", required = false) Long businessId,
            @RequestParam(value = "businessType", required = false) String businessType) {
        try {
            // 直接调用带业务信息的上传方法
            FileInfo fileInfo = fileInfoService.uploadFileWithBusiness(file, category, description, businessId, businessType);
            return success(fileInfo, "文件上传成功");
        } catch (IOException e) {
            return exception(e, "文件上传");
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }

    /**
     * 下载文件
     *
     * @param id 文件ID
     * @param request HTTP请求
     * @return 文件流
     */
    @GetMapping("/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id, HttpServletRequest request) {
        try {
            FileInfo fileInfo = fileInfoService.getById(id);
            if (fileInfo == null) {
                return ResponseEntity.notFound().build();
            }

            InputStream inputStream = fileInfoService.getFileInputStream(id);
            InputStreamResource resource = new InputStreamResource(inputStream);

            // 设置Content-Type
            String contentType = fileInfo.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                contentType = "application/octet-stream";
            }

            // 设置文件名编码
            String filename = URLEncoder.encode(fileInfo.getOriginalName(), StandardCharsets.UTF_8);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 删除文件
     *
     * @param id 文件ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> deleteFile(@PathVariable Long id) {
        try {
            boolean result = fileInfoService.deleteFile(id);
            return success(result, result ? "文件删除成功" : "文件删除失败");
        } catch (Exception e) {
            return exception(e, "文件删除");
        }
    }

    /**
     * 获取文件信息
     *
     * @param id 文件ID
     * @return 文件信息
     */
    @GetMapping("/info/{id}")
    public ApiResponse<FileInfo> getFileInfo(@PathVariable Long id) {
        try {
            FileInfo fileInfo = fileInfoService.getById(id);
            if (fileInfo == null) {
                return error("文件不存在");
            }
            return success(fileInfo);
        } catch (Exception e) {
            return exception(e, "获取文件信息");
        }
    }

    /**
     * 分页查询文件列表
     *
     * @param category 文件分类（可选）
     * @param pageRequest 分页参数
     * @return 文件列表
     */
    @GetMapping("/list")
    public ApiResponse<PagedResult<FileInfo>> listFiles(
            @RequestParam(value = "category", required = false) String category,
            @ModelAttribute PageRequestDto pageRequest) {
        try {
            pageRequest = processPageRequest(pageRequest);
            PagedResult<FileInfo> result = fileInfoService.getFilesByCategory(
                category, pageRequest.getPageIndex(), pageRequest.getPageSize());
            return success(result);
        } catch (Exception e) {
            return exception(e, "查询文件列表");
        }
    }

    /**
     * 根据业务信息查询文件
     *
     * @param businessId 业务ID
     * @param businessType 业务类型
     * @return 文件列表
     */
    @GetMapping("/business")
    public ApiResponse<java.util.List<FileInfo>> getFilesByBusiness(
            @RequestParam Long businessId,
            @RequestParam String businessType) {
        try {
            java.util.List<FileInfo> files = fileInfoService.getFilesByBusiness(businessId, businessType);
            return success(files);
        } catch (Exception e) {
            return exception(e, "查询业务相关文件");
        }
    }
}
