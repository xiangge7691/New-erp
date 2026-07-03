package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Data.Entity.FileInfo;
import com.tonghui.erp.Data.Entity.PersonnelCertificate;
import com.tonghui.erp.Service.FileInfoService;
import com.tonghui.erp.Service.PersonnelCertificateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

/**
 * 人员证书控制器
 * <p>
 * 提供人员证书的CRUD操作、批量保存（先删后插）及证书附件的文件管理功能，用于GMP合规管理中的人员资质证书管理
 * </p>
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────────────────┬────────┬─────────────────────────────────────┐
 * │ #  │ 接口                                         │ 方法   │ 说明                                │
 * ├────┼──────────────────────────────────────────────┼────────┼─────────────────────────────────────┤
 * │ 1  │ /api/personnelCertificate/personnel/{personnelFileId} │ GET │ 根据人员档案ID查询所有证书  │
 * │ 2  │ /api/personnelCertificate/personnel/{personnelFileId} │ POST│ 批量保存人员证书（先删后插）│
 * │ 3  │ /api/personnelCertificate                  │ POST  │ 单独新增一条证书                    │
 * │ 4  │ /api/personnelCertificate/{id}             │ PUT   │ 修改证书                            │
 * │ 5  │ /api/personnelCertificate/{id}             │ DELETE│ 删除证书                            │
 * │ 6  │ /api/personnelCertificate/{id}/attachments │ POST  │ 上传文件到证书                      │
 * │ 7  │ /api/personnelCertificate/{id}/attachments │ GET   │ 获取证书的所有文件                  │
 * │ 8  │ /api/personnelCertificate/{id}/attachments/{fileId} │ DELETE │ 删除证书的某个文件    │
 * └────┴──────────────────────────────────────────────┴────────┴─────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/personnelCertificate")
public class PersonnelCertificateController extends BaseController {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 人员证书服务
     */
    @Autowired
    private PersonnelCertificateService personnelCertificateService;

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
     * 根据人员档案ID查询所有证书
     *
     * 示例请求：
     * GET /api/personnelCertificate/personnel/1
     *
     * @param personnelFileId 人员档案ID（路径参数）
     * @return ApiResponse&lt;List&lt;PersonnelCertificate&gt;&gt; 证书列表
     */
    @GetMapping("/personnel/{personnelFileId}")
    public ApiResponse<List<PersonnelCertificate>> getByPersonnelFileId(@PathVariable Long personnelFileId) {
        List<PersonnelCertificate> list = personnelCertificateService.getByPersonnelFileId(personnelFileId);
        return success(list);
    }

    /**
     * 批量保存人员证书（先删后插）
     * <p>
     * 先删除该人员档案下的所有证书，再批量插入新的证书列表
     * </p>
     *
     * 示例请求：
     * POST /api/personnelCertificate/personnel/1
     * Content-Type: application/json
     * [
     *   {
     *     "certificateName": "药剂师资格证",
     *     "certificateNo": "YZ2026001",
     *     "issueDate": "2026-01-15",
     *     "expiryDate": "2030-01-15"
     *   },
     *   {
     *     "certificateName": "健康证",
     *     "certificateNo": "JK2026001",
     *     "issueDate": "2026-03-01",
     *     "expiryDate": "2027-03-01"
     *   }
     * ]
     *
     * @param personnelFileId 人员档案ID（路径参数）
     * @param certificates 证书列表
     * @return ApiResponse&lt;Void&gt; 操作结果
     */
    @PostMapping("/personnel/{personnelFileId}")
    public ApiResponse<Void> saveCertificates(@PathVariable Long personnelFileId,
                                              @RequestBody List<PersonnelCertificate> certificates) {
        personnelCertificateService.saveCertificates(personnelFileId, certificates);
        return success(null, "保存成功");
    }

    /**
     * 单独新增一条证书
     *
     * 示例请求：
     * POST /api/personnelCertificate
     * Content-Type: application/json
     * {
     *   "personnelFileId": 1,
     *   "certificateName": "执业药师证",
     *   "certificateNo": "ZY2026001",
     *   "issueDate": "2026-01-15",
     *   "expiryDate": "2030-01-15"
     * }
     *
     * @param certificate 证书实体对象
     * @return ApiResponse&lt;PersonnelCertificate&gt; 新增的证书
     */
    @PostMapping
    public ApiResponse<PersonnelCertificate> create(@RequestBody PersonnelCertificate certificate) {
        certificate.setIsDeleted(0);
        certificate.setVersion(0);
        personnelCertificateService.save(certificate);
        return success(certificate, "新增成功");
    }

    /**
     * 修改证书
     *
     * 示例请求：
     * PUT /api/personnelCertificate/1
     * Content-Type: application/json
     * {
     *   "certificateName": "执业药师证（更新）",
     *   "expiryDate": "2031-01-15"
     * }
     *
     * @param id 证书ID（路径参数）
     * @param certificate 证书实体对象
     * @return ApiResponse&lt;PersonnelCertificate&gt; 修改后的证书
     */
    @PutMapping("/{id}")
    public ApiResponse<PersonnelCertificate> update(@PathVariable Long id,
                                                     @RequestBody PersonnelCertificate certificate) {
        PersonnelCertificate existing = personnelCertificateService.getById(id);
        if (existing == null) {
            return error("证书不存在");
        }
        certificate.setCertificateId(id);
        personnelCertificateService.updateById(certificate);
        return success(certificate, "修改成功");
    }

    /**
     * 删除证书
     *
     * 示例请求：
     * DELETE /api/personnelCertificate/1
     *
     * @param id 证书ID（路径参数）
     * @return ApiResponse&lt;Void&gt; 删除结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        personnelCertificateService.removeById(id);
        return success(null, "删除成功");
    }

    // endregion

    // region 证书文件管理接口
    // ===================================
    // 证书文件管理接口
    // ===================================

    /**
     * 上传文件到证书
     *
     * 示例请求：
     * POST /api/personnelCertificate/1/attachments
     * Content-Type: multipart/form-data
     * body:
     *   file: [选择文件]
     *   description: 证书扫描件
     *
     * @param id 证书ID（路径参数）
     * @param file 文件对象（multipart/form-data，字段名 file）
     * @param customPath 自定义存储路径（可选）
     * @param description 文件描述（可选）
     * @return ApiResponse&lt;FileInfo&gt; 上传的文件信息
     */
    @PostMapping("/{id}/attachments")
    public ApiResponse<FileInfo> uploadAttachment(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String customPath,
            @RequestParam(required = false) String description) throws Exception {
        PersonnelCertificate cert = personnelCertificateService.getById(id);
        if (cert == null) {
            return error("证书不存在");
        }
        String entityName = cert.getCertificateName() != null ? cert.getCertificateName() : "证书" + id;
        FileInfo fileInfo = fileInfoService.uploadFileWithBusinessPath(
                file, "PERSONNEL_CERTIFICATE", id, entityName, description, customPath);
        return success(fileInfo, "文件上传成功");
    }

    /**
     * 获取证书的所有文件
     *
     * 示例请求：
     * GET /api/personnelCertificate/1/attachments
     *
     * @param id 证书ID（路径参数）
     * @return ApiResponse&lt;List&lt;FileInfo&gt;&gt; 证书文件列表
     */
    @GetMapping("/{id}/attachments")
    public ApiResponse<List<FileInfo>> getAttachments(@PathVariable Long id) {
        List<FileInfo> files = fileInfoService.getFilesByBusiness(id, "PERSONNEL_CERTIFICATE", null);
        return success(files);
    }

    /**
     * 删除证书的某个文件
     *
     * 示例请求：
     * DELETE /api/personnelCertificate/1/attachments/101
     *
     * @param id 证书ID（路径参数）
     * @param fileId 文件ID（路径参数）
     * @return ApiResponse&lt;Void&gt; 删除结果
     */
    @DeleteMapping("/{id}/attachments/{fileId}")
    public ApiResponse<Void> deleteAttachment(@PathVariable Long id, @PathVariable Long fileId) {
        fileInfoService.deleteFile(fileId);
        return success(null, "删除成功");
    }

    // endregion
}
