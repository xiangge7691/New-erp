package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Data.Entity.PurchaseOrders;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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

}
