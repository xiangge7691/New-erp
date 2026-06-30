package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Stock.StockOutWithDetailsDto;
import com.tonghui.erp.Data.Entity.StockOut;

import com.tonghui.erp.Data.Entity.StockOutDetail;
import com.tonghui.erp.Data.mapper.StockOutMapper;
import com.tonghui.erp.Data.mapper.StockOutDetailMapper;
import com.tonghui.erp.Service.StockOutService;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 出库单业务实现类
 */
@Service
public class StockOutServiceImpl extends ServiceImpl<StockOutMapper, StockOut> implements StockOutService {

    private final StockOutMapper stockOutMapper;
    private final StockOutDetailMapper stockOutDetailMapper;
    private final SequenceServiceImpl sequenceService;

    @Autowired
    public StockOutServiceImpl(StockOutMapper stockOutMapper,
                               StockOutDetailMapper stockOutDetailMapper,
                               SequenceServiceImpl sequenceService) {
        this.stockOutMapper = stockOutMapper;
        this.stockOutDetailMapper = stockOutDetailMapper;
        this.sequenceService = sequenceService;
    }

    @Override
    @Transactional
    public void addStockOut(StockOut stockOut, List<StockOutDetail> details) {
        // 生成出库单号（调用数据库存储过程）
        if (!StringUtils.hasText(stockOut.getOutCode())) {
            stockOut.setOutCode(sequenceService.generateStockOutCode());
        }

        // 保存出库单主表
        stockOutMapper.insert(stockOut);

        // 保存明细表
        if (details != null && !details.isEmpty()) {
            for (StockOutDetail detail : details) {
                detail.setOutId(stockOut.getOutId());
                stockOutDetailMapper.insert(detail);
            }
        }
    }

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

    // #region 查询操作

    /**
     * 根据出库单号查询出库单
     *
     * @param stockOutCode 出库单号
     * @return 出库单实体
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

    @Override
    public StockOut getStockOutById(Long stockOutId) {
        return stockOutMapper.selectById(stockOutId);
    }

    @Override
    public List<StockOutDetail> getStockOutDetailsByStockOutId(Long stockOutId) {
        QueryWrapper<StockOutDetail> wrapper = new QueryWrapper<>();
        wrapper.eq("out_id", stockOutId);
        return stockOutDetailMapper.selectList(wrapper);
    }

    @Override
    public void addStockOutDetail(StockOutDetail detail) {
        stockOutDetailMapper.insert(detail);
    }

    @Override
    public void addStockOutDetails(List<StockOutDetail> details) {
        for (StockOutDetail detail : details) {
            stockOutDetailMapper.insert(detail);
        }
    }

    @Override
    public void updateStockOutDetail(StockOutDetail detail) {
        stockOutDetailMapper.updateById(detail);
    }

    @Override
    public void deleteStockOutDetail(Long detailId) {
        stockOutDetailMapper.deleteById(detailId);
    }

    // #endregion

    // #region 单号生成

    @Override
    public String generateStockOutCode() {
        // 调用数据库存储过程生成出库单号，与StockInServiceImpl保持一致
        return sequenceService.generateStockOutCode();
    }

    // #endregion

    // #region 高级查询

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
        // 添加创建时间范围查询条件
        if (createdTimeStart != null) {
            wrapper.ge("created_time", createdTimeStart);
        }
        if (createdTimeEnd != null) {
            wrapper.le("created_time", createdTimeEnd);
        }
        // 添加更新时间范围查询条件
        if (updatedTimeStart != null) {
            wrapper.ge("updated_time", updatedTimeStart);
        }
        if (updatedTimeEnd != null) {
            wrapper.le("updated_time", updatedTimeEnd);
        }
        // 添加创建人和更新人查询条件
        if (stockOut.getCreatedBy() != null) {
            wrapper.eq("created_by", stockOut.getCreatedBy());
        }
        if (stockOut.getUpdatedBy() != null) {
            wrapper.eq("updated_by", stockOut.getUpdatedBy());
        }
        // 添加出库类型查询条件
        if (stockOut.getOutType() != null) {
            wrapper.eq("out_type", stockOut.getOutType());
        }
        // 添加出库状态查询条件
        if (StringUtils.hasText(stockOut.getOutStatus())) {
            wrapper.eq("out_status", stockOut.getOutStatus());
        }

        // 按编号倒序排列
        wrapper.orderByDesc("out_code");

        return stockOutMapper.selectPage(page, wrapper);
    }

    // #endregion

    // #region 带子表查询

    @Override
    public PagedResult<StockOutWithDetailsDto> searchWithDetails(StockOut stockOut, LocalDateTime createdTimeStart, LocalDateTime createdTimeEnd, LocalDateTime updatedTimeStart, LocalDateTime updatedTimeEnd, LocalDate startDate, LocalDate endDate, int pageNum, int pageSize) {
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

        List<Long> parentIds = parents.stream().map(StockOut::getOutId).collect(Collectors.toList());
        QueryWrapper<StockOutDetail> wrapper = new QueryWrapper<>();
        wrapper.in("out_id", parentIds);
        List<StockOutDetail> allDetails = stockOutDetailMapper.selectList(wrapper);
        Map<Long, List<StockOutDetail>> detailsMap = allDetails.stream()
                .collect(Collectors.groupingBy(StockOutDetail::getOutId));

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

    // #endregion
}