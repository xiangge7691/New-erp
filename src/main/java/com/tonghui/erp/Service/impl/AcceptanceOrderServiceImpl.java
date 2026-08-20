package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Stock.AcceptanceWithDetailsDto;
import com.tonghui.erp.Common.Dto.Stock.StockInWithNamesDto;
import com.tonghui.erp.Data.Entity.AcceptanceDetail;
import com.tonghui.erp.Data.Entity.AcceptanceOrder;
import com.tonghui.erp.Data.Entity.Material;
import com.tonghui.erp.Data.Entity.ProductionUnit;
import com.tonghui.erp.Data.Entity.PurchaseOrders;
import com.tonghui.erp.Data.Entity.StockIn;
import com.tonghui.erp.Data.Entity.StockInDetail;
import com.tonghui.erp.Data.Entity.User;
import com.tonghui.erp.Data.mapper.AcceptanceDetailMapper;
import com.tonghui.erp.Data.mapper.AcceptanceOrderMapper;
import com.tonghui.erp.Data.mapper.MaterialMapper;
import com.tonghui.erp.Data.mapper.ProductionUnitMapper;
import com.tonghui.erp.Data.mapper.PurchaseOrdersMapper;
import com.tonghui.erp.Data.mapper.StockInDetailMapper;
import com.tonghui.erp.Data.mapper.StockInMapper;
import com.tonghui.erp.Data.mapper.UserMapper;
import com.tonghui.erp.Service.AcceptanceOrderService;
import com.tonghui.erp.Service.StockService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 货物验收单业务实现类
 * <p>
 * 实现AcceptanceOrderService接口，提供验收单的增删改查、明细管理、状态流转
 * （运输中→到货初验→物料检验→已入库/待退货→已退换）以及检验合格入库的库存联动能力。
 * 验收单每次状态变更时按同名映射同步关联采购订单状态（到货初验/物料检验/待退货/已入库），
 * 保证货物验收与采购状态实时对应
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

    /** 物料主数据访问层，入库明细名称/分类以物料主数据为权威来源 */
    private final MaterialMapper materialMapper;

    /** 用户数据访问层，解析入库单操作人姓名 */
    private final UserMapper userMapper;

    /** 生产单位数据访问层，解析入库单仓库名称 */
    private final ProductionUnitMapper productionUnitMapper;

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
     * @param materialMapper          物料主数据访问层
     * @param userMapper              用户数据访问层
     * @param productionUnitMapper    生产单位数据访问层
     */
    @Autowired
    public AcceptanceOrderServiceImpl(AcceptanceOrderMapper acceptanceOrderMapper,
                                      AcceptanceDetailMapper acceptanceDetailMapper,
                                      SequenceServiceImpl sequenceService,
                                      StockService stockService,
                                      StockInMapper stockInMapper,
                                      StockInDetailMapper stockInDetailMapper,
                                      PurchaseOrdersMapper purchaseOrdersMapper,
                                      MaterialMapper materialMapper,
                                      UserMapper userMapper,
                                      ProductionUnitMapper productionUnitMapper) {
        this.acceptanceOrderMapper = acceptanceOrderMapper;
        this.acceptanceDetailMapper = acceptanceDetailMapper;
        this.sequenceService = sequenceService;
        this.stockService = stockService;
        this.stockInMapper = stockInMapper;
        this.stockInDetailMapper = stockInDetailMapper;
        this.purchaseOrdersMapper = purchaseOrdersMapper;
        this.materialMapper = materialMapper;
        this.userMapper = userMapper;
        this.productionUnitMapper = productionUnitMapper;
    }

    // endregion

    // region 基础CRUD操作
    // ===================================
    // 基础CRUD操作
    // ===================================

    /**
     * 新增验收单（含明细）
     * <p>自动生成验收单号（如果未提供），初始状态为"到货初验"，同时保存主表和明细数据</p>
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
        // 初始状态：未指定时默认"到货初验"
        if (!StringUtils.hasText(acceptance.getStatus())) {
            acceptance.setStatus("到货初验");
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
            // 携带实际到货数量或单价时，读取原记录补全后重算金额（以实际到货数量 × 单价）
            if (detail.getActualArrivalQty() != null || detail.getUnitPrice() != null) {
                AcceptanceDetail existing = acceptanceDetailMapper.selectById(detail.getDetailId());
                BigDecimal qty = detail.getActualArrivalQty() != null
                        ? detail.getActualArrivalQty()
                        : existing != null ? existing.getActualArrivalQty() : null;
                BigDecimal price = detail.getUnitPrice() != null
                        ? detail.getUnitPrice()
                        : existing != null ? existing.getUnitPrice() : null;
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

        return acceptanceOrderMapper.selectPage(page, wrapper);
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
     * 确认到货：运输中 → 到货初验
     * <p>确认到货后同步将关联采购订单状态置为"到货初验"，保证验收与采购状态一致</p>
     *
     * @param acceptanceId 验收单ID
     */
    @Override
    @Transactional
    public void confirmArrival(Long acceptanceId) {
        AcceptanceOrder acceptance = getAcceptanceOrThrow(acceptanceId);
        if (!"运输中".equals(acceptance.getStatus())) {
            throw new RuntimeException("仅运输中的验收单可确认到货");
        }
        acceptance.setStatus("到货初验");
        appendRemark(acceptance, "确认到货");
        acceptanceOrderMapper.updateById(acceptance);

        // 同步关联采购订单状态为"到货初验"
        syncPurchaseOrderStatus(acceptance);
    }

    /**
     * 初验处理：到货初验 → 物料检验（合格）/ 待退货（不合格）
     *
     * @param acceptanceId 验收单ID
     * @param pass         是否合格
     * @param remark       初验备注
     */
    @Override
    @Transactional
    public void inspect(Long acceptanceId, boolean pass, String remark) {
        AcceptanceOrder acceptance = getAcceptanceOrThrow(acceptanceId);
        if (!"到货初验".equals(acceptance.getStatus())) {
            throw new RuntimeException("仅到货初验状态的验收单可进行初验");
        }
        if (pass) {
            acceptance.setStatus("物料检验");
            appendRemark(acceptance, "初验合格: " + (StringUtils.hasText(remark) ? remark : "无异常"));
        } else {
            acceptance.setStatus("待退货");
            appendRemark(acceptance, "初验不合格: " + (StringUtils.hasText(remark) ? remark : "存在质量问题"));
        }
        acceptanceOrderMapper.updateById(acceptance);

        // 同步关联采购订单状态为"物料检验"或"待退货"
        syncPurchaseOrderStatus(acceptance);
    }

    /**
     * 检验处理：物料检验 → 已入库（合格，自动增加库存并写流水）/ 待退货（不合格）
     * <p>合格时校验每种物料必须填写批号，并选择入库仓库，随后联动库存表和库存流水，
     * 同时自动生成入库单（主表携带关联生产计划编号/总金额/仓库/操作人）并返回，
     * 并同步关联采购订单状态为"已入库"；不合格时同步为"待退货"</p>
     *
     * @param acceptanceId 验收单ID
     * @param pass         是否合格
     * @param prodUnitId   入库仓库（生产单位ID，合格时必填）
     * @param remark       检验备注
     * @return 合格时返回自动生成的入库单，不合格时返回null
     */
    @Override
    @Transactional
    public StockInWithNamesDto qualityCheck(Long acceptanceId, boolean pass, Long prodUnitId, String remark) {
        AcceptanceOrder acceptance = getAcceptanceOrThrow(acceptanceId);
        if (!"物料检验".equals(acceptance.getStatus())) {
            throw new RuntimeException("仅物料检验状态的验收单可进行检验");
        }

        List<AcceptanceDetail> details = getDetailsByAcceptanceId(acceptanceId);

        if (pass) {
            // 校验：每种物料必须填写批号，否则无法入库
            boolean noBatch = details.stream().anyMatch(d -> !StringUtils.hasText(d.getBatchNumber()));
            if (noBatch) {
                throw new RuntimeException("存在未填写批号的物料，请先补充批号后再入库");
            }
            // 校验：必须选择入库仓库
            if (prodUnitId == null) {
                throw new RuntimeException("请选择入库仓库");
            }

            // 记录入库仓库并置为已入库
            acceptance.setProdUnitId(prodUnitId);
            acceptance.setStatus("已入库");
            appendRemark(acceptance, "检验合格入库: " + (StringUtils.hasText(remark) ? remark : "合格"));
            acceptanceOrderMapper.updateById(acceptance);

            // 库存联动：构造入库单与明细，调用公共库存服务增加库存并写流水，返回自动生成的入库单
            StockIn stockIn = applyAcceptanceInbound(acceptance, details);

            // 同步关联采购订单状态为"已入库"（触发式闭环，与验收单状态一致）
            syncPurchaseOrderStatus(acceptance);

            // 组装返回DTO：补全操作人姓名与仓库名称
            return buildStockInWithNames(stockIn);
        } else {
            acceptance.setStatus("待退货");
            appendRemark(acceptance, "检验不合格: " + (StringUtils.hasText(remark) ? remark : "质量不达标"));
            acceptanceOrderMapper.updateById(acceptance);

            // 同步关联采购订单状态为"待退货"
            syncPurchaseOrderStatus(acceptance);

            return null;
        }
    }

    /**
     * 重新收货：待退货 → 生成新验收单（明细沿用原单），原单标记为已退换
     * <p>同步关联采购订单状态为"到货初验"，跟随新生成的验收单</p>
     *
     * @param acceptanceId 验收单ID
     * @return 新生成的验收单
     */
    @Override
    @Transactional
    public AcceptanceOrder reReceive(Long acceptanceId) {
        AcceptanceOrder original = getAcceptanceOrThrow(acceptanceId);
        if (!"待退货".equals(original.getStatus())) {
            throw new RuntimeException("仅待退货状态的验收单可重新收货");
        }

        // 基于原单生成新验收单（明细沿用原单、批号清空）
        AcceptanceOrder newAcceptance = new AcceptanceOrder();
        BeanUtils.copyProperties(original, newAcceptance, "acceptanceId", "acceptanceCode", "status",
                "prodUnitId", "remark", "originalAcceptanceCode", "createdBy", "createdTime");
        newAcceptance.setAcceptanceCode(sequenceService.generateAcceptanceCode());
        newAcceptance.setStatus("到货初验");
        newAcceptance.setProdUnitId(null);
        newAcceptance.setRemark("由 " + original.getAcceptanceCode() + " 退货后重新收货");
        newAcceptance.setOriginalAcceptanceCode(null);
        acceptanceOrderMapper.insert(newAcceptance);

        // 同步关联采购订单状态为"到货初验"（跟随新验收单）
        syncPurchaseOrderStatus(newAcceptance);

        // 明细沿用原单、批号清空
        List<AcceptanceDetail> originalDetails = getDetailsByAcceptanceId(acceptanceId);
        List<AcceptanceDetail> newDetails = originalDetails.stream().map(d -> {
            AcceptanceDetail nd = new AcceptanceDetail();
            BeanUtils.copyProperties(d, nd, "detailId", "acceptanceId", "batchNumber");
            nd.setBatchNumber("");
            return nd;
        }).collect(Collectors.toList());
        saveDetails(newAcceptance.getAcceptanceId(), newDetails);

        // 原单标记为已退换，备注记录新验收单号
        original.setStatus("已退换");
        appendRemark(original, "重新收货: " + newAcceptance.getAcceptanceCode());
        acceptanceOrderMapper.updateById(original);

        return newAcceptance;
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
        stockIn.setProdUnitId(acceptance.getProdUnitId());
        stockIn.setRelatedOrder(acceptance.getAcceptanceCode());
        // 关联生产计划编号：优先取关联采购订单的生产计划编号，其次回退验收单自带计划编号
        stockIn.setPlanNumber(resolvePlanNumber(acceptance));
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
        String purchaseNumber = StringUtils.hasText(acceptance.getPurchaseNumber())
                ? acceptance.getPurchaseNumber() : acceptance.getRelatedOrder();
        if (StringUtils.hasText(purchaseNumber)) {
            PurchaseOrders order = purchaseOrdersMapper.selectOne(new QueryWrapper<PurchaseOrders>()
                    .eq("purchase_number", purchaseNumber));
            if (order != null && StringUtils.hasText(order.getProductionPlanCode())) {
                return order.getProductionPlanCode();
            }
        }
        return acceptance.getPlanCode();
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
     * 同步关联采购订单状态（按验收单当前状态同名映射）
     * <p>
     * 状态对应：运输中→运输中、到货初验→到货初验、物料检验→物料检验、
     * 待退货→待退货、已入库→已入库；已退换为验收单终态不映射（采购订单跟随重新收货生成的新验收单）。
     * 按验收单关联的采购订单号（优先 purchase_number，其次 related_order）定位采购订单，
     * 存在且状态与目标不同时更新；无关联采购订单或状态无映射时静默跳过（如非采购来源的验收单）
     * </p>
     *
     * @param acceptance 状态已变更的验收单
     */
    private void syncPurchaseOrderStatus(AcceptanceOrder acceptance) {
        String acceptanceStatus = String.valueOf(acceptance.getStatus());
        // 已退换为验收单终态，不参与采购订单同步
        if ("已退换".equals(acceptanceStatus) || !StringUtils.hasText(acceptanceStatus)) {
            return;
        }
        String purchaseNumber = StringUtils.hasText(acceptance.getPurchaseNumber())
                ? acceptance.getPurchaseNumber() : acceptance.getRelatedOrder();
        if (!StringUtils.hasText(purchaseNumber)) {
            return;
        }
        PurchaseOrders order = purchaseOrdersMapper.selectOne(new QueryWrapper<PurchaseOrders>()
                .eq("purchase_number", purchaseNumber));
        if (order != null && !acceptanceStatus.equals(String.valueOf(order.getStatus()))) {
            order.setStatus(acceptanceStatus);
            purchaseOrdersMapper.updateById(order);
        }
    }

    // endregion
}
