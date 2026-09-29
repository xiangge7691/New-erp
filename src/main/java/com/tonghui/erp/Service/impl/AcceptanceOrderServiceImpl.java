package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Stock.AcceptanceInboundRequest;
import com.tonghui.erp.Common.Dto.Stock.AcceptanceWithDetailsDto;
import com.tonghui.erp.Common.Dto.Stock.StockInWithNamesDto;
import com.tonghui.erp.Common.utils.AcceptanceStatusPolicy;
import com.tonghui.erp.Data.Entity.AcceptanceDetail;
import com.tonghui.erp.Data.Entity.AcceptanceOrder;
import com.tonghui.erp.Data.Entity.Material;
import com.tonghui.erp.Data.Entity.ProductionPlan;
import com.tonghui.erp.Data.Entity.ProductionUnit;
import com.tonghui.erp.Data.Entity.PurchaseOrderItems;
import com.tonghui.erp.Data.Entity.PurchaseOrders;
import com.tonghui.erp.Data.Entity.StockIn;
import com.tonghui.erp.Data.Entity.StockInDetail;
import com.tonghui.erp.Data.Entity.User;
import com.tonghui.erp.Data.mapper.AcceptanceDetailMapper;
import com.tonghui.erp.Data.mapper.AcceptanceOrderMapper;
import com.tonghui.erp.Data.mapper.MaterialMapper;
import com.tonghui.erp.Data.mapper.ProductionPlanMapper;
import com.tonghui.erp.Data.mapper.ProductionUnitMapper;
import com.tonghui.erp.Data.mapper.PurchaseOrderItemsMapper;
import com.tonghui.erp.Data.mapper.PurchaseOrdersMapper;
import com.tonghui.erp.Data.mapper.StockInDetailMapper;
import com.tonghui.erp.Data.mapper.StockInMapper;
import com.tonghui.erp.Data.mapper.UserMapper;
import com.tonghui.erp.Service.AcceptanceOrderService;
import com.tonghui.erp.Service.MaterialRequisitionSlipService;
import com.tonghui.erp.Service.StockService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 货物验收单业务实现类
 * <p>
 * 实现AcceptanceOrderService接口，提供验收单的增删改查、明细管理、行级状态流转
 * （运输中→验收中，逐行初验/检验/部分验收拆行，全部明细终结后整单入库）以及入库的库存联动能力。
 * 主单状态与明细状态词表及派生规则统一由 AcceptanceStatusPolicy 承载：
 * 验收单每次状态变更时，按明细状态回写采购订单明细（purchase_order_items.status），
 * 并将主单状态按同名映射同步关联采购订单主单状态，保证货物验收与采购状态实时对应
 * </p>
 */
@Service
public class AcceptanceOrderServiceImpl extends ServiceImpl<AcceptanceOrderMapper, AcceptanceOrder>
        implements AcceptanceOrderService {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /** 验收单数据访问层 */
    private final AcceptanceOrderMapper acceptanceOrderMapper;

    /** 验收单明细数据访问层 */
    private final AcceptanceDetailMapper acceptanceDetailMapper;

    /** 序列号生成服务，用于自动生成验收单号 */
    private final SequenceServiceImpl sequenceService;

    /** 库存服务，用于检验合格入库时的库存联动 */
    private final StockService stockService;

    /** 入库单数据访问层，验收入库时生成真实入库单 */
    private final StockInMapper stockInMapper;

    /** 入库单明细数据访问层，验收入库时生成入库明细 */
    private final StockInDetailMapper stockInDetailMapper;

    /** 采购订单数据访问层，验收合格入库后回写采购订单状态 */
    private final PurchaseOrdersMapper purchaseOrdersMapper;

    /** 采购订单明细数据访问层，验收明细状态变化时回写采购订单明细状态 */
    private final PurchaseOrderItemsMapper purchaseOrderItemsMapper;

    /** 物料主数据访问层，入库明细名称/分类以物料主数据为权威来源 */
    private final MaterialMapper materialMapper;

    /** 用户数据访问层，解析入库单操作人姓名 */
    private final UserMapper userMapper;

    /** 生产单位数据访问层，解析入库单仓库名称 */
    private final ProductionUnitMapper productionUnitMapper;

    /** 生产计划数据访问层，解析关联生产计划标题 */
    private final ProductionPlanMapper productionPlanMapper;

    /** 领料单服务，验收单状态变更时回写领料单状态 */
    private MaterialRequisitionSlipService materialRequisitionSlipService;

    /**
     * 构造函数注入依赖
     *
     * @param acceptanceOrderMapper   验收单数据访问层
     * @param acceptanceDetailMapper  验收单明细数据访问层
     * @param sequenceService         序列号生成服务
     * @param stockService            库存服务
     * @param stockInMapper           入库单数据访问层
     * @param stockInDetailMapper     入库单明细数据访问层
     * @param purchaseOrdersMapper    采购订单数据访问层
     * @param purchaseOrderItemsMapper 采购订单明细数据访问层
     * @param materialMapper          物料主数据访问层
     * @param userMapper              用户数据访问层
     * @param productionUnitMapper    生产单位数据访问层
     * @param productionPlanMapper    生产计划数据访问层
     */
    @Autowired
    public AcceptanceOrderServiceImpl(AcceptanceOrderMapper acceptanceOrderMapper,
                                      AcceptanceDetailMapper acceptanceDetailMapper,
                                      SequenceServiceImpl sequenceService,
                                      StockService stockService,
                                      StockInMapper stockInMapper,
                                      StockInDetailMapper stockInDetailMapper,
                                      PurchaseOrdersMapper purchaseOrdersMapper,
                                      PurchaseOrderItemsMapper purchaseOrderItemsMapper,
                                      MaterialMapper materialMapper,
                                      UserMapper userMapper,
                                      ProductionUnitMapper productionUnitMapper,
                                      ProductionPlanMapper productionPlanMapper) {
        this.acceptanceOrderMapper = acceptanceOrderMapper;
        this.acceptanceDetailMapper = acceptanceDetailMapper;
        this.sequenceService = sequenceService;
        this.stockService = stockService;
        this.stockInMapper = stockInMapper;
        this.stockInDetailMapper = stockInDetailMapper;
        this.purchaseOrdersMapper = purchaseOrdersMapper;
        this.purchaseOrderItemsMapper = purchaseOrderItemsMapper;
        this.materialMapper = materialMapper;
        this.userMapper = userMapper;
        this.productionUnitMapper = productionUnitMapper;
        this.productionPlanMapper = productionPlanMapper;
    }

    /**
     * 设置领料单服务（setter注入，@Lazy打破循环依赖）
     */
    @Lazy
    @Autowired
    public void setMaterialRequisitionSlipService(MaterialRequisitionSlipService materialRequisitionSlipService) {
        this.materialRequisitionSlipService = materialRequisitionSlipService;
    }

    // endregion

    // region 基础CRUD操作
    // ===================================
    // 基础CRUD操作
    // ===================================

    /**
     * 新增验收单（含明细）
     * <p>自动生成验收单号（如果未提供），初始状态为"运输中"，同时保存主表和明细数据</p>
     *
     * @param acceptance 验收单主表实体
     * @param details    验收明细列表，可为null
     */
    @Override
    @Transactional
    public void addAcceptance(AcceptanceOrder acceptance, List<AcceptanceDetail> details) {
        // 自动生成验收单号（如果未提供）
        if (!StringUtils.hasText(acceptance.getAcceptanceCode())) {
            acceptance.setAcceptanceCode(sequenceService.generateAcceptanceCode());
        }
        // 初始状态：未指定时默认"运输中"（确认到货后切换为验收中）
        if (!StringUtils.hasText(acceptance.getStatus())) {
            acceptance.setStatus(AcceptanceStatusPolicy.MAIN_TRANSPORTING);
        }

        // 保存验收单主表
        acceptanceOrderMapper.insert(acceptance);

        // 保存明细表
        saveDetails(acceptance.getAcceptanceId(), details);
    }

    /**
     * 更新验收单（含明细）
     * <p>已入库的验收单已产生库存记录，禁止更新</p>
     *
     * @param acceptance 验收单主表实体
     * @param details    验收明细列表
     */
    @Override
    @Transactional
    public void updateAcceptance(AcceptanceOrder acceptance, List<AcceptanceDetail> details) {
        AcceptanceOrder existing = acceptanceOrderMapper.selectById(acceptance.getAcceptanceId());
        if (existing == null) {
            throw new RuntimeException("验收单不存在");
        }
        // 已入库不可修改（批号/单价锁定）
        if ("已入库".equals(existing.getStatus())) {
            throw new RuntimeException("已入库的验收单不可修改");
        }

        // 更新验收单主表
        acceptanceOrderMapper.updateById(acceptance);

        // 删除原有明细并重新插入（如果提供了明细）
        if (details != null) {
            QueryWrapper<AcceptanceDetail> deleteWrapper = new QueryWrapper<>();
            deleteWrapper.eq("acceptance_id", acceptance.getAcceptanceId());
            acceptanceDetailMapper.delete(deleteWrapper);
            saveDetails(acceptance.getAcceptanceId(), details);
        }
    }

    /**
     * 删除验收单（含明细）
     * <p>已入库的验收单已产生库存记录，禁止删除</p>
     *
     * @param acceptanceId 验收单ID
     */
    @Override
    @Transactional
    public void deleteAcceptance(Long acceptanceId) {
        AcceptanceOrder existing = acceptanceOrderMapper.selectById(acceptanceId);
        if (existing == null) {
            throw new RuntimeException("验收单不存在");
        }
        if ("已入库".equals(existing.getStatus())) {
            throw new RuntimeException("已入库的验收单不可删除");
        }

        // 删除明细表
        QueryWrapper<AcceptanceDetail> detailWrapper = new QueryWrapper<>();
        detailWrapper.eq("acceptance_id", acceptanceId);
        acceptanceDetailMapper.delete(detailWrapper);

        // 删除主表
        acceptanceOrderMapper.deleteById(acceptanceId);
    }

    // endregion

    // region 查询操作
    // ===================================
    // 查询操作
    // ===================================

    /**
     * 根据ID查询验收单
     *
     * @param acceptanceId 验收单ID
     * @return 验收单实体，不存在则返回null
     */
    @Override
    public AcceptanceOrder getAcceptanceById(Long acceptanceId) {
        return acceptanceOrderMapper.selectById(acceptanceId);
    }

    /**
     * 根据验收单号查询验收单
     *
     * @param acceptanceCode 验收单号
     * @return 验收单实体，不存在则返回null
     */
    @Override
    public AcceptanceOrder getAcceptanceByCode(String acceptanceCode) {
        QueryWrapper<AcceptanceOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("acceptance_code", acceptanceCode);
        return acceptanceOrderMapper.selectOne(wrapper);
    }

    /**
     * 查询所有验收单
     *
     * @return 验收单集合
     */
    @Override
    public List<AcceptanceOrder> getAllAcceptances() {
        return acceptanceOrderMapper.selectList(null);
    }

    // endregion

    // region 验收明细操作
    // ===================================
    // 验收明细操作
    // ===================================

    /**
     * 根据验收单ID查询所有验收明细
     *
     * @param acceptanceId 验收单ID
     * @return 该验收单下所有明细的集合
     */
    @Override
    public List<AcceptanceDetail> getDetailsByAcceptanceId(Long acceptanceId) {
        QueryWrapper<AcceptanceDetail> wrapper = new QueryWrapper<>();
        wrapper.eq("acceptance_id", acceptanceId);
        wrapper.orderByAsc("seq");
        return acceptanceDetailMapper.selectList(wrapper);
    }

    /**
     * 批量更新验收明细（批号/单价/实际到货数量等）
     * <p>逐条更新；明细携带实际到货数量或单价时，自动按 实际到货数量 × 单价 重算金额，
     * 与新增时的金额计算规则保持一致</p>
     *
     * @param details 验收明细列表
     */
    @Override
    @Transactional
    public void updateAcceptanceDetails(List<AcceptanceDetail> details) {
        if (details == null || details.isEmpty()) {
            throw new RuntimeException("明细列表不能为空");
        }
        for (AcceptanceDetail detail : details) {
            if (detail == null || detail.getDetailId() == null) {
                throw new RuntimeException("明细ID不能为空");
            }
            // 无条件读取原记录：校验存在性与"已入库锁定"，并复用原记录重算金额
            AcceptanceDetail existing = acceptanceDetailMapper.selectById(detail.getDetailId());
            if (existing == null) {
                throw new RuntimeException("验收明细不存在(ID=" + detail.getDetailId() + ")");
            }
            if (AcceptanceStatusPolicy.DETAIL_INBOUND.equals(existing.getStatus())) {
                throw new RuntimeException("明细「" + (existing.getMaterialName() != null ? existing.getMaterialName() : "未知物料") + "」(序号" + existing.getSeq() + ")已入库，批号/单价/仓库已锁定，不可修改");
            }
            // 携带实际到货数量或单价时，读取原记录补全后重算金额（以实际到货数量 × 单价）
            if (detail.getActualArrivalQty() != null || detail.getUnitPrice() != null) {
                BigDecimal qty = detail.getActualArrivalQty() != null
                        ? detail.getActualArrivalQty()
                        : existing.getActualArrivalQty();
                BigDecimal price = detail.getUnitPrice() != null
                        ? detail.getUnitPrice()
                        : existing.getUnitPrice();
                if (qty != null && price != null) {
                    detail.setAmount(qty.multiply(price));
                }
            }
            acceptanceDetailMapper.updateById(detail);
        }
    }

    /**
     * 删除验收明细
     *
     * @param detailId 明细ID
     */
    @Override
    public void deleteAcceptanceDetail(Long detailId) {
        acceptanceDetailMapper.deleteById(detailId);
    }

    // endregion

    // region 单号生成
    // ===================================
    // 单号生成
    // ===================================

    /**
     * 生成验收单号（格式 YS-YYYYMMDD-NNN）
     *
     * @return 自动生成的唯一验收单号
     */
    @Override
    public String generateAcceptanceCode() {
        return sequenceService.generateAcceptanceCode();
    }

    // endregion

    // region 高级查询
    // ===================================
    // 高级查询
    // ===================================

    /**
     * 高级查询验收单（支持分页、状态/来源筛选）
     *
     * @param acceptance 查询条件实体
     * @param keyword    关键字（对验收编号、验收标题进行模糊匹配，可选）
     * @param pageIndex  页码，从0开始
     * @param pageSize   每页数量
     * @return 验收单分页结果
     */
    @Override
    public Page<AcceptanceOrder> queryAcceptances(AcceptanceOrder acceptance, String keyword, int pageIndex, int pageSize) {
        // 将页码从0开始转换为1开始
        int actualPageIndex = pageIndex + 1;

        Page<AcceptanceOrder> page = new Page<>(actualPageIndex, pageSize);
        QueryWrapper<AcceptanceOrder> wrapper = new QueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            // 关键字对验收编号、验收标题进行模糊匹配
            wrapper.and(w -> w.like("acceptance_code", keyword).or().like("title", keyword));
        }
        if (acceptance.getAcceptanceId() != null) {
            wrapper.eq("acceptance_id", acceptance.getAcceptanceId());
        }
        if (StringUtils.hasText(acceptance.getAcceptanceCode())) {
            wrapper.like("acceptance_code", acceptance.getAcceptanceCode());
        }
        if (StringUtils.hasText(acceptance.getSourceType())) {
            wrapper.eq("source_type", acceptance.getSourceType());
        }
        if (StringUtils.hasText(acceptance.getStatus())) {
            wrapper.eq("status", acceptance.getStatus());
        }
        if (StringUtils.hasText(acceptance.getRelatedOrder())) {
            wrapper.like("related_order", acceptance.getRelatedOrder());
        }
        if (StringUtils.hasText(acceptance.getPlanCode())) {
            wrapper.like("plan_code", acceptance.getPlanCode());
        }

        // 按编号倒序排列
        wrapper.orderByDesc("acceptance_code");

        Page<AcceptanceOrder> result = acceptanceOrderMapper.selectPage(page, wrapper);

        // 批量解析关联制剂名称（通过 plan_code 关联 production_plan）
        if (!result.getRecords().isEmpty()) {
            Map<String, String> preparationNameMap = loadPreparationNameMap(result.getRecords());
            result.getRecords().forEach(r -> {
                if (StringUtils.hasText(r.getPlanCode())) {
                    r.setPreparationName(preparationNameMap.get(r.getPlanCode()));
                }
            });
        }

        return result;
    }

    /**
     * 高级查询验收单（包含明细子表）
     * <p>先分页查询验收单主表数据，再批量查询关联的验收明细</p>
     *
     * @param acceptance 查询条件实体
     * @param keyword    关键字（对验收编号、验收标题进行模糊匹配，可选）
     * @param pageIndex  页码，从0开始
     * @param pageSize   每页数量
     * @return 带子表关联数据的验收单分页结果
     */
    @Override
    public PagedResult<AcceptanceWithDetailsDto> searchWithDetails(AcceptanceOrder acceptance, String keyword, int pageIndex, int pageSize) {
        // 查询验收单主表分页数据
        Page<AcceptanceOrder> parentPage = queryAcceptances(acceptance, keyword, pageIndex, pageSize);
        List<AcceptanceOrder> parents = parentPage.getRecords();

        PagedResult<AcceptanceWithDetailsDto> result = new PagedResult<>();
        if (parents.isEmpty()) {
            result.setItems(List.of());
            result.setTotalCount(parentPage.getTotal());
            result.setPageIndex(pageIndex);
            result.setPageSize(pageSize);
            return result;
        }

        // 批量查询关联的验收明细
        List<Long> parentIds = parents.stream().map(AcceptanceOrder::getAcceptanceId).collect(Collectors.toList());
        QueryWrapper<AcceptanceDetail> wrapper = new QueryWrapper<>();
        wrapper.in("acceptance_id", parentIds);
        wrapper.orderByAsc("seq");
        List<AcceptanceDetail> allDetails = acceptanceDetailMapper.selectList(wrapper);
        Map<Long, List<AcceptanceDetail>> detailsMap = allDetails.stream()
                .collect(Collectors.groupingBy(AcceptanceDetail::getAcceptanceId));

        // 批量解析仓库名称（按生产单位ID关联 production_unit 表）
        Map<Long, ProductionUnit> unitMap = loadProductionUnitMap(parents);

        // 组装带子表数据的DTO
        List<AcceptanceWithDetailsDto> dtos = parents.stream().map(parent -> {
            AcceptanceWithDetailsDto dto = new AcceptanceWithDetailsDto();
            BeanUtils.copyProperties(parent, dto);
            dto.setDetails(detailsMap.getOrDefault(parent.getAcceptanceId(), List.of()));
            // 回填仓库名称
            if (parent.getProdUnitId() != null) {
                ProductionUnit unit = unitMap.get(parent.getProdUnitId());
                if (unit != null) {
                    dto.setWarehouseName(unit.getProdUnitName());
                }
            }
            return dto;
        }).collect(Collectors.toList());

        result.setItems(dtos);
        result.setTotalCount(parentPage.getTotal());
        result.setPageIndex(pageIndex);
        result.setPageSize(pageSize);
        return result;
    }

    // endregion

    // region 状态流转
    // ===================================
    // 状态流转
    // ===================================

    /**
     * 确认到货：运输中 → 验收中
     * <p>确认到货后明细进入"待初验"，同步将关联采购订单主单状态置为"验收中"、采购订单明细置为"待初验"，
     * 保证验收与采购状态一致</p>
     *
     * @param acceptanceId 验收单ID
     */
    @Override
    @Transactional
    public void confirmArrival(Long acceptanceId) {
        AcceptanceOrder acceptance = getAcceptanceOrThrow(acceptanceId);
        if (!AcceptanceStatusPolicy.MAIN_TRANSPORTING.equals(acceptance.getStatus())) {
            throw new RuntimeException("仅运输中的验收单可确认到货");
        }
        acceptance.setStatus(AcceptanceStatusPolicy.MAIN_INSPECTING);
        appendRemark(acceptance, "确认到货");
        acceptanceOrderMapper.updateById(acceptance);

        // 明细进入"待初验"（流转起点，幂等）
        List<AcceptanceDetail> details = getDetailsByAcceptanceId(acceptanceId);
        for (AcceptanceDetail detail : details) {
            if (!StringUtils.hasText(detail.getStatus())) {
                detail.setStatus(AcceptanceStatusPolicy.DETAIL_PENDING_INITIAL);
                acceptanceDetailMapper.updateById(detail);
            }
        }

        // 同步关联采购订单主单状态为"验收中"、采购订单明细为"待初验"
        syncPurchaseOrderStatus(acceptance);

        // 同步关联领料单状态为"验收中"
        syncMaterialRequisitionSlipStatus(acceptance, AcceptanceStatusPolicy.MAIN_INSPECTING);
    }

    /**
     * 初验处理（逐行）：所选明细行 待初验 → 合格：待检验 / 不合格：待退货
     * <p>
     * detailIds 为空时自动推进全部「待初验」行（批量推进）；
     * 各行独立流转互不影响，主单状态按全部明细汇总派生
     * </p>
     *
     * @param acceptanceId 验收单ID
     * @param detailIds    目标明细行ID列表，空/null表示全部待初验行
     * @param pass         是否合格（true-合格/false-不合格，不允许为空）
     * @param remark       初验备注说明
     */
    @Override
    @Transactional
    public void inspect(Long acceptanceId, List<Long> detailIds, Boolean pass, String remark) {
        if (pass == null) {
            throw new RuntimeException("必须明确指定检验结果（pass=true合格/false不合格）");
        }
        AcceptanceOrder acceptance = getAcceptanceOrThrow(acceptanceId);
        if (!AcceptanceStatusPolicy.MAIN_INSPECTING.equals(acceptance.getStatus())) {
            throw new RuntimeException("仅验收中的验收单可进行初验");
        }
        List<AcceptanceDetail> allDetails = getDetailsByAcceptanceId(acceptanceId);
        // 解析目标行：所选行必须全部处于待初验（空则取全部待初验行）
        List<AcceptanceDetail> targets = resolveActionRows(allDetails, detailIds,
                AcceptanceStatusPolicy.DETAIL_PENDING_INITIAL, AcceptanceStatusPolicy.DETAIL_PENDING_INITIAL);

        if (pass) {
            // 合格：所选行进入待检验
            markDetailsStatus(targets, AcceptanceStatusPolicy.DETAIL_PENDING_QUALITY, null, null);
            appendRemark(acceptance, "初验合格: " + (StringUtils.hasText(remark) ? remark : "无异常"));
        } else {
            // 不合格：所选行进入待退货，退货原因只读带出"初验不合格"
            markDetailsStatus(targets, AcceptanceStatusPolicy.DETAIL_PENDING_RETURN,
                    AcceptanceStatusPolicy.REASON_INITIAL_UNQUALIFIED,
                    StringUtils.hasText(remark) ? remark : "存在质量问题");
            appendRemark(acceptance, "初验不合格: " + (StringUtils.hasText(remark) ? remark : "存在质量问题"));
        }
        // 主单状态按全部明细派生（部分行推进后仍为验收中，全退则已结束）
        applyDerivedMainStatus(acceptance, allDetails);
        acceptanceOrderMapper.updateById(acceptance);

        // 同步关联采购订单明细状态与主单状态
        syncPurchaseOrderStatus(acceptance);
    }

    /**
     * 解析行级操作的目标明细行
     * <p>
     * detailIds 为空时自动取全部处于期望状态的行（批量推进语义）；
     * 非空时逐行校验存在性与状态，任一行不满足即整体拒绝
     * </p>
     *
     * @param allDetails   验收单全部明细
     * @param detailIds    请求指定的明细行ID，空/null表示自动选取
     * @param expectStatus 期望的明细状态
     * @param statusLabel  状态中文名（错误提示用）
     * @return 目标明细行列表
     */
    private List<AcceptanceDetail> resolveActionRows(List<AcceptanceDetail> allDetails,
                                                     List<Long> detailIds,
                                                     String expectStatus,
                                                     String statusLabel) {
        if (detailIds == null || detailIds.isEmpty()) {
            List<AcceptanceDetail> rows = allDetails.stream()
                    .filter(d -> !StringUtils.hasText(d.getStatus()) || expectStatus.equals(d.getStatus()))
                    .collect(Collectors.toList());
            if (rows.isEmpty()) {
                throw new RuntimeException("没有" + statusLabel + "的明细行");
            }
            return rows;
        }
        Set<Long> idSet = new HashSet<>(detailIds);
        List<AcceptanceDetail> rows = allDetails.stream()
                .filter(d -> idSet.contains(d.getDetailId()))
                .collect(Collectors.toList());
        if (rows.size() != idSet.size()) {
            throw new RuntimeException("部分明细行不存在或不属于该验收单");
        }
        boolean allMatch = rows.stream().allMatch(d ->
                !StringUtils.hasText(d.getStatus()) || expectStatus.equals(d.getStatus()));
        if (!allMatch) {
            throw new RuntimeException("所选明细行不全部处于" + statusLabel + "状态");
        }
        return rows;
    }

    /**
     * 检验处理（逐行）：所选明细行 待检验 → 合格：待入库 / 不合格：待退货
     * <p>
     * detailIds 为空时自动推进全部「待检验」行（批量推进）；
     * 合格时校验所选行批号必填；入库由 {@link #inbound} 在全部明细终结后整单执行，
     * 本方法不再直接产生库存
     * </p>
     *
     * @param acceptanceId 验收单ID
     * @param detailIds    目标明细行ID列表，空/null表示全部待检验行
     * @param pass         是否合格（true-合格/false-不合格，不允许为空）
     * @param remark       检验备注说明
     */
    @Override
    @Transactional
    public void qualityCheck(Long acceptanceId, List<Long> detailIds, Boolean pass, String remark) {
        if (pass == null) {
            throw new RuntimeException("必须明确指定检验结果（pass=true合格/false不合格）");
        }
        AcceptanceOrder acceptance = getAcceptanceOrThrow(acceptanceId);
        if (!AcceptanceStatusPolicy.MAIN_INSPECTING.equals(acceptance.getStatus())) {
            throw new RuntimeException("仅验收中的验收单可进行检验");
        }
        List<AcceptanceDetail> allDetails = getDetailsByAcceptanceId(acceptanceId);
        // 解析目标行：所选行必须全部处于待检验（空则取全部待检验行）
        List<AcceptanceDetail> targets = resolveActionRows(allDetails, detailIds,
                AcceptanceStatusPolicy.DETAIL_PENDING_QUALITY, AcceptanceStatusPolicy.DETAIL_PENDING_QUALITY);

        if (pass) {
            // 校验：所选行必须填写批号（入库前锁定）
            boolean noBatch = targets.stream().anyMatch(d -> !StringUtils.hasText(d.getBatchNumber()));
            if (noBatch) {
                throw new RuntimeException("存在未填写批号的物料，请先补充批号后再检验");
            }
            // 合格：所选行进入待入库，主单按明细派生（全终结含待入库 → 验收中）
            markDetailsStatus(targets, AcceptanceStatusPolicy.DETAIL_PENDING_INBOUND, null, null);
            appendRemark(acceptance, "检验合格: " + (StringUtils.hasText(remark) ? remark : "合格"));
        } else {
            // 不合格：所选行进入待退货，退货原因只读带出"检验不合格"
            markDetailsStatus(targets, AcceptanceStatusPolicy.DETAIL_PENDING_RETURN,
                    AcceptanceStatusPolicy.REASON_QUALITY_UNQUALIFIED,
                    StringUtils.hasText(remark) ? remark : "质量不达标");
            appendRemark(acceptance, "检验不合格: " + (StringUtils.hasText(remark) ? remark : "质量不达标"));
        }
        // 主单按全部明细派生（全退则为已结束，存在待入库则仍为验收中）
        applyDerivedMainStatus(acceptance, allDetails);
        acceptanceOrderMapper.updateById(acceptance);

        // 同步关联采购订单明细状态与主单状态
        syncPurchaseOrderStatus(acceptance);
    }

    /**
     * 整单入库：全部明细终结后一次性生成整单入库单
     * <p>
     * 可执行条件：全部明细均为终结状态且至少一条「待入库」；
     * 为每条待入库明细指定仓库（缺省回退验收单级仓库）、校验批号必填后，
     * 生成入库单并写库存与流水，合格明细→已入库、主单→已入库，
     * 同步关联采购订单与领料单状态
     * </p>
     *
     * @param acceptanceId 验收单ID
     * @param items        待入库明细的仓库指定列表
     * @return 自动生成的入库单（含操作人姓名/仓库名）
     */
    @Override
    @Transactional
    public StockInWithNamesDto inbound(Long acceptanceId, List<AcceptanceInboundRequest.Item> items) {
        AcceptanceOrder acceptance = getAcceptanceOrThrow(acceptanceId);
        if (!AcceptanceStatusPolicy.MAIN_INSPECTING.equals(acceptance.getStatus())) {
            throw new RuntimeException("仅验收中的验收单可入库");
        }
        List<AcceptanceDetail> allDetails = getDetailsByAcceptanceId(acceptanceId);
        // 前置校验：所有明细均已终结（待入库/待退货/已退货/已重发/已入库/已取消）
        boolean hasNonTerminal = allDetails.stream()
                .anyMatch(d -> !AcceptanceStatusPolicy.isDetailTerminal(d.getStatus()));
        if (hasNonTerminal) {
            throw new RuntimeException("存在未完成初验/检验的明细行，请先将全部明细推进至终结状态");
        }
        List<AcceptanceDetail> pendingInbound = allDetails.stream()
                .filter(d -> AcceptanceStatusPolicy.DETAIL_PENDING_INBOUND.equals(d.getStatus()))
                .collect(Collectors.toList());
        if (pendingInbound.isEmpty()) {
            throw new RuntimeException("没有可入库的明细（待入库）");
        }

        // 解析每条待入库明细的仓库（明细级优先，缺省回退验收单级仓库），并校验批号
        Map<Long, Long> unitMap = new java.util.HashMap<>();
        if (items != null) {
            for (AcceptanceInboundRequest.Item item : items) {
                if (item != null && item.getDetailId() != null && item.getProdUnitId() != null) {
                    unitMap.put(item.getDetailId(), item.getProdUnitId());
                }
            }
        }
        for (AcceptanceDetail detail : pendingInbound) {
            Long unitId = unitMap.get(detail.getDetailId());
            if (unitId == null) {
                unitId = acceptance.getProdUnitId();
            }
            if (unitId == null) {
                throw new RuntimeException("请为待入库明细选择仓库: " + detail.getMaterialName());
            }
            if (!StringUtils.hasText(detail.getBatchNumber())) {
                throw new RuntimeException("存在未填写批号的物料，无法入库: " + detail.getMaterialName());
            }
            detail.setProdUnitId(unitId);
        }
        // 单据级仓库取第一明细（兼容旧展示），明细级仓库各自保留
        acceptance.setProdUnitId(pendingInbound.get(0).getProdUnitId());
        appendRemark(acceptance, "整单入库");

        // 生成入库单与库存流水（入库单主表仓库=第一明细仓库，明细级仓库各自写入）
        StockIn stockIn = applyAcceptanceInbound(acceptance, pendingInbound);

        // 待入库明细 → 已入库，主单派生为已入库
        markDetailsStatus(pendingInbound, AcceptanceStatusPolicy.DETAIL_INBOUND, null, null);
        applyDerivedMainStatus(acceptance, allDetails);
        acceptanceOrderMapper.updateById(acceptance);

        // 同步关联采购订单明细状态与主单状态
        syncPurchaseOrderStatus(acceptance);

        // 同步关联领料单状态为"已入库"
        syncMaterialRequisitionSlipStatus(acceptance, AcceptanceStatusPolicy.MAIN_INBOUND);

        // 组装返回DTO：补全操作人姓名与仓库名称
        return buildStockInWithNames(stockIn);
    }

    /**
     * 部分验收（拆行）：将一行物料拆为「验收子行 + 退货子行」
     * <p>
     * 校验两段数量均大于 0 且之和等于原行实际到货数量（未填实际到货数量时回退采购数量）；
     * 采购数量保持不变，实际到货数量按验收/退货数量拆分；
     * 验收子行按环节推进（初验→待检验，检验→待入库），退货子行→待退货并记录原行序号，
     * 退货子行批号沿用原明细行、仓库清空；
     * 关联采购订单明细同步拆行（退货子行获得独立订单明细锚点），金额与标准量差值按拆分后数量重算
     * </p>
     *
     * @param detailId  被拆分的验收明细ID
     * @param stage     拆分环节（初验 / 检验）
     * @param acceptQty 验收数量
     * @param returnQty 退货数量
     * @param remark    备注说明
     */
    @Override
    @Transactional
    public void partialAcceptance(Long detailId, String stage, BigDecimal acceptQty,
                                  BigDecimal returnQty, String remark) {
        // 参数校验
        if (detailId == null) {
            throw new RuntimeException("明细ID不能为空");
        }
        boolean isInitialStage = AcceptanceStatusPolicy.STAGE_INITIAL.equals(stage);
        boolean isQualityStage = AcceptanceStatusPolicy.STAGE_QUALITY.equals(stage);
        if (!isInitialStage && !isQualityStage) {
            throw new RuntimeException("不支持的拆分环节: " + stage);
        }
        if (acceptQty == null || acceptQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("验收数量必须大于0");
        }
        if (returnQty == null || returnQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("退货数量必须大于0");
        }
        AcceptanceDetail detail = acceptanceDetailMapper.selectById(detailId);
        if (detail == null) {
            throw new RuntimeException("验收明细不存在");
        }
        AcceptanceOrder acceptance = getAcceptanceOrThrow(detail.getAcceptanceId());
        if (!AcceptanceStatusPolicy.MAIN_INSPECTING.equals(acceptance.getStatus())) {
            throw new RuntimeException("仅验收中的验收单可部分验收");
        }

        // 环节与明细状态匹配校验，并确定验收子行状态与退货子行原因
        String acceptChildStatus;
        String returnReason;
        if (isInitialStage) {
            if (!AcceptanceStatusPolicy.DETAIL_PENDING_INITIAL.equals(detail.getStatus())) {
                throw new RuntimeException("仅待初验的明细行可在初验环节部分验收");
            }
            acceptChildStatus = AcceptanceStatusPolicy.DETAIL_PENDING_QUALITY;
            returnReason = AcceptanceStatusPolicy.REASON_INITIAL_UNQUALIFIED;
        } else {
            if (!AcceptanceStatusPolicy.DETAIL_PENDING_QUALITY.equals(detail.getStatus())) {
                throw new RuntimeException("仅待检验的明细行可在检验环节部分验收");
            }
            if (!StringUtils.hasText(detail.getBatchNumber())) {
                throw new RuntimeException("检验环节部分验收前须填写批号: " + detail.getMaterialName());
            }
            acceptChildStatus = AcceptanceStatusPolicy.DETAIL_PENDING_INBOUND;
            returnReason = AcceptanceStatusPolicy.REASON_QUALITY_UNQUALIFIED;
        }

        // 数量守恒：验收数量 + 退货数量 = 原行实际到货数量（未填实际到货数量时回退采购数量）
        boolean baseIsActualArrival = detail.getActualArrivalQty() != null;
        BigDecimal baseQty = baseIsActualArrival ? detail.getActualArrivalQty() : detail.getQuantity();
        if (baseQty == null) {
            throw new RuntimeException("明细采购数量缺失，无法拆分");
        }
        if (acceptQty.add(returnQty).compareTo(baseQty) != 0) {
            throw new RuntimeException("验收数量与退货数量之和必须等于"
                    + (baseIsActualArrival ? "实际到货数量" : "采购数量"));
        }

        // 关联采购订单明细同步拆行：退货子行获得独立订单明细锚点
        Long newPurchaseItemId = null;
        PurchaseOrderItems originalItem = detail.getPurchaseItemId() != null
                ? purchaseOrderItemsMapper.selectById(detail.getPurchaseItemId()) : null;
        if (originalItem != null) {
            PurchaseOrderItems returnItem = new PurchaseOrderItems();
            BeanUtils.copyProperties(originalItem, returnItem, "id");
            returnItem.setPurchaseQuantity(returnQty);
            returnItem.setActualArrivalQty(null);
            returnItem.setAmount(originalItem.getUnitPrice() != null
                    ? returnQty.multiply(originalItem.getUnitPrice()) : null);
            returnItem.setDifference(returnQty.subtract(originalItem.getStandardDosage() != null
                    ? originalItem.getStandardDosage() : BigDecimal.ZERO));
            returnItem.setStatus(AcceptanceStatusPolicy.DETAIL_PENDING_RETURN);
            purchaseOrderItemsMapper.insert(returnItem);
            newPurchaseItemId = returnItem.getId();

            // 原订单明细数量调整为验收数量（状态由后续同步按验收子行回写）
            originalItem.setPurchaseQuantity(acceptQty);
            originalItem.setActualArrivalQty(null);
            originalItem.setAmount(originalItem.getUnitPrice() != null
                    ? acceptQty.multiply(originalItem.getUnitPrice()) : null);
            originalItem.setDifference(acceptQty.subtract(originalItem.getStandardDosage() != null
                    ? originalItem.getStandardDosage() : BigDecimal.ZERO));
            purchaseOrderItemsMapper.updateById(originalItem);
        }

        // 构造退货子行（在原行被改造前基于原始值复制，与原行同属该验收单，批号随复制沿用原行）
        AcceptanceDetail returnChild = new AcceptanceDetail();
        BeanUtils.copyProperties(detail, returnChild,
                "detailId", "seq", "quantity", "amount", "diffQuantity",
                "actualArrivalQty", "inboundQty", "status", "returnReason", "returnRemark",
                "purchaseItemId", "prodUnitId", "originalSeq",
                "createdBy", "createdTime");
        returnChild.setOriginalSeq(detail.getSeq());
        // 退货子行：采购数量不变（保持原值），实际到货数量=退货数量
        returnChild.setQuantity(detail.getQuantity());
        returnChild.setActualArrivalQty(returnQty);
        returnChild.setStatus(AcceptanceStatusPolicy.DETAIL_PENDING_RETURN);
        returnChild.setReturnReason(returnReason);
        returnChild.setReturnRemark(StringUtils.hasText(remark) ? remark : null);
        returnChild.setPurchaseItemId(newPurchaseItemId);
        // 退货子行不入库：清空仓库（批号沿用原明细行便于追溯）；原行批号为空时置空串保持一致
        returnChild.setBatchNumber(StringUtils.hasText(detail.getBatchNumber()) ? detail.getBatchNumber() : "");
        returnChild.setProdUnitId(null);
        recalculateAmountAndDiff(returnChild, returnQty);

        // 退货子行序号取该单最大序号 + 1（原行序号保持不变）
        List<AcceptanceDetail> siblings = getDetailsByAcceptanceId(acceptance.getAcceptanceId());
        int maxSeq = siblings.stream()
                .map(AcceptanceDetail::getSeq)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .max().orElse(0);
        returnChild.setSeq(maxSeq + 1);
        acceptanceDetailMapper.insert(returnChild);

        // 原行改造为验收子行：采购数量不变，实际到货数量=验收数量，状态按环节推进，金额与差值重算
        detail.setActualArrivalQty(acceptQty);
        detail.setStatus(acceptChildStatus);
        detail.setReturnReason(null);
        detail.setReturnRemark(null);
        recalculateAmountAndDiff(detail, acceptQty);
        acceptanceDetailMapper.updateById(detail);

        // 主单备注与状态派生
        appendRemark(acceptance, "部分验收: 验收" + acceptQty.toPlainString()
                + " 退货" + returnQty.toPlainString()
                + (StringUtils.hasText(remark) ? "（" + remark + "）" : ""));
        List<AcceptanceDetail> latestDetails = getDetailsByAcceptanceId(acceptance.getAcceptanceId());
        applyDerivedMainStatus(acceptance, latestDetails);
        acceptanceOrderMapper.updateById(acceptance);

        // 同步关联采购订单明细状态与主单状态（验收子行与退货子行各自回写）
        syncPurchaseOrderStatus(acceptance);
    }

    /**
     * 按指定采购数量重算明细金额与标准量差值
     * <p>金额 = 数量 × 单价；差值 = 数量 - 标准处方量（与新增明细计算规则一致）</p>
     *
     * @param detail 验收明细（内存对象就地更新）
     * @param qty    计算基准数量
     */
    private void recalculateAmountAndDiff(AcceptanceDetail detail, BigDecimal qty) {
        detail.setAmount(qty.multiply(detail.getUnitPrice() != null ? detail.getUnitPrice() : BigDecimal.ZERO));
        BigDecimal std = detail.getStandardDosage() != null ? detail.getStandardDosage() : BigDecimal.ZERO;
        detail.setDiffQuantity(qty.subtract(std));
    }

    // endregion

    // region 对外同步入口
    // ===================================
    // 对外同步入口
    // ===================================

    /**
     * 将验收单状态同步至关联采购订单（供物料退货管理等模块调用）
     * <p>委托私有同步方法：明细级回写 + 主单同名映射，已作废订单不覆盖</p>
     *
     * @param acceptance 状态已变更且明细已落库的验收单
     */
    @Override
    @Transactional
    public void syncPurchaseStatus(AcceptanceOrder acceptance) {
        syncPurchaseOrderStatus(acceptance);
    }

    // endregion

    // region 私有工具方法
    // ===================================
    // 私有工具方法
    // ===================================

    /**
     * 查询验收单，不存在时抛出异常
     *
     * @param acceptanceId 验收单ID
     * @return 验收单实体
     */
    private AcceptanceOrder getAcceptanceOrThrow(Long acceptanceId) {
        AcceptanceOrder acceptance = acceptanceOrderMapper.selectById(acceptanceId);
        if (acceptance == null) {
            throw new RuntimeException("验收单不存在");
        }
        return acceptance;
    }

    /**
     * 保存验收明细列表（自动计算序号、金额、标准量差值）
     * <p>金额以实际到货数量 × 单价计算；实际到货数量未填写时回退使用采购数量</p>
     *
     * @param acceptanceId 验收单ID
     * @param details     明细列表
     */
    private void saveDetails(Long acceptanceId, List<AcceptanceDetail> details) {
        if (details != null && !details.isEmpty()) {
            int seq = 1;
            for (AcceptanceDetail detail : details) {
                detail.setAcceptanceId(acceptanceId);
                detail.setSeq(seq++);
                // 明细状态默认"待初验"（流转起点）
                if (!StringUtils.hasText(detail.getStatus())) {
                    detail.setStatus(AcceptanceStatusPolicy.DETAIL_PENDING_INITIAL);
                }
                // 计算金额 = 实际到货数量 × 单价（未填实际到货数量时回退采购数量）
                if (detail.getAmount() == null) {
                    BigDecimal qtyForAmount = detail.getActualArrivalQty() != null
                            ? detail.getActualArrivalQty()
                            : detail.getQuantity();
                    if (qtyForAmount != null) {
                        detail.setAmount(qtyForAmount.multiply(
                                detail.getUnitPrice() != null ? detail.getUnitPrice() : BigDecimal.ZERO));
                    }
                }
                // 计算标准量差值 = 实际到货数量 - 标准处方量
                if (detail.getDiffQuantity() == null) {
                    BigDecimal qtyForDiff = detail.getActualArrivalQty() != null
                            ? detail.getActualArrivalQty()
                            : detail.getQuantity();
                    if (qtyForDiff != null) {
                        BigDecimal std = detail.getStandardDosage() != null ? detail.getStandardDosage() : BigDecimal.ZERO;
                        detail.setDiffQuantity(qtyForDiff.subtract(std));
                    }
                }
                acceptanceDetailMapper.insert(detail);
            }
        }
    }

    /**
     * 追加备注（流程节点自动记录）
     *
     * @param acceptance 验收单实体
     * @param content    追加内容
     */
    private void appendRemark(AcceptanceOrder acceptance, String content) {
        String oldRemark = acceptance.getRemark();
        acceptance.setRemark(StringUtils.hasText(oldRemark)
                ? oldRemark + " | " + content
                : content);
    }

    /**
     * 验收合格入库的库存联动
     * <p>
     * 将验收明细转换为入库明细（状态=合格），先创建真实的入库单（已入库状态）及明细，
     * 再调用公共库存服务增加库存并写流水，保证流水可追溯、数据完整
     * </p>
     *
     * @param acceptance 验收单主表
     * @param details    验收明细列表
     * @return 自动生成的入库单（含主表关键字段：关联生产计划编号/总金额/仓库/操作人）
     */
    private StockIn applyAcceptanceInbound(AcceptanceOrder acceptance, List<AcceptanceDetail> details) {
        // 构造入库单（inType 取验收来源类型，关联单号取验收单号，直接为已入库状态）
        StockIn stockIn = new StockIn();
        stockIn.setInCode(sequenceService.generateStockInCode());
        stockIn.setInType(acceptance.getSourceType() != null ? acceptance.getSourceType() : "采购入库");
        // 单据级仓库取验收单级值（整单入库时已回填为第一条明细仓库，兼容旧展示）
        stockIn.setProdUnitId(acceptance.getProdUnitId());
        stockIn.setRelatedOrder(acceptance.getAcceptanceCode());
        // 关联生产计划编号：优先取关联采购订单的生产计划编号，其次回退验收单自带计划编号
        stockIn.setPlanNumber(resolvePlanNumber(acceptance));
        // 关联采购单标题：复用采购订单查询结果，无匹配时置空
        PurchaseOrders purchaseOrder = resolvePurchaseOrder(acceptance);
        stockIn.setRelatedOrderTitle(purchaseOrder != null ? purchaseOrder.getTitle() : null);
        // 关联生产计划标题：按生产计划编号反查计划名称，无匹配时置空
        stockIn.setPlanTitle(resolvePlanTitle(stockIn.getPlanNumber()));
        stockIn.setInDate(LocalDateTime.now());
        stockIn.setInStatus("已入库");
        stockIn.setRemark("货物验收合格自动入库: " + acceptance.getAcceptanceCode());
        stockInMapper.insert(stockIn);

        // 构造并保存入库明细（库存状态默认合格）
        // 物料名称/分类/单位以物料主数据（material表）为权威来源，防止验收明细误传分类值污染入库单与库存
        Map<String, Material> materialMap = loadMaterialMapByCode(details);
        List<StockInDetail> stockInDetails = details.stream().map(d -> {
            StockInDetail sid = new StockInDetail();
            sid.setInId(stockIn.getInId());
            sid.setItemType(StringUtils.hasText(d.getItemType()) ? d.getItemType() : "material");
            sid.setItemId(d.getItemId());
            sid.setItemCode(d.getMaterialCode());
            Material material = materialMap.get(d.getMaterialCode());
            sid.setItemName(material != null && StringUtils.hasText(material.getMaterialName())
                    ? material.getMaterialName() : d.getMaterialName());
            sid.setCategoryName(material != null && StringUtils.hasText(material.getCategoryName())
                    ? material.getCategoryName() : d.getMaterialCategory());
            sid.setUnitName(material != null && StringUtils.hasText(material.getUnitName())
                    ? material.getUnitName() : d.getUnitName());
            sid.setBatchNumber(d.getBatchNumber());
            // 明细级入库仓库（每条物料独立选择，缺省回退单据级仓库）
            sid.setProdUnitId(d.getProdUnitId() != null ? d.getProdUnitId() : acceptance.getProdUnitId());
            // 入库数量：优先取入库数量，其次实际到货数量，最后回退采购数量
            BigDecimal inboundQty = d.getInboundQty() != null ? d.getInboundQty()
                    : d.getActualArrivalQty() != null ? d.getActualArrivalQty()
                    : d.getQuantity();
            sid.setQuantity(inboundQty);
            sid.setUnitPrice(d.getUnitPrice());
            // 金额以实际到货数量 × 单价计算
            sid.setAmount(inboundQty != null && d.getUnitPrice() != null
                    ? inboundQty.multiply(d.getUnitPrice())
                    : d.getAmount());
            sid.setExpiryDate(d.getExpiryDate());
            sid.setStockStatus("合格");
            stockInDetailMapper.insert(sid);
            return sid;
        }).collect(Collectors.toList());

        // 主表总金额：汇总入库明细金额（未携带金额的明细按0计入）
        BigDecimal totalAmount = stockInDetails.stream()
                .map(StockInDetail::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stockIn.setTotalAmount(totalAmount);
        stockInMapper.updateById(stockIn);

        // 调用公共库存服务：增加库存并写流水
        stockService.applyInbound(stockIn, stockInDetails);

        return stockIn;
    }

    /**
     * 解析关联采购订单
     * <p>
     * 优先取验收单携带的采购单号（purchase_number），无则回退关联单号。
     * 用于复用同一查询结果获取采购单标题与生产计划编号，避免重复查询
     * </p>
     *
     * @param acceptance 验收单
     * @return 关联采购订单，无关联采购订单时返回null
     */
    private PurchaseOrders resolvePurchaseOrder(AcceptanceOrder acceptance) {
        String purchaseNumber = StringUtils.hasText(acceptance.getPurchaseNumber())
                ? acceptance.getPurchaseNumber() : acceptance.getRelatedOrder();
        if (StringUtils.hasText(purchaseNumber)) {
            return purchaseOrdersMapper.selectOne(new QueryWrapper<PurchaseOrders>()
                    .eq("purchase_number", purchaseNumber));
        }
        return null;
    }

    /**
     * 解析关联生产计划编号
     * <p>
     * 优先取关联采购订单携带的生产计划编号（purchase_orders.production_plan_code，
     * 与 production_plan.plan_number 对应），无关联采购订单或未携带时回退验收单自带计划编号
     * </p>
     *
     * @param acceptance 验收单
     * @return 生产计划编号，无匹配时返回null
     */
    private String resolvePlanNumber(AcceptanceOrder acceptance) {
        PurchaseOrders order = resolvePurchaseOrder(acceptance);
        if (order != null && StringUtils.hasText(order.getProductionPlanCode())) {
            return order.getProductionPlanCode();
        }
        return acceptance.getPlanCode();
    }

    /**
     * 解析关联生产计划标题
     * <p>
     * 按生产计划编号（production_plan.plan_number）反查计划名称（plan_name），
     * 用于入库单展示关联生产计划标题，无匹配时返回null
     * </p>
     *
     * @param planNumber 生产计划编号
     * @return 生产计划标题（计划名称），无匹配时返回null
     */
    private String resolvePlanTitle(String planNumber) {
        if (!StringUtils.hasText(planNumber)) {
            return null;
        }
        ProductionPlan plan = productionPlanMapper.selectOne(new QueryWrapper<ProductionPlan>()
                .eq("plan_number", planNumber));
        return plan != null ? plan.getPlanName() : null;
    }

    /**
     * 按物料编码加载物料主数据映射
     * <p>
     * 用于验收入库时以物料主数据名称/分类/单位为权威来源覆盖入库明细，
     * 避免验收明细误传分类值（如"原料"/"辅料"）污染入库单与库存
     * </p>
     *
     * @param details 验收明细列表
     * @return 物料编码到物料主数据的映射，未匹配时为空映射
     */
    private Map<String, Material> loadMaterialMapByCode(List<AcceptanceDetail> details) {
        List<String> codes = details.stream()
                .map(AcceptanceDetail::getMaterialCode)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
        if (codes.isEmpty()) {
            return Map.of();
        }
        QueryWrapper<Material> wrapper = new QueryWrapper<>();
        wrapper.in("material_code", codes);
        return materialMapper.selectList(wrapper).stream()
                .collect(Collectors.toMap(Material::getMaterialCode, m -> m, (a, b) -> a));
    }

    /**
     * 批量加载关联制剂名称映射（通过验收单 plan_code 关联 production_plan）
     *
     * @param parents 验收单列表
     * @return 生产计划编号到制剂名称的映射，无数据时返回空映射
     */
    private Map<String, String> loadPreparationNameMap(List<AcceptanceOrder> parents) {
        List<String> planCodes = parents.stream()
                .map(AcceptanceOrder::getPlanCode)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
        if (planCodes.isEmpty()) {
            return Map.of();
        }
        QueryWrapper<ProductionPlan> wrapper = new QueryWrapper<>();
        wrapper.in("plan_number", planCodes);
        wrapper.select("plan_number", "preparation_name");
        return productionPlanMapper.selectList(wrapper).stream()
                .collect(Collectors.toMap(
                        ProductionPlan::getPlanNumber,
                        p -> p.getPreparationName() != null ? p.getPreparationName() : "",
                        (a, b) -> a));
    }

    /**
     * 批量加载生产单位映射（按生产单位ID索引）
     *
     * @param parents 验收单列表
     * @return 生产单位ID到生产单位实体的映射，无数据时返回空映射
     */
    private Map<Long, ProductionUnit> loadProductionUnitMap(List<AcceptanceOrder> parents) {
        List<Long> unitIds = parents.stream()
                .map(AcceptanceOrder::getProdUnitId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (unitIds.isEmpty()) {
            return Map.of();
        }
        return productionUnitMapper.selectBatchIds(unitIds).stream()
                .collect(Collectors.toMap(ProductionUnit::getProdUnitId, u -> u, (a, b) -> a));
    }

    /**
     * 组装入库单返回DTO，补全操作人姓名与仓库名称
     * <p>
     * 操作人姓名取用户表 userName（空则回退 userAccount），仓库名称取生产单位表 prodUnitName，
     * 均未找到时保持null，不影响正常返回
     * </p>
     *
     * @param stockIn 自动生成的入库单
     * @return 携带操作人姓名与仓库名称的入库单DTO
     */
    private StockInWithNamesDto buildStockInWithNames(StockIn stockIn) {
        StockInWithNamesDto dto = new StockInWithNamesDto();
        BeanUtils.copyProperties(stockIn, dto);
        if (stockIn.getCreatedBy() != null) {
            User user = userMapper.selectById(stockIn.getCreatedBy());
            if (user != null) {
                dto.setCreatedByName(StringUtils.hasText(user.getUserName())
                        ? user.getUserName() : user.getUserAccount());
            }
        }
        if (stockIn.getProdUnitId() != null) {
            ProductionUnit unit = productionUnitMapper.selectById(stockIn.getProdUnitId());
            if (unit != null) {
                dto.setWarehouseName(unit.getProdUnitName());
            }
        }
        return dto;
    }

    /**
     * 批量更新明细状态（流转落库）
     * <p>同时可写入退货原因与退货备注留痕（传null表示不修改对应留痕）</p>
     *
     * @param details      验收明细列表（内存对象同步更新）
     * @param status       目标明细状态
     * @param returnReason 退货原因，null表示不修改
     * @param returnRemark 退货备注，null表示不修改
     */
    private void markDetailsStatus(List<AcceptanceDetail> details, String status,
                                   String returnReason, String returnRemark) {
        if (details == null) {
            return;
        }
        for (AcceptanceDetail detail : details) {
            detail.setStatus(status);
            if (returnReason != null) {
                detail.setReturnReason(returnReason);
            }
            if (returnRemark != null) {
                detail.setReturnRemark(returnRemark);
            }
            acceptanceDetailMapper.updateById(detail);
        }
    }

    /**
     * 按明细状态派生并回填主单状态
     * <p>派生规则由 AcceptanceStatusPolicy.deriveMainStatus 承载</p>
     *
     * @param acceptance 验收单实体（状态就地更新）
     * @param details    验收明细列表（明细状态须已更新为最终值）
     */
    private void applyDerivedMainStatus(AcceptanceOrder acceptance, List<AcceptanceDetail> details) {
        List<String> statuses = details.stream()
                .map(AcceptanceDetail::getStatus)
                .collect(Collectors.toList());
        acceptance.setStatus(AcceptanceStatusPolicy.deriveMainStatus(acceptance.getStatus(), statuses));
    }

    /**
     * 同步关联采购订单状态（明细级 + 主单级）
     * <p>
     * 同步三步：
     * <ol>
     *   <li>已作废的采购订单不覆盖（作废为终态）</li>
     *   <li>按验收明细的 purchaseItemId 回写采购订单明细状态（purchase_order_items.status），
     *       实现"明细带动订单明细"</li>
     *   <li>验收单主单状态按同名映射同步采购订单主单状态（验收单主单已由明细派生，即权威结果）</li>
     * </ol>
     * 按验收单关联的采购订单号（优先 purchase_number，其次 related_order）定位采购订单；
     * 无关联采购订单时静默跳过（如非采购来源的验收单）
     * </p>
     *
     * @param acceptance 状态已变更的验收单（明细状态须已落库）
     */
    private void syncPurchaseOrderStatus(AcceptanceOrder acceptance) {
        if (acceptance == null || !StringUtils.hasText(acceptance.getStatus())) {
            return;
        }
        PurchaseOrders order = resolvePurchaseOrder(acceptance);
        if (order == null) {
            return;
        }
        // 已作废为终态，不覆盖
        if (AcceptanceStatusPolicy.MAIN_VOIDED.equals(String.valueOf(order.getStatus()))) {
            return;
        }

        // 1) 明细级回写采购订单明细状态
        List<AcceptanceDetail> details = getDetailsByAcceptanceId(acceptance.getAcceptanceId());
        for (AcceptanceDetail detail : details) {
            if (detail.getPurchaseItemId() == null || !StringUtils.hasText(detail.getStatus())) {
                continue;
            }
            PurchaseOrderItems item = purchaseOrderItemsMapper.selectById(detail.getPurchaseItemId());
            if (item != null && !detail.getStatus().equals(String.valueOf(item.getStatus()))) {
                item.setStatus(detail.getStatus());
                purchaseOrderItemsMapper.updateById(item);
            }
        }

        // 2) 主单同名映射同步（验收单主单状态即派生权威结果）
        String acceptanceStatus = acceptance.getStatus();
        if (!acceptanceStatus.equals(String.valueOf(order.getStatus()))) {
            order.setStatus(acceptanceStatus);
            purchaseOrdersMapper.updateById(order);
        }
    }

    /**
     * 同步关联领料单状态
     * <p>
     * 按验收单关联的领料单ID（related_slip_id）定位领料单，
     * 存在且领料单服务已注入时调用updateStatus回写状态；无关联领料单时静默跳过
     * </p>
     *
     * @param acceptance     状态已变更的验收单
     * @param targetStatus   目标状态
     */
    private void syncMaterialRequisitionSlipStatus(AcceptanceOrder acceptance, String targetStatus) {
        if (acceptance.getRelatedSlipId() == null || materialRequisitionSlipService == null) {
            return;
        }
        try {
            materialRequisitionSlipService.updateStatus(acceptance.getRelatedSlipId(), targetStatus);
        } catch (Exception e) {
            // 领料单状态回写失败不影响验收单主流程
        }
    }

    // endregion
}
