package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Data.Entity.PersonnelCertificate;
import com.tonghui.erp.Service.PersonnelCertificateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 人员证书控制器
 */
@RestController
@RequestMapping("/api/personnelCertificate")
public class PersonnelCertificateController extends BaseController {

    @Autowired
    private PersonnelCertificateService personnelCertificateService;

    /**
     * 根据人员档案ID查询所有证书
     */
    @GetMapping("/personnel/{personnelFileId}")
    public ApiResponse<List<PersonnelCertificate>> getByPersonnelFileId(@PathVariable Long personnelFileId) {
        List<PersonnelCertificate> list = personnelCertificateService.getByPersonnelFileId(personnelFileId);
        return success(list);
    }

    /**
     * 批量保存人员证书（先删后插）
     */
    @PostMapping("/personnel/{personnelFileId}")
    public ApiResponse<Void> saveCertificates(@PathVariable Long personnelFileId,
                                              @RequestBody List<PersonnelCertificate> certificates) {
        personnelCertificateService.saveCertificates(personnelFileId, certificates);
        return success(null, "保存成功");
    }

    /**
     * 单独新增一条证书
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
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        personnelCertificateService.removeById(id);
        return success(null, "删除成功");
    }
}
