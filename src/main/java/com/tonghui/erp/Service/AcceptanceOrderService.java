package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Stock.AcceptanceInboundRequest;
import com.tonghui.erp.Common.Dto.Stock.AcceptanceWithDetailsDto;
import com.tonghui.erp.Common.Dto.Stock.StockInWithNamesDto;
import com.tonghui.erp.Data.Entity.AcceptanceDetail;
import com.tonghui.erp.Data.Entity.AcceptanceOrder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 货物验收单业务接口
 * <p>
 * 提供验收单的增删改查、明细管理、行级状态流转
 * （确认到货/逐行初验/逐行检验/部分验收拆行/整单入库）及库存联动能力；
 * 退货后的重新发货由物料退货管理模块承接
 * </p>
 */
public interface AcceptanceOrderService extends IService<AcceptanceOrder> {

    // region 基础操作
    // ===================================
    // 基础操作
    // ===================================

    /**
     * 新增验收单（包含明细）
     *
     * @param acceptance 验收单实体
     * @param details    验收明细列表
     */
    void addAcceptance(AcceptanceOrder acceptance, List<AcceptanceDetail> details);

    /**
     * 更新验收单（包含明细，已入库状态禁止更新）
     *
     * @param acceptance 验收单实体
     * @param details    验收明细列表
     */
    void updateAcceptance(AcceptanceOrder acceptance, List<AcceptanceDetail> details);

    /**
     * 删除验收单（同时删除明细，已入库状态禁止删除）
     *
     * @param acceptanceId 验收单ID
     */
    void deleteAcceptance(Long acceptanceId);

    // endregion

    // region 查询操作
    // ===================================
    // 查询操作
    // ===================================

    /**
     * 根据ID查询验收单
     *
     * @param acceptanceId 验收单ID
     * @return 验收单实体
     */
    AcceptanceOrder getAcceptanceById(Long acceptanceId);

    /**
     * 根据验收单号查询验收单
     *
     * @param acceptanceCode 验收单号
     * @return 验收单实体
     */
    AcceptanceOrder getAcceptanceByCode(String acceptanceCode);

    /**
     * 查询所有验收单
     *
     * @return 验收单集合
     */
    List<AcceptanceOrder> getAllAcceptances();

    // endregion

    // region 验收明细操作
    // ===================================
    // 验收明细操作
    // ===================================

    /**
     * 根据验收单ID获取明细列表
     *
     * @param acceptanceId 验收单ID
     * @return 明细列表
     */
    List<AcceptanceDetail> getDetailsByAcceptanceId(Long acceptanceId);

    /**
     * 批量更新验收明细（批号/单价/实际到货数量等）
     * <p>携带实际到货数量或单价时，自动按 实际到货数量 × 单价 重算金额</p>
     *
     * @param details 验收明细列表
     */
    void updateAcceptanceDetails(List<AcceptanceDetail> details);

    /**
     * 删除验收明细
     *
     * @param detailId 明细ID
     */
    void deleteAcceptanceDetail(Long detailId);

    // endregion

    // region 单号生成
    // ===================================
    // 单号生成
    // ===================================

    /**
     * 生成验收单号（格式 YS-YYYYMMDD-NNN）
     *
     * @return 验收单号
     */
    String generateAcceptanceCode();

    // endregion

    // region 高级查询
    // ===================================
    // 高级查询
    // ===================================

    /**
     * 高级查询验收单（支持分页、状态/来源筛选）
     *
     * @param acceptance 查询条件
     * @param keyword    关键字（对验收编号、验收标题进行模糊匹配，可选）
     * @param pageIndex  页码
     * @param pageSize   每页大小
     * @return 分页结果
     */
    Page<AcceptanceOrder> queryAcceptances(AcceptanceOrder acceptance, String keyword, int pageIndex, int pageSize);

    /**
     * 高级查询验收单（包含明细子表）
     *
     * @param acceptance 查询条件
     * @param keyword    关键字（对验收编号、验收标题进行模糊匹配，可选）
     * @param pageIndex  页码
     * @param pageSize   每页大小
     * @return 分页结果（包含明细）
     */
    PagedResult<AcceptanceWithDetailsDto> searchWithDetails(AcceptanceOrder acceptance, String keyword, int pageIndex, int pageSize);

    // endregion

    // region 状态流转
    // ===================================
    // 状态流转
    // ===================================

    /**
     * 确认到货：运输中 → 验收中（明细进入待初验）
     *
     * @param acceptanceId 验收单ID
     */
    void confirmArrival(Long acceptanceId);

    /**
     * 初验处理（逐行）：所选明细行 待初验 → 合格：待检验 / 不合格：待退货
     * <p>
     * detailIds 为空时自动推进该单全部「待初验」行（批量推进）；
     * 主单状态按全部明细汇总派生，备注自动追加
     * </p>
     *
     * @param acceptanceId 验收单ID
     * @param detailIds    目标明细行ID列表，空/null表示全部待初验行
     * @param pass         是否合格
     * @param remark       初验备注说明
     */
    void inspect(Long acceptanceId, List<Long> detailIds, boolean pass, String remark);

    /**
     * 检验处理（逐行）：所选明细行 待检验 → 合格：待入库 / 不合格：待退货
     * <p>
     * detailIds 为空时自动推进该单全部「待检验」行（批量推进）；
     * 合格时校验所选行批号必填；入库由 {@link #inbound} 在全部明细终结后整单执行
     * </p>
     *
     * @param acceptanceId 验收单ID
     * @param detailIds    目标明细行ID列表，空/null表示全部待检验行
     * @param pass         是否合格
     * @param remark       检验备注说明
     */
    void qualityCheck(Long acceptanceId, List<Long> detailIds, boolean pass, String remark);

    /**
     * 部分验收（拆行）：将一行物料拆为「验收子行 + 退货子行」
     * <p>
     * 验收数量 + 退货数量必须等于原行采购数量且均大于 0；
     * 验收子行状态按环节推进（初验环节→待检验，检验环节→待入库），
     * 退货子行状态→待退货并记录原行序号（originalSeq）用于追溯；
     * 关联采购订单明细同步拆行，金额与标准量差值按拆分后数量重算
     * </p>
     *
     * @param detailId   被拆分的验收明细ID
     * @param stage      拆分环节（初验 / 检验）
     * @param acceptQty  验收数量（进入验收子行）
     * @param returnQty  退货数量（进入退货子行）
     * @param remark     备注说明（追加到验收单备注）
     */
    void partialAcceptance(Long detailId, String stage, BigDecimal acceptQty, BigDecimal returnQty, String remark);

    /**
     * 整单入库：全部明细终结后一次性生成整单入库单
     * <p>
     * 可执行条件：所有明细均处于终结状态（待入库/待退货/已退货/已重发/已入库/已取消），
     * 且至少有一条「待入库」明细；为每条待入库明细指定入库仓库（支持不同仓库），
     * 校验批号必填后生成入库单、写库存与流水，合格明细→已入库，主单→已入库，
     * 并同步关联采购订单与领料单状态
     * </p>
     *
     * @param acceptanceId 验收单ID
     * @param items        待入库明细的仓库指定列表（detailId + prodUnitId），缺省回退验收单级仓库
     * @return 自动生成的入库单（含操作人姓名/仓库名）
     */
    StockInWithNamesDto inbound(Long acceptanceId, List<AcceptanceInboundRequest.Item> items);

    /**
     * 将验收单状态同步至关联采购订单
     * <p>
     * 明细级回写采购订单明细状态（purchase_order_items.status），
     * 并将验收单主单状态按同名映射同步采购订单主单状态（已作废订单不覆盖）；
     * 供物料退货管理等模块在状态变更后调用
     * </p>
     *
     * @param acceptance 状态已变更且明细已落库的验收单
     */
    void syncPurchaseStatus(AcceptanceOrder acceptance);

    // endregion
}
