package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Data.Entity.Customer;

/**
 * 客户信息服务接口
 * <p>
 * 提供客户信息的基础CRUD、客户编号生成及编号唯一性校验等业务能力，
 * 用于维护制剂成品客户的基本信息（本院/外院）
 * </p>
 */
public interface CustomerService extends IService<Customer> {

    /**
     * 生成客户编号
     * <p>
     * 编号格式：4位数字递增（0001、0002...），不带前缀，全局递增
     * </p>
     *
     * @return 生成的唯一客户编号
     */
    String generateCode();

    /**
     * 校验客户编号是否唯一
     * <p>
     * 用于新增或修改时校验编号，排除指定ID自身的记录，避免修改时误判重复
     * </p>
     *
     * @param code      客户编号
     * @param excludeId 需要排除的记录ID（修改时传入，新增时传null）
     * @return 唯一返回true，否则返回false
     */
    boolean isCodeUnique(String code, Long excludeId);

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
    Page<Customer> queryCustomers(String keyword, String type, String status, int pageIndex, int pageSize);
}
