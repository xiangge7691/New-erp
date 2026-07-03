package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Stock.ExpiryWarningDTO;
import com.tonghui.erp.Common.Dto.Stock.ExpiryWarningStatsDTO;
import com.tonghui.erp.Common.Dto.Stock.StockWithDetailsDto;
import com.tonghui.erp.Data.Entity.Stock;
import com.tonghui.erp.Data.Entity.StockOutDetail;
import com.tonghui.erp.Data.Entity.StockTransaction;
import com.tonghui.erp.Data.mapper.StockMapper;
import com.tonghui.erp.Data.mapper.StockOutDetailMapper;
import com.tonghui.erp.Data.mapper.StockTransactionMapper;
import com.tonghui.erp.Service.StockService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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

    // endregion

    // region 高级查询
    // ===================================
    // 高级查询
    // ===================================

    /**
     * 高级查询库存（支持多条件组合查询和自定义时间范围筛选）
     *
     * @param stock             查询条件实体，非null字段将作为等值或模糊查询条件
     * @param createdTimeStart  创建时间起始值（含）
     * @param createdTimeEnd    创建时间结束值（含）
     * @param updatedTimeStart  更新时间起始值（含）
     * @param updatedTimeEnd    更新时间结束值（含）
     * @param pageIndex         页码，从0开始
     * @param pageSize          每页数量
     * @return 库存分页结果
     */
    @Override
    public Page<Stock> queryStocks(Stock stock, LocalDateTime createdTimeStart, LocalDateTime createdTimeEnd, LocalDateTime updatedTimeStart, LocalDateTime updatedTimeEnd, int pageIndex, int pageSize) {
        // 页码处理，MyBatis Plus Page页码从1开始
        int actualPageIndex = pageIndex + 1;

        Page<Stock> page = new Page<>(actualPageIndex, pageSize);
        QueryWrapper<Stock> wrapper = new QueryWrapper<>();

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
     * @param pageNum  页码，从0开始
     * @param pageSize 每页数量
     * @return 库存分页结果
     */
    @Override
    public Page<Stock> queryStocks(Stock stock, int pageNum, int pageSize) {
        return queryStocks(stock, null, null, null, null, pageNum, pageSize);
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
     * @param pageNum  页码，从0开始
     * @param pageSize 每页数量
     * @return 带子表关联数据的库存分页结果
     */
    @Override
    public PagedResult<StockWithDetailsDto> searchWithDetails(Stock stock, int pageNum, int pageSize) {
        // 查询库存主表分页数据
        Page<Stock> parentPage = queryStocks(stock, pageNum, pageSize);
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

    // region 私有工具方法
    // ===================================
    // 私有工具方法
    // ===================================

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
