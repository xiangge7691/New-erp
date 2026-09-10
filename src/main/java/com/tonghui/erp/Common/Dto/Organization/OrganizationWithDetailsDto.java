package com.tonghui.erp.Common.Dto.Organization;

import com.tonghui.erp.Data.Entity.Organization;
import com.tonghui.erp.Data.Entity.OrganizationCertificate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

/**
 * 机构信息扩展数据传输对象
 * <p>
 * 包含机构基础信息及关联的证书列表、许可证扫描件等子表数据
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrganizationWithDetailsDto extends Organization {

    /**
     * 该机构的证书列表
     */
    private List<OrganizationCertificate> certificates;
}
