package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.FileInfo;
import com.tonghui.erp.Data.Entity.PersonnelFile;
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

/**
 * 人员档案控制器
 */
@RestController
@RequestMapping("/api/personnelFile")
public class PersonnelFileController extends BaseController {

    @Autowired
    private PersonnelFileService personnelFileService;

    @Autowired
    private FileInfoService fileInfoService;

    @Autowired
    private PositionService positionService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private UserService userService;

    @Autowired
    private PersonnelCertificateService personnelCertificateService;

    /**
     * 分页查询人员档案列表
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

        PagedResult<PersonnelFile> pagedResult = new PagedResult<>();
        pagedResult.setItems(pageResult.getRecords());
        pagedResult.setTotalCount(pageResult.getTotal());
        pagedResult.setPageIndex(pageIndex);
        pagedResult.setPageSize(pageSize);

        return success(pagedResult);
    }

    /**
     * 根据ID查询人员档案详情
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
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        personnelFileService.removeById(id);
        return success(null, "删除成功");
    }

    /**
     * 健康证到期提醒查询
     */
    @GetMapping("/expiring")
    public ApiResponse<List<PersonnelFile>> expiring(
            @RequestParam(defaultValue = "30") int days) {
        List<PersonnelFile> list = personnelFileService.findExpiringHealthCerts(days);
        fillNameFieldsForList(list);
        return success(list);
    }

    /**
     * 根据用户ID查询人员档案
     */
    @GetMapping("/byUser/{userId}")
    public ApiResponse<PersonnelFile> getByUserId(@PathVariable Long userId) {
        PersonnelFile file = personnelFileService.findByUserId(userId);
        if (file == null) {
            return error("人员档案不存在");
        }
        fillNameFields(file);
        file.setCertificates(personnelCertificateService.getByPersonnelFileId(file.getPersonnelFileId()));
        return success(file);
    }

    /**
     * 填充关联名称字段
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
     */
    private void fillNameFieldsForList(List<PersonnelFile> list) {
        if (list == null) return;
        for (PersonnelFile file : list) {
            fillNameFields(file);
        }
    }

    // ========== 文件管理端点 ==========

    /**
     * 上传健康档案文件
     */
    @PostMapping("/{id}/health-files")
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
     */
    @GetMapping("/{id}/health-files")
    public ApiResponse<List<FileInfo>> getHealthFiles(@PathVariable Long id) {
        List<FileInfo> files = fileInfoService.getFilesByBusiness(id, "PERSONNEL_HEALTH_FILE");
        return success(files);
    }

    /**
     * 删除健康档案文件
     */
    @DeleteMapping("/{id}/health-files/{fileId}")
    public ApiResponse<Void> deleteHealthFile(@PathVariable Long id, @PathVariable Long fileId) {
        fileInfoService.deleteFile(fileId);
        return success(null, "删除成功");
    }

    /**
     * 上传附件文件
     */
    @PostMapping("/{id}/attachments")
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
     */
    @GetMapping("/{id}/attachments")
    public ApiResponse<List<FileInfo>> getAttachments(@PathVariable Long id) {
        List<FileInfo> files = fileInfoService.getFilesByBusiness(id, "PERSONNEL_ATTACHMENT");
        return success(files);
    }

    /**
     * 删除附件文件
     */
    @DeleteMapping("/{id}/attachments/{fileId}")
    public ApiResponse<Void> deleteAttachment(@PathVariable Long id, @PathVariable Long fileId) {
        fileInfoService.deleteFile(fileId);
        return success(null, "删除成功");
    }
}
