package com.tonghui.erp.Common.Dto;

import com.tonghui.erp.Data.Entity.PersonnelFile;
import com.tonghui.erp.Data.Entity.PersonnelCertificate;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 人事档案包含证书的扩展数据传输对象
 * <p>
 * 在人事档案基础上扩展了人员资质证书列表，用于展示完整的人员档案及证书信息
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PersonnelFileWithDetailsDto extends PersonnelFile {

    /**
     * 该人员的资质证书列表
     */
    private List<PersonnelCertificate> certificates;
}
