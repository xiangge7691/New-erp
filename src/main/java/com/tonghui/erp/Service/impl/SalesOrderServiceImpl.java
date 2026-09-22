package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.utils.CodeUniqueChecker;
import com.tonghui.erp.Data.Entity.SalesOrder;
import com.tonghui.erp.Data.Entity.StockOut;
import com.tonghui.erp.Data.mapper.SalesOrderMapper;
import com.tonghui.erp.Data.mapper.StockOutMapper;
import com.tonghui.erp.Service.SalesOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成品出库台账服务实现类
 * <p>
 * 实现SalesOrderService接口，提供成品出库台账的单号生成、唯一性校验、
 * 查询统计及作废等业务逻辑。单号生成通过对Mapper原生SQL查询绕过软删除过滤，
 * 保证单号不与已删除记录冲突
 * </p>
 */
@Service
public class SalesOrderServiceImpl extends ServiceImpl<SalesOrderMapper, SalesOrder> implements SalesOrderService {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 序列号生成服务
     */
    @Autowired
    private SequenceServiceImpl sequenceService;

    /**
     * 出库单Mapper（用于作废联动）
     */
    @Autowired
    private StockOutMapper stockOutMapper;

    // endregion

    // region 编号生成与校验
    // ===================================
    // 编号生成与校验
    // ===================================

    /**
     * 生成成品出库单号
     * <p>
     * 编号格式：CPCK-YYYYMMDD-NNN，当天全局最大+1
     * </p>
     *
     * @return 生成的唯一成品出库单号
     */
    @Override
    public String generateCode() {
        return sequenceService.generateSalesOrderCode();
    }

    /**
     * 校验成品出库单号是否唯一
     * <p>
     * 通过Mapper原生SQL统计包含软删除记录在内的全部记录，
     * 避免已软删除记录占用的单号被误判为可用导致唯一索引冲突
     * </p>
     *
     * @param code      成品出库单号
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
     * 查询成品出库台账列表（支持多条件 + 分页）
     *
     * @param keyword   关键字（模糊匹配单号/客户/制剂，可选）
     * @param status    状态（已开单/已出库/已作废，可选）
     * @param customerId 客户ID（可选）
     * @param startDate 出库日期起始（可选）
     * @param endDate   出库日期结束（可选）
     * @param pageIndex 页码（从0开始）
     * @param pageSize  每页大小
     * @return 分页结果
     */
    @Override
    public Page<SalesOrder> querySalesOrders(String keyword, String status, Long customerId,
                                              LocalDateTime startDate, LocalDateTime endDate,
                                              int pageIndex, int pageSize) {
        QueryWrapper<SalesOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("is_deleted", 0);

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w
                    .like("sales_order_code", keyword)
                    .or().like("preparation_name", keyword));
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq("status", status);
        }
        if (customerId != null) {
            wrapper.eq("customer_id", customerId);
        }
        if (startDate != null) {
            wrapper.ge("sales_order_date", startDate);
        }
        if (endDate != null) {
            wrapper.le("sales_order_date", endDate);
        }
        wrapper.orderByDesc("sales_order_code");

        Page<SalesOrder> page = new Page<>(pageIndex + 1, pageSize);
        return baseMapper.selectPage(page, wrapper);
    }

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
    @Override
    public BigDecimal[] getStatistics(String keyword, String status, Long customerId,
                                       LocalDateTime startDate, LocalDateTime endDate) {
        QueryWrapper<SalesOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("is_deleted", 0);
        wrapper.ne("status", "已作废");

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w
                    .like("sales_order_code", keyword)
                    .or().like("preparation_name", keyword));
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq("status", status);
        }
        if (customerId != null) {
            wrapper.eq("customer_id", customerId);
        }
        if (startDate != null) {
            wrapper.ge("sales_order_date", startDate);
        }
        if (endDate != null) {
            wrapper.le("sales_order_date", endDate);
        }

        java.util.List<SalesOrder> orders = baseMapper.selectList(wrapper);
        BigDecimal totalAmount = orders.stream()
                .map(o -> o.getAmount() != null ? o.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new BigDecimal[]{new BigDecimal(orders.size()), totalAmount};
    }

    // endregion

    // region 作废
    // ===================================
    // 作废
    // ===================================

    /**
     * 作废成品出库台账
     * <p>
     * 将台账状态设置为"已作废"，同时把出库管理中对应"待确认"的出库单置为"已取消"
     * </p>
     *
     * @param salesOrderId 台账ID
     * @return 是否作废成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean voidOrder(Long salesOrderId) {
        SalesOrder salesOrder = getById(salesOrderId);
        if (salesOrder == null || "已作废".equals(salesOrder.getStatus())) {
            return false;
        }

        // 更新台账状态为已作废
        salesOrder.setStatus("已作废");
        updateById(salesOrder);

        // 联动：将关联出库单中仍为"草稿"的置为"已取消"
        if (salesOrder.getRelatedOutCode() != null && !salesOrder.getRelatedOutCode().isEmpty()) {
            QueryWrapper<StockOut> outWrapper = new QueryWrapper<>();
            outWrapper.eq("out_code", salesOrder.getRelatedOutCode());
            outWrapper.eq("out_status", "草稿");
            StockOut stockOut = stockOutMapper.selectOne(outWrapper);
            if (stockOut != null) {
                stockOut.setOutStatus("已取消");
                stockOutMapper.updateById(stockOut);
            }
        }

        return true;
    }

    // endregion
}
