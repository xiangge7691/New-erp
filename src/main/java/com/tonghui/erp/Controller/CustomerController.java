package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.Customer;
import com.tonghui.erp.Service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 客户信息控制器
 * <p>
 * 提供客户信息的增删改查、高级查询以及客户编号自动生成等功能
 * </p>
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────────┬────────┬──────────────────────────────┐
 * │ #  │ 接口                                 │ 方法   │ 说明                         │
 * ├────┼──────────────────────────────────────┼────────┼──────────────────────────────┤
 * │ 1  │ /api/customer                        │ GET    │ 分页查询客户列表             │
 * │ 2  │ /api/customer/{id}                   │ GET    │ 获取客户详情                 │
 * │ 3  │ /api/customer                        │ POST   │ 新增客户                     │
 * │ 4  │ /api/customer/{id}                   │ PUT    │ 修改客户                     │
 * │ 5  │ /api/customer/{id}                   │ DELETE │ 删除客户                     │
 * │ 6  │ /api/customer/search                 │ GET    │ 高级查询客户（支持多条件）   │
 * │ 7  │ /api/customer/generate-code          │ GET    │ 自动生成客户编号             │
 * └────┴──────────────────────────────────────┴────────┴──────────────────────────────┘
 */
@RestController
@RequestMapping("/api/customer")
public class CustomerController extends BaseCrudController<Customer, Customer, Long> {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 客户信息服务
     */
    @Autowired
    private CustomerService customerService;

    // endregion

    // region 基础CRUD实现
    // ===================================
    // 基础CRUD实现
    // ===================================

    @Override
    protected PagedResult<Customer> getAllData(int pageIndex, int pageSize) {
        int safePageIndex = Math.max(0, pageIndex);
        int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);

        Page<Customer> pageResult = customerService.queryCustomers(null, null, null, safePageIndex, safePageSize);

        PagedResult<Customer> pagedResult = new PagedResult<>();
        pagedResult.setItems(pageResult.getRecords());
        pagedResult.setTotalCount(pageResult.getTotal());
        pagedResult.setPageIndex(safePageIndex);
        pagedResult.setPageSize((int) pageResult.getSize());

        return pagedResult;
    }

    @Override
    protected Customer getDataById(Long id) {
        return customerService.getById(id);
    }

    @Override
    protected Customer doCreate(Customer customer) {
        // 自动生成客户编号（如未手动填写）
        if (customer.getCustomerCode() == null || customer.getCustomerCode().isEmpty()) {
            customer.setCustomerCode(customerService.generateCode());
        }
        // 校验编号唯一性
        if (!customerService.isCodeUnique(customer.getCustomerCode(), null)) {
            throw new RuntimeException("客户编号已存在");
        }
        customerService.save(customer);
        return customer;
    }

    @Override
    protected Customer doUpdate(Long id, Customer customer) {
        customer.setCustomerId(id);
        customerService.updateById(customer);
        return customer;
    }

    @Override
    protected boolean doDelete(Long id) {
        return customerService.removeById(id);
    }

    // endregion

    // region 高级查询
    // ===================================
    // 高级查询
    // ===================================

    /**
     * 高级查询客户（支持多条件 + 分页）
     * <p>
     * 可选查询条件：keyword（模糊匹配客户编号/名称/联系人/电话）、
     * type（本院/外院）、status（启用/停用）
     * </p>
     *
     * 示例请求：
     * GET /api/customer/search?pageIndex=0&pageSize=20&keyword=邵阳
     * GET /api/customer/search?pageIndex=0&pageSize=20&type=本院&status=启用
     *
     * @param keyword   关键字（模糊匹配客户编号/名称/联系人/电话，可选）
     * @param type      类型（本院/外院，可选）
     * @param status    状态（启用/停用，可选）
     * @param pageIndex 页码（从0开始）
     * @param pageSize  每页大小
     * @return ApiResponse&lt;PagedResult&lt;Customer&gt;&gt; 分页结果
     */
    @GetMapping("/search")
    public ApiResponse<PagedResult<Customer>> searchCustomers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "20") int pageSize) {
        try {
            int safePageIndex = Math.max(0, pageIndex);
            int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);

            Page<Customer> pageResult = customerService.queryCustomers(keyword, type, status, safePageIndex, safePageSize);

            PagedResult<Customer> pagedResult = new PagedResult<>();
            pagedResult.setItems(pageResult.getRecords());
            pagedResult.setTotalCount(pageResult.getTotal());
            pagedResult.setPageIndex(safePageIndex);
            pagedResult.setPageSize((int) pageResult.getSize());

            return success(pagedResult);
        } catch (Exception ex) {
            return exception(ex, "高级查询客户");
        }
    }

    // endregion

    // region 客户编号生成
    // ===================================
    // 客户编号生成
    // ===================================

    /**
     * 自动生成客户编号
     * <p>
     * 根据系统规则自动生成唯一的客户编号（4位数字递增，如0001）
     * </p>
     *
     * 示例请求：
     * GET /api/customer/generate-code
     *
     * @return ApiResponse&lt;String&gt; 客户编号
     */
    @GetMapping("/generate-code")
    public ApiResponse<String> generateCustomerCode() {
        try {
            String code = customerService.generateCode();
            return success(code, "客户编号生成成功");
        } catch (Exception ex) {
            return exception(ex, "生成客户编号");
        }
    }

    // endregion
}
