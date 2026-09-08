package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.Dto.MaterialRequisitionSlip.MaterialRequisitionSlipWithDetailsDto;
import com.tonghui.erp.Common.utils.EntityUtils;
import com.tonghui.erp.Data.Entity.AcceptanceDetail;
import com.tonghui.erp.Data.Entity.AcceptanceOrder;
import com.tonghui.erp.Data.Entity.MaterialRequisitionSlip;
import com.tonghui.erp.Data.Entity.MaterialRequisitionSlipDetail;
import com.tonghui.erp.Data.Entity.PreparationFormula;
import com.tonghui.erp.Data.Entity.ProductionPlan;
import com.tonghui.erp.Data.mapper.MaterialRequisitionSlipDetailMapper;
import com.tonghui.erp.Data.mapper.MaterialRequisitionSlipMapper;
import com.tonghui.erp.Data.mapper.PreparationFormulaMapper;
import com.tonghui.erp.Data.mapper.ProductionPlanMapper;
import com.tonghui.erp.Service.AcceptanceOrderService;
import com.tonghui.erp.Service.MaterialRequisitionSlipService;
import com.tonghui.erp.Service.impl.SequenceServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 物料领料单业务服务实现类
 * <p>
 * 实现MaterialRequisitionSlipService接口，提供领料单的增删改查、明细管理、
 * 作废联动、生产计划联动等功能。新增领料单时自动生成货物验收单，
 * 货物验收状态变更时回写领料单状态。
 * </p>
 */
@Service
public class MaterialRequisitionSlipServiceImpl extends ServiceImpl<MaterialRequisitionSlipMapper, MaterialRequisitionSlip>
        implements MaterialRequisitionSlipService {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /** 领料单主表数据访问层 */
    private final MaterialRequisitionSlipMapper slipMapper;

    /** 领料单明细数据访问层 */
    private final MaterialRequisitionSlipDetailMapper detailMapper;

    /** 序列号生成服务 */
    private final SequenceServiceImpl sequenceService;

    /** 货物验收单服务（新增领料单时自动生成验收单） */
    private final AcceptanceOrderService acceptanceOrderService;

    /** 生产计划数据访问层（生产计划联动时查询计划信息） */
    private final ProductionPlanMapper productionPlanMapper;

    /** 制剂处方数据访问层（生产计划联动时查询处方明细） */
    private final PreparationFormulaMapper preparationFormulaMapper;

    /**
     * 构造函数注入依赖
     */
    @Autowired
    public MaterialRequisitionSlipServiceImpl(MaterialRequisitionSlipMapper slipMapper,
                                              MaterialRequisitionSlipDetailMapper detailMapper,
                                              SequenceServiceImpl sequenceService,
                                              AcceptanceOrderService acceptanceOrderService,
                                              ProductionPlanMapper productionPlanMapper,
                                              PreparationFormulaMapper preparationFormulaMapper) {
        this.slipMapper = slipMapper;
        this.detailMapper = detailMapper;
        this.sequenceService = sequenceService;
        this.acceptanceOrderService = acceptanceOrderService;
        this.productionPlanMapper = productionPlanMapper;
        this.preparationFormulaMapper = preparationFormulaMapper;
    }

    // endregion

    // region 基础CRUD操作
    // ===================================
    // 基础CRUD操作
    // ===================================

    /**
     * 新增领料单（含明细，自动生成货物验收单）
     * <p>
     * 1. 自动生成领料单号（LL-YYYYMMDD-NNN）
     * 2. 设置默认状态为"待发放"
     * 3. 保存主表和明细
     * 4. 自动生成货物验收单（状态"运输中"）
     * 5. 回写验收单ID到领料单
     * </p>
     *
     * @param slip    领料单主表实体
     * @param details 领料明细列表
     * @return 保存后的领料单实体
     */
    @Override
    @Transactional
    public MaterialRequisitionSlip addSlip(MaterialRequisitionSlip slip, List<MaterialRequisitionSlipDetail> details) {
        // 自动生成领料单号（如果未提供）
        if (!StringUtils.hasText(slip.getSlipCode())) {
            slip.setSlipCode(sequenceService.generateSlipCode());
        }

        // 设置默认状态
        if (!StringUtils.hasText(slip.getStatus())) {
            slip.setStatus("待发放");
        }

        // 设置默认科室
        if (!StringUtils.hasText(slip.getFromDept())) {
            slip.setFromDept("制剂室");
        }
        if (!StringUtils.hasText(slip.getToDept())) {
            slip.setToDept("医院药剂科");
        }

        // 保存领料单主表
        slipMapper.insert(slip);

        // 保存明细
        saveDetails(slip.getId(), details);

        // 自动生成货物验收单
        if (details != null && !details.isEmpty()) {
            AcceptanceOrder acceptance = buildAcceptanceFromSlip(slip, details);
            List<AcceptanceDetail> acceptanceDetails = buildAcceptanceDetailsFromSlip(details);
            acceptanceOrderService.addAcceptance(acceptance, acceptanceDetails);

            // 回写验收单ID到领料单
            slip.setAcceptanceOrderId(acceptance.getAcceptanceId());
            slipMapper.updateById(slip);
        }

        return slip;
    }

    /**
     * 更新领料单（含明细，仅待发放状态可编辑）
     *
     * @param slipId  领料单ID
     * @param slip    领料单主表实体
     * @param details 领料明细列表
     */
    @Override
    @Transactional
    public void updateSlip(Long slipId, MaterialRequisitionSlip slip, List<MaterialRequisitionSlipDetail> details) {
        MaterialRequisitionSlip existing = slipMapper.selectById(slipId);
        if (existing == null) {
            throw new RuntimeException("领料单不存在");
        }
        if (!"待发放".equals(existing.getStatus())) {
            throw new RuntimeException("仅待发放状态的领料单可编辑");
        }

        // 更新主表（只更新允许修改的字段）
        existing.setProductionPlanId(slip.getProductionPlanId());
        existing.setProductionPlanCode(slip.getProductionPlanCode());
        existing.setProductionPlanTitle(slip.getProductionPlanTitle());
        existing.setApplicant(slip.getApplicant());
        existing.setApplyTime(slip.getApplyTime());
        existing.setRemark(slip.getRemark());
        existing.setUpdatedTime(LocalDateTime.now());
        Long currentUserId = EntityUtils.getCurrentUserId();
        if (currentUserId != null) {
            existing.setUpdatedBy(currentUserId);
        }
        slipMapper.updateById(existing);

        // 先删后插明细
        if (details != null) {
            detailMapper.physicalDeleteBySlipId(slipId);
            saveDetails(slipId, details);
        }
    }

    /**
     * 删除领料单（含明细，仅待发放状态可删除）
     *
     * @param slipId 领料单ID
     */
    @Override
    @Transactional
    public void deleteSlip(Long slipId) {
        MaterialRequisitionSlip existing = slipMapper.selectById(slipId);
        if (existing == null) {
            throw new RuntimeException("领料单不存在");
        }
        if (!"待发放".equals(existing.getStatus())) {
            throw new RuntimeException("仅待发放状态的领料单可删除");
        }

        // 删除明细
        detailMapper.physicalDeleteBySlipId(slipId);

        // 删除主表（物理删除）
        slipMapper.physicalDeleteById(slipId);
    }

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
    @Override
    public MaterialRequisitionSlipWithDetailsDto getSlipWithDetails(Long slipId) {
        MaterialRequisitionSlip slip = slipMapper.selectById(slipId);
        if (slip == null) {
            return null;
        }

        MaterialRequisitionSlipWithDetailsDto dto = new MaterialRequisitionSlipWithDetailsDto();
        BeanUtils.copyProperties(slip, dto);

        // 查询明细
        List<MaterialRequisitionSlipDetail> details = getDetailsBySlipId(slipId);
        dto.setDetails(details);

        return dto;
    }

    /**
     * 查询领料单明细列表
     *
     * @param slipId 领料单ID
     * @return 明细列表
     */
    @Override
    public List<MaterialRequisitionSlipDetail> getDetailsBySlipId(Long slipId) {
        QueryWrapper<MaterialRequisitionSlipDetail> wrapper = new QueryWrapper<>();
        wrapper.eq("slip_id", slipId);
        wrapper.eq("is_deleted", 0);
        wrapper.orderByAsc("seq");
        return detailMapper.selectList(wrapper);
    }

    /**
     * 分页查询领料单（支持关键字/状态筛选）
     *
     * @param keyword   关键字
     * @param status    状态筛选
     * @param pageIndex 页码（从0开始）
     * @param pageSize  每页大小
     * @return 领料单分页结果
     */
    @Override
    public Page<MaterialRequisitionSlip> querySlips(String keyword, String status, int pageIndex, int pageSize) {
        int actualPageIndex = pageIndex + 1;
        Page<MaterialRequisitionSlip> page = new Page<>(actualPageIndex, pageSize);
        QueryWrapper<MaterialRequisitionSlip> wrapper = new QueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like("slip_code", keyword)
                    .or().like("production_plan_title", keyword)
                    .or().like("applicant", keyword));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }

        wrapper.orderByDesc("created_time");
        return slipMapper.selectPage(page, wrapper);
    }

    /**
     * 分页查询领料单（含明细子表）
     *
     * @param keyword   关键字
     * @param status    状态筛选
     * @param pageIndex 页码
     * @param pageSize  每页大小
     * @return 领料单分页结果（含明细）
     */
    @Override
    public List<MaterialRequisitionSlipWithDetailsDto> querySlipsWithDetails(String keyword, String status, int pageIndex, int pageSize) {
        Page<MaterialRequisitionSlip> parentPage = querySlips(keyword, status, pageIndex, pageSize);
        List<MaterialRequisitionSlip> parents = parentPage.getRecords();

        if (parents.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量查询关联的明细
        List<Long> slipIds = parents.stream().map(MaterialRequisitionSlip::getId).collect(Collectors.toList());
        QueryWrapper<MaterialRequisitionSlipDetail> detailWrapper = new QueryWrapper<>();
        detailWrapper.in("slip_id", slipIds);
        detailWrapper.eq("is_deleted", 0);
        detailWrapper.orderByAsc("seq");
        List<MaterialRequisitionSlipDetail> allDetails = detailMapper.selectList(detailWrapper);
        Map<Long, List<MaterialRequisitionSlipDetail>> detailsMap = allDetails.stream()
                .collect(Collectors.groupingBy(MaterialRequisitionSlipDetail::getSlipId));

        // 组装带子表数据的DTO
        return parents.stream().map(parent -> {
            MaterialRequisitionSlipWithDetailsDto dto = new MaterialRequisitionSlipWithDetailsDto();
            BeanUtils.copyProperties(parent, dto);
            dto.setDetails(detailsMap.getOrDefault(parent.getId(), List.of()));
            return dto;
        }).collect(Collectors.toList());
    }

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
    @Override
    @Transactional
    public void voidSlip(Long slipId) {
        MaterialRequisitionSlip slip = slipMapper.selectById(slipId);
        if (slip == null) {
            throw new RuntimeException("领料单不存在");
        }
        if (!"待发放".equals(slip.getStatus())) {
            throw new RuntimeException("仅待发放状态的领料单可作废");
        }

        // 更新领料单状态为已作废
        slip.setStatus("已作废");
        slip.setUpdatedTime(LocalDateTime.now());
        Long currentUserId = EntityUtils.getCurrentUserId();
        if (currentUserId != null) {
            slip.setUpdatedBy(currentUserId);
        }
        slipMapper.updateById(slip);

        // 联动：关联货物验收单状态→已退货
        if (slip.getAcceptanceOrderId() != null) {
            AcceptanceOrder acceptance = acceptanceOrderService.getAcceptanceById(slip.getAcceptanceOrderId());
            if (acceptance != null && !"已入库".equals(acceptance.getStatus())) {
                acceptance.setStatus("已退货");
                acceptanceOrderService.updateById(acceptance);
            }
        }
    }

    /**
     * 更新领料单状态（供验收单状态联动回调使用）
     *
     * @param slipId       领料单ID
     * @param targetStatus 目标状态
     */
    @Override
    @Transactional
    public void updateStatus(Long slipId, String targetStatus) {
        MaterialRequisitionSlip slip = slipMapper.selectById(slipId);
        if (slip == null) {
            return;
        }

        // 状态合法性校验
        String currentStatus = slip.getStatus();
        if ("已作废".equals(currentStatus) || "已入库".equals(currentStatus)) {
            // 终态不可变更
            return;
        }

        // 允许的状态流转：待发放→验收中→已入库
        if ("待发放".equals(currentStatus) && "验收中".equals(targetStatus)) {
            slip.setStatus(targetStatus);
            slip.setUpdatedTime(LocalDateTime.now());
            slipMapper.updateById(slip);
        } else if ("验收中".equals(currentStatus) && "已入库".equals(targetStatus)) {
            slip.setStatus(targetStatus);
            slip.setUpdatedTime(LocalDateTime.now());
            slipMapper.updateById(slip);
        }
    }

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
    @Override
    public String generateSlipCode() {
        return sequenceService.generateSlipCode();
    }

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
     * @return 领料明细列表
     */
    @Override
    public List<MaterialRequisitionSlipDetail> generateDetailsFromProductionPlan(Long productionPlanId, BigDecimal multiplier) {
        // 查询生产计划
        ProductionPlan plan = productionPlanMapper.selectById(productionPlanId);
        if (plan == null) {
            throw new RuntimeException("生产计划不存在");
        }

        if (multiplier == null || multiplier.compareTo(BigDecimal.ZERO) <= 0) {
            multiplier = BigDecimal.ONE;
        }

        // 查询该制剂的处方明细
        QueryWrapper<PreparationFormula> formulaWrapper = new QueryWrapper<>();
        formulaWrapper.eq("preparation_code", plan.getPreparationCode());
        formulaWrapper.eq("is_deleted", 0);
        List<PreparationFormula> formulas = preparationFormulaMapper.selectList(formulaWrapper);

        if (formulas.isEmpty()) {
            throw new RuntimeException("该制剂未配置处方信息");
        }

        // 构造领料明细，计算请领数量
        List<MaterialRequisitionSlipDetail> details = new ArrayList<>();
        int seq = 1;
        for (PreparationFormula formula : formulas) {
            MaterialRequisitionSlipDetail detail = new MaterialRequisitionSlipDetail();
            detail.setSeq(seq++);
            detail.setMaterialId(formula.getMaterialId());
            detail.setMaterialCode(formula.getMaterialCode());
            detail.setMaterialName(formula.getMaterialName());
            detail.setUnitName(formula.getUnitName());
            // 请领数量 = 处方量 × 倍数 × 计划数量
            BigDecimal applyQty = formula.getDosage().multiply(multiplier).multiply(plan.getPlanQuantity());
            detail.setApplyQty(applyQty);
            detail.setIsDeleted(0);
            detail.setVersion(1);
            details.add(detail);
        }

        return details;
    }

    // endregion

    // region 私有工具方法
    // ===================================
    // 私有工具方法
    // ===================================

    /**
     * 保存领料明细列表（自动计算序号）
     *
     * @param slipId  领料单ID
     * @param details 明细列表
     */
    private void saveDetails(Long slipId, List<MaterialRequisitionSlipDetail> details) {
        if (details != null && !details.isEmpty()) {
            int seq = 1;
            for (MaterialRequisitionSlipDetail detail : details) {
                detail.setId(null);
                detail.setSlipId(slipId);
                detail.setSeq(seq++);
                detail.setIsDeleted(0);
                if (detail.getVersion() == null) {
                    detail.setVersion(1);
                }
                detailMapper.insert(detail);
            }
        }
    }

    /**
     * 根据领料单构造货物验收单主表
     * <p>
     * 验收单初始状态为"运输中"，来源类型为"领料入库"
     * </p>
     *
     * @param slip    领料单
     * @param details 领料明细
     * @return 验收单主表实体
     */
    private AcceptanceOrder buildAcceptanceFromSlip(MaterialRequisitionSlip slip,
                                                    List<MaterialRequisitionSlipDetail> details) {
        AcceptanceOrder acceptance = new AcceptanceOrder();
        acceptance.setAcceptanceCode(sequenceService.generateAcceptanceCode());
        acceptance.setSourceType("领料入库");
        acceptance.setRelatedOrder(slip.getSlipCode());
        acceptance.setRelatedSlipId(slip.getId());
        acceptance.setTitle("领料单 " + slip.getSlipCode() + " 自动生成");
        acceptance.setUnitName(slip.getFromDept());
        acceptance.setStatus("运输中");

        // 如果关联了生产计划，填入计划编号和标题
        if (StringUtils.hasText(slip.getProductionPlanCode())) {
            acceptance.setPlanCode(slip.getProductionPlanCode());
        }
        if (StringUtils.hasText(slip.getProductionPlanTitle())) {
            acceptance.setTitle(slip.getProductionPlanTitle() + " - 领料入库");
        }

        return acceptance;
    }

    /**
     * 将领料明细映射为货物验收明细
     * <p>
     * 请领数量映射为采购数量(quantity)，实发数量/批号/单价暂为空（药剂科后续回填）
     * </p>
     *
     * @param slipDetails 领料明细列表
     * @return 验收明细列表
     */
    private List<AcceptanceDetail> buildAcceptanceDetailsFromSlip(List<MaterialRequisitionSlipDetail> slipDetails) {
        if (slipDetails == null) {
            return List.of();
        }

        return slipDetails.stream().map(sd -> {
            AcceptanceDetail ad = new AcceptanceDetail();
            ad.setMaterialCode(sd.getMaterialCode());
            ad.setMaterialName(sd.getMaterialName());
            ad.setMaterialCategory("物料");
            ad.setUnitName(sd.getUnitName());
            ad.setItemId(sd.getMaterialId());
            ad.setItemType("material");
            ad.setQuantity(sd.getApplyQty()); // 请领数量 → 采购数量
            ad.setStandardDosage(BigDecimal.ZERO);
            return ad;
        }).collect(Collectors.toList());
    }

    // endregion
}
