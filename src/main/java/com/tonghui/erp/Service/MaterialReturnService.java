package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Dto.MaterialReturn.MaterialResendRequestDto;
import com.tonghui.erp.Common.Dto.MaterialReturn.MaterialReturnItemDto;
import com.tonghui.erp.Common.Dto.MaterialReturn.MaterialReturnQueryDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.AcceptanceOrder;

/**
 * 物料退货管理服务接口
 * <p>
 * 采购部门集中处理不合格退货的唯一工作台：
 * 查询退货明细行、退货、取消、重新发货（合并生成新验收单并留痕）
 * </p>
 */
public interface MaterialReturnService {

    // region 查询
    // ===================================
    // 查询
    // ===================================

    /**
     * 分页查询退货明细行
     * <p>数据来源：验收明细中状态为 待退货/已退货/已重发/已取消 的行，
     * 关联验收单号、采购订单编号、制剂名称、供应商与最近重发时间；
     * 支持按退货来源（采购退货/领料退货）筛选，来源由所属验收单来源类型派生</p>
     *
     * @param query 查询条件（退货来源/状态/退货原因/供应商/关键字/日期）
     * @return 退货明细分页结果
     */
    PagedResult<MaterialReturnItemDto> searchItems(MaterialReturnQueryDto query);

    // endregion

    // region 状态操作
    // ===================================
    // 状态操作
    // ===================================

    /**
     * 退货：明细 待退货 → 已退货（二次确认后调用）
     * <p>同步回写关联采购订单明细状态</p>
     *
     * @param detailId 验收明细ID
     */
    void returnDetail(Long detailId);

    /**
     * 取消：明细 已退货 → 已取消（该物料取消采购，不影响整单其他明细）
     * <p>同步回写关联采购订单明细状态</p>
     *
     * @param detailId 验收明细ID
     */
    void cancelDetail(Long detailId);

    /**
     * 重新发货：同一验收单的多条已退货明细合并生成一条新验收单
     * <p>
     * 新验收单来源类型=退货重发、关联单号=原采购订单编号、记录原验收单号、初始状态运输中，
     * 每条采购数量=输入的重新发货数量（金额与标准量差值自动重算）；
     * 所选明细状态→已重发，并逐条写入重新发货操作留痕
     * </p>
     *
     * @param request 重新发货请求（验收单ID + 明细ID与重新发货数量列表）
     * @return 新生成的验收单
     */
    AcceptanceOrder resend(MaterialResendRequestDto request);

    // endregion
}
