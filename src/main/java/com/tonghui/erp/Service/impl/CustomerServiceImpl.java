package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.utils.CodeUniqueChecker;
import com.tonghui.erp.Data.Entity.Customer;
import com.tonghui.erp.Data.mapper.CustomerMapper;
import com.tonghui.erp.Service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 客户信息服务实现类
 * <p>
 * 实现CustomerService接口，提供客户信息的编号生成、唯一性校验及查询业务逻辑，
 * 客户编号通过对Mapper原生SQL查询绕过软删除过滤，保证编号不与已删除记录冲突
 * </p>
 */
@Service
public class CustomerServiceImpl extends ServiceImpl<CustomerMapper, Customer> implements CustomerService {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 序列号生成服务
     */
    @Autowired
    private SequenceServiceImpl sequenceService;

    // endregion

    // region 编号生成与校验
    // ===================================
    // 编号生成与校验
    // ===================================

    /**
     * 生成客户编号
     * <p>
     * 编号格式：4位数字递增（0001、0002...），不带前缀，全局递增
     * </p>
     *
     * @return 生成的唯一客户编号
     */
    @Override
    public String generateCode() {
        return sequenceService.generateCustomerCode();
    }

    /**
     * 校验客户编号是否唯一
     * <p>
     * 通过Mapper原生SQL统计包含软删除记录在内的全部记录，
     * 避免已软删除记录占用的编号被误判为可用导致唯一索引冲突
     * </p>
     *
     * @param code      客户编号
     * @param excludeId 需要排除的记录ID（修改时传入，新增时传null）
     * @return 唯一返回true，否则返回false
     */
    @Override
    public boolean isCodeUnique(String code, Long excludeId) {
        return CodeUniqueChecker.isCodeUnique(code, excludeId, baseMapper::countByCodeIncludeDeleted);
    }

    // endregion

    // region 查询
    // ===================================
    // 查询
    // ===================================

    /**
     * 查询客户列表（支持多条件 + 分页）
     *
     * @param keyword   关键字（模糊匹配客户编号/名称/联系人/电话，可选）
     * @param type      类型（本院/外院，可选）
     * @param status    状态（启用/停用，可选）
     * @param pageIndex 页码（从0开始）
     * @param pageSize  每页大小
     * @return 分页结果
     */
    @Override
    public Page<Customer> queryCustomers(String keyword, String type, String status, int pageIndex, int pageSize) {
        QueryWrapper<Customer> wrapper = new QueryWrapper<>();
        wrapper.eq("is_deleted", 0);

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w
                    .like("customer_code", keyword)
                    .or().like("customer_name", keyword)
                    .or().like("contact_person", keyword)
                    .or().like("phone", keyword));
        }
        if (type != null && !type.isEmpty()) {
            wrapper.eq("type", type);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("customer_code");

        Page<Customer> page = new Page<>(pageIndex + 1, pageSize);
        return baseMapper.selectPage(page, wrapper);
    }

    // endregion
}
