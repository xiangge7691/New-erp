package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Stock.ExpiryWarningDTO;
import com.tonghui.erp.Common.Dto.Stock.ExpiryWarningStatsDTO;
import com.tonghui.erp.Common.Dto.Stock.StockBatchDto;
import com.tonghui.erp.Common.Dto.Stock.StockGroupedDto;
import com.tonghui.erp.Common.Dto.Stock.StockWithDetailsDto;
import com.tonghui.erp.Data.Entity.ProductionUnit;
import com.tonghui.erp.Data.Entity.Stock;
import com.tonghui.erp.Data.Entity.StockIn;
import com.tonghui.erp.Data.Entity.StockInDetail;
import com.tonghui.erp.Data.Entity.StockOut;
import com.tonghui.erp.Data.Entity.StockOutDetail;
import com.tonghui.erp.Data.Entity.StockTransaction;
import com.tonghui.erp.Data.mapper.ProductionUnitMapper;
import com.tonghui.erp.Data.mapper.StockMapper;
import com.tonghui.erp.Data.mapper.StockOutDetailMapper;
import com.tonghui.erp.Data.mapper.StockTransactionMapper;
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

        return this.getBaseMapper().selectPage(page, wrapper);
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
        Map<Long, List<StockTransaction>> transactionsMap = allTransactions.stream()
                .collect(Collectors.groupingBy(StockTransaction::getStockId));

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
            // 按 物品编码 + 生产单位 + 批号 定位库存批次
            QueryWrapper<Stock> wrapper = new QueryWrapper<>();
            wrapper.eq("item_code", detail.getItemCode());
            wrapper.eq("prod_unit_id", stockIn.getProdUnitId());
            wrapper.eq("batch_number", detail.getBatchNumber());
            Stock existing = this.getBaseMapper().selectOne(wrapper);

            BigDecimal before = existing != null ? existing.getQuantity() : BigDecimal.ZERO;
            Stock stock;
            if (existing != null) {
                // 同批次再次入库：数量累加，单价以最新入库单价为准
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
                // 新增库存批次
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

    /**
     * 根据库存ID查询库存流水列表
     *
     * @param stockId 库存ID
     * @return 流水列表
     */
    @Override
    public List<StockTransaction> getTransactionsByStockId(Long stockId) {
        QueryWrapper<StockTransaction> wrapper = new QueryWrapper<>();
        wrapper.eq("stock_id", stockId);
        wrapper.orderByDesc("transaction_date");
        return stockTransactionMapper.selectList(wrapper);
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
            return Map.of();
        }
        QueryWrapper<ProductionUnit> wrapper = new QueryWrapper<>();
        wrapper.in("prod_unit_id", unitIds);
        return productionUnitMapper.selectList(wrapper).stream()
                .collect(Collectors.toMap(ProductionUnit::getProdUnitId,
                        ProductionUnit::getProdUnitName, (a, b) -> a));
    }

    // endregion

    // region 私有工具方法
    // ===================================
    // 私有工具方法
    // ===================================

    /**
     * 写入库存流水记录
     *
     * @param stock           库存实体
     * @param transactionType 交易类型（入库类型/出库类型中文值）
     * @param relatedType     关联单据类型（stock_in/stock_out）
     * @param relatedId       关联单据ID
     * @param remark          备注
     * @param quantityBefore  交易前数量
     * @param quantityChange  变动数量（正数入库，负数出库）
     */
    private void insertTransaction(Stock stock, String transactionType, String relatedType,
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
}
