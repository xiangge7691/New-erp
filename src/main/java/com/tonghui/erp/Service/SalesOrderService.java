package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Data.Entity.SalesOrder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成品出库台账服务接口
 * <p>
 * 提供成品出库台账的基础CRUD、单号生成、唯一性校验、统计汇总及作废等业务能力，
 * 成品出库开单后自动联动出库管理完成成品出库
 * </p>
 */
public interface SalesOrderService extends IService<SalesOrder> {

    /**
     * 生成成品出库单号
     * <p>
     * 编号格式：CPCK-YYYYMMDD-NNN，当天全局最大+1
     * </p>
     *
     * @return 生成的唯一成品出库单号
     */
    String generateCode();

    /**
     * 校验成品出库单号是否唯一
     * <p>
     * 用于新增或修改时校验单号，排除指定ID自身的记录，避免修改时误判重复
     * </p>
     *
     * @param code      成品出库单号
     * @param excludeId 需要排除的记录ID（修改时传入，新增时传null）
     * @return 唯一返回true，否则返回false
     */
    boolean isCodeUnique(String code, Long excludeId);

    /**
     * 查询成品出库台账列表（支持多条件 + 分页）
     *
     * @param keyword           关键字（模糊匹配单号/客户/制剂，可选）
     * @param status            状态（已开单/已出库/已作废，可选）
     * @param customerId        客户ID（可选）
     * @param startDate         出库日期起始（可选）
     * @param endDate           出库日期结束（可选）
     * @param pageIndex         页码（从0开始）
     * @param pageSize          每页大小
     * @return 分页结果
     */
    Page<SalesOrder> querySalesOrders(String keyword, String status, Long customerId,
                                       LocalDateTime startDate, LocalDateTime endDate,
                                       int pageIndex, int pageSize);

    /**
     * 统计成品出库汇总数据
     *
     * @param keyword    关键字（可选）
     * @param status     状态（可选）
     * @param customerId 客户ID（可选）
     * @param startDate  出库日期起始（可选）
     * @param endDate    出库日期结束（可选）
     * @return [总单数, 总金额]
     */
    BigDecimal[] getStatistics(String keyword, String status, Long customerId,
                                LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 作废成品出库台账
     * <p>
     * 将台账状态设置为"已作废"，同时把出库管理中对应"待确认"的出库单置为"已取消"
     * </p>
     *
     * @param salesOrderId 台账ID
     * @return 是否作废成功
     */
    boolean voidOrder(Long salesOrderId);
}
