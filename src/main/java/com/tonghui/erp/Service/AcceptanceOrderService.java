package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Stock.AcceptanceWithDetailsDto;
import com.tonghui.erp.Data.Entity.AcceptanceDetail;
import com.tonghui.erp.Data.Entity.AcceptanceOrder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 货物验收单业务接口
 * <p>
 * 提供验收单的增删改查、明细管理、状态流转（确认到货/初验/检验/重新收货）及库存联动能力
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
     * 更新验收明细（批号/单价等）
     *
     * @param detail 验收明细
     */
    void updateAcceptanceDetail(AcceptanceDetail detail);

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
     * 确认到货：运输中 → 到货初验
     *
     * @param acceptanceId 验收单ID
     */
    void confirmArrival(Long acceptanceId);

    /**
     * 初验处理：到货初验 → 物料检验（合格）/ 待退货（不合格）
     *
     * @param acceptanceId 验收单ID
     * @param pass         是否合格
     * @param remark       初验备注
     */
    void inspect(Long acceptanceId, boolean pass, String remark);

    /**
     * 检验处理：物料检验 → 已入库（合格，自动增加库存并写流水）/ 待退货（不合格）
     *
     * @param acceptanceId 验收单ID
     * @param pass         是否合格
     * @param prodUnitId   入库仓库（生产单位ID，合格时必填）
     * @param remark       检验备注
     */
    void qualityCheck(Long acceptanceId, boolean pass, Long prodUnitId, String remark);

    /**
     * 重新收货：待退货 → 生成新验收单（明细沿用原单），原单标记为已退换
     *
     * @param acceptanceId 验收单ID
     * @return 新生成的验收单
     */
    AcceptanceOrder reReceive(Long acceptanceId);

    // endregion
}
