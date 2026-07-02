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
* @author 87954
* @description 针对表【stock(库存表（统一管理物料和制剂库存，按生产单位分配）)】的数据库操作Service实现
* @createDate 2025-10-22 10:42:53
*/
@Service
public class StockServiceImpl extends ServiceImpl<StockMapper, Stock>
    implements StockService{

    @Autowired
    private StockTransactionMapper stockTransactionMapper;

    @Autowired
    private StockOutDetailMapper stockOutDetailMapper;

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
        
        // Handle created time range query
        if (createdTimeStart != null) {
            wrapper.ge("created_time", createdTimeStart);
        }
        if (createdTimeEnd != null) {
            wrapper.le("created_time", createdTimeEnd);
        }
        
        // Handle updated time range query - 加强判断条件确保参数有效
        if (updatedTimeStart != null) {
            wrapper.ge("updated_time", updatedTimeStart);
        }
        if (updatedTimeEnd != null) {
            wrapper.le("updated_time", updatedTimeEnd);
        }

        return this.getBaseMapper().selectPage(page, wrapper);
    }

    @Override
    public Page<Stock> queryStocks(Stock stock, int pageNum, int pageSize) {
        return queryStocks(stock, null, null, null, null, pageNum, pageSize);
    }

    @Override
    public PagedResult<StockWithDetailsDto> searchWithDetails(Stock stock, int pageNum, int pageSize) {
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

        List<Long> parentIds = parents.stream().map(Stock::getStockId).collect(Collectors.toList());
        QueryWrapper<StockTransaction> transactionWrapper = new QueryWrapper<>();
        transactionWrapper.in("stock_id", parentIds);
        List<StockTransaction> allTransactions = stockTransactionMapper.selectList(transactionWrapper);
        Map<Long, List<StockTransaction>> transactionsMap = allTransactions.stream()
                .collect(Collectors.groupingBy(StockTransaction::getStockId));

        QueryWrapper<StockOutDetail> outDetailWrapper = new QueryWrapper<>();
        outDetailWrapper.in("stock_id", parentIds);
        List<StockOutDetail> allOutDetails = stockOutDetailMapper.selectList(outDetailWrapper);
        Map<Long, List<StockOutDetail>> outDetailsMap = allOutDetails.stream()
                .collect(Collectors.groupingBy(StockOutDetail::getStockId));

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

    @Override
    public List<ExpiryWarningDTO> getExpiringStocks(int warningDays) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(warningDays);
        
        List<ExpiryWarningDTO> warnings = this.getBaseMapper().selectExpiringStocksWithDetail(
                startDate, endDate, null, null);
        
        // 计算剩余天数和预警级别
        warnings.forEach(this::calculateWarningInfo);
        
        return warnings;
    }

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
        
        // 内存分页
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

    /**
     * 计算预警信息（剩余天数、预警级别）
     */
    private void calculateWarningInfo(ExpiryWarningDTO dto) {
        if (dto.getExpiryDate() != null) {
            long days = ChronoUnit.DAYS.between(LocalDate.now(), dto.getExpiryDate());
            dto.setRemainingDays((int) days);
            
            // 确定预警级别
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

}




