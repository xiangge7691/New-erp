package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.Organization.OrganizationWithDetailsDto;
import com.tonghui.erp.Data.Entity.FileInfo;
import com.tonghui.erp.Data.Entity.Organization;
import com.tonghui.erp.Service.FileInfoService;
import com.tonghui.erp.Service.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 机构信息控制器
 * <p>
 * 提供机构许可证基础信息的查询、更新，许可证扫描件的上传/删除，以及到期预警查询功能。
 * 单机构模式，系统中仅维护一条机构记录。
 * </p>
 *
 * 接口清单：
 * ┌────┬────────────────────────────────────┬────────┬──────────────────────────────────────┐
 * │ #  │ 接口                               │ 方法   │ 说明                                 │
 * ├────┼────────────────────────────────────┼────────┼──────────────────────────────────────┤
 * │ 1  │ /api/organization                  │ GET   │ 获取当前机构信息（含证书+实时状态）  │
 * │ 2  │ /api/organization                  │ PUT   │ 更新机构信息                         │
 * │ 3  │ /api/organization/scan-file        │ POST  │ 上传许可证扫描件                     │
 * │ 4  │ /api/organization/scan-file/{fileId}│ DELETE│ 删除许可证扫描件                    │
 * │ 5  │ /api/organization/expiring         │ GET   │ 到期预警查询                         │
 * └────┴────────────────────────────────────┴────────┴──────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/organization")
public class OrganizationController extends BaseController {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

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

    // region 机构信息CRUD接口
    // ===================================
    // 机构信息CRUD接口
    // ===================================

    /**
     * 获取当前机构信息（含证书列表+实时状态）
     * <p>
     * 单机构模式，返回唯一的一条机构记录。
     * 许可证状态根据有效期至实时计算（有效/即将到期/已过期）。
     * 同时返回许可证扫描件列表（通过 businessType=ORGANIZATION_LICENSE 查询）。
     * </p>
     *
     * 示例请求：
     * GET /api/organization
     *
     * @return ApiResponse&lt;OrganizationWithDetailsDto&gt; 机构信息（含证书列表、许可证扫描件）
     */
    @GetMapping
    public ApiResponse<OrganizationWithDetailsDto> getOrganization() {
        try {
            OrganizationWithDetailsDto dto = organizationService.getWithDetails();
            if (dto == null) {
                return error("机构信息不存在，请先初始化");
            }

            // 查询许可证扫描件
            List<FileInfo> scanFiles = fileInfoService.getFilesByBusiness(
                    dto.getId(), "ORGANIZATION_LICENSE", null);
            dto.setCertificates(dto.getCertificates());

            return success(dto);
        } catch (Exception ex) {
            return exception(ex, "获取机构信息");
        }
    }

    /**
     * 更新机构信息
     * <p>
     * 自动计算有效期至 = 发证日期 + 5年，不可手动修改。
     * 仅允许修改持久状态（注销/吊销），有效/即将到期/已过期由系统实时计算。
     * </p>
     *
     * 示例请求：
     * PUT /api/organization
     * Content-Type: application/json
     * {
     *   "licenseNo": "湘20260001",
     *   "orgName": "长好医院",
     *   "orgCategory": "医院",
     *   "unifiedSocialCreditCode": "91430100MA******",
     *   "practiceLicenseNo": "430**********",
     *   "legalRepresentative": "张三",
     *   "enterpriseLeader": "李四",
     *   "prepRoomLeader": "王五",
     *   "preparationAddress": "XX省XX市XX区XX路XX号",
     *   "preparationScope": "丸剂(z)、颗粒剂(z)、片剂(z)、胶囊剂(z)",
     *   "issuingAuthority": "XX省药品监督管理局",
     *   "issueDate": "2026-01-01",
     *   "licenseStatus": "有效"
     * }
     *
     * @param organization 机构信息实体对象
     * @return ApiResponse&lt;Organization&gt; 更新后的机构信息
     */
    @PutMapping
    public ApiResponse<Organization> updateOrganization(@RequestBody Organization organization) {
        try {
            Organization updated = organizationService.updateOrganization(organization);
            return success(updated, "更新成功");
        } catch (Exception ex) {
            return exception(ex, "更新机构信息");
        }
    }

    // endregion

    // region 许可证扫描件管理接口
    // ===================================
    // 许可证扫描件管理接口
    // ===================================

    /**
     * 上传许可证扫描件
     *
     * 示例请求：
     * POST /api/organization/scan-file
     * Content-Type: multipart/form-data
     * body:
     *   file: [选择文件]
     *   description: 许可证扫描件
     *
     * @param file 文件对象（multipart/form-data，字段名 file）
     * @param description 文件描述（可选）
     * @return ApiResponse&lt;FileInfo&gt; 上传的文件信息
     */
    @PostMapping("/scan-file")
    public ApiResponse<FileInfo> uploadScanFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String description) throws Exception {
        Organization org = getCurrentOrganization();
        if (org == null) {
            return error("机构信息不存在");
        }

        String entityName = org.getOrgName() != null ? org.getOrgName() : "机构" + org.getId();
        FileInfo fileInfo = fileInfoService.uploadFileWithBusinessPath(
                file, "ORGANIZATION_LICENSE", org.getId(), entityName, description);
        return success(fileInfo, "文件上传成功");
    }

    /**
     * 删除许可证扫描件
     *
     * 示例请求：
     * DELETE /api/organization/scan-file/101
     *
     * @param fileId 文件ID（路径参数）
     * @return ApiResponse&lt;Void&gt; 删除结果
     */
    @DeleteMapping("/scan-file/{fileId}")
    public ApiResponse<Void> deleteScanFile(@PathVariable Long fileId) {
        fileInfoService.deleteFile(fileId);
        return success(null, "删除成功");
    }

    // endregion

    // region 到期预警接口
    // ===================================
    // 到期预警接口
    // ===================================

    /**
     * 到期预警查询
     * <p>
     * 查询在未来指定天数内许可证即将到期的机构，用于首页待办提醒
     * </p>
     *
     * 示例请求：
     * GET /api/organization/expiring?days=30
     *
     * @param days 提前天数（默认30天）
     * @return ApiResponse&lt;List&lt;Organization&gt;&gt; 到期预警机构列表
     */
    @GetMapping("/expiring")
    public ApiResponse<List<Organization>> expiring(
            @RequestParam(defaultValue = "30") int days) {
        List<Organization> list = organizationService.findExpiringOrganizations(days);
        return success(list);
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
