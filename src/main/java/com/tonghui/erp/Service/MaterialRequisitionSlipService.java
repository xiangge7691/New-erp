package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Common.Dto.MaterialRequisitionSlip.MaterialRequisitionSlipWithDetailsDto;
import com.tonghui.erp.Data.Entity.MaterialRequisitionSlip;
import com.tonghui.erp.Data.Entity.MaterialRequisitionSlipDetail;

import java.util.List;

/**
 * 物料领料单业务服务接口
 * <p>
 * 提供领料单的增删改查、明细管理、作废联动、生产计划联动等功能
 * </p>
 */
public interface MaterialRequisitionSlipService extends IService<MaterialRequisitionSlip> {

    // region 基础操作
    // ===================================
    // 基础操作
    // ===================================

    /**
     * 新增领料单（含明细，自动生成货物验收单）
     *
     * @param slip    领料单主表实体
     * @param details 领料明细列表
     * @return 保存后的领料单实体（含ID和自动生成的单号）
     */
    MaterialRequisitionSlip addSlip(MaterialRequisitionSlip slip, List<MaterialRequisitionSlipDetail> details);

    /**
     * 更新领料单（含明细，仅待发放状态可编辑）
     *
     * @param slipId  领料单ID
     * @param slip    领料单主表实体
     * @param details 领料明细列表
     */
    void updateSlip(Long slipId, MaterialRequisitionSlip slip, List<MaterialRequisitionSlipDetail> details);

    /**
     * 删除领料单（含明细，仅待发放状态可删除）
     *
     * @param slipId 领料单ID
     */
    void deleteSlip(Long slipId);

    // endregion

    // region 查询操作
    // ===================================
    // 查询操作
    // ===================================

    /**
     * 根据ID查询领料单（含明细）
     *
     * @param slipId 领料单ID
     * @return 领料单详情（含明细）
     */
    MaterialRequisitionSlipWithDetailsDto getSlipWithDetails(Long slipId);

    /**
     * 查询领料单明细列表
     *
     * @param slipId 领料单ID
     * @return 明细列表
     */
    List<MaterialRequisitionSlipDetail> getDetailsBySlipId(Long slipId);

    /**
     * 分页查询领料单（支持关键字/状态筛选）
     *
     * @param keyword   关键字（对领料单号/物料名称进行模糊匹配，可选）
     * @param status    状态筛选（可选）
     * @param pageIndex 页码（从0开始）
     * @param pageSize  每页大小
     * @return 领料单分页结果
     */
    Page<MaterialRequisitionSlip> querySlips(String keyword, String status, int pageIndex, int pageSize);

    /**
     * 分页查询领料单（含明细子表）
     *
     * @param keyword   关键字
     * @param status    状态筛选
     * @param pageIndex 页码（从0开始）
     * @param pageSize  每页大小
     * @return 领料单分页结果（含明细）
     */
    List<MaterialRequisitionSlipWithDetailsDto> querySlipsWithDetails(String keyword, String status, int pageIndex, int pageSize);

    // endregion

    // region 状态操作
    // ===================================
    // 状态操作
    // ===================================

    /**
     * 作废领料单（待发放→已作废，联动验收单→已退货）
     *
     * @param slipId 领料单ID
     */
    void voidSlip(Long slipId);

    /**
     * 更新领料单状态（供验收单状态联动回调使用）
     *
     * @param slipId      领料单ID
     * @param targetStatus 目标状态
     */
    void updateStatus(Long slipId, String targetStatus);

    // endregion

    // region 单号生成
    // ===================================
    // 单号生成
    // ===================================

    /**
     * 生成领料单号（格式 LL-YYYYMMDD-NNN）
     *
     * @return 领料单号
     */
    String generateSlipCode();

    // endregion

    // region 生产计划联动
    // ===================================
    // 生产计划联动
    // ===================================

    /**
     * 根据生产计划自动带出物料明细（按制剂处方计算请领数量）
     * <p>
     * 请领数量 = 处方量(dosage) × 倍数(multiplier) × 计划数量(planQuantity)
     * </p>
     *
     * @param productionPlanId 生产计划ID
     * @param multiplier       倍数（默认1）
     * @return 领料明细列表（已计算请领数量，前端确认后调用addSlip保存）
     */
    List<MaterialRequisitionSlipDetail> generateDetailsFromProductionPlan(Long productionPlanId, java.math.BigDecimal multiplier);

    // endregion
}
