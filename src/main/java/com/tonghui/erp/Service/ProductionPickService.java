package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.ProductionPick.ProductionPickBatchViewDto;
import com.tonghui.erp.Common.Dto.ProductionPick.ProductionPickIssueRequest;
import com.tonghui.erp.Common.Dto.ProductionPick.ProductionPickWithDetailsDto;
import com.tonghui.erp.Data.Entity.ProductionPick;
import com.tonghui.erp.Data.Entity.ProductionPickDetail;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 生产领料单业务接口
 * <p>
 * 提供生产领料单的分页查询、详情查询、按生产任务预填、新增/修改/删除、
 * 发料（生成草稿出库单）与作废等业务能力
 * </p>
 */
public interface ProductionPickService extends IService<ProductionPick> {

    // region 查询操作
    // ===================================
    // 查询操作
    // ===================================

    /**
     * 分页查询生产领料单（含明细）
     *
     * @param keyword      关键字（领料单号/生产任务号/生产任务名/领料人/用途，模糊匹配）
     * @param status       状态筛选（待发料/已发料/已作废，可选）
     * @param workOrderId  关联生产任务ID（可选）
     * @param stockOutCode 关联出库单号（可选，模糊匹配）
     * @param startDate    领料时间起始日期（可选）
     * @param endDate      领料时间结束日期（可选）
     * @param pageIndex    页码（从0开始）
     * @param pageSize     每页数量
     * @return 分页结果（含明细与出库单状态）
     */
    PagedResult<ProductionPickWithDetailsDto> queryPicks(String keyword, String status, Long workOrderId,
                                                         String stockOutCode, LocalDate startDate, LocalDate endDate,
                                                         int pageIndex, int pageSize);

    /**
     * 根据ID查询生产领料单详情（含明细）
     *
     * @param id 生产领料单ID
     * @return 生产领料单详情，不存在时返回 null
     */
    ProductionPickWithDetailsDto getPickWithDetails(Long id);

    /**
     * 根据生产任务生成领料预填数据（不落库）
     * <p>
     * 快照生产任务号/任务名/制剂/批号，并按制剂处方计算请领数量（处方量 × 倍数）
     * </p>
     *
     * @param workOrderId 生产任务ID
     * @param multiplier  处方倍数（为空或小于等于0时按1处理）
     * @return 预填的领料单主表与明细
     */
    ProductionPickWithDetailsDto buildPrefillFromWorkOrder(Long workOrderId, BigDecimal multiplier);

    /**
     * 查询某张领料单每条明细的可用库存批次
     *
     * @param pickId 生产领料单ID
     * @return 明细物料可用库存批次列表
     */
    List<ProductionPickBatchViewDto> getAvailableBatches(Long pickId);

    // endregion

    // region 写操作
    // ===================================
    // 写操作
    // ===================================

    /**
     * 新增生产领料单（含明细）
     *
     * @param pick    领料单主表信息
     * @param details 领料明细列表
     * @return 保存后的领料单（含明细）
     */
    ProductionPick createPick(ProductionPick pick, List<ProductionPickDetail> details);

    /**
     * 修改生产领料单（含明细，仅待发料状态可修改）
     *
     * @param id      领料单ID
     * @param pick    领料单主表信息
     * @param details 领料明细列表
     */
    void updatePick(Long id, ProductionPick pick, List<ProductionPickDetail> details);

    /**
     * 发料确认（仅待发料状态）
     * <p>
     * 校验实发数量并分配库存批次，生成“生产领料出库”草稿出库单（不扣库存），
     * 回写出库单号、审核人、发料人及实发信息，领料单状态改为“已发料”
     * </p>
     *
     * @param id      领料单ID
     * @param request 发料请求（审核人/发料人/实发明细）
     * @return 发料后的领料单
     */
    ProductionPick issuePick(Long id, ProductionPickIssueRequest request);

    /**
     * 作废生产领料单
     * <p>
     * 待发料直接作废；已发料时若出库单为草稿则删除草稿单，
     * 若出库单已出库则调用出库取消回滚库存
     * </p>
     *
     * @param id 领料单ID
     */
    void cancelPick(Long id);

    /**
     * 删除生产领料单（仅待发料状态，物理删除主表与明细）
     *
     * @param id 领料单ID
     */
    void deletePick(Long id);

    /**
     * 生成生产领料单号（格式 SCLL-YYYYMMDD-NNN）
     *
     * @return 领料单号
     */
    String generatePickCode();

    // endregion
}
