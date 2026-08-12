package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Stock.AvailableBatchDto;
import com.tonghui.erp.Common.Dto.Stock.BatchOutboundItemDto;
import com.tonghui.erp.Common.Dto.Stock.BatchOutboundRequest;
import com.tonghui.erp.Common.Dto.Stock.PlanDetailItemDto;
import com.tonghui.erp.Common.Dto.Stock.StockOutWithDetailsDto;
import com.tonghui.erp.Data.Entity.ProductionPlan;
import com.tonghui.erp.Data.Entity.ProductionUnit;
import com.tonghui.erp.Data.Entity.PreparationFormula;
import com.tonghui.erp.Data.Entity.Stock;
import com.tonghui.erp.Data.Entity.StockOut;
import com.tonghui.erp.Data.Entity.StockOutDetail;
import com.tonghui.erp.Data.mapper.PreparationFormulaMapper;
import com.tonghui.erp.Data.mapper.ProductionPlanMapper;
import com.tonghui.erp.Data.mapper.ProductionUnitMapper;
import com.tonghui.erp.Data.mapper.StockMapper;
import com.tonghui.erp.Data.mapper.StockOutMapper;
import com.tonghui.erp.Data.mapper.StockOutDetailMapper;
import com.tonghui.erp.Service.StockOutService;
import com.tonghui.erp.Service.StockService;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 出库单业务实现类
 * <p>
 * 实现StockOutService接口，提供出库单相关的业务逻辑处理，包括出库单及明细的增删改查、
 * 部分更新、单号自动生成、高级查询、带子表关联查询等功能的具体实现
 * </p>
 *
 */
@Service
public class StockOutServiceImpl extends ServiceImpl<StockOutMapper, StockOut> implements StockOutService {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /** 出库单数据访问层 */
    private final StockOutMapper stockOutMapper;

    /** 出库单明细数据访问层 */
    private final StockOutDetailMapper stockOutDetailMapper;

    /** 序列号生成服务，用于自动生成出库单号 */
    private final SequenceServiceImpl sequenceService;

    /** 库存服务，用于出库确认时的库存联动 */
    private final StockService stockService;

    /** 生产计划数据访问层，用于批量出库按处方查询 */
    private final ProductionPlanMapper productionPlanMapper;

    /** 制剂处方数据访问层，用于批量出库按处方查询 */
    private final PreparationFormulaMapper preparationFormulaMapper;

    /** 库存数据访问层，用于匹配可用库存批次 */
    private final StockMapper stockMapper;

    /** 生产单位数据访问层，用于仓库名称映射 */
    @Autowired
    private ProductionUnitMapper productionUnitMapper;

    /**
     * 构造函数注入依赖
     *
     * @param stockOutMapper            出库单数据访问层
     * @param stockOutDetailMapper      出库单明细数据访问层
     * @param sequenceService           序列号生成服务
     * @param stockService              库存服务
     * @param productionPlanMapper      生产计划数据访问层
     * @param preparationFormulaMapper  制剂处方数据访问层
     * @param stockMapper               库存数据访问层
     */
    @Autowired
    public StockOutServiceImpl(StockOutMapper stockOutMapper,
                               StockOutDetailMapper stockOutDetailMapper,
                               SequenceServiceImpl sequenceService,
                               StockService stockService,
                               ProductionPlanMapper productionPlanMapper,
                               PreparationFormulaMapper preparationFormulaMapper,
                               StockMapper stockMapper) {
        this.stockOutMapper = stockOutMapper;
        this.stockOutDetailMapper = stockOutDetailMapper;
        this.sequenceService = sequenceService;
        this.stockService = stockService;
        this.productionPlanMapper = productionPlanMapper;
        this.preparationFormulaMapper = preparationFormulaMapper;
        this.stockMapper = stockMapper;
    }

    // endregion

    // region 基础CRUD操作
    // ===================================
    // 基础CRUD操作
    // ===================================

    /**
     * 新增出库单（含明细）并直接生效出库
     * <p>
     * 自动生成出库单号（如果未提供），保存主表和明细数据后立即扣减库存批次并写入库存流水。
     * 逐条校验库存是否充足，任一明细库存不足（如"xx库存不足"）即抛出异常，
     * 整个事务回滚（单据、明细、库存均不落库），保证原子性
     * </p>
     *
     * @param stockOut 出库单主表实体
     * @param details  出库单明细列表，不可为空（须携带stockId定位库存批次）
     */
    @Override
    @Transactional
    public void addStockOut(StockOut stockOut, List<StockOutDetail> details) {
        // 校验明细不能为空（添加即出库，无明细无法联动库存）
        if (details == null || details.isEmpty()) {
            throw new RuntimeException("出库单没有明细，无法出库");
        }

        // 自动生成出库单号（如果未提供）
        if (!StringUtils.hasText(stockOut.getOutCode())) {
            stockOut.setOutCode(sequenceService.generateStockOutCode());
        }
        // 明细补充仓库与物品类型：明细可来自不同仓库（prod_unit_id 允许不同）
        for (StockOutDetail detail : details) {
            if (detail.getProdUnitId() == null && detail.getStockId() != null) {
                Stock stock = stockMapper.selectById(detail.getStockId());
                if (stock != null) {
                    detail.setProdUnitId(stock.getProdUnitId());
                }
            }
            if (detail.getItemType() == null) {
                detail.setItemType("material");
            }
        }
        // 主表仓库缺失时取第一条明细的仓库（跨仓库出库时主表仅作汇总展示）
        if (stockOut.getProdUnitId() == null) {
            stockOut.setProdUnitId(details.stream()
                    .map(StockOutDetail::getProdUnitId)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null));
        }
        if (stockOut.getProdUnitId() == null) {
            throw new RuntimeException("无法确定出库仓库，请携带 prodUnitId");
        }
        // 添加即生效：直接置为已出库状态，无需草稿确认流程
        stockOut.setOutStatus("已出库");

        // 保存出库单主表
        stockOutMapper.insert(stockOut);

        // 保存明细表
        for (StockOutDetail detail : details) {
            detail.setOutId(stockOut.getOutId());
            stockOutDetailMapper.insert(detail);
        }

        // 库存联动：逐条校验库存充足并扣减库存批次写流水（库存不足抛异常整体回滚）
        stockService.applyOutbound(stockOut, details);
    }

    /**
     * 更新出库单（含明细）
     * <p>更新主表数据，如果提供了明细则先删除原有明细再重新插入</p>
     *
     * @param stockOut 出库单主表实体
     * @param details  出库单明细列表，null表示不更新明细，空列表表示清空明细
     */
    @Override
    @Transactional
    public void updateStockOut(StockOut stockOut, List<StockOutDetail> details) {
        // 更新出库单主表
        stockOutMapper.updateById(stockOut);

        // 只有当明确提供了明细数据时才更新明细（null 表示未提供，空列表表示清空）
        if (details != null) {
            // 删除原有明细
            QueryWrapper<StockOutDetail> deleteWrapper = new QueryWrapper<>();
            deleteWrapper.eq("out_id", stockOut.getOutId());
            stockOutDetailMapper.delete(deleteWrapper);

            // 重新插入明细
            if (!details.isEmpty()) {
                for (StockOutDetail detail : details) {
                    detail.setOutId(stockOut.getOutId());
                    stockOutDetailMapper.insert(detail);
                }
            }
        }
        // 如果 details 为 null，不处理明细，保持原样
    }

    /**
     * 部分更新出库单（仅更新非null字段）
     * <p>使用UpdateWrapper实现动态字段更新，避免将null值覆盖已有数据</p>
     *
     * @param stockOut 出库单实体，仅非null字段会被更新
     */
    @Override
    @Transactional
    public void partialUpdateStockOut(StockOut stockOut) {
        // 构建 UpdateWrapper，只更新非 null 字段
        UpdateWrapper<StockOut> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("out_id", stockOut.getOutId());
        
        // 动态添加 SET 子句
        if (stockOut.getOutCode() != null) {
            updateWrapper.set("out_code", stockOut.getOutCode());
        }
        if (stockOut.getOutType() != null) {
            updateWrapper.set("out_type", stockOut.getOutType());
        }
        if (stockOut.getProdUnitId() != null) {
            updateWrapper.set("prod_unit_id", stockOut.getProdUnitId());
        }
        if (stockOut.getCustomerId() != null) {
            updateWrapper.set("customer_id", stockOut.getCustomerId());
        }
        if (stockOut.getRelatedOrder() != null) {
            updateWrapper.set("related_order", stockOut.getRelatedOrder());
        }
        if (stockOut.getOutDate() != null) {
            updateWrapper.set("out_date", stockOut.getOutDate());
        }
        if (stockOut.getTotalAmount() != null) {
            updateWrapper.set("total_amount", stockOut.getTotalAmount());
        }
        if (stockOut.getOutStatus() != null) {
            updateWrapper.set("out_status", stockOut.getOutStatus());
        }
        if (stockOut.getRemark() != null) {
            updateWrapper.set("remark", stockOut.getRemark());
        }
        if (stockOut.getUpdatedBy() != null) {
            updateWrapper.set("updated_by", stockOut.getUpdatedBy());
        }
        
        // 执行更新
        int rows = baseMapper.update(null, updateWrapper);
        if (rows == 0) {
            throw new RuntimeException("出库单不存在或未被更新");
        }
    }

    /**
     * 删除出库单（含明细）
     * <p>先删除关联的出库明细，再删除出库单主表</p>
     *
     * @param stockOutId 出库单ID
     */
    @Override
    @Transactional
    public void deleteStockOut(Long stockOutId) {
        // 删除明细表
        QueryWrapper<StockOutDetail> detailWrapper = new QueryWrapper<>();
        detailWrapper.eq("out_id", stockOutId);
        stockOutDetailMapper.delete(detailWrapper);

        // 删除主表
        stockOutMapper.deleteById(stockOutId);
    }

    /**
     * 确认出库：草稿 → 已出库
     * <p>校验出库单为草稿状态且有明细，随后调用公共库存服务扣减库存并写入库存流水</p>
     *
     * @param stockOutId 出库单ID
     */
    @Override
    @Transactional
    public void confirmStockOut(Long stockOutId) {
        StockOut stockOut = stockOutMapper.selectById(stockOutId);
        if (stockOut == null) {
            throw new RuntimeException("出库单不存在");
        }
        if (!"草稿".equals(stockOut.getOutStatus())) {
            throw new RuntimeException("仅草稿状态的出库单可确认");
        }
        List<StockOutDetail> details = getStockOutDetailsByStockOutId(stockOutId);
        if (details.isEmpty()) {
            throw new RuntimeException("出库单没有明细，无法确认");
        }
        // 库存联动：扣减库存批次并写流水
        stockService.applyOutbound(stockOut, details);
        // 更新出库单状态为已出库
        stockOut.setOutStatus("已出库");
        stockOutMapper.updateById(stockOut);
    }

    /**
     * 取消出库：已出库 → 已取消
     * <p>校验出库单为已出库状态，随后回滚库存（恢复对应库存批次）并写入调整流水</p>
     *
     * @param stockOutId 出库单ID
     */
    @Override
    @Transactional
    public void cancelStockOut(Long stockOutId) {
        StockOut stockOut = stockOutMapper.selectById(stockOutId);
        if (stockOut == null) {
            throw new RuntimeException("出库单不存在");
        }
        if (!"已出库".equals(stockOut.getOutStatus())) {
            throw new RuntimeException("仅已出库状态的出库单可取消");
        }
        List<StockOutDetail> details = getStockOutDetailsByStockOutId(stockOutId);
        if (details.isEmpty()) {
            throw new RuntimeException("出库单没有明细，无法取消");
        }
        // 库存回滚：恢复库存批次并写调整流水
        stockService.rollbackOutbound(stockOut, details);
        // 更新出库单状态为已取消
        stockOut.setOutStatus("已取消");
        stockOutMapper.updateById(stockOut);
    }

    // endregion

    // region 查询操作
    // ===================================
    // 查询操作
    // ===================================

    /**
     * 根据出库单号查询出库单
     *
     * @param stockOutCode 出库单号
     * @return 出库单实体，不存在则返回null
     */
    @Override
    public StockOut getStockOutByCode(String stockOutCode) {
        QueryWrapper<StockOut> wrapper = new QueryWrapper<>();
        wrapper.eq("out_code", stockOutCode);
        return stockOutMapper.selectOne(wrapper);
    }

    /**
     * 查询所有出库单
     *
     * @return 出库单集合
     */
    @Override
    public List<StockOut> getAllStockOuts() {
        return stockOutMapper.selectList(null);
    }

    /**
     * 根据ID查询出库单
     *
     * @param stockOutId 出库单ID
     * @return 出库单实体，不存在则返回null
     */
    @Override
    public StockOut getStockOutById(Long stockOutId) {
        return stockOutMapper.selectById(stockOutId);
    }

    /**
     * 根据出库单ID查询所有出库明细
     *
     * @param stockOutId 出库单ID
     * @return 该出库单下所有明细的集合
     */
    @Override
    public List<StockOutDetail> getStockOutDetailsByStockOutId(Long stockOutId) {
        QueryWrapper<StockOutDetail> wrapper = new QueryWrapper<>();
        wrapper.eq("out_id", stockOutId);
        return stockOutDetailMapper.selectList(wrapper);
    }

    /**
     * 新增单条出库明细
     *
     * @param detail 出库明细实体
     */
    @Override
    public void addStockOutDetail(StockOutDetail detail) {
        stockOutDetailMapper.insert(detail);
    }

    /**
     * 批量新增出库明细
     *
     * @param details 出库明细列表
     */
    @Override
    public void addStockOutDetails(List<StockOutDetail> details) {
        for (StockOutDetail detail : details) {
            stockOutDetailMapper.insert(detail);
        }
    }

    /**
     * 更新出库明细
     *
     * @param detail 出库明细实体
     */
    @Override
    public void updateStockOutDetail(StockOutDetail detail) {
        stockOutDetailMapper.updateById(detail);
    }

    /**
     * 删除出库明细
     *
     * @param detailId 出库明细ID
     */
    @Override
    public void deleteStockOutDetail(Long detailId) {
        stockOutDetailMapper.deleteById(detailId);
    }

    // endregion

    // region 单号生成
    // ===================================
    // 单号生成
    // ===================================

    /**
     * 生成出库单号
     *
     * @return 自动生成的唯一出库单号
     */
    @Override
    public String generateStockOutCode() {
        // 调用序列号生成服务获取出库单号
        return sequenceService.generateStockOutCode();
    }

    // endregion

    // region 高级查询
    // ===================================
    // 高级查询
    // ===================================

    /**
     * 高级查询出库单（支持多条件组合查询和时间范围筛选）
     *
     * @param stockOut           查询条件实体
     * @param createdTimeStart   创建时间起始值（含）
     * @param createdTimeEnd     创建时间结束值（含）
     * @param updatedTimeStart   更新时间起始值（含）
     * @param updatedTimeEnd     更新时间结束值（含）
     * @param startDate          出库日期起始值（含）
     * @param endDate            出库日期结束值（含）
     * @param pageIndex          页码，从0开始
     * @param pageSize           每页数量
     * @return 出库单分页结果
     */
    @Override
    public Page<StockOut> queryStockOuts(StockOut stockOut, LocalDateTime createdTimeStart, LocalDateTime createdTimeEnd, LocalDateTime updatedTimeStart, LocalDateTime updatedTimeEnd, LocalDate startDate, LocalDate endDate, int pageIndex, int pageSize) {
        // 将页码从0开始转换为1开始
        int actualPageIndex = pageIndex + 1;

        Page<StockOut> page = new Page<>(actualPageIndex, pageSize);
        QueryWrapper<StockOut> wrapper = new QueryWrapper<>();

        if (stockOut.getOutId() != null) {
            wrapper.eq("out_id", stockOut.getOutId());
        }
        if (StringUtils.hasText(stockOut.getOutCode())) {
            wrapper.like("out_code", stockOut.getOutCode());
        }
        if (stockOut.getProdUnitId() != null) {
            wrapper.eq("prod_unit_id", stockOut.getProdUnitId());
        }
        if (stockOut.getCustomerId() != null) {
            wrapper.eq("customer_id", stockOut.getCustomerId());
        }
        if (StringUtils.hasText(stockOut.getRelatedOrder())) {
            wrapper.like("related_order", stockOut.getRelatedOrder());
        }
        // 创建时间范围查询
        if (createdTimeStart != null) {
            wrapper.ge("created_time", createdTimeStart);
        }
        if (createdTimeEnd != null) {
            wrapper.le("created_time", createdTimeEnd);
        }
        // 更新时间范围查询
        if (updatedTimeStart != null) {
            wrapper.ge("updated_time", updatedTimeStart);
        }
        if (updatedTimeEnd != null) {
            wrapper.le("updated_time", updatedTimeEnd);
        }
        // 创建人和更新人查询
        if (stockOut.getCreatedBy() != null) {
            wrapper.eq("created_by", stockOut.getCreatedBy());
        }
        if (stockOut.getUpdatedBy() != null) {
            wrapper.eq("updated_by", stockOut.getUpdatedBy());
        }
        // 出库类型查询
        if (stockOut.getOutType() != null) {
            wrapper.eq("out_type", stockOut.getOutType());
        }
        // 出库状态查询
        if (StringUtils.hasText(stockOut.getOutStatus())) {
            wrapper.eq("out_status", stockOut.getOutStatus());
        }

        // 按编号倒序排列
        wrapper.orderByDesc("out_code");

        return stockOutMapper.selectPage(page, wrapper);
    }

    // endregion

    // region 带子表关联查询
    // ===================================
    // 带子表关联查询
    // ===================================

    /**
     * 查询出库单列表并关联出库明细信息
     * <p>先分页查询出库单主表数据，再批量查询关联的出库明细</p>
     *
     * @param stockOut           查询条件实体
     * @param createdTimeStart   创建时间起始值（含）
     * @param createdTimeEnd     创建时间结束值（含）
     * @param updatedTimeStart   更新时间起始值（含）
     * @param updatedTimeEnd     更新时间结束值（含）
     * @param startDate          出库日期起始值（含）
     * @param endDate            出库日期结束值（含）
     * @param pageNum            页码，从0开始
     * @param pageSize           每页数量
     * @return 带子表关联数据的出库单分页结果
     */
    @Override
    public PagedResult<StockOutWithDetailsDto> searchWithDetails(StockOut stockOut, LocalDateTime createdTimeStart, LocalDateTime createdTimeEnd, LocalDateTime updatedTimeStart, LocalDateTime updatedTimeEnd, LocalDate startDate, LocalDate endDate, int pageNum, int pageSize) {
        // 查询出库单主表分页数据
        Page<StockOut> parentPage = queryStockOuts(stockOut, createdTimeStart, createdTimeEnd, updatedTimeStart, updatedTimeEnd, startDate, endDate, pageNum, pageSize);
        List<StockOut> parents = parentPage.getRecords();

        PagedResult<StockOutWithDetailsDto> result = new PagedResult<>();
        if (parents.isEmpty()) {
            result.setItems(List.of());
            result.setTotalCount(parentPage.getTotal());
            result.setPageIndex(pageNum);
            result.setPageSize(pageSize);
            return result;
        }

        // 批量查询关联的出库明细
        List<Long> parentIds = parents.stream().map(StockOut::getOutId).collect(Collectors.toList());
        QueryWrapper<StockOutDetail> wrapper = new QueryWrapper<>();
        wrapper.in("out_id", parentIds);
        List<StockOutDetail> allDetails = stockOutDetailMapper.selectList(wrapper);
        Map<Long, List<StockOutDetail>> detailsMap = allDetails.stream()
                .collect(Collectors.groupingBy(StockOutDetail::getOutId));

        // 组装带子表数据的DTO
        List<StockOutWithDetailsDto> dtos = parents.stream().map(parent -> {
            StockOutWithDetailsDto dto = new StockOutWithDetailsDto();
            BeanUtils.copyProperties(parent, dto);
            dto.setDetails(detailsMap.getOrDefault(parent.getOutId(), List.of()));
            return dto;
        }).collect(Collectors.toList());

        result.setItems(dtos);
        result.setTotalCount(parentPage.getTotal());
        result.setPageIndex(pageNum);
        result.setPageSize(pageSize);
        return result;
    }

    // endregion

    // region 批量出库（按制剂处方）
    // ===================================
    // 批量出库（按制剂处方）
    // ===================================

    /**
     * 按生产计划获取批量出库处方明细
     * <p>
     * 根据生产计划关联的制剂，取制剂处方明细，计算每个物料应出数量（处方量×生产倍数），
     * 并匹配合格的可用库存批次（FIFO排序）
     * </p>
     *
     * @param planCode   生产计划编号
     * @param multiplier 生产倍数（可为空，默认1倍）
     * @return 处方明细列表（含可用库存批次）
     */
    @Override
    public List<PlanDetailItemDto> getPlanDetail(String planCode, BigDecimal multiplier) {
        if (!StringUtils.hasText(planCode)) {
            throw new RuntimeException("生产计划编号不能为空");
        }

        // 查询生产计划，获取关联制剂
        QueryWrapper<ProductionPlan> planWrapper = new QueryWrapper<>();
        planWrapper.eq("plan_number", planCode);
        ProductionPlan plan = productionPlanMapper.selectOne(planWrapper);
        if (plan == null) {
            throw new RuntimeException("生产计划不存在: " + planCode);
        }

        // 按制剂ID组装处方明细与可用库存批次
        return buildPreparationDetailItems(plan.getPreparationId(), multiplier);
    }

    /**
     * 根据制剂ID获取批量出库处方明细
     * <p>
     * 按制剂ID取制剂处方明细，计算每个物料应出数量（处方量×生产倍数），
     * 并匹配合格的可用库存批次（FIFO排序），批次携带单价与金额（应出数量×单价）
     * </p>
     *
     * @param preparationId 制剂ID（必填）
     * @param multiplier    生产倍数（可为空，默认1倍）
     * @return 处方明细列表（含序号、可用库存批次、单价、金额、库存状态）
     */
    @Override
    public List<PlanDetailItemDto> getPreparationDetail(Long preparationId, BigDecimal multiplier) {
        if (preparationId == null) {
            throw new RuntimeException("制剂ID不能为空");
        }
        return buildPreparationDetailItems(preparationId, multiplier);
    }

    /**
     * 组装制剂处方明细与可用库存批次（按制剂ID）
     * <p>
     * 查询制剂处方明细，计算每个物料应出数量（处方量×生产倍数），
     * 并匹配合格的可用库存批次（FIFO排序），批次携带单价与金额（应出数量×单价），
     * 同时为每个物料填充序号（从1开始）
     * </p>
     *
     * @param preparationId 制剂ID
     * @param multiplier    生产倍数（可为空，默认1倍）
     * @return 处方明细列表
     */
    private List<PlanDetailItemDto> buildPreparationDetailItems(Long preparationId, BigDecimal multiplier) {
        BigDecimal times = multiplier != null ? multiplier : BigDecimal.ONE;

        // 查询制剂处方明细
        QueryWrapper<PreparationFormula> formulaWrapper = new QueryWrapper<>();
        formulaWrapper.eq("preparation_id", preparationId);
        List<PreparationFormula> formulas = preparationFormulaMapper.selectList(formulaWrapper);
        if (formulas == null || formulas.isEmpty()) {
            throw new RuntimeException("该制剂没有处方明细: " + preparationId);
        }

        // 组装处方明细与可用库存批次（带序号与金额）
        List<PlanDetailItemDto> items = new ArrayList<>();
        int index = 1;
        for (PreparationFormula formula : formulas) {
            PlanDetailItemDto item = new PlanDetailItemDto();
            item.setIndex(index++);
            item.setMaterialCode(formula.getMaterialCode());
            item.setMaterialName(formula.getMaterialName());
            item.setMaterialCategory(formula.getMaterialCategory());
            item.setUnitName(formula.getUnitName());
            item.setDosage(formula.getDosage());
            BigDecimal requiredQty = formula.getDosage() != null
                    ? formula.getDosage().multiply(times) : BigDecimal.ZERO;
            item.setRequiredQty(requiredQty);
            item.setAvailableBatches(findAvailableBatches(formula.getMaterialCode(), requiredQty));
            items.add(item);
        }
        return items;
    }

    /**
     * 批量出库确认：一次事务内创建出库单并确认生效
     * <p>
     * 创建出库单（自动生成单号）及明细，随后扣减对应库存批次并写入库存流水，
     * 库存不足时整体回滚
     * </p>
     *
     * @param request 批量出库请求（出库类型、关联单号、仓库、明细列表）
     * @return 已确认的出库单
     */
    @Override
    @Transactional
    public StockOut batchConfirm(BatchOutboundRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("出库明细不能为空");
        }
        if (!StringUtils.hasText(request.getOutType())) {
            throw new RuntimeException("出库类型不能为空");
        }

        // 创建出库单（草稿状态）
        StockOut stockOut = new StockOut();
        stockOut.setOutCode(sequenceService.generateStockOutCode());
        stockOut.setOutType(request.getOutType());
        stockOut.setProdUnitId(request.getProdUnitId());
        stockOut.setRelatedOrder(request.getRelatedOrder());
        stockOut.setOutDate(LocalDate.now());
        stockOut.setOutStatus("草稿");
        stockOut.setRemark(request.getRemark());
        stockOutMapper.insert(stockOut);

        // 保存出库明细
        List<StockOutDetail> details = new ArrayList<>();
        for (BatchOutboundItemDto item : request.getItems()) {
            if (item.getQuantity() == null || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("出库数量必须大于0: " + item.getItemName());
            }
            StockOutDetail detail = new StockOutDetail();
            detail.setOutId(stockOut.getOutId());
            detail.setStockId(item.getStockId());
            detail.setItemCode(item.getItemCode());
            detail.setItemName(item.getItemName());
            detail.setCategoryName(item.getCategoryName());
            detail.setUnitName(item.getUnitName());
            detail.setBatchNumber(item.getBatchNumber());
            detail.setQuantity(item.getQuantity());
            detail.setUnitPrice(item.getUnitPrice());
            detail.setAmount(item.getQuantity() != null && item.getUnitPrice() != null
                    ? item.getQuantity().multiply(item.getUnitPrice()) : null);
            stockOutDetailMapper.insert(detail);
            details.add(detail);
        }

        // 库存联动：扣减库存批次并写流水（库存不足抛异常整体回滚）
        stockService.applyOutbound(stockOut, details);

        // 更新出库单状态为已出库
        stockOut.setOutStatus("已出库");
        stockOutMapper.updateById(stockOut);
        return stockOut;
    }

    /**
     * 查询某物料的合格可用库存批次（FIFO排序，按入库时间升序）
     *
     * @param materialCode 物料编码
     * @param requiredQty  应出数量（用于计算批次金额：应出数量×单价）
     * @return 可用库存批次列表（含单价与金额）
     */
    private List<AvailableBatchDto> findAvailableBatches(String materialCode, BigDecimal requiredQty) {
        QueryWrapper<Stock> wrapper = new QueryWrapper<>();
        wrapper.eq("item_code", materialCode);
        wrapper.eq("stock_status", "合格");
        wrapper.gt("quantity", 0);
        wrapper.orderByAsc("created_time");
        List<Stock> stocks = stockMapper.selectList(wrapper);

        // 仓库名称映射（一次性查询生产单位表）
        List<Long> unitIds = stocks.stream()
                .map(Stock::getProdUnitId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> unitNames = unitIds.isEmpty() ? Map.of() :
                productionUnitMapper.selectList(new QueryWrapper<ProductionUnit>().in("prod_unit_id", unitIds))
                        .stream()
                        .collect(Collectors.toMap(ProductionUnit::getProdUnitId,
                                ProductionUnit::getProdUnitName, (a, b) -> a));

        return stocks.stream().map(s -> {
            AvailableBatchDto batch = new AvailableBatchDto();
            batch.setStockId(s.getStockId());
            batch.setBatchNumber(s.getBatchNumber());
            batch.setProdUnitId(s.getProdUnitId());
            batch.setWarehouseName(unitNames.getOrDefault(s.getProdUnitId(), ""));
            batch.setQuantity(s.getQuantity());
            batch.setUnitPrice(s.getUnitPrice());
            batch.setAmount(s.getUnitPrice() != null && requiredQty != null
                    ? requiredQty.multiply(s.getUnitPrice()) : null);
            batch.setStockStatus(s.getStockStatus() != null ? String.valueOf(s.getStockStatus()) : null);
            return batch;
        }).collect(Collectors.toList());
    }

    // endregion
}
