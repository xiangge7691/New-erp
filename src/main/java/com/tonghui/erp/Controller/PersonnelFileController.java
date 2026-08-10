package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.PersonnelFileWithDetailsDto;
import com.tonghui.erp.Data.Entity.FileInfo;
import com.tonghui.erp.Data.Entity.PersonnelFile;
import com.tonghui.erp.Data.Entity.PersonnelCertificate;
import com.tonghui.erp.Data.Entity.Position;
import com.tonghui.erp.Data.Entity.Department;
import com.tonghui.erp.Data.Entity.User;
import com.tonghui.erp.Service.FileInfoService;
import com.tonghui.erp.Service.PersonnelFileService;
import com.tonghui.erp.Service.PositionService;
import com.tonghui.erp.Service.DepartmentService;
import com.tonghui.erp.Service.UserService;
import com.tonghui.erp.Service.PersonnelCertificateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 人员档案控制器
 * <p>
 * 提供人员档案的CRUD操作、带子表查询、健康证到期提醒、关联用户查询及健康档案/附件的文件管理功能，用于GMP合规管理中的人员资质管理
 * </p>
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────────┬────────┬─────────────────────────────────────┐
 * │ #  │ 接口                                 │ 方法   │ 说明                                │
 * ├────┼──────────────────────────────────────┼────────┼─────────────────────────────────────┤
 * │ 1  │ /api/personnelFile                   │ GET   │ 分页查询人员档案列表                │
 * │ 2  │ /api/personnelFile/{id}              │ GET   │ 根据ID查询人员档案详情              │
 * │ 3  │ /api/personnelFile                   │ POST  │ 新增人员档案                        │
 * │ 4  │ /api/personnelFile/{id}              │ PUT   │ 修改人员档案                        │
 * │ 5  │ /api/personnelFile/{id}              │ DELETE│ 删除人员档案                        │
 * │ 6  │ /api/personnelFile/search-with-details│ GET │ 带子表查询人员档案列表              │
 * │ 7  │ /api/personnelFile/expiring          │ GET   │ 健康证到期提醒查询                  │
 * │ 8  │ /api/personnelFile/byUser/{userId}   │ GET   │ 根据用户ID查询人员档案              │
 * │ 9  │ /api/personnelFile/{id}/health-files │ POST  │ 上传健康档案文件                    │
 * │ 10 │ /api/personnelFile/{id}/health-files │ GET   │ 获取健康档案文件列表                │
 * │ 11 │ /api/personnelFile/{id}/health-files/{fileId} │ DELETE │ 删除健康档案文件        │
 * │ 12 │ /api/personnelFile/{id}/attachments  │ POST  │ 上传附件文件                        │
 * │ 13 │ /api/personnelFile/{id}/attachments  │ GET   │ 获取附件文件列表                    │
 * │ 14 │ /api/personnelFile/{id}/attachments/{fileId} │ DELETE │ 删除附件文件            │
 * └────┴──────────────────────────────────────┴────────┴─────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/personnelFile")
public class PersonnelFileController extends BaseController {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 人员档案服务
     */
    @Autowired
    private PersonnelFileService personnelFileService;

    /**
     * 文件信息服务
     */
    @Autowired
    private FileInfoService fileInfoService;

    /**
     * 岗位服务
     */
    @Autowired
    private PositionService positionService;

    /**
     * 部门服务
     */
    @Autowired
    private DepartmentService departmentService;

    /**
     * 用户服务
     */
    @Autowired
    private UserService userService;

    /**
     * 人员证书服务
     */
    @Autowired
    private PersonnelCertificateService personnelCertificateService;

    // endregion

    // region 人员档案CRUD接口
    // ===================================
    // 人员档案CRUD接口
    // ===================================

    /**
     * 分页查询人员档案列表
     * <p>
     * 支持按关键词（姓名、工号、身份证号）、部门、岗位、资质、状态等条件筛选，按创建时间倒序排列
     * </p>
     *
     * 示例请求：
     * GET /api/personnelFile?keyword=张三&departmentId=1&positionId=1&qualification=药剂师&status=1&pageIndex=0&pageSize=10
     *
     * @param keyword 关键词（可选，模糊匹配姓名、工号、身份证号）
     * @param departmentId 部门ID（可选）
     * @param positionId 岗位ID（可选）
     * @param qualification 资质（可选，模糊匹配）
     * @param status 状态（可选，1-在职，0-离职）
     * @param pageIndex 页码，从0开始（默认0）
     * @param pageSize 每页大小（默认10）
     * @return ApiResponse&lt;PagedResult&lt;PersonnelFile&gt;&gt; 分页结果，包含人员档案列表
     */
    @GetMapping
    public ApiResponse<PagedResult<PersonnelFile>> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long positionId,
            @RequestParam(required = false) String qualification,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "10") int pageSize) {
        Page<PersonnelFile> page = new Page<>(pageIndex + 1, pageSize);
        QueryWrapper<PersonnelFile> wrapper = new QueryWrapper<>();

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like("name", keyword)
                              .or().like("employee_no", keyword)
                              .or().like("id_card_no", keyword));
        }
        if (departmentId != null) {
            wrapper.eq("department_id", departmentId);
        }
        if (positionId != null) {
            wrapper.eq("position_id", positionId);
        }
        if (qualification != null && !qualification.isEmpty()) {
            wrapper.like("qualification", qualification);
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("created_time");

        Page<PersonnelFile> pageResult = personnelFileService.page(page, wrapper);
        fillNameFieldsForList(pageResult.getRecords());

        // 批量填充证书列表
        List<Long> ids = pageResult.getRecords().stream()
                .map(PersonnelFile::getPersonnelFileId).collect(Collectors.toList());
        if (!ids.isEmpty()) {
            QueryWrapper<PersonnelCertificate> certWrapper = new QueryWrapper<>();
            certWrapper.in("personnel_file_id", ids);
            Map<Long, List<PersonnelCertificate>> certMap = personnelCertificateService
                    .list(certWrapper).stream()
                    .collect(Collectors.groupingBy(PersonnelCertificate::getPersonnelFileId));
            pageResult.getRecords().forEach(f ->
                    f.setCertificates(certMap.getOrDefault(f.getPersonnelFileId(), List.of())));
        }

        PagedResult<PersonnelFile> pagedResult = new PagedResult<>();
        pagedResult.setItems(pageResult.getRecords());
        pagedResult.setTotalCount(pageResult.getTotal());
        pagedResult.setPageIndex(pageIndex);
        pagedResult.setPageSize(pageSize);

        return success(pagedResult);
    }

    /**
     * 带子表查询人员档案列表
     * <p>
     * 返回人员档案及其关联的部门、岗位、证书等子表数据
     * </p>
     *
     * 示例请求：
     * GET /api/personnelFile/search-with-details?name=张三&pageIndex=0&pageSize=10
     *
     * @param personnelFile 人员档案查询条件对象
     * @param pageRequest 分页请求参数
     * @return ApiResponse&lt;PagedResult&lt;PersonnelFileWithDetailsDto&gt;&gt; 人员档案列表（含子表信息）
     */
// @GetMapping("/search-with-details")
    public ApiResponse<PagedResult<PersonnelFileWithDetailsDto>> searchWithDetails(
            PersonnelFile personnelFile,
            @ModelAttribute PageRequestDto pageRequest) {
        try {
            pageRequest = processPageRequest(pageRequest);
            PagedResult<PersonnelFileWithDetailsDto> result = personnelFileService.searchWithDetails(
                personnelFile, pageRequest.getPageIndex(), pageRequest.getPageSize());
            return success(result);
        } catch (Exception ex) {
            return exception(ex, "searchWithDetails");
        }
    }

    /**
     * 根据ID查询人员档案详情
     * <p>
     * 返回人员档案详情，包含关联的用户名、部门名、岗位名及证书列表
     * </p>
     *
     * 示例请求：
     * GET /api/personnelFile/1
     *
     * @param id 人员档案ID（路径参数）
     * @return ApiResponse&lt;PersonnelFile&gt; 人员档案详情
     */
    @GetMapping("/{id}")
    public ApiResponse<PersonnelFile> getById(@PathVariable Long id) {
        PersonnelFile file = personnelFileService.getById(id);
        if (file == null) {
            return error("人员档案不存在");
        }
        fillNameFields(file);
        file.setCertificates(personnelCertificateService.getByPersonnelFileId(id));
        return success(file);
    }

    /**
     * 新增人员档案
     *
     * 示例请求：
     * POST /api/personnelFile
     * Content-Type: application/json
     * {
     *   "name": "张三",
     *   "employeeNo": "EMP001",
     *   "departmentId": 1,
     *   "positionId": 1,
     *   "qualification": "药剂师",
     *   "status": 1
     * }
     *
     * @param personnelFile 人员档案实体对象
     * @return ApiResponse&lt;PersonnelFile&gt; 新增的人员档案
     */
    @PostMapping
    public ApiResponse<PersonnelFile> create(@RequestBody PersonnelFile personnelFile) {
        personnelFile.setIsDeleted(0);
        personnelFile.setVersion(0);
        personnelFileService.save(personnelFile);
        return success(personnelFile, "新增成功");
    }

    /**
     * 修改人员档案
     *
     * 示例请求：
     * PUT /api/personnelFile/1
     * Content-Type: application/json
     * {
     *   "name": "张三（更新）",
     *   "qualification": "主管药剂师"
     * }
     *
     * @param id 人员档案ID（路径参数）
     * @param personnelFile 人员档案实体对象
     * @return ApiResponse&lt;PersonnelFile&gt; 修改后的人员档案
     */
    @PutMapping("/{id}")
    public ApiResponse<PersonnelFile> update(@PathVariable Long id, @RequestBody PersonnelFile personnelFile) {
        PersonnelFile existing = personnelFileService.getById(id);
        if (existing == null) {
            return error("人员档案不存在");
        }
        personnelFile.setPersonnelFileId(id);
        personnelFileService.updateById(personnelFile);
        return success(personnelFile, "修改成功");
    }

    /**
     * 删除人员档案
     *
     * 示例请求：
     * DELETE /api/personnelFile/1
     *
     * @param id 人员档案ID（路径参数）
     * @return ApiResponse&lt;Void&gt; 删除结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        personnelFileService.removeById(id);
        return success(null, "删除成功");
    }

    // endregion

    // region 人员档案查询接口
    // ===================================
    // 人员档案查询接口
    // ===================================

    /**
     * 健康证到期提醒查询
     * <p>
     * 查询在未来指定天数内健康证即将到期的人员列表，用于GMP合规管理中的健康证续期提醒
     * </p>
     *
     * 示例请求：
     * GET /api/personnelFile/expiring?days=30
     *
     * @param days 提前天数（默认30天）
     * @return ApiResponse&lt;List&lt;PersonnelFile&gt;&gt; 健康证即将到期的人员列表
     */
// @GetMapping("/expiring")
    public ApiResponse<List<PersonnelFile>> expiring(
            @RequestParam(defaultValue = "30") int days) {
        List<PersonnelFile> list = personnelFileService.findExpiringHealthCerts(days);
        fillNameFieldsForList(list);
        return success(list);
    }

    /**
     * 根据用户ID查询人员档案
     * <p>
     * 返回人员档案详情，包含关联的用户名、部门名、岗位名及证书列表
     * </p>
     *
     * 示例请求：
     * GET /api/personnelFile/byUser/1
     *
     * @param userId 用户ID（路径参数）
     * @return ApiResponse&lt;PersonnelFile&gt; 人员档案详情
     */
// @GetMapping("/byUser/{userId}")
    public ApiResponse<PersonnelFile> getByUserId(@PathVariable Long userId) {
        PersonnelFile file = personnelFileService.findByUserId(userId);
        if (file == null) {
            return error("人员档案不存在");
        }
        fillNameFields(file);
        file.setCertificates(personnelCertificateService.getByPersonnelFileId(file.getPersonnelFileId()));
        return success(file);
    }

    // endregion

    // region 文件管理接口
    // ===================================
    // 文件管理接口
    // ===================================

    /**
     * 上传健康档案文件
     *
     * 示例请求：
     * POST /api/personnelFile/1/health-files
     * Content-Type: multipart/form-data
     * body:
     *   file: [选择文件]
     *   description: 体检报告
     *
     * @param id 人员档案ID（路径参数）
     * @param file 文件对象（multipart/form-data，字段名 file）
     * @param customPath 自定义存储路径（可选）
     * @param description 文件描述（可选）
     * @return ApiResponse&lt;FileInfo&gt; 上传的文件信息
     */
// @PostMapping("/{id}/health-files")
    public ApiResponse<FileInfo> uploadHealthFile(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String customPath,
            @RequestParam(required = false) String description) throws Exception {
        PersonnelFile pf = personnelFileService.getById(id);
        if (pf == null) {
            return error("人员档案不存在");
        }
        String entityName = pf.getName() != null ? pf.getName() : "人员" + id;
        FileInfo fileInfo = fileInfoService.uploadFileWithBusinessPath(
                file, "PERSONNEL_HEALTH_FILE", id, entityName, description, customPath);
        return success(fileInfo, "文件上传成功");
    }

    /**
     * 获取健康档案文件列表
     *
     * 示例请求：
     * GET /api/personnelFile/1/health-files
     *
     * @param id 人员档案ID（路径参数）
     * @return ApiResponse&lt;List&lt;FileInfo&gt;&gt; 健康档案文件列表
     */
// @GetMapping("/{id}/health-files")
    public ApiResponse<List<FileInfo>> getHealthFiles(@PathVariable Long id) {
        List<FileInfo> files = fileInfoService.getFilesByBusiness(id, "PERSONNEL_HEALTH_FILE", null);
        return success(files);
    }

    /**
     * 删除健康档案文件
     *
     * 示例请求：
     * DELETE /api/personnelFile/1/health-files/101
     *
     * @param id 人员档案ID（路径参数）
     * @param fileId 文件ID（路径参数）
     * @return ApiResponse&lt;Void&gt; 删除结果
     */
// @DeleteMapping("/{id}/health-files/{fileId}")
    public ApiResponse<Void> deleteHealthFile(@PathVariable Long id, @PathVariable Long fileId) {
        fileInfoService.deleteFile(fileId);
        return success(null, "删除成功");
    }

    /**
     * 上传附件文件
     *
     * 示例请求：
     * POST /api/personnelFile/1/attachments
     * Content-Type: multipart/form-data
     * body:
     *   file: [选择文件]
     *   description: 学历证书
     *
     * @param id 人员档案ID（路径参数）
     * @param file 文件对象（multipart/form-data，字段名 file）
     * @param customPath 自定义存储路径（可选）
     * @param description 文件描述（可选）
     * @return ApiResponse&lt;FileInfo&gt; 上传的文件信息
     */
// @PostMapping("/{id}/attachments")
    public ApiResponse<FileInfo> uploadAttachment(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String customPath,
            @RequestParam(required = false) String description) throws Exception {
        PersonnelFile pf = personnelFileService.getById(id);
        if (pf == null) {
            return error("人员档案不存在");
        }
        String entityName = pf.getName() != null ? pf.getName() : "人员" + id;
        FileInfo fileInfo = fileInfoService.uploadFileWithBusinessPath(
                file, "PERSONNEL_ATTACHMENT", id, entityName, description, customPath);
        return success(fileInfo, "文件上传成功");
    }

    /**
     * 获取附件文件列表
     *
     * 示例请求：
     * GET /api/personnelFile/1/attachments
     *
     * @param id 人员档案ID（路径参数）
     * @return ApiResponse&lt;List&lt;FileInfo&gt;&gt; 附件文件列表
     */
// @GetMapping("/{id}/attachments")
    public ApiResponse<List<FileInfo>> getAttachments(@PathVariable Long id) {
        List<FileInfo> files = fileInfoService.getFilesByBusiness(id, "PERSONNEL_ATTACHMENT", null);
        return success(files);
    }

    /**
     * 删除附件文件
     *
     * 示例请求：
     * DELETE /api/personnelFile/1/attachments/101
     *
     * @param id 人员档案ID（路径参数）
     * @param fileId 文件ID（路径参数）
     * @return ApiResponse&lt;Void&gt; 删除结果
     */
// @DeleteMapping("/{id}/attachments/{fileId}")
    public ApiResponse<Void> deleteAttachment(@PathVariable Long id, @PathVariable Long fileId) {
        fileInfoService.deleteFile(fileId);
        return success(null, "删除成功");
    }

    // endregion

    // region 私有辅助方法
    // ===================================
    // 私有辅助方法
    // ===================================

    /**
     * 填充关联名称字段
     *
     * @param file 人员档案对象
     */
    private void fillNameFields(PersonnelFile file) {
        if (file == null) return;
        if (file.getUserId() != null) {
            User user = userService.getById(file.getUserId());
            if (user != null) {
                file.setUserName(user.getUserName());
            }
        }
        if (file.getPositionId() != null) {
            Position position = positionService.getById(file.getPositionId());
            if (position != null) {
                file.setPositionName(position.getPositionName());
            }
        }
        if (file.getDepartmentId() != null) {
            Department department = departmentService.getById(file.getDepartmentId());
            if (department != null) {
                file.setDepartmentName(department.getDepartmentName());
            }
        }
    }

    /**
     * 批量填充关联名称字段
     *
     * @param list 人员档案列表
     */
    private void fillNameFieldsForList(List<PersonnelFile> list) {
        if (list == null) return;
        for (PersonnelFile file : list) {
            fillNameFields(file);
        }
    }

    // endregion
}
