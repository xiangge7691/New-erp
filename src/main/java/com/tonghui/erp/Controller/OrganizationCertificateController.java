package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Data.Entity.FileInfo;
import com.tonghui.erp.Data.Entity.Organization;
import com.tonghui.erp.Data.Entity.OrganizationCertificate;
import com.tonghui.erp.Service.FileInfoService;
import com.tonghui.erp.Service.OrganizationCertificateService;
import com.tonghui.erp.Service.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 机构证书控制器
 * <p>
 * 管理机构级证书（如GMP证书、医疗机构执业许可证、消防验收合格证等），
 * 支持证书的增删改查、证书文件的上传/删除。
 * </p>
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────────────────┬────────┬──────────────────────────────────────┐
 * │ #  │ 接口                                         │ 方法   │ 说明                                 │
 * ├────┼──────────────────────────────────────────────┼────────┼──────────────────────────────────────┤
 * │ 1  │ /api/organizationCertificate                 │ GET   │ 查询当前机构所有证书                 │
 * │ 2  │ /api/organizationCertificate                 │ POST  │ 新增证书                             │
 * │ 3  │ /api/organizationCertificate/{id}            │ PUT   │ 修改证书                             │
 * │ 4  │ /api/organizationCertificate/{id}            │ DELETE│ 删除证书                             │
 * │ 5  │ /api/organizationCertificate/{id}/file       │ POST  │ 上传证书文件                         │
 * │ 6  │ /api/organizationCertificate/{id}/file       │ GET   │ 获取证书文件列表                     │
 * │ 7  │ /api/organizationCertificate/{id}/file/{fileId}│ DELETE│ 删除证书文件                       │
 * └────┴──────────────────────────────────────────────┴────────┴──────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/organizationCertificate")
public class OrganizationCertificateController extends BaseController {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 机构证书服务
     */
    @Autowired
    private OrganizationCertificateService organizationCertificateService;

    /**
     * 机构信息服务
     */
    @Autowired
    private OrganizationService organizationService;

    /**
     * 文件信息服务
     */
    @Autowired
    private FileInfoService fileInfoService;

    // endregion

    // region 证书CRUD接口
    // ===================================
    // 证书CRUD接口
    // ===================================

    /**
     * 查询当前机构所有证书
     *
     * 示例请求：
     * GET /api/organizationCertificate
     *
     * @return ApiResponse&lt;List&lt;OrganizationCertificate&gt;&gt; 证书列表
     */
    @GetMapping
    public ApiResponse<List<OrganizationCertificate>> getAll() {
        try {
            Organization org = getCurrentOrganization();
            if (org == null) {
                return error("机构信息不存在");
            }
            List<OrganizationCertificate> list = organizationCertificateService
                    .getByOrganizationId(org.getId());
            return success(list);
        } catch (Exception ex) {
            return exception(ex, "查询证书列表");
        }
    }

    /**
     * 新增证书
     *
     * 示例请求：
     * POST /api/organizationCertificate
     * Content-Type: application/json
     * {
     *   "certificateName": "GMP证书",
     *   "certificateNo": "XXGMP20260001",
     *   "certificateType": "机构资质",
     *   "issuingAuthority": "XX省药监局",
     *   "issueDate": "2026-07-01",
     *   "expiryDate": "2028-06-30"
     * }
     *
     * @param certificate 证书实体对象
     * @return ApiResponse&lt;OrganizationCertificate&gt; 新增的证书
     */
    @PostMapping
    public ApiResponse<OrganizationCertificate> create(@RequestBody OrganizationCertificate certificate) {
        try {
            Organization org = getCurrentOrganization();
            if (org == null) {
                return error("机构信息不存在");
            }
            certificate.setOrganizationId(org.getId());
            certificate.setIsDeleted(0);
            certificate.setVersion(0);
            organizationCertificateService.save(certificate);
            return success(certificate, "新增成功");
        } catch (Exception ex) {
            return exception(ex, "新增证书");
        }
    }

    /**
     * 修改证书
     *
     * 示例请求：
     * PUT /api/organizationCertificate/1
     * Content-Type: application/json
     * {
     *   "certificateName": "GMP证书（更新）",
     *   "expiryDate": "2029-06-30"
     * }
     *
     * @param id 证书ID（路径参数）
     * @param certificate 证书实体对象
     * @return ApiResponse&lt;OrganizationCertificate&gt; 修改后的证书
     */
    @PutMapping("/{id}")
    public ApiResponse<OrganizationCertificate> update(@PathVariable Long id,
                                                       @RequestBody OrganizationCertificate certificate) {
        try {
            OrganizationCertificate existing = organizationCertificateService.getById(id);
            if (existing == null) {
                return error("证书不存在");
            }
            certificate.setId(id);
            organizationCertificateService.updateById(certificate);
            return success(organizationCertificateService.getById(id), "修改成功");
        } catch (Exception ex) {
            return exception(ex, "修改证书");
        }
    }

    /**
     * 删除证书
     *
     * 示例请求：
     * DELETE /api/organizationCertificate/1
     *
     * @param id 证书ID（路径参数）
     * @return ApiResponse&lt;Void&gt; 删除结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        try {
            OrganizationCertificate existing = organizationCertificateService.getById(id);
            if (existing == null) {
                return error("证书不存在");
            }
            organizationCertificateService.removeById(id);
            return success(null, "删除成功");
        } catch (Exception ex) {
            return exception(ex, "删除证书");
        }
    }

    // endregion

    // region 证书文件管理接口
    // ===================================
    // 证书文件管理接口
    // ===================================

    /**
     * 上传证书文件
     *
     * 示例请求：
     * POST /api/organizationCertificate/1/file
     * Content-Type: multipart/form-data
     * body:
     *   file: [选择文件]
     *   description: GMP证书扫描件
     *
     * @param id 证书ID（路径参数）
     * @param file 文件对象（multipart/form-data，字段名 file）
     * @param description 文件描述（可选）
     * @return ApiResponse&lt;FileInfo&gt; 上传的文件信息
     */
    @PostMapping("/{id}/file")
    public ApiResponse<FileInfo> uploadFile(@PathVariable Long id,
                                            @RequestParam("file") MultipartFile file,
                                            @RequestParam(required = false) String description) throws Exception {
        OrganizationCertificate cert = organizationCertificateService.getById(id);
        if (cert == null) {
            return error("证书不存在");
        }

        String entityName = cert.getCertificateName() != null ? cert.getCertificateName() : "证书" + id;
        FileInfo fileInfo = fileInfoService.uploadFileWithBusinessPath(
                file, "ORGANIZATION_CERTIFICATE", id, entityName, description);
        return success(fileInfo, "文件上传成功");
    }

    /**
     * 获取证书文件列表
     *
     * 示例请求：
     * GET /api/organizationCertificate/1/file
     *
     * @param id 证书ID（路径参数）
     * @return ApiResponse&lt;List&lt;FileInfo&gt;&gt; 证书文件列表
     */
    @GetMapping("/{id}/file")
    public ApiResponse<List<FileInfo>> getFiles(@PathVariable Long id) {
        List<FileInfo> files = fileInfoService.getFilesByBusiness(id, "ORGANIZATION_CERTIFICATE", null);
        return success(files);
    }

    /**
     * 删除证书文件
     *
     * 示例请求：
     * DELETE /api/organizationCertificate/1/file/101
     *
     * @param id 证书ID（路径参数）
     * @param fileId 文件ID（路径参数）
     * @return ApiResponse&lt;Void&gt; 删除结果
     */
    @DeleteMapping("/{id}/file/{fileId}")
    public ApiResponse<Void> deleteFile(@PathVariable Long id, @PathVariable Long fileId) {
        fileInfoService.deleteFile(fileId);
        return success(null, "删除成功");
    }

    // endregion

    // region 私有辅助方法
    // ===================================
    // 私有辅助方法
    // ===================================

    /**
     * 获取当前机构记录（单机构模式）
     */
    private Organization getCurrentOrganization() {
        var result = organizationService.getWithDetails();
        if (result == null) {
            return null;
        }
        return organizationService.getById(result.getId());
    }

    // endregion
}
