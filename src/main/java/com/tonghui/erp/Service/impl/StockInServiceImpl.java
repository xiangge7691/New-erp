package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Stock.StockInWithDetailsDto;
import com.tonghui.erp.Data.Entity.StockIn;
import com.tonghui.erp.Data.Entity.StockInDetail;
import com.tonghui.erp.Data.mapper.StockInMapper;
import com.tonghui.erp.Data.mapper.StockInDetailMapper;
import com.tonghui.erp.Service.StockInService;
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
 * 入库单业务实现类
 */
@Service
public class StockInServiceImpl extends ServiceImpl<StockInMapper, StockIn> implements StockInService {

    private final StockInMapper stockInMapper;
    private final StockInDetailMapper stockInDetailMapper;
    private final SequenceServiceImpl sequenceService;

    @Autowired
    public StockInServiceImpl(StockInMapper stockInMapper,
                              StockInDetailMapper stockInDetailMapper,
                              SequenceServiceImpl sequenceService) {
        this.stockInMapper = stockInMapper;
        this.stockInDetailMapper = stockInDetailMapper;
        this.sequenceService = sequenceService;
    }

    @Override
    @Transactional
    public void addStockIn(StockIn stockIn, List<StockInDetail> details) {
        // 生成入库单号（调用数据库存储过程）
        if (!StringUtils.hasText(stockIn.getInCode())) {
            stockIn.setInCode(sequenceService.generateStockInCode());
        }

        // 保存入库单主表
        stockInMapper.insert(stockIn);

        // 保存明细表
        if (details != null && !details.isEmpty()) {
            java.time.LocalDate defaultDate = stockIn.getInDate() != null ? stockIn.getInDate() : java.time.LocalDate.now();
            for (StockInDetail detail : details) {
                detail.setInId(stockIn.getInId());
                // 确保 production_date 不为空
                if (detail.getProductionDate() == null) {
                    detail.setProductionDate(defaultDate);
                }
                // 确保 expiry_date 不为空（如果也需要的话）
                if (detail.getExpiryDate() == null && detail.getProductionDate() != null) {
                    // 默认设置 1 年有效期
                    detail.setExpiryDate(detail.getProductionDate().plusYears(1));
                }
                stockInDetailMapper.insert(detail);
            }
        }
    }

    @Override
    @Transactional
    public void updateStockIn(StockIn stockIn, List<StockInDetail> details) {
        // 更新入库单主表
        stockInMapper.updateById(stockIn);

        // 只有当明确提供了明细数据时才更新明细（null 表示未提供，空列表表示清空）
        if (details != null) {
            // 删除原有明细
            QueryWrapper<StockInDetail> deleteWrapper = new QueryWrapper<>();
            deleteWrapper.eq("in_id", stockIn.getInId());
            stockInDetailMapper.delete(deleteWrapper);

            // 重新插入明细
            if (!details.isEmpty()) {
                java.time.LocalDate defaultDate = stockIn.getInDate() != null ? stockIn.getInDate() : java.time.LocalDate.now();
                for (StockInDetail detail : details) {
                    detail.setInId(stockIn.getInId());
                    // 确保 production_date 不为空
                    if (detail.getProductionDate() == null) {
                        detail.setProductionDate(defaultDate);
                    }
                    // 确保 expiry_date 不为空（如果也需要的话）
                    if (detail.getExpiryDate() == null && detail.getProductionDate() != null) {
                        detail.setExpiryDate(detail.getProductionDate().plusYears(1));
                    }
                    stockInDetailMapper.insert(detail);
                }
            }
        }
        // 如果 details 为 null，不处理明细，保持原样
    }

    @Override
    @Transactional
    public void partialUpdateStockIn(StockIn stockIn) {
        // 构建 UpdateWrapper，只更新非 null 字段
        UpdateWrapper<StockIn> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("in_id", stockIn.getInId());
        
        // 动态添加 SET 子句
        if (stockIn.getInCode() != null) {
            updateWrapper.set("in_code", stockIn.getInCode());
        }
        if (stockIn.getInType() != null) {
            updateWrapper.set("in_type", stockIn.getInType());
        }
        if (stockIn.getProdUnitId() != null) {
            updateWrapper.set("prod_unit_id", stockIn.getProdUnitId());
        }
        if (stockIn.getSupplierId() != null) {
            updateWrapper.set("supplier_id", stockIn.getSupplierId());
        }
        if (stockIn.getRelatedOrder() != null) {
            updateWrapper.set("related_order", stockIn.getRelatedOrder());
        }
        if (stockIn.getInDate() != null) {
            updateWrapper.set("in_date", stockIn.getInDate());
        }
        if (stockIn.getTotalAmount() != null) {
            updateWrapper.set("total_amount", stockIn.getTotalAmount());
        }
        if (stockIn.getInStatus() != null) {
            updateWrapper.set("in_status", stockIn.getInStatus());
        }
        if (stockIn.getRemark() != null) {
            updateWrapper.set("remark", stockIn.getRemark());
        }
        if (stockIn.getUpdatedBy() != null) {
            updateWrapper.set("updated_by", stockIn.getUpdatedBy());
        }
        
        // 执行更新，直接调用 update 方法，避免传入 null 实体对象
        boolean updated = this.update(updateWrapper);
        if (!updated) {
            throw new RuntimeException("入库单不存在或未被更新");
        }
    }

    @Override
    @Transactional
    public void deleteStockIn(Long stockInId) {
        // 删除明细表
        QueryWrapper<StockInDetail> detailWrapper = new QueryWrapper<>();
        detailWrapper.eq("in_id", stockInId);
        stockInDetailMapper.delete(detailWrapper);

        // 删除主表
        stockInMapper.deleteById(stockInId);
    }

    // #region 查询操作

    /**
     * 根据入库单号查询入库单
     *
     * @param stockInCode 入库单号
     * @return 入库单实体
     */
    @Override
    public StockIn getStockInByCode(String stockInCode) {
        QueryWrapper<StockIn> wrapper = new QueryWrapper<>();
        wrapper.eq("in_code", stockInCode);
        return stockInMapper.selectOne(wrapper);
    }

    /**
     * 查询所有入库单
     *
     * @return 入库单集合
     */
    @Override
    public List<StockIn> getAllStockIns() {
        return stockInMapper.selectList(null);
    }

    @Override
    public StockIn getStockInById(Long stockInId) {
        return stockInMapper.selectById(stockInId);
    }

    @Override
    public List<StockInDetail> getStockInDetailsByStockInId(Long stockInId) {
        QueryWrapper<StockInDetail> wrapper = new QueryWrapper<>();
        wrapper.eq("in_id", stockInId);
        return stockInDetailMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public void addStockInDetail(StockInDetail detail) {
        // 确保 production_date 不为空
        if (detail.getProductionDate() == null) {
            detail.setProductionDate(java.time.LocalDate.now());
        }
        stockInDetailMapper.insert(detail);
    }

    @Override
    @Transactional
    public void addStockInDetails(List<StockInDetail> details) {
        java.time.LocalDate defaultDate = java.time.LocalDate.now();
        for (StockInDetail detail : details) {
            // 确保 production_date 不为空
            if (detail.getProductionDate() == null) {
                detail.setProductionDate(defaultDate);
            }
            stockInDetailMapper.insert(detail);
        }
    }

    @Override
    @Transactional
    public void updateStockInDetail(StockInDetail detail) {
        // 如果要更新的 production_date 为 null，先查询原有记录保留原值
        if (detail.getProductionDate() == null) {
            StockInDetail existingDetail = stockInDetailMapper.selectById(detail.getInDetailId());
            if (existingDetail != null && existingDetail.getProductionDate() != null) {
                detail.setProductionDate(existingDetail.getProductionDate());
            } else {
                detail.setProductionDate(java.time.LocalDate.now());
            }
        }
        stockInDetailMapper.updateById(detail);
    }

    @Override
    public void deleteStockInDetail(Long detailId) {
        stockInDetailMapper.deleteById(detailId);
    }

    // #endregion

    // #region 单号生成

    @Override
    public String generateStockInCode() {
        // 调用数据库存储过程生成入库单号
        return sequenceService.generateStockInCode();
    }

    // #endregion

    // #region 高级查询

    @Override
    public Page<StockIn> queryStockIns(StockIn stockIn, LocalDateTime createdTimeStart, LocalDateTime createdTimeEnd, LocalDateTime updatedTimeStart, LocalDateTime updatedTimeEnd, LocalDate startDate, LocalDate endDate, int pageIndex, int pageSize) {
        // 将页码从0开始转换为1开始
        int actualPageIndex = pageIndex + 1;

        Page<StockIn> page = new Page<>(actualPageIndex, pageSize);
        QueryWrapper<StockIn> wrapper = new QueryWrapper<>();

        if (stockIn.getInId() != null) {
            wrapper.eq("in_id", stockIn.getInId());
        }
        if (StringUtils.hasText(stockIn.getInCode())) {
            wrapper.like("in_code", stockIn.getInCode());
        }
        if (stockIn.getProdUnitId() != null) {
            wrapper.eq("prod_unit_id", stockIn.getProdUnitId());
        }
        if (stockIn.getSupplierId() != null) {
            wrapper.eq("supplier_id", stockIn.getSupplierId());
        }
        if (StringUtils.hasText(stockIn.getRelatedOrder())) {
            wrapper.like("related_order", stockIn.getRelatedOrder());
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
        if (stockIn.getCreatedBy() != null) {
            wrapper.eq("created_by", stockIn.getCreatedBy());
        }
        if (stockIn.getUpdatedBy() != null) {
            wrapper.eq("updated_by", stockIn.getUpdatedBy());
        }
        // 添加入库类型查询条件
        if (stockIn.getInType() != null) {
            wrapper.eq("in_type", stockIn.getInType());
        }
        // 添加入库状态查询条件
        if (StringUtils.hasText(stockIn.getInStatus())) {
            wrapper.eq("in_status", stockIn.getInStatus());
        }
        
        // 按编号倒序排列
        wrapper.orderByDesc("in_code");

        return stockInMapper.selectPage(page, wrapper);
    }

    // #endregion

    // #region 带子表查询

    @Override
    public PagedResult<StockInWithDetailsDto> searchWithDetails(StockIn stockIn, LocalDateTime createdTimeStart, LocalDateTime createdTimeEnd, LocalDateTime updatedTimeStart, LocalDateTime updatedTimeEnd, LocalDate startDate, LocalDate endDate, int pageNum, int pageSize) {
        Page<StockIn> parentPage = queryStockIns(stockIn, createdTimeStart, createdTimeEnd, updatedTimeStart, updatedTimeEnd, startDate, endDate, pageNum, pageSize);
        List<StockIn> parents = parentPage.getRecords();

        PagedResult<StockInWithDetailsDto> result = new PagedResult<>();
        if (parents.isEmpty()) {
            result.setItems(List.of());
            result.setTotalCount(parentPage.getTotal());
            result.setPageIndex(pageNum);
            result.setPageSize(pageSize);
            return result;
        }

        List<Long> parentIds = parents.stream().map(StockIn::getInId).collect(Collectors.toList());
        QueryWrapper<StockInDetail> wrapper = new QueryWrapper<>();
        wrapper.in("in_id", parentIds);
        List<StockInDetail> allDetails = stockInDetailMapper.selectList(wrapper);
        Map<Long, List<StockInDetail>> detailsMap = allDetails.stream()
                .collect(Collectors.groupingBy(StockInDetail::getInId));

        List<StockInWithDetailsDto> dtos = parents.stream().map(parent -> {
            StockInWithDetailsDto dto = new StockInWithDetailsDto();
            BeanUtils.copyProperties(parent, dto);
            dto.setDetails(detailsMap.getOrDefault(parent.getInId(), List.of()));
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
