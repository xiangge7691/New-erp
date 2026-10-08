package com.tonghui.erp.Data.mapper;

import com.tonghui.erp.Common.Dto.Purchase.PurchaseAnnualAggDto;
import com.tonghui.erp.Data.Entity.MaterialRequisitionSlipDetail;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 物料领料单明细数据访问Mapper接口
 */
public interface MaterialRequisitionSlipDetailMapper extends BaseMapper<MaterialRequisitionSlipDetail> {

    /**
     * 根据领料单ID物理删除所有明细
     *
     * @param slipId 领料单ID
     * @return 删除的记录数
     */
    @Delete("DELETE FROM material_requisition_slip_detail WHERE slip_id = #{slipId}")
    int physicalDeleteBySlipId(@Param("slipId") Long slipId);

    /**
     * 按时间范围聚合领料数据（年度采购统计·领料侧）
     * <p>
     * 以领料单申请时间 apply_time 过滤（左闭右开，endExclusive 为次日零点），按物料分组汇总；
     * 数量口径：实发数量（空回退请领数量），金额口径：数量*单价。
     * 手写 is_deleted = 0，绕过全局软删除过滤的自动追加
     * </p>
     *
     * @param start        开始时间（含，当天零点）
     * @param endExclusive 结束时间（不含，次日零点）
     * @return 按物料分组的领料汇总列表
     */
    @Select("SELECT d.material_id AS materialId, d.material_code AS materialCode, " +
            "SUM(COALESCE(d.actual_qty, d.apply_qty)) AS quantity, " +
            "SUM(COALESCE(d.actual_qty, d.apply_qty) * COALESCE(d.unit_price, 0)) AS amount " +
            "FROM material_requisition_slip s " +
            "JOIN material_requisition_slip_detail d ON d.slip_id = s.id " +
            "WHERE s.apply_time >= #{start} AND s.apply_time < #{endExclusive} " +
            "AND s.is_deleted = 0 AND d.is_deleted = 0 " +
            "GROUP BY d.material_id, d.material_code")
    List<PurchaseAnnualAggDto> selectAnnualAggByApplyTimeRange(@Param("start") LocalDateTime start,
                                                               @Param("endExclusive") LocalDateTime endExclusive);
}
