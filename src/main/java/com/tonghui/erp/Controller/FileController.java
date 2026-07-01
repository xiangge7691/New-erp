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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 文件管理控制器
 *
 * 接口清单：
 * ┌────┬──────────────────────────┬────────┬─────────────────────────────────────┐
 * │ #  │ 接口                     │ 方法   │ 说明                                │
 * ├────┼──────────────────────────┼────────┼─────────────────────────────────────┤
 * │ 1  │ /api/files/upload-business│ POST  │ 【推荐】按业务路径上传（主要方式）   │
 * │ 2  │ /api/files/upload         │ POST  │ 通用上传（旧版兼容，按文件类型分目录）│
 * │ 3  │ /api/files/{id}           │ GET   │ 下载文件                            │
 * │ 4  │ /api/files/info/{id}      │ GET   │ 获取文件元数据                      │
 * │ 5  │ /api/files/{id}           │ DELETE│ 删除文件（磁盘+数据库）             │
 * │ 6  │ /api/files/business       │ GET   │ 按业务ID+类型查询文件列表           │
 * │ 7  │ /api/files/list           │ GET   │ 按分类分页查询（辅助，较少使用）     │
 * └────┴──────────────────────────┴────────┴─────────────────────────────────────┘
 *
 * 业务类型枚举（businessType）：
 * 
 * 设备管理：
 * - EQUIPMENT_MAINTENANCE    → 设备维保记录
 * - EQUIPMENT_PHOTO          → 设备照片
 * - EQUIPMENT_DOCUMENT       → 设备文档（说明书、合格证等）
 * 
 * 生产管理：
 * - PRODUCTION_PLAN          → 生产计划
 * - PRODUCTION_RECORD        → 生产记录
 * - PRODUCTION_PROCESS       → 生产工序记录
 * - PRODUCTION_REPORT        → 生产报告
 * 
 * 制剂管理：
 * - PREPARATION_DOCUMENT     → 制剂文档
 * - PREPARATION_FORMULA      → 制剂配方
 * - PREPARATION_SPEC         → 制剂规格标准
 * 
 * 物料管理：
 * - MATERIAL_FILE            → 物料文件
 * - MATERIAL_CERTIFICATE     → 物料资质证书
 * 
 * 库存管理：
 * - STOCK_IN_PURCHASE        → 原料入库单
 * - STOCK_IN_AUXILIARY       → 辅料入库单
 * - STOCK_IN_PACKAGING       → 包材入库单
 * - STOCK_IN_PRODUCT         → 成品入库单
 * - STOCK_OUT_SALES          → 销售出库单
 * - STOCK_OUT_PRODUCTION     → 生产领料出库单
 * - STOCK_OUT_RETURN         → 退货出库单
 * 
 * 采购管理：
 * - PURCHASE_ORDER           → 采购订单
 * - PURCHASE_CONTRACT        → 采购合同
 * - PURCHASE_INVOICE         → 采购发票
 * 
 * 质量管理：
 * - QUALITY_RECORD           → 质量记录
 * - QUALITY_INSPECTION       → 质检报告
 * - QUALITY_CERTIFICATE      → 质量证书
 * 
 * 人员管理：
 * - PERSONNEL_FILE           → 人员档案
 * - PERSONNEL_HEALTH_CERT    → 健康证
 * - PERSONNEL_WORK_CERT      → 工作证
 * - PERSONNEL_QUALIFICATION  → 资格证书
 * - PERSONNEL_EDUCATION      → 学历证书
 * - PERSONNEL_TRAINING       → 培训记录
 * 
 * 车间/环境管理：
 * - ROOM_DOCUMENT            → 车间洁净规程
 * - DISINFECTION_RECORD      → 消毒记录
 * - CLEAN_INSPECTION_RECORD  → 洁净检测记录
 * - TEMPERATURE_HUMIDITY_RECORD → 温湿度记录
 * - PRESSURE_DIFFERENCE_RECORD  → 压差记录
 * - ENVIRONMENT_LICENSE      → 环境许可证
 * - ENVIRONMENT_REPORT       → 环境检测报告
 * 
 * 审批管理：
 * - APPROVAL                 → 审批附件
 * 
 * 供应商/客户管理：
 * - SUPPLIER_QUALIFICATION   → 供应商资质
 * - CUSTOMER_QUALIFICATION   → 客户资质
 * 
 * 通用：
 * - GENERAL                  → 通用文件
 */
@RestController
@RequestMapping("/api/files")
public class FileController extends BaseController {

    @Autowired
    private FileInfoService fileInfoService;

    // =====================================================================
    // 1. 【推荐】按业务路径上传
    // =====================================================================

    /**
     * 按业务路径上传文件（推荐使用）
     *
     * 文件存储路径格式：{basePath}/{中文目录}/{年}/{月}/{实体名}/{uuid.ext}
     * 示例：uploaded-files/设备维保/2026/06/热风循环烘箱/a1b2c3d4.pdf
     *
     * @param file         文件对象（multipart/form-data，字段名 file）
     * @param businessType 业务类型（必填），见上方业务类型枚举
     * @param businessId   业务ID（必填），如维保记录ID、生产计划ID等
     * @param entityName   实体名称（选填），用于目录名，如设备名称。为空时默认"未命名"
     * @param description  文件描述（选填）
     * @return FileInfo 对象，包含 fileId、originalName、filePath、fileUrl 等
     *
     * 请求示例（Apifox）：
     * POST /api/files/upload-business
     * Content-Type: multipart/form-data
     * body:
     *   file:           [选择文件]
     *   businessType:   EQUIPMENT_MAINTENANCE
     *   businessId:     1
     *   entityName:     热风循环烘箱
     *   description:    维修单据
     *
     * 响应示例：
     * {
     *   "code": 200,
     *   "message": "文件上传成功",
     *   "data": {
     *     "fileId": 101,
     *     "originalName": "维修单.jpg",
     *     "storedName": "a1b2c3d4e5f6.jpg",
     *     "filePath": "uploaded-files/设备维保/2026/06/热风循环烘箱/a1b2c3d4e5f6.jpg",
     *     "fileSize": 204800,
     *     "contentType": "image/jpeg",
     *     "fileExtension": "jpg",
     *     "category": "设备维保",
     *     "description": "维修单据",
     *     "storageType": "LOCAL",
     *     "businessId": 1,
     *     "businessType": "EQUIPMENT_MAINTENANCE",
     *     "fileUrl": "/api/files/101"
     *   },
     *   "timestamp": 1719734400000
     * }
     */
    @PostMapping("/upload-business")
    public ApiResponse<FileInfo> uploadFileWithBusinessPath(
            @RequestParam("file") MultipartFile file,
            @RequestParam String businessType,
            @RequestParam Long businessId,
            @RequestParam(required = false) String entityName,
            @RequestParam(required = false) String description) {
        try {
            FileInfo fileInfo = fileInfoService.uploadFileWithBusinessPath(file, businessType, businessId, entityName, description);
            return success(fileInfo, "文件上传成功");
        } catch (IOException e) {
            return exception(e, "文件上传");
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }

    // =====================================================================
    // 2. 通用上传（旧版兼容）
    // =====================================================================

    /**
     * 通用文件上传（旧版接口，建议改用 /upload-business）
     *
     * 与 /upload-business 的区别：
     * - 存储路径按文件类型分目录：images / documents / archives
     * - 不创建业务路径目录结构
     * - 仍支持 businessId + businessType 参数关联业务
     *
     * @param file         文件对象（multipart/form-data，字段名 file）
     * @param category     文件分类（选填），如 image/document/archive，不填默认 documents
     * @param description  文件描述（选填）
     * @param businessId   业务ID（选填）
     * @param businessType 业务类型（选填）
     * @return FileInfo 对象
     */
    @PostMapping("/upload")
    public ApiResponse<FileInfo> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "businessId", required = false) Long businessId,
            @RequestParam(value = "businessType", required = false) String businessType) {
        try {
            FileInfo fileInfo = fileInfoService.uploadFileWithBusiness(file, category, description, businessId, businessType);
            return success(fileInfo, "文件上传成功");
        } catch (IOException e) {
            return exception(e, "文件上传");
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }

    // =====================================================================
    // 3. 下载文件
    // =====================================================================

    /**
     * 下载文件
     *
     * 返回文件流，Content-Type 根据文件类型自动设置。
     * 前端可直接通过 window.open('/api/files/{fileId}') 或 <a> 标签下载。
     *
     * @param id 文件ID（路径参数）
     * @return 文件流（二进制），HTTP 响应头包含 Content-Disposition 触发下载
     *
     * 请求示例：
     * GET /api/files/101
     *
     * 响应：
     * HTTP/1.1 200 OK
     * Content-Type: image/jpeg
     * Content-Disposition: attachment; filename*=UTF-8''维修单.jpg
     * [二进制文件内容]
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

            String contentType = fileInfo.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                contentType = "application/octet-stream";
            }

            String filename = URLEncoder.encode(fileInfo.getOriginalName(), StandardCharsets.UTF_8);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // =====================================================================
    // 4. 获取文件元数据
    // =====================================================================

    /**
     * 获取文件元数据（不返回文件内容）
     *
     * 返回 FileInfo 对象，包含原始文件名、存储路径、大小、类型、业务关联信息等。
     * 用于前端展示文件列表、获取下载链接等场景。
     *
     * @param id 文件ID（路径参数）
     * @return FileInfo 对象
     *
     * 请求示例：
     * GET /api/files/info/101
     *
     * 响应示例：
     * {
     *   "code": 200,
     *   "message": "操作成功",
     *   "data": {
     *     "fileId": 101,
     *     "originalName": "维修单.jpg",
     *     "storedName": "a1b2c3d4e5f6.jpg",
     *     "filePath": "uploaded-files/维保记录/2026/06/热风循环烘箱/a1b2c3d4e5f6.jpg",
     *     "fileSize": 204800,
     *     "contentType": "image/jpeg",
     *     "fileExtension": "jpg",
     *     "fileMd5": "d41d8cd98f00b204e9800998ecf8427e",
     *     "category": "维保记录",
     *     "description": "维修单据",
     *     "storageType": "LOCAL",
     *     "businessId": 1,
     *     "businessType": "EQUIPMENT_MAINTENANCE",
     *     "createdBy": 1,
     *     "createdTime": "2026-06-30T10:00:00",
     *     "fileUrl": "/api/files/101"
     *   },
     *   "timestamp": 1719734400000
     * }
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

    // =====================================================================
    // 5. 删除文件
    // =====================================================================

    /**
     * 删除文件（同时删除磁盘文件和数据库记录）
     *
     * 注意：此操作不可逆。如果文件被业务关联（如维保附件），
     * 应先通过业务接口（如 DELETE /api/equipmentMaintenance/{id}/attachments/{fileId}）删除。
     *
     * @param id 文件ID（路径参数）
     * @return 是否删除成功
     *
     * 请求示例：
     * DELETE /api/files/101
     *
     * 响应示例：
     * {
     *   "code": 200,
     *   "message": "文件删除成功",
     *   "data": true,
     *   "timestamp": 1719734400000
     * }
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

    // =====================================================================
    // 6. 按业务查询文件（常用）
    // =====================================================================

    /**
     * 按业务ID和业务类型查询文件列表
     *
     * 用于查询某个业务记录关联的所有附件。
     * 如：查询维保记录ID=1的所有附件、查询生产计划ID=10的所有文件等。
     *
     * @param businessId   业务ID（必填），如维保记录ID
     * @param businessType 业务类型（必填），见上方业务类型枚举
     * @return FileInfo 列表
     *
     * 请求示例：
     * GET /api/files/business?businessId=1&businessType=EQUIPMENT_MAINTENANCE
     *
     * 响应示例：
     * {
     *   "code": 200,
     *   "message": "操作成功",
     *   "data": [
     *     {
     *       "fileId": 101,
     *       "originalName": "维修单.jpg",
     *       "fileSize": 204800,
     *       "contentType": "image/jpeg",
     *       "fileUrl": "/api/files/101"
     *     },
     *     {
     *       "fileId": 102,
     *       "originalName": "保养记录.pdf",
     *       "fileSize": 102400,
     *       "contentType": "application/pdf",
     *       "fileUrl": "/api/files/102"
     *     }
     *   ],
     *   "timestamp": 1719734400000
     * }
     */
    @GetMapping("/business")
    public ApiResponse<List<FileInfo>> getFilesByBusiness(
            @RequestParam Long businessId,
            @RequestParam String businessType) {
        try {
            List<FileInfo> files = fileInfoService.getFilesByBusiness(businessId, businessType);
            return success(files);
        } catch (Exception e) {
            return exception(e, "查询业务相关文件");
        }
    }

    // =====================================================================
    // 7. 按分类分页查询（辅助，较少使用）
    // =====================================================================

    /**
     * 按文件分类分页查询文件列表（辅助接口）
     *
     * 通常不需要直接调用此接口，建议使用 /business 按业务查询。
     * 此接口适用于管理员查看所有图片、所有文档等场景。
     *
     * @param category    文件分类（选填），如 image/document/archive，不填查询全部
     * @param pageIndex   页码，从0开始（必填）
     * @param pageSize    每页大小（必填）
     * @return 分页结果
     *
     * 请求示例：
     * GET /api/files/list?category=image&pageIndex=0&pageSize=20
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
}
