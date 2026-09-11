package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Stock.ExpiryWarningDTO;
import com.tonghui.erp.Common.Dto.Stock.ExpiryWarningStatsDTO;
import com.tonghui.erp.Common.Dto.Stock.StockBatchDetailDto;
import com.tonghui.erp.Common.Dto.Stock.StockBatchDto;
import com.tonghui.erp.Common.Dto.Stock.StockGroupedDto;
import com.tonghui.erp.Common.Dto.Stock.StockGroupedByBatchDto;
import com.tonghui.erp.Common.Dto.Stock.StockTransactionDetailDto;
import com.tonghui.erp.Common.Dto.Stock.StockTransactionDto;
import com.tonghui.erp.Common.Dto.Stock.StockWithDetailsDto;
import com.tonghui.erp.Data.Entity.CheckOrder;
import com.tonghui.erp.Data.Entity.ProductionPlan;
import com.tonghui.erp.Data.Entity.ProductionUnit;
import com.tonghui.erp.Data.Entity.ReturnOrder;
import com.tonghui.erp.Data.Entity.Stock;
import com.tonghui.erp.Data.Entity.StockIn;
import com.tonghui.erp.Data.Entity.StockInDetail;
import com.tonghui.erp.Data.Entity.StockOut;
import com.tonghui.erp.Data.Entity.StockOutDetail;
import com.tonghui.erp.Data.Entity.StockTransaction;
import com.tonghui.erp.Data.Entity.TransferOrder;
import com.tonghui.erp.Data.Entity.User;
import com.tonghui.erp.Data.mapper.CheckOrderMapper;
import com.tonghui.erp.Data.mapper.ProductionPlanMapper;
import com.tonghui.erp.Data.mapper.ProductionUnitMapper;
import com.tonghui.erp.Data.mapper.ReturnOrderMapper;
import com.tonghui.erp.Data.mapper.StockInMapper;
import com.tonghui.erp.Data.mapper.StockMapper;
import com.tonghui.erp.Data.mapper.StockOutDetailMapper;
import com.tonghui.erp.Data.mapper.StockOutMapper;
import com.tonghui.erp.Data.mapper.StockTransactionMapper;
import com.tonghui.erp.Data.mapper.TransferOrderMapper;
import com.tonghui.erp.Data.mapper.UserMapper;
import com.tonghui.erp.Service.StockService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 库存服务实现类
 * <p>
 * 实现StockService接口，提供库存相关的业务逻辑处理，包括库存的高级查询、
 * 带子表关联查询、库存预警查询、预警统计等功能的具体实现
 * </p>
 *
 */
@Service
public class StockServiceImpl extends ServiceImpl<StockMapper, Stock>
    implements StockService{

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /** 库存交易记录数据访问层，用于关联查询库存交易流水 */
    @Autowired
    private StockTransactionMapper stockTransactionMapper;

    /** 出库明细数据访问层，用于关联查询库存出库明细 */
    @Autowired
    private StockOutDetailMapper stockOutDetailMapper;

    /** 生产单位数据访问层，用于仓库名称映射 */
    @Autowired
    private ProductionUnitMapper productionUnitMapper;

    /** 用户数据访问层，用于操作人姓名映射 */
    @Autowired
    private UserMapper userMapper;

    /** 入库单数据访问层，用于解析流水绑定的入库单与验收单 */
    @Autowired
    private StockInMapper stockInMapper;

    /** 调拨单数据访问层，用于解析调拨流水对应的调拨单号 */
    @Autowired
    private TransferOrderMapper transferOrderMapper;

    /** 盘点单数据访问层，用于解析盘点流水对应的盘点单号 */
    @Autowired
    private CheckOrderMapper checkOrderMapper;

    /** 退库单数据访问层，用于解析退库流水对应的退库单号 */
    @Autowired
    private ReturnOrderMapper returnOrderMapper;

    /** 出库单数据访问层，用于解析出库流水对应的出库单号 */
    @Autowired
    private StockOutMapper stockOutMapper;

    /** 生产计划数据访问层，用于解析关联制剂名称 */
    @Autowired
    private ProductionPlanMapper productionPlanMapper;

    // endregion

    // region 高级查询
    // ===================================
    // 高级查询
    // ===================================

    /**
     * 高级查询库存（支持多条件组合查询和自定义时间范围筛选）
     *
     * @param stock             查询条件实体，非null字段将作为等值或模糊查询条件
     * @param keyword           关键字（对物品编码、物品名称进行模糊匹配，可选）
     * @param createdTimeStart  创建时间起始值（含）
     * @param createdTimeEnd    创建时间结束值（含）
     * @param updatedTimeStart  更新时间起始值（含）
     * @param updatedTimeEnd    更新时间结束值（含）
     * @param pageIndex         页码，从0开始
     * @param pageSize          每页数量
     * @return 库存分页结果
     */
    @Override
    public Page<Stock> queryStocks(Stock stock, String keyword, LocalDateTime createdTimeStart, LocalDateTime createdTimeEnd, LocalDateTime updatedTimeStart, LocalDateTime updatedTimeEnd, int pageIndex, int pageSize) {
        // 页码处理，MyBatis Plus Page页码从1开始
        int actualPageIndex = pageIndex + 1;

        Page<Stock> page = new Page<>(actualPageIndex, pageSize);
        QueryWrapper<Stock> wrapper = new QueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            // 关键字对物品编码、物品名称进行模糊匹配
            wrapper.and(w -> w.like("item_code", keyword).or().like("item_name", keyword));
        }
        if (stock.getStockId() != null) {
            wrapper.eq("stock_id", stock.getStockId());
        }
        if (StringUtils.hasText(stock.getItemCode())) {
            wrapper.like("item_code", stock.getItemCode());
        }
        if (StringUtils.hasText(stock.getItemName())) {
            wrapper.like("item_name", stock.getItemName());
        }
        if (StringUtils.hasText(stock.getCategoryName())) {
            wrapper.eq("category_name", stock.getCategoryName());
        }
        if (StringUtils.hasText(stock.getUnitName())) {
            wrapper.eq("unit_name", stock.getUnitName());
        }
        if (stock.getQuantity() != null) {
            wrapper.ge("quantity", stock.getQuantity());
        }
        if (stock.getProdUnitId() != null) {
            wrapper.eq("prod_unit_id", stock.getProdUnitId());
        }
        if (StringUtils.hasText(stock.getBatchNumber())) {
            wrapper.like("batch_number", stock.getBatchNumber());
        }
        if (stock.getProductionDate() != null) {
            wrapper.eq("production_date", stock.getProductionDate());
        }
        if (stock.getExpiryDate() != null) {
            wrapper.eq("expiry_date", stock.getExpiryDate());
        }
        if (StringUtils.hasText(stock.getStorageLocation())) {
            wrapper.like("storage_location", stock.getStorageLocation());
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

        Page<Stock> result = this.getBaseMapper().selectPage(page, wrapper);

        // 批量解析关联制剂名称（通过 plan_number 关联 production_plan）
        List<Stock> records = result.getRecords();
        if (!records.isEmpty()) {
            Map<String, String> preparationNameMap = loadPreparationNameMapByPlanNumber(records);
            records.forEach(r -> {
                if (StringUtils.hasText(r.getPlanNumber())) {
                    r.setPreparationName(preparationNameMap.get(r.getPlanNumber()));
                }
            });
        }

        return result;
    }

    /**
     * 高级查询库存（使用默认时间范围，即不过滤时间）
     *
     * @param stock    查询条件实体
     * @param keyword  关键字（对物品编码、物品名称进行模糊匹配，可选）
     * @param pageNum  页码，从0开始
     * @param pageSize 每页数量
     * @return 库存分页结果
     */
    @Override
    public Page<Stock> queryStocks(Stock stock, String keyword, int pageNum, int pageSize) {
        return queryStocks(stock, keyword, null, null, null, null, pageNum, pageSize);
    }

    // endregion

    // region 带子表关联查询
    // ===================================
    // 带子表关联查询
    // ===================================

    /**
     * 查询库存列表并关联交易记录和出库明细信息
     * <p>先分页查询库存主表数据，再批量查询关联的交易记录和出库明细</p>
     *
     * @param stock    查询条件实体
     * @param keyword  关键字（对物品编码、物品名称进行模糊匹配，可选）
     * @param pageNum  页码，从0开始
     * @param pageSize 每页数量
     * @return 带子表关联数据的库存分页结果
     */
    @Override
    public PagedResult<StockWithDetailsDto> searchWithDetails(Stock stock, String keyword, int pageNum, int pageSize) {
        // 查询库存主表分页数据
        Page<Stock> parentPage = queryStocks(stock, keyword, pageNum, pageSize);
        List<Stock> parents = parentPage.getRecords();

        PagedResult<StockWithDetailsDto> result = new PagedResult<>();
        if (parents.isEmpty()) {
            result.setItems(List.of());
            result.setTotalCount(parentPage.getTotal());
            result.setPageIndex(pageNum);
            result.setPageSize(pageSize);
            return result;
        }

        // 批量查询关联的库存交易记录
        List<Long> parentIds = parents.stream().map(Stock::getStockId).collect(Collectors.toList());
        QueryWrapper<StockTransaction> transactionWrapper = new QueryWrapper<>();
        transactionWrapper.in("stock_id", parentIds);
        List<StockTransaction> allTransactions = stockTransactionMapper.selectList(transactionWrapper);
        // 解析流水绑定的入库单与验收单信息
        List<StockTransactionDto> transactionDtos = resolveInboundLink(allTransactions);
        Map<Long, List<StockTransactionDto>> transactionsMap = transactionDtos.stream()
                .collect(Collectors.groupingBy(StockTransactionDto::getStockId));

        // 批量查询关联的出库明细
        QueryWrapper<StockOutDetail> outDetailWrapper = new QueryWrapper<>();
        outDetailWrapper.in("stock_id", parentIds);
        List<StockOutDetail> allOutDetails = stockOutDetailMapper.selectList(outDetailWrapper);
        Map<Long, List<StockOutDetail>> outDetailsMap = allOutDetails.stream()
                .collect(Collectors.groupingBy(StockOutDetail::getStockId));

        // 组装带子表数据的DTO
        List<StockWithDetailsDto> dtos = parents.stream().map(parent -> {
            StockWithDetailsDto dto = new StockWithDetailsDto();
            BeanUtils.copyProperties(parent, dto);
            dto.setTransactions(transactionsMap.getOrDefault(parent.getStockId(), List.of()));
            dto.setOutDetails(outDetailsMap.getOrDefault(parent.getStockId(), List.of()));
            return dto;
        }).collect(Collectors.toList());

        result.setItems(dtos);
        result.setTotalCount(parentPage.getTotal());
        result.setPageIndex(pageNum);
        result.setPageSize(pageSize);
        return result;
    }

    // endregion

    // region 库存预警查询
    // ===================================
    // 库存预警查询
    // ===================================

    /**
     * 查询即将过期的库存预警列表
     * <p>查询有效期内且在指定天数内将过期的库存，自动计算剩余天数和预警级别</p>
     *
     * @param warningDays 预警天数范围，查询从今天起warningDays天内将过期的库存
     * @return 即将过期的库存预警列表
     */
    @Override
    public List<ExpiryWarningDTO> getExpiringStocks(int warningDays) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(warningDays);
        
        // 查询即将过期的库存
        List<ExpiryWarningDTO> warnings = this.getBaseMapper().selectExpiringStocksWithDetail(
                startDate, endDate, null, null);
        
        // 计算剩余天数和预警级别
        warnings.forEach(this::calculateWarningInfo);
        
        return warnings;
    }

    /**
     * 获取库存过期预警统计信息
     * <p>统计各级别预警数量：紧急(urgent)、警告(warning)、提示(info)和总数</p>
     *
     * @return 预警统计信息DTO
     */
    @Override
    public ExpiryWarningStatsDTO getExpiryWarningStats() {
        Map<String, Object> statsMap = this.getBaseMapper().countExpiringStocksByLevel();
        
        ExpiryWarningStatsDTO stats = new ExpiryWarningStatsDTO();
        stats.setUrgentCount(((Number) statsMap.getOrDefault("urgentCount", 0)).intValue());
        stats.setWarningCount(((Number) statsMap.getOrDefault("warningCount", 0)).intValue());
        stats.setInfoCount(((Number) statsMap.getOrDefault("infoCount", 0)).intValue());
        stats.setTotalCount(((Number) statsMap.getOrDefault("totalCount", 0)).intValue());
        
        return stats;
    }

    /**
     * 高级查询即将过期的库存（支持按物品类型、生产单位、预警级别筛选和分页）
     *
     * @param warningDays  预警天数范围
     * @param itemType     物品类型，可选值：MATERIAL(物料)、PREPARATION(制剂)
     * @param prodUnitId   生产单位ID，筛选指定生产单位的库存
     * @param warningLevel 预警级别，可选值：urgent、warning、info
     * @param pageIndex    页码，从0开始
     * @param pageSize     每页数量
     * @return 预警库存分页结果
     */
    @Override
    public Page<ExpiryWarningDTO> queryExpiringStocks(int warningDays, String itemType, 
                                                       Long prodUnitId, String warningLevel,
                                                       int pageIndex, int pageSize) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(warningDays);
        
        // 查询所有符合条件的数据
        List<ExpiryWarningDTO> allWarnings = this.getBaseMapper().selectExpiringStocksWithDetail(
                startDate, endDate, itemType, prodUnitId);
        
        // 计算预警信息
        allWarnings.forEach(this::calculateWarningInfo);
        
        // 按预警级别过滤
        List<ExpiryWarningDTO> filteredWarnings = allWarnings;
        if (StringUtils.hasText(warningLevel)) {
            filteredWarnings = allWarnings.stream()
                    .filter(w -> warningLevel.equals(w.getWarningLevel()))
                    .collect(Collectors.toList());
        }
        
        // 内存分页（因为预警级别是计算字段，需要先全量查询再分页）
        int fromIndex = pageIndex * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, filteredWarnings.size());
        
        List<ExpiryWarningDTO> pageData = fromIndex < filteredWarnings.size() 
                ? filteredWarnings.subList(fromIndex, toIndex) 
                : new ArrayList<>();
        
        Page<ExpiryWarningDTO> resultPage = new Page<>(pageIndex + 1, pageSize);
        resultPage.setRecords(pageData);
        resultPage.setTotal(filteredWarnings.size());
        
        return resultPage;
    }

    // endregion

    // region 库存联动（入库/出库确认）
    // ===================================
    // 库存联动（入库/出库确认）
    // ===================================

    /**
     * 入库库存联动：按明细逐行 upsert 库存表并写入库存流水
     * <p>
     * 库存唯一键：物品编码 + 生产单位 + 批号；存在则数量累加并覆盖单价/有效期，不存在则新增。
     * 供入库单确认、货物验收检验合格入库共用，必须处于事务中执行
     * </p>
     *
     * @param stockIn 入库单信息（inType作为流水类型，prodUnitId为仓库）
     * @param details 入库明细列表
     */
    @Override
    @Transactional
    public void applyInbound(StockIn stockIn, List<StockInDetail> details) {
        if (details == null || details.isEmpty()) {
            return;
        }
        if (stockIn.getProdUnitId() == null) {
            throw new RuntimeException("请选择入库仓库");
        }
        for (StockInDetail detail : details) {
            if (detail.getQuantity() == null || detail.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("入库数量必须大于0: " + detail.getItemName());
            }

            // 按 物品编码 + 生产单位 + 批号 + 入库单ID + 库存状态 定位库存批次
            QueryWrapper<Stock> wrapper = new QueryWrapper<>();
            wrapper.eq("item_code", detail.getItemCode());
            wrapper.eq("prod_unit_id", stockIn.getProdUnitId());
            wrapper.eq("batch_number", detail.getBatchNumber());
            wrapper.eq("stock_in_id", stockIn.getInId());
            wrapper.eq("stock_status", StringUtils.hasText(detail.getStockStatus()) ? detail.getStockStatus() : "合格");
            Stock existing = this.getBaseMapper().selectOne(wrapper);

            BigDecimal before = existing != null ? existing.getQuantity() : BigDecimal.ZERO;
            Stock stock;
            if (existing != null) {
                // 同入库单+同批次再次入库：数量累加，单价以最新入库单价为准
                stock = existing;
                stock.setQuantity(stock.getQuantity().add(detail.getQuantity()));
                if (detail.getUnitPrice() != null) {
                    stock.setUnitPrice(detail.getUnitPrice());
                }
                if (detail.getExpiryDate() != null) {
                    stock.setExpiryDate(detail.getExpiryDate());
                }
                if (StringUtils.hasText(detail.getStockStatus())) {
                    stock.setStockStatus(detail.getStockStatus());
                }
                this.getBaseMapper().updateById(stock);
            } else {
                // 新增库存批次（每条入库明细创建独立库存记录）
                stock = new Stock();
                stock.setProdUnitId(stockIn.getProdUnitId());
                // 物品类型字段为Object类型，先转换为String再判断
                String itemType = detail.getItemType() != null ? String.valueOf(detail.getItemType()) : "";
                stock.setItemType(StringUtils.hasText(itemType) ? itemType : "material");
                stock.setItemId(detail.getItemId());
                stock.setItemCode(detail.getItemCode());
                stock.setItemName(detail.getItemName());
                stock.setCategoryName(detail.getCategoryName());
                stock.setUnitName(detail.getUnitName());
                stock.setBatchNumber(detail.getBatchNumber());
                stock.setQuantity(detail.getQuantity());
                stock.setUnitPrice(detail.getUnitPrice());
                stock.setProductionDate(detail.getProductionDate());
                stock.setExpiryDate(detail.getExpiryDate());
                stock.setStockStatus(StringUtils.hasText(detail.getStockStatus()) ? detail.getStockStatus() : "合格");
                stock.setPlanNumber(stockIn.getPlanNumber());
                // 设置入库单关联字段（用于追溯来源）
                stock.setStockInId(stockIn.getInId());
                stock.setStockInDetailId(detail.getInDetailId());
                this.getBaseMapper().insert(stock);
            }

            // 写入库存流水（入库）
            insertTransaction(stock, stockIn.getInType() != null ? stockIn.getInType() : "入库",
                    "stock_in", stockIn.getInId(), stockIn.getRemark(), before, detail.getQuantity());
        }
    }

    /**
     * 出库库存联动：按明细逐行扣减库存并写入库存流水
     * <p>
     * 通过明细中的stockId定位库存批次，校验库存充足（不足抛异常整体回滚），
     * 扣减后数量小于等于0时删除库存记录
     * </p>
     *
     * @param stockOut 出库单信息（outType作为流水类型）
     * @param details  出库明细列表（须携带stockId）
     */
    @Override
    @Transactional
    public void applyOutbound(StockOut stockOut, List<StockOutDetail> details) {
        if (details == null || details.isEmpty()) {
            return;
        }
        for (StockOutDetail detail : details) {
            if (detail.getStockId() == null) {
                throw new RuntimeException("出库明细缺少库存批次(stockId): " + detail.getItemName());
            }
            if (detail.getQuantity() == null || detail.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("出库数量必须大于0: " + detail.getItemName());
            }
            Stock stock = this.getBaseMapper().selectById(detail.getStockId());
            if (stock == null) {
                throw new RuntimeException("库存批次不存在: " + detail.getItemName());
            }
            BigDecimal before = stock.getQuantity();
            // 校验库存充足，不足抛异常整体回滚
            if (before.compareTo(detail.getQuantity()) < 0) {
                throw new RuntimeException("库存不足: " + detail.getItemName()
                        + "（当前 " + before.toPlainString() + "，需出库 " + detail.getQuantity().toPlainString() + "）");
            }
            BigDecimal after = before.subtract(detail.getQuantity());
            if (after.compareTo(BigDecimal.ZERO) <= 0) {
                // 出库完毕后删除该批次库存记录
                this.getBaseMapper().deleteById(stock.getStockId());
            } else {
                stock.setQuantity(after);
                this.getBaseMapper().updateById(stock);
            }

            // 写入库存流水（出库，数量为负）
            insertTransaction(stock, stockOut.getOutType() != null ? stockOut.getOutType() : "出库",
                    "stock_out", stockOut.getOutId(), stockOut.getRemark(),
                    before, detail.getQuantity().negate());
        }
    }

    /**
     * 入库取消回滚：按明细逐行扣减已入库的库存并写入调整流水
     * <p>
     * 定位键与入库一致（物品编码+生产单位+批号），数量不足抛异常整体回滚，
     * 扣减后数量小于等于0时删除库存记录
     * </p>
     *
     * @param stockIn 入库单信息
     * @param details 入库明细列表
     */
    @Override
    @Transactional
    public void rollbackInbound(StockIn stockIn, List<StockInDetail> details) {
        if (details == null || details.isEmpty()) {
            return;
        }
        for (StockInDetail detail : details) {
            // 按 物品编码 + 生产单位 + 批号 定位库存批次
            QueryWrapper<Stock> wrapper = new QueryWrapper<>();
            wrapper.eq("item_code", detail.getItemCode());
            wrapper.eq("prod_unit_id", stockIn.getProdUnitId());
            wrapper.eq("batch_number", detail.getBatchNumber());
            Stock stock = this.getBaseMapper().selectOne(wrapper);
            if (stock == null) {
                throw new RuntimeException("库存批次不存在，无法回滚: " + detail.getItemName());
            }
            BigDecimal before = stock.getQuantity();
            if (before.compareTo(detail.getQuantity()) < 0) {
                throw new RuntimeException("库存不足，无法回滚: " + detail.getItemName());
            }
            BigDecimal after = before.subtract(detail.getQuantity());
            if (after.compareTo(BigDecimal.ZERO) <= 0) {
                this.getBaseMapper().deleteById(stock.getStockId());
            } else {
                stock.setQuantity(after);
                this.getBaseMapper().updateById(stock);
            }
            // 写入调整流水（数量为负）
            insertTransaction(stock, "调整", "stock_in", stockIn.getInId(),
                    "入库单取消回滚: " + (stockIn.getInCode() != null ? stockIn.getInCode() : ""),
                    before, detail.getQuantity().negate());
        }
    }

    /**
     * 出库取消回滚：按明细逐行恢复库存并写入调整流水
     * <p>
     * 通过明细中的stockId定位库存批次，存在则数量累加；若该批次已因出库清零被删除则按明细信息重建
     * </p>
     *
     * @param stockOut 出库单信息
     * @param details  出库明细列表（须携带stockId）
     */
    @Override
    @Transactional
    public void rollbackOutbound(StockOut stockOut, List<StockOutDetail> details) {
        if (details == null || details.isEmpty()) {
            return;
        }
        for (StockOutDetail detail : details) {
            Stock stock = detail.getStockId() != null
                    ? this.getBaseMapper().selectById(detail.getStockId())
                    : null;
            BigDecimal before;
            if (stock != null) {
                // 批次仍存在：数量累加
                before = stock.getQuantity();
                stock.setQuantity(before.add(detail.getQuantity()));
                this.getBaseMapper().updateById(stock);
            } else {
                // 批次已因出库清零被删除：按明细信息重建
                stock = new Stock();
                stock.setProdUnitId(stockOut.getProdUnitId());
                stock.setItemType(detail.getItemType() != null ? String.valueOf(detail.getItemType()) : "material");
                stock.setItemId(detail.getItemId());
                stock.setItemCode(detail.getItemCode());
                stock.setItemName(detail.getItemName());
                stock.setCategoryName(detail.getCategoryName());
                stock.setUnitName(detail.getUnitName());
                stock.setBatchNumber(detail.getBatchNumber());
                stock.setQuantity(detail.getQuantity());
                stock.setUnitPrice(detail.getUnitPrice());
                stock.setStockStatus("合格");
                this.getBaseMapper().insert(stock);
                before = BigDecimal.ZERO;
            }
            // 写入调整流水（数量为正）
            insertTransaction(stock, "调整", "stock_out", stockOut.getOutId(),
                    "出库单取消回滚: " + (stockOut.getOutCode() != null ? stockOut.getOutCode() : ""),
                    before, detail.getQuantity());
        }
    }

    // endregion

    // region 出库扣减（FIFO/手动/退库）
    // ===================================
    // 出库扣减（FIFO/手动/退库）
    // ===================================

    /**
     * 先入先出扣减库存（按 stock_in_id 升序，先入库的先扣减）
     *
     * @param outId       出库单ID
     * @param itemCode    物品编码
     * @param prodUnitId  仓库ID
     * @param batchNumber 批号
     * @param quantity    扣减数量
     */
    @Override
    @Transactional
    public void deductStockFIFO(Long outId, String itemCode, Long prodUnitId,
                                String batchNumber, BigDecimal quantity) {
        // 查询可用库存（按 stock_in_id 升序，先入库的先扣减）
        QueryWrapper<Stock> wrapper = new QueryWrapper<>();
        wrapper.eq("item_code", itemCode);
        wrapper.eq("prod_unit_id", prodUnitId);
        wrapper.eq("batch_number", batchNumber);
        wrapper.gt("quantity", 0);
        wrapper.orderByAsc("stock_in_id");
        List<Stock> availableStocks = this.getBaseMapper().selectList(wrapper);

        BigDecimal remaining = quantity;
        for (Stock stock : availableStocks) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal deduct = stock.getQuantity().min(remaining);
            BigDecimal before = stock.getQuantity();
            stock.setQuantity(stock.getQuantity().subtract(deduct));
            this.getBaseMapper().updateById(stock);

            // 记录出库流水
            insertTransaction(stock, "出库", "stock_out", outId,
                    "FIFO出库扣减", before, deduct.negate());

            remaining = remaining.subtract(deduct);
        }

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            throw new RuntimeException("库存不足: " + itemCode + " " + batchNumber
                    + "（还需 " + remaining.toPlainString() + "）");
        }
    }

    /**
     * 手动选择扣减库存
     *
     * @param outId             出库单ID
     * @param stockIdQuantityMap stock_id → 扣减数量
     */
    @Override
    @Transactional
    public void deductStockManual(Long outId, Map<Long, BigDecimal> stockIdQuantityMap) {
        for (Map.Entry<Long, BigDecimal> entry : stockIdQuantityMap.entrySet()) {
            Long stockId = entry.getKey();
            BigDecimal deductQuantity = entry.getValue();

            Stock stock = this.getBaseMapper().selectById(stockId);
            if (stock == null) {
                throw new RuntimeException("库存记录不存在: stockId=" + stockId);
            }
            if (stock.getQuantity().compareTo(deductQuantity) < 0) {
                throw new RuntimeException("库存不足: " + stock.getItemName() + " " + stock.getBatchNumber()
                        + "（当前 " + stock.getQuantity().toPlainString()
                        + "，需出库 " + deductQuantity.toPlainString() + "）");
            }

            BigDecimal before = stock.getQuantity();
            stock.setQuantity(stock.getQuantity().subtract(deductQuantity));
            this.getBaseMapper().updateById(stock);

            // 记录出库流水
            insertTransaction(stock, "出库", "stock_out", outId,
                    "手动选择出库扣减", before, deductQuantity.negate());
        }
    }

    /**
     * 退库扣减：反向扣减原出库单对应的库存记录
     *
     * @param returnOutId   退库单ID（新出库单）
     * @param originalOutId 原出库单ID
     */
    @Override
    @Transactional
    public void deductStockReturn(Long returnOutId, Long originalOutId) {
        // 查询原出库单的所有出库明细
        QueryWrapper<StockOutDetail> wrapper = new QueryWrapper<>();
        wrapper.eq("out_id", originalOutId);
        List<StockOutDetail> originalDetails = stockOutDetailMapper.selectList(wrapper);

        if (originalDetails == null || originalDetails.isEmpty()) {
            throw new RuntimeException("原出库单无出库明细: outId=" + originalOutId);
        }

        // 反向扣减这些库存记录
        for (StockOutDetail detail : originalDetails) {
            Stock stock = this.getBaseMapper().selectById(detail.getStockId());
            if (stock == null) {
                throw new RuntimeException("原库存记录不存在: stockId=" + detail.getStockId());
            }

            BigDecimal before = stock.getQuantity();
            stock.setQuantity(stock.getQuantity().subtract(detail.getQuantity()));
            this.getBaseMapper().updateById(stock);

            // 记录退库流水
            insertTransaction(stock, "退库", "stock_out", returnOutId,
                    "退库扣减（原出库单: " + originalOutId + "）", before, detail.getQuantity().negate());
        }
    }

    // endregion

    /**
     * 根据库存ID查询库存流水列表（含绑定的入库单与验收单信息）
     *
     * @param stockId 库存ID
     * @return 流水列表
     */
    @Override
    public List<StockTransactionDto> getTransactionsByStockId(Long stockId) {
        QueryWrapper<StockTransaction> wrapper = new QueryWrapper<>();
        wrapper.eq("stock_id", stockId);
        wrapper.orderByDesc("transaction_date");
        List<StockTransaction> transactions = stockTransactionMapper.selectList(wrapper);
        return resolveInboundLink(transactions);
    }

    /**
     * 解析流水绑定的单据信息（入库单/调拨单/盘点单/退库单）
     * <p>
     * 按 related_type 分别反查对应单据表，回填单号到 DTO.inCode 字段：
     * stock_in → 入库单号（同时回填 inId 与验收单号），
     * transfer → 调拨单号，check → 盘点单号，return → 退库单号
     * </p>
     *
     * @param transactions 库存交易流水列表
     * @return 携带单据绑定信息的流水DTO列表
     */
    private List<StockTransactionDto> resolveInboundLink(List<StockTransaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return List.of();
        }

        // 按关联类型分组收集ID
        List<Long> inIds = transactions.stream()
                .filter(t -> "stock_in".equals(String.valueOf(t.getRelatedType())))
                .map(StockTransaction::getRelatedId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<Long> transferIds = transactions.stream()
                .filter(t -> "transfer".equals(String.valueOf(t.getRelatedType())))
                .map(StockTransaction::getRelatedId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<Long> checkIds = transactions.stream()
                .filter(t -> "check".equals(String.valueOf(t.getRelatedType())))
                .map(StockTransaction::getRelatedId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<Long> returnIds = transactions.stream()
                .filter(t -> "return".equals(String.valueOf(t.getRelatedType())))
                .map(StockTransaction::getRelatedId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<Long> outIds = transactions.stream()
                .filter(t -> "stock_out".equals(String.valueOf(t.getRelatedType())))
                .map(StockTransaction::getRelatedId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        // 批量查询各单据表，构建ID→单号映射
        Map<Long, StockIn> inMap = inIds.isEmpty() ? new HashMap<>()
                : stockInMapper.selectBatchIds(inIds).stream()
                        .collect(Collectors.toMap(StockIn::getInId, si -> si, (a, b) -> a));
        Map<Long, TransferOrder> transferMap = transferIds.isEmpty() ? new HashMap<>()
                : transferOrderMapper.selectBatchIds(transferIds).stream()
                        .collect(Collectors.toMap(TransferOrder::getId, t -> t, (a, b) -> a));
        Map<Long, CheckOrder> checkMap = checkIds.isEmpty() ? new HashMap<>()
                : checkOrderMapper.selectBatchIds(checkIds).stream()
                        .collect(Collectors.toMap(CheckOrder::getId, c -> c, (a, b) -> a));
        Map<Long, ReturnOrder> returnMap = returnIds.isEmpty() ? new HashMap<>()
                : returnOrderMapper.selectBatchIds(returnIds).stream()
                        .collect(Collectors.toMap(ReturnOrder::getId, r -> r, (a, b) -> a));
        Map<Long, StockOut> outMap = outIds.isEmpty() ? new HashMap<>()
                : stockOutMapper.selectBatchIds(outIds).stream()
                        .collect(Collectors.toMap(StockOut::getOutId, o -> o, (a, b) -> a));

        // 批量加载操作人姓名（按流水创建人ID关联用户表）
        List<Long> userIds = transactions.stream()
                .map(StockTransaction::getCreatedBy)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, User> userMap = userIds.isEmpty() ? new HashMap<>()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getUserId, u -> u, (a, b) -> a));

        // 组装流水DTO
        return transactions.stream().map(t -> {
            StockTransactionDto dto = new StockTransactionDto();
            BeanUtils.copyProperties(t, dto);
            String type = String.valueOf(t.getRelatedType());
            Long relatedId = t.getRelatedId();
            // 按关联类型反查单据号填入 inCode
            if ("stock_in".equals(type)) {
                StockIn stockIn = relatedId != null ? inMap.get(relatedId) : null;
                if (stockIn != null) {
                    dto.setInId(stockIn.getInId());
                    dto.setInCode(stockIn.getInCode());
                    dto.setAcceptanceCode(stockIn.getRelatedOrder());
                }
            } else if ("transfer".equals(type)) {
                TransferOrder to = relatedId != null ? transferMap.get(relatedId) : null;
                if (to != null) {
                    dto.setInCode(to.getTransferNo());
                }
            } else if ("check".equals(type)) {
                CheckOrder co = relatedId != null ? checkMap.get(relatedId) : null;
                if (co != null) {
                    dto.setInCode(co.getCheckNo());
                }
            } else if ("return".equals(type)) {
                ReturnOrder ro = relatedId != null ? returnMap.get(relatedId) : null;
                if (ro != null) {
                    dto.setInCode(ro.getReturnNo());
                }
            } else if ("stock_out".equals(type)) {
                StockOut so = relatedId != null ? outMap.get(relatedId) : null;
                if (so != null) {
                    dto.setInCode(so.getOutCode());
                }
            }
            // 回填操作人姓名
            if (t.getCreatedBy() != null) {
                User creator = userMap.get(t.getCreatedBy());
                dto.setCreatedByName(creator != null
                        ? (StringUtils.hasText(creator.getUserName()) ? creator.getUserName() : creator.getUserAccount())
                        : null);
            }
            return dto;
        }).collect(Collectors.toList());
    }

    // endregion

    // region 分组查询
    // ===================================
    // 分组查询
    // ===================================

    /**
     * 按物料编码分组查询库存（支持筛选与分页）
     * <p>
     * 先按筛选条件查询全部库存，再按物料编码分组聚合（含仓库名称），最后内存分页。
     * 库存规模相对有限，采用全量查询+内存分组分页的方式保证实现简洁
     * </p>
     *
     * @param itemCode     物料编码（模糊匹配）
     * @param itemName     物料名称（模糊匹配）
     * @param categoryName 分类名称（等值匹配）
     * @param prodUnitId   仓库（生产单位ID，等值匹配）
     * @param stockStatus  库存状态（等值匹配：合格/待检/不合格）
     * @param showZero     是否显示零库存
     * @param pageIndex    页码，从0开始
     * @param pageSize     每页大小
     * @return 分组分页结果
     */
    @Override
    public PagedResult<StockGroupedDto> groupedSearch(String itemCode, String itemName, String categoryName,
                                                      Long prodUnitId, String stockStatus, boolean showZero,
                                                      int pageIndex, int pageSize) {
        // 组装库存查询条件
        QueryWrapper<Stock> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(itemCode)) {
            wrapper.like("item_code", itemCode);
        }
        if (StringUtils.hasText(itemName)) {
            wrapper.like("item_name", itemName);
        }
        if (StringUtils.hasText(categoryName)) {
            wrapper.eq("category_name", categoryName);
        }
        if (prodUnitId != null) {
            wrapper.eq("prod_unit_id", prodUnitId);
        }
        if (StringUtils.hasText(stockStatus)) {
            wrapper.eq("stock_status", stockStatus);
        }
        if (!showZero) {
            wrapper.gt("quantity", 0);
        }
        wrapper.orderByAsc("item_code");
        List<Stock> allStocks = this.getBaseMapper().selectList(wrapper);

        // 仓库名称映射（一次性查询生产单位表）
        Map<Long, String> unitNames = loadUnitNames(allStocks);

        // 批量解析关联制剂名称（通过 plan_number 关联 production_plan）
        Map<String, String> preparationNameMap = loadPreparationNameMapByPlanNumber(allStocks);

        // 按物料编码分组
        Map<String, List<Stock>> grouped = allStocks.stream()
                .collect(Collectors.groupingBy(Stock::getItemCode));
        List<StockGroupedDto> groups = grouped.entrySet().stream().map(entry -> {
            List<Stock> batchStocks = entry.getValue();
            Stock first = batchStocks.get(0);
            StockGroupedDto dto = new StockGroupedDto();
            dto.setItemCode(first.getItemCode());
            dto.setItemName(first.getItemName());
            dto.setCategoryName(first.getCategoryName());
            dto.setUnitName(first.getUnitName());
            // 关联制剂名称（取组内第一个有值的）
            String prepName = batchStocks.stream()
                    .map(s -> preparationNameMap.get(s.getPlanNumber()))
                    .filter(StringUtils::hasText)
                    .findFirst().orElse(null);
            dto.setPreparationName(prepName);
            // 总库存 = 所有批次数量之和
            dto.setTotalQuantity(batchStocks.stream()
                    .map(Stock::getQuantity)
                    .filter(q -> q != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            dto.setBatchCount(batchStocks.size());
            // 批次明细
            dto.setBatches(batchStocks.stream().map(s -> {
                StockBatchDto batch = new StockBatchDto();
                batch.setStockId(s.getStockId());
                batch.setBatchNumber(s.getBatchNumber());
                batch.setProdUnitId(s.getProdUnitId());
                batch.setWarehouseName(unitNames.getOrDefault(s.getProdUnitId(), ""));
                batch.setStockStatus(s.getStockStatus() != null ? String.valueOf(s.getStockStatus()) : null);
                batch.setQuantity(s.getQuantity());
                batch.setProductionDate(s.getProductionDate());
                batch.setExpiryDate(s.getExpiryDate());
                batch.setUnitPrice(s.getUnitPrice());
                return batch;
            }).collect(Collectors.toList()));
            return dto;
        }).collect(Collectors.toList());

        // 内存分页
        int total = groups.size();
        int from = pageIndex * pageSize;
        int to = Math.min(from + pageSize, total);
        List<StockGroupedDto> pageData = from < total ? groups.subList(from, to) : List.of();

        PagedResult<StockGroupedDto> result = new PagedResult<>();
        result.setItems(pageData);
        result.setTotalCount(total);
        result.setPageIndex(pageIndex);
        result.setPageSize(pageSize);
        return result;
    }

    /**
     * 加载库存涉及的生产单位名称映射（prodUnitId -> 名称）
     *
     * @param stocks 库存列表
     * @return 生产单位ID到名称的映射
     */
    private Map<Long, String> loadUnitNames(List<Stock> stocks) {
        List<Long> unitIds = stocks.stream()
                .map(Stock::getProdUnitId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (unitIds.isEmpty()) {
            return new HashMap<>();
        }
        QueryWrapper<ProductionUnit> wrapper = new QueryWrapper<>();
        wrapper.in("prod_unit_id", unitIds);
        return productionUnitMapper.selectList(wrapper).stream()
                .filter(u -> u.getProdUnitId() != null && StringUtils.hasText(u.getProdUnitName()))
                .collect(Collectors.toMap(ProductionUnit::getProdUnitId,
                        ProductionUnit::getProdUnitName, (a, b) -> a));
    }

    /**
     * 批量加载关联制剂名称映射（通过库存 plan_number 关联生产计划）
     *
     * @param stocks 库存列表
     * @return 生产计划编号到制剂名称的映射
     */
    private Map<String, String> loadPreparationNameMapByPlanNumber(List<Stock> stocks) {
        List<String> planNumbers = stocks.stream()
                .map(Stock::getPlanNumber)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
        if (planNumbers.isEmpty()) {
            return new HashMap<>();
        }
        QueryWrapper<ProductionPlan> wrapper = new QueryWrapper<>();
        wrapper.in("plan_number", planNumbers);
        wrapper.select("plan_number", "preparation_name");
        return productionPlanMapper.selectList(wrapper).stream()
                .collect(Collectors.toMap(
                        ProductionPlan::getPlanNumber,
                        p -> p.getPreparationName() != null ? p.getPreparationName() : "",
                        (a, b) -> a));
    }

    // endregion

    // region 私有工具方法
    // ===================================
    // 私有工具方法
    // ===================================

    /**
     * 写入库存流水记录
     * <p>
     * 供库存联动（入库/出库确认）及调拨、盘点、退库等业务共用，
     * 统一记录交易前后数量、变动数量与关联单据信息
     * </p>
     *
     * @param stock           库存实体
     * @param transactionType 交易类型（入库类型/出库类型中文值）
     * @param relatedType     关联单据类型（stock_in/stock_out/transfer/check/return）
     * @param relatedId       关联单据ID
     * @param remark          备注
     * @param quantityBefore  交易前数量
     * @param quantityChange  变动数量（正数入库，负数出库）
     */
    public void insertTransaction(Stock stock, String transactionType, String relatedType,
                                   Long relatedId, String remark,
                                   BigDecimal quantityBefore, BigDecimal quantityChange) {
        StockTransaction transaction = new StockTransaction();
        transaction.setStockId(stock.getStockId());
        transaction.setTransactionType(transactionType);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setRelatedId(relatedId);
        transaction.setRelatedType(relatedType);
        transaction.setQuantityBefore(quantityBefore);
        transaction.setQuantityChange(quantityChange);
        BigDecimal after = quantityBefore.add(quantityChange);
        transaction.setQuantityAfter(after.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : after);
        transaction.setBatchNumber(stock.getBatchNumber());
        // 设置物品和仓库信息（用于流水查询）
        transaction.setItemCode(stock.getItemCode());
        transaction.setItemName(stock.getItemName());
        transaction.setProdUnitId(stock.getProdUnitId());
        transaction.setRemark(remark);
        stockTransactionMapper.insert(transaction);
    }

    /**
     * 计算预警信息（剩余天数、预警级别）
     * <p>
     * 预警级别规则：
     * - urgent：7天内过期
     * - warning：30天内过期
     * - info：90天内过期
     * - normal：90天以上
     * </p>
     *
     * @param dto 预警信息DTO，需要设置expiryDate字段
     */
    private void calculateWarningInfo(ExpiryWarningDTO dto) {
        if (dto.getExpiryDate() != null) {
            // 计算距离过期的剩余天数
            long days = ChronoUnit.DAYS.between(LocalDate.now(), dto.getExpiryDate());
            dto.setRemainingDays((int) days);
            
            // 根据剩余天数确定预警级别
            if (days <= 7) {
                dto.setWarningLevel("urgent");
            } else if (days <= 30) {
                dto.setWarningLevel("warning");
            } else if (days <= 90) {
                dto.setWarningLevel("info");
            } else {
                dto.setWarningLevel("normal");
            }
        }
    }

    // endregion

    // region 按批次+制剂分组查询
    // ===================================
    // 按批次+制剂分组查询
    // ===================================

    /**
     * 按批次号+制剂名称分组查询库存（支持筛选与分页）
     */
    @Override
    public PagedResult<StockGroupedByBatchDto> groupedSearchByBatchAndPreparation(
            String itemCode, String itemName, String batchNumber, String preparationName,
            String categoryName, Long prodUnitId, String stockStatus, boolean showZero,
            int pageIndex, int pageSize) {

        // 1. 查询所有库存记录
        QueryWrapper<Stock> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(itemCode)) wrapper.like("item_code", itemCode);
        if (StringUtils.hasText(itemName)) wrapper.like("item_name", itemName);
        if (StringUtils.hasText(batchNumber)) wrapper.like("batch_number", batchNumber);
        if (StringUtils.hasText(categoryName)) wrapper.eq("category_name", categoryName);
        if (prodUnitId != null) wrapper.eq("prod_unit_id", prodUnitId);
        if (StringUtils.hasText(stockStatus)) wrapper.eq("stock_status", stockStatus);
        if (!showZero) wrapper.gt("quantity", 0);
        wrapper.orderByAsc("batch_number");

        List<Stock> allStocks = this.getBaseMapper().selectList(wrapper);

        // 2. 批量加载关联数据
        Map<Long, String> unitNames = loadUnitNames(allStocks);
        Map<String, String> preparationNameMap = loadPreparationNameMapByPlanNumber(allStocks);
        Map<Long, String> stockInCodeMap = loadStockInCodeMap(allStocks);

        // 3. 填充 preparationName
        for (Stock stock : allStocks) {
            if (!StringUtils.hasText(stock.getPreparationName())) {
                stock.setPreparationName(preparationNameMap.get(stock.getPlanNumber()));
            }
        }

        // 4. 过滤 preparationName（如果指定了筛选条件）
        if (StringUtils.hasText(preparationName)) {
            allStocks = allStocks.stream()
                    .filter(s -> s.getPreparationName() != null &&
                                 s.getPreparationName().contains(preparationName))
                    .collect(Collectors.toList());
        }

        // 5. 按 itemCode 分组
        Map<String, List<Stock>> byItemCode = allStocks.stream()
                .collect(Collectors.groupingBy(Stock::getItemCode));

        // 6. 构建物料分组 DTO
        List<StockGroupedByBatchDto> groups = byItemCode.entrySet().stream()
                .map(entry -> {
                    List<Stock> itemStocks = entry.getValue();
                    Stock first = itemStocks.get(0);

                    StockGroupedByBatchDto dto = new StockGroupedByBatchDto();
                    dto.setItemCode(first.getItemCode());
                    dto.setItemName(first.getItemName());
                    dto.setCategoryName(first.getCategoryName());
                    dto.setUnitName(first.getUnitName());

                    // 构建扁平明细列表（按批号+制剂排序）
                    List<StockBatchDetailDto> details = itemStocks.stream()
                            .sorted(Comparator.comparing(Stock::getBatchNumber,
                                            Comparator.nullsLast(Comparator.naturalOrder()))
                                    .thenComparing(s -> s.getPreparationName() != null
                                            ? s.getPreparationName() : "",
                                        Comparator.naturalOrder()))
                            .map(s -> {
                                StockBatchDetailDto detail = new StockBatchDetailDto();
                                detail.setStockId(s.getStockId());
                                detail.setBatchNumber(s.getBatchNumber());
                                detail.setPreparationName(s.getPreparationName());
                                detail.setWarehouseName(unitNames.getOrDefault(s.getProdUnitId(), ""));
                                detail.setStockStatus(s.getStockStatus() != null
                                        ? String.valueOf(s.getStockStatus()) : null);
                                detail.setQuantity(s.getQuantity());
                                detail.setRelatedOrderCode(stockInCodeMap.get(s.getStockInId()));
                                detail.setProductionDate(s.getProductionDate());
                                detail.setExpiryDate(s.getExpiryDate());
                                detail.setUnitPrice(s.getUnitPrice());
                                // 计算金额
                                if (s.getQuantity() != null && s.getUnitPrice() != null) {
                                    detail.setAmount(s.getQuantity().multiply(s.getUnitPrice()));
                                } else {
                                    detail.setAmount(BigDecimal.ZERO);
                                }
                                return detail;
                            })
                            .collect(Collectors.toList());

                    dto.setDetails(details);
                    dto.setEntryCount(details.size());

                    // 批次数（distinct batchNumber）
                    dto.setBatchCount((int) itemStocks.stream()
                            .map(Stock::getBatchNumber)
                            .filter(java.util.Objects::nonNull)
                            .distinct()
                            .count());

                    // 总库存
                    dto.setTotalQuantity(itemStocks.stream()
                            .map(Stock::getQuantity)
                            .filter(q -> q != null)
                            .reduce(BigDecimal.ZERO, BigDecimal::add));

                    // 总金额
                    dto.setTotalValue(details.stream()
                            .map(StockBatchDetailDto::getAmount)
                            .filter(a -> a != null)
                            .reduce(BigDecimal.ZERO, BigDecimal::add));

                    return dto;
                })
                .collect(Collectors.toList());

        // 7. 排序：按分类名称→物料编码（null 排最后）
        groups.sort(Comparator.comparing(StockGroupedByBatchDto::getCategoryName,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(StockGroupedByBatchDto::getItemCode,
                        Comparator.nullsLast(Comparator.naturalOrder())));

        // 8. 内存分页
        int total = groups.size();
        int from = pageIndex * pageSize;
        int to = Math.min(from + pageSize, total);
        List<StockGroupedByBatchDto> pageData = from < total ? groups.subList(from, to) : List.of();

        PagedResult<StockGroupedByBatchDto> result = new PagedResult<>();
        result.setItems(pageData);
        result.setTotalCount(total);
        result.setPageIndex(pageIndex);
        result.setPageSize(pageSize);
        return result;
    }

    /**
     * 加载库存关联的入库单号映射
     *
     * @param stocks 库存列表
     * @return 入库单ID到入库单号的映射
     */
    private Map<Long, String> loadStockInCodeMap(List<Stock> stocks) {
        List<Long> stockInIds = stocks.stream()
                .map(Stock::getStockInId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        if (stockInIds.isEmpty()) return new HashMap<>();

        QueryWrapper<StockIn> wrapper = new QueryWrapper<>();
        wrapper.in("in_id", stockInIds);
        wrapper.select("in_id", "in_code");
        return stockInMapper.selectList(wrapper).stream()
                .filter(si -> si.getInId() != null && StringUtils.hasText(si.getInCode()))
                .collect(Collectors.toMap(StockIn::getInId, StockIn::getInCode, (a, b) -> a));
    }

    // endregion

    // region 细化流水查询
    // ===================================
    // 细化流水查询
    // ===================================

    /**
     * 根据库存ID查询流水列表（增强版）
     */
    @Override
    public List<StockTransactionDetailDto> getTransactionDetailsByStockId(Long stockId) {
        QueryWrapper<StockTransaction> wrapper = new QueryWrapper<>();
        wrapper.eq("stock_id", stockId);
        wrapper.orderByDesc("transaction_date");
        List<StockTransaction> transactions = stockTransactionMapper.selectList(wrapper);
        return buildTransactionDetails(transactions);
    }

    /**
     * 根据批次号+制剂名称查询流水列表
     */
    @Override
    public List<StockTransactionDetailDto> getTransactionDetailsByBatch(
            String batchNumber, String preparationName, String itemCode) {

        // 1. 先查询匹配的库存记录
        QueryWrapper<Stock> stockWrapper = new QueryWrapper<>();
        if (StringUtils.hasText(batchNumber)) stockWrapper.like("batch_number", batchNumber);
        if (StringUtils.hasText(itemCode)) stockWrapper.like("item_code", itemCode);
        List<Stock> stocks = this.getBaseMapper().selectList(stockWrapper);

        // 2. 过滤 preparationName（需要先填充）
        Map<String, String> preparationNameMap = loadPreparationNameMapByPlanNumber(stocks);
        List<Long> stockIds = stocks.stream()
                .filter(s -> {
                    String prepName = s.getPreparationName();
                    if (!StringUtils.hasText(prepName)) {
                        prepName = preparationNameMap.get(s.getPlanNumber());
                    }
                    return preparationName == null ||
                           (prepName != null && prepName.contains(preparationName));
                })
                .map(Stock::getStockId)
                .collect(Collectors.toList());

        if (stockIds.isEmpty()) return List.of();

        // 3. 查询这些库存记录的流水
        QueryWrapper<StockTransaction> txWrapper = new QueryWrapper<>();
        txWrapper.in("stock_id", stockIds);
        txWrapper.orderByDesc("transaction_date");
        List<StockTransaction> transactions = stockTransactionMapper.selectList(txWrapper);
        return buildTransactionDetails(transactions);
    }

    /**
     * 根据入库单ID查询流水列表
     */
    @Override
    public List<StockTransactionDetailDto> getTransactionDetailsByStockInId(Long stockInId) {
        QueryWrapper<StockTransaction> wrapper = new QueryWrapper<>();
        wrapper.eq("related_type", "stock_in");
        wrapper.eq("related_id", stockInId);
        wrapper.orderByDesc("transaction_date");
        List<StockTransaction> transactions = stockTransactionMapper.selectList(wrapper);
        return buildTransactionDetails(transactions);
    }

    /**
     * 根据出库单ID查询流水列表
     */
    @Override
    public List<StockTransactionDetailDto> getTransactionDetailsByStockOutId(Long stockOutId) {
        QueryWrapper<StockTransaction> wrapper = new QueryWrapper<>();
        wrapper.eq("related_type", "stock_out");
        wrapper.eq("related_id", stockOutId);
        wrapper.orderByDesc("transaction_date");
        List<StockTransaction> transactions = stockTransactionMapper.selectList(wrapper);
        return buildTransactionDetails(transactions);
    }

    /**
     * 构建流水详细DTO（核心方法）
     *
     * @param transactions 流水记录列表
     * @return 流水详细DTO列表
     */
    private List<StockTransactionDetailDto> buildTransactionDetails(List<StockTransaction> transactions) {
        if (transactions == null || transactions.isEmpty()) return List.of();

        // 1. 批量加载关联数据
        Map<Long, Stock> stockMap = loadStockMapForTransactions(transactions);
        Map<Long, String> unitNames = loadUnitNamesForTransactions(transactions);
        Map<String, String> docCodeMap = loadDocumentCodes(transactions);
        Map<Long, User> userMap = loadUserMapForTransactions(transactions);

        // 2. 转换为 DTO
        return transactions.stream().map(t -> {
            StockTransactionDetailDto dto = new StockTransactionDetailDto();

            // 基本信息
            dto.setTransactionId(t.getTransactionId());
            dto.setTransactionType(t.getTransactionType() != null ? String.valueOf(t.getTransactionType()) : null);
            dto.setTransactionDate(t.getTransactionDate());

            // 物品信息（从 stock 表获取，优先使用 transaction 中的字段）
            Stock stock = stockMap.get(t.getStockId());
            if (stock != null) {
                dto.setItemCode(stock.getItemCode());
                dto.setItemName(stock.getItemName());
                dto.setCategoryName(stock.getCategoryName());
                dto.setUnitName(stock.getUnitName());
                dto.setProdUnitId(stock.getProdUnitId());
                dto.setWarehouseName(unitNames.getOrDefault(stock.getProdUnitId(), ""));
            } else {
                // 如果 stock 已删除，从 transaction 字段获取
                dto.setItemCode(t.getItemCode());
                dto.setItemName(t.getItemName());
                dto.setProdUnitId(t.getProdUnitId());
                dto.setWarehouseName(unitNames.getOrDefault(t.getProdUnitId(), ""));
            }

            dto.setBatchNumber(t.getBatchNumber());

            // 数量信息
            dto.setQuantityBefore(t.getQuantityBefore());
            dto.setQuantityChange(t.getQuantityChange());
            dto.setQuantityAfter(t.getQuantityAfter());

            // 关联单据
            String relatedType = t.getRelatedType() != null ? String.valueOf(t.getRelatedType()) : null;
            String docCode = docCodeMap.get(relatedType + "_" + t.getRelatedId());
            dto.setRelatedDocCode(docCode);
            dto.setRelatedDocType(resolveDocType(relatedType));
            dto.setRelatedOrderCode(t.getRelatedOrderCode());
            dto.setRelatedOrderType(t.getRelatedOrderType());

            // 操作人
            User user = userMap.get(t.getCreatedBy());
            dto.setCreatedByName(user != null ? user.getUserName() : null);

            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 加载库存记录映射（用于流水查询）
     *
     * @param transactions 流水记录列表
     * @return 库存ID到库存记录的映射
     */
    private Map<Long, Stock> loadStockMapForTransactions(List<StockTransaction> transactions) {
        List<Long> stockIds = transactions.stream()
                .map(StockTransaction::getStockId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        if (stockIds.isEmpty()) return new HashMap<>();

        return this.getBaseMapper().selectBatchIds(stockIds).stream()
                .collect(Collectors.toMap(Stock::getStockId, s -> s, (a, b) -> a));
    }

    /**
     * 加载仓库名称映射（用于流水查询）
     *
     * @param transactions 流水记录列表
     * @return 仓库ID到仓库名称的映射
     */
    private Map<Long, String> loadUnitNamesForTransactions(List<StockTransaction> transactions) {
        List<Long> unitIds = transactions.stream()
                .map(StockTransaction::getProdUnitId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        // 也从关联的 stock 记录获取仓库ID
        Map<Long, Stock> stockMap = loadStockMapForTransactions(transactions);
        stockMap.values().stream()
                .map(Stock::getProdUnitId)
                .filter(id -> id != null)
                .forEach(unitIds::add);

        unitIds = unitIds.stream().distinct().collect(Collectors.toList());

        if (unitIds.isEmpty()) return new HashMap<>();

        QueryWrapper<ProductionUnit> wrapper = new QueryWrapper<>();
        wrapper.in("prod_unit_id", unitIds);
        return productionUnitMapper.selectList(wrapper).stream()
                .collect(Collectors.toMap(ProductionUnit::getProdUnitId,
                        ProductionUnit::getProdUnitName, (a, b) -> a));
    }

    /**
     * 加载关联单据号映射
     *
     * @param transactions 流水记录列表
     * @return 关联类型_ID 到单据号的映射
     */
    private Map<String, String> loadDocumentCodes(List<StockTransaction> transactions) {
        Map<String, String> docCodeMap = new java.util.HashMap<>();

        // 按关联类型分组收集ID
        List<Long> inIds = transactions.stream()
                .filter(t -> "stock_in".equals(String.valueOf(t.getRelatedType())))
                .map(StockTransaction::getRelatedId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<Long> transferIds = transactions.stream()
                .filter(t -> "transfer".equals(String.valueOf(t.getRelatedType())))
                .map(StockTransaction::getRelatedId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<Long> checkIds = transactions.stream()
                .filter(t -> "check".equals(String.valueOf(t.getRelatedType())))
                .map(StockTransaction::getRelatedId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<Long> returnIds = transactions.stream()
                .filter(t -> "return".equals(String.valueOf(t.getRelatedType())))
                .map(StockTransaction::getRelatedId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<Long> outIds = transactions.stream()
                .filter(t -> "stock_out".equals(String.valueOf(t.getRelatedType())))
                .map(StockTransaction::getRelatedId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        // 批量查询各单据表
        if (!inIds.isEmpty()) {
            stockInMapper.selectBatchIds(inIds).forEach(si ->
                docCodeMap.put("stock_in_" + si.getInId(), si.getInCode()));
        }
        if (!transferIds.isEmpty()) {
            transferOrderMapper.selectBatchIds(transferIds).forEach(to ->
                docCodeMap.put("transfer_" + to.getId(), to.getTransferNo()));
        }
        if (!checkIds.isEmpty()) {
            checkOrderMapper.selectBatchIds(checkIds).forEach(co ->
                docCodeMap.put("check_" + co.getId(), co.getCheckNo()));
        }
        if (!returnIds.isEmpty()) {
            returnOrderMapper.selectBatchIds(returnIds).forEach(ro ->
                docCodeMap.put("return_" + ro.getId(), ro.getReturnNo()));
        }
        if (!outIds.isEmpty()) {
            stockOutMapper.selectBatchIds(outIds).forEach(so ->
                docCodeMap.put("stock_out_" + so.getOutId(), so.getOutCode()));
        }

        return docCodeMap;
    }

    /**
     * 加载用户映射（用于流水查询）
     *
     * @param transactions 流水记录列表
     * @return 用户ID到用户的映射
     */
    private Map<Long, User> loadUserMapForTransactions(List<StockTransaction> transactions) {
        List<Long> userIds = transactions.stream()
                .map(StockTransaction::getCreatedBy)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (userIds.isEmpty()) return new HashMap<>();

        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getUserId, u -> u, (a, b) -> a));
    }

    /**
     * 解析单据类型中文名
     *
     * @param relatedType 关联类型
     * @return 中文名称
     */
    private String resolveDocType(String relatedType) {
        if (relatedType == null) return null;
        switch (relatedType) {
            case "stock_in": return "入库";
            case "stock_out": return "出库";
            case "transfer": return "调拨";
            case "check": return "盘点";
            case "return": return "退库";
            default: return relatedType;
        }
    }

    // endregion
}
