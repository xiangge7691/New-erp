package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Data.Entity.Organization;
import com.tonghui.erp.Common.Dto.Organization.OrganizationWithDetailsDto;
import java.util.List;

/**
 * 机构信息服务接口
 * <p>
 * 提供机构信息的CRUD操作、带证书子表的详情查询、许可证状态实时计算及到期预警查询
 * </p>
 */
public interface OrganizationService extends IService<Organization> {

    // region 查询操作
    // ===================================
    // 查询操作
    // ===================================

    /**
     * 获取当前机构信息（含证书列表+实时状态计算）
     * <p>
     * 单机构模式，始终返回唯一的一条机构记录。
     * 许可证状态根据 expiryDate 实时计算：有效/即将到期/已过期。
     * </p>
     *
     * @return 机构信息（含证书列表），不存在时返回null
     */
    OrganizationWithDetailsDto getWithDetails();

    /**
     * 查询即将到期和已过期的机构列表
     * <p>
     * 用于首页待办提醒，查询在未来指定天数内到期的机构
     * </p>
     *
     * @param days 提前天数（默认30天）
     * @return 到期预警机构列表
     */
    List<Organization> findExpiringOrganizations(int days);

    // endregion

    // region 更新操作
    // ===================================
    // 更新操作
    // ===================================

    /**
     * 更新机构信息
     * <p>
     * 自动计算有效期至 = 发证日期 + 5年
     * </p>
     *
     * @param organization 机构信息（不含ID，使用当前机构记录）
     * @return 更新后的机构信息
     */
    Organization updateOrganization(Organization organization);

    // endregion
}
