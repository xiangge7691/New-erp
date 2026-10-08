package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Common.Dto.Purchase.PurchaseAnnualAggDto;
import com.tonghui.erp.Data.Entity.PurchaseOrders;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 采购订单数据访问Mapper接口
 */
public interface PurchaseOrdersMapper extends BaseMapper<PurchaseOrders> {

    /**
     * 根据编号前缀查询最大采购订单编号（用于自动生成编号）
     * <p>
     * 使用原生SQL查询，绕过全局软删除过滤，避免与已软删除订单的编号冲突
     * </p>
     *
     * @param prefix 编号前缀，如 CG20260803
     * @return 最大采购订单编号，无记录时返回null
     */
    @Select("SELECT purchase_number FROM purchase_orders WHERE purchase_number LIKE CONCAT(#{prefix}, '%') ORDER BY purchase_number DESC LIMIT 1")
    String selectMaxPurchaseNumberByPrefix(@Param("prefix") String prefix);

    /**
     * 按时间范围聚合采购到货数据（年度采购统计·采购侧）
     * <p>
     * 以采购订单业务日期 processing_date 过滤，按物料分组汇总；
     * 数量口径：实际到货数量（空回退采购数量），金额口径：明细金额 amount。
     * 手写 is_deleted = 0，绕过全局软删除过滤的自动追加
     * </p>
     *
     * @param startDate 开始日期（含当天）
     * @param endDate   结束日期（含当天）
     * @return 按物料分组的采购汇总列表
     */
    @Select("SELECT poi.material_id AS materialId, poi.material_code AS materialCode, " +
            "SUM(COALESCE(poi.actual_arrival_qty, poi.purchase_quantity)) AS quantity, " +
            "SUM(COALESCE(poi.amount, 0)) AS amount " +
            "FROM purchase_orders po " +
            "JOIN purchase_order_items poi ON poi.order_id = po.id " +
            "WHERE po.processing_date BETWEEN #{startDate} AND #{endDate} " +
            "AND po.is_deleted = 0 AND poi.is_deleted = 0 " +
            "GROUP BY poi.material_id, poi.material_code")
    List<PurchaseAnnualAggDto> selectAnnualAggByDateRange(@Param("startDate") LocalDate startDate,
                                                          @Param("endDate") LocalDate endDate);

}
