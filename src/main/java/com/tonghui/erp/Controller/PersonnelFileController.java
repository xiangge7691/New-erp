package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.utils.EntityUtils;
import com.tonghui.erp.Data.Entity.PersonnelFile;
import com.tonghui.erp.Data.Entity.Position;
import com.tonghui.erp.Data.Entity.Department;
import com.tonghui.erp.Data.Entity.User;
import com.tonghui.erp.Service.PersonnelFileService;
import com.tonghui.erp.Service.PositionService;
import com.tonghui.erp.Service.DepartmentService;
import com.tonghui.erp.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
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
    private PositionService positionService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private UserService userService;

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
        try {
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
            wrapper.orderByDesc("created_at");

            Page<PersonnelFile> pageResult = personnelFileService.page(page, wrapper);
            fillNameFieldsForList(pageResult.getRecords());

            PagedResult<PersonnelFile> pagedResult = new PagedResult<>();
            pagedResult.setItems(pageResult.getRecords());
            pagedResult.setTotalCount(pageResult.getTotal());
            pagedResult.setPageIndex(pageIndex);
            pagedResult.setPageSize(pageSize);

            return success(pagedResult);
        } catch (Exception e) {
            return exception(e, "操作");
        }
    }

    /**
     * 根据ID查询人员档案详情
     */
    @GetMapping("/{id}")
    public ApiResponse<PersonnelFile> getById(@PathVariable Long id) {
        try {
            PersonnelFile file = personnelFileService.getById(id);
            if (file == null) {
                return error("人员档案不存在");
            }
            fillNameFields(file);
            return success(file);
        } catch (Exception e) {
            return exception(e, "操作");
        }
    }

    /**
     * 新增人员档案
     */
    @PostMapping
    public ApiResponse<PersonnelFile> create(@RequestBody PersonnelFile personnelFile) {
        try {
            personnelFile.setCreatedBy(EntityUtils.getCurrentUserId());
            personnelFile.setCreatedAt(LocalDateTime.now());
            personnelFile.setIsDeleted(0);
            personnelFile.setVersion(0);
            personnelFileService.save(personnelFile);
            return success(personnelFile, "新增成功");
        } catch (Exception e) {
            return exception(e, "操作");
        }
    }

    /**
     * 修改人员档案
     */
    @PutMapping("/{id}")
    public ApiResponse<PersonnelFile> update(@PathVariable Long id, @RequestBody PersonnelFile personnelFile) {
        try {
            PersonnelFile existing = personnelFileService.getById(id);
            if (existing == null) {
                return error("人员档案不存在");
            }
            personnelFile.setPersonnelFileId(id);
            personnelFile.setUpdatedBy(EntityUtils.getCurrentUserId());
            personnelFile.setUpdatedAt(LocalDateTime.now());
            personnelFileService.updateById(personnelFile);
            return success(personnelFile, "修改成功");
        } catch (Exception e) {
            return exception(e, "操作");
        }
    }

    /**
     * 删除人员档案
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        try {
            personnelFileService.removeById(id);
            return success(null, "删除成功");
        } catch (Exception e) {
            return exception(e, "操作");
        }
    }

    /**
     * 健康证到期提醒查询
     */
    @GetMapping("/expiring")
    public ApiResponse<List<PersonnelFile>> expiring(
            @RequestParam(defaultValue = "30") int days) {
        try {
            List<PersonnelFile> list = personnelFileService.findExpiringHealthCerts(days);
            fillNameFieldsForList(list);
            return success(list);
        } catch (Exception e) {
            return exception(e, "操作");
        }
    }

    /**
     * 根据用户ID查询人员档案
     */
    @GetMapping("/byUser/{userId}")
    public ApiResponse<PersonnelFile> getByUserId(@PathVariable Long userId) {
        try {
            PersonnelFile file = personnelFileService.findByUserId(userId);
            if (file == null) {
                return error("人员档案不存在");
            }
            fillNameFields(file);
            return success(file);
        } catch (Exception e) {
            return exception(e, "操作");
        }
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
}
