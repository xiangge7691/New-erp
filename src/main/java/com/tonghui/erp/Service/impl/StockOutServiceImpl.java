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

    /**
     * 构造函数注入依赖
     *
     * @param stockOutMapper       出库单数据访问层
     * @param stockOutDetailMapper 出库单明细数据访问层
     * @param sequenceService      序列号生成服务
     */
    @Autowired
    public StockOutServiceImpl(StockOutMapper stockOutMapper,
                               StockOutDetailMapper stockOutDetailMapper,
                               SequenceServiceImpl sequenceService) {
        this.stockOutMapper = stockOutMapper;
        this.stockOutDetailMapper = stockOutDetailMapper;
        this.sequenceService = sequenceService;
    }

    // endregion

    // region 基础CRUD操作
    // ===================================
    // 基础CRUD操作
    // ===================================

    /**
     * 新增出库单（含明细）
     * <p>自动生成出库单号（如果未提供），同时保存主表和明细数据</p>
     *
     * @param stockOut 出库单主表实体
     * @param details  出库单明细列表，可为null
     */
    @Override
    @Transactional
    public void addStockOut(StockOut stockOut, List<StockOutDetail> details) {
        // 自动生成出库单号（如果未提供）
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
}
