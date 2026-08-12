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
import com.tonghui.erp.Service.StockService;
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
 * <p>
 * 实现StockInService接口，提供入库单相关的业务逻辑处理，包括入库单及明细的增删改查、
 * 部分更新、单号自动生成、高级查询、带子表关联查询等功能的具体实现
 * </p>
 *
 */
@Service
public class StockInServiceImpl extends ServiceImpl<StockInMapper, StockIn> implements StockInService {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /** 入库单数据访问层 */
    private final StockInMapper stockInMapper;

    /** 入库单明细数据访问层 */
    private final StockInDetailMapper stockInDetailMapper;

    /** 序列号生成服务，用于自动生成入库单号 */
    private final SequenceServiceImpl sequenceService;

    /** 库存服务，用于入库确认时的库存联动 */
    private final StockService stockService;

    /**
     * 构造函数注入依赖
     *
     * @param stockInMapper      入库单数据访问层
     * @param stockInDetailMapper 入库单明细数据访问层
     * @param sequenceService    序列号生成服务
     * @param stockService       库存服务
     */
    @Autowired
    public StockInServiceImpl(StockInMapper stockInMapper,
                              StockInDetailMapper stockInDetailMapper,
                              SequenceServiceImpl sequenceService,
                              StockService stockService) {
        this.stockInMapper = stockInMapper;
        this.stockInDetailMapper = stockInDetailMapper;
        this.sequenceService = sequenceService;
        this.stockService = stockService;
    }

    // endregion

    // region 基础CRUD操作
    // ===================================
    // 基础CRUD操作
    // ===================================

    /**
     * 新增入库单（含明细）并直接生效入库
     * <p>
     * 自动生成入库单号（如果未提供），保存主表和明细数据后立即联动库存表（按物品+仓库+批号 upsert）并写入库存流水。
     * 任一步失败抛出异常，整个事务回滚，保证原子性
     * </p>
     *
     * @param stockIn 入库单主表实体
     * @param details 入库单明细列表，不可为空
     */
    @Override
    @Transactional
    public void addStockIn(StockIn stockIn, List<StockInDetail> details) {
        // 校验明细不能为空（添加即入库，无明细无法联动库存）
        if (details == null || details.isEmpty()) {
            throw new RuntimeException("入库单没有明细，无法入库");
        }

        // 自动生成入库单号（如果未提供）
        if (!StringUtils.hasText(stockIn.getInCode())) {
            stockIn.setInCode(sequenceService.generateStockInCode());
        }
        // 添加即生效：直接置为已入库状态，无需草稿确认流程
        stockIn.setInStatus("已入库");

        // 保存入库单主表（入库日期未传时默认为当前时间，精确到时分秒）
        if (stockIn.getInDate() == null) {
            stockIn.setInDate(java.time.LocalDateTime.now());
        }
        stockInMapper.insert(stockIn);

        // 保存明细表（生产日期取入库日期当天）
        java.time.LocalDate defaultDate = stockIn.getInDate().toLocalDate();
        for (StockInDetail detail : details) {
            detail.setInId(stockIn.getInId());
            // 确保 production_date 不为空
            if (detail.getProductionDate() == null) {
                detail.setProductionDate(defaultDate);
            }
            // 确保 expiry_date 不为空，如果未设置则默认1年有效期
            if (detail.getExpiryDate() == null && detail.getProductionDate() != null) {
                detail.setExpiryDate(detail.getProductionDate().plusYears(1));
            }
            stockInDetailMapper.insert(detail);
        }

        // 库存联动：按明细 upsert 库存批次并写流水（库存校验失败抛异常整体回滚）
        stockService.applyInbound(stockIn, details);
    }

    /**
     * 更新入库单（含明细）
     * <p>更新主表数据，如果提供了明细则先删除原有明细再重新插入</p>
     *
     * @param stockIn 入库单主表实体
     * @param details 入库单明细列表，null表示不更新明细，空列表表示清空明细
     */
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

            // 重新插入明细（生产日期取入库日期当天）
            if (!details.isEmpty()) {
                java.time.LocalDate defaultDate = stockIn.getInDate() != null ? stockIn.getInDate().toLocalDate() : java.time.LocalDate.now();
                for (StockInDetail detail : details) {
                    detail.setInId(stockIn.getInId());
                    // 确保 production_date 不为空
                    if (detail.getProductionDate() == null) {
                        detail.setProductionDate(defaultDate);
                    }
                    // 确保 expiry_date 不为空，如果未设置则默认1年有效期
                    if (detail.getExpiryDate() == null && detail.getProductionDate() != null) {
                        detail.setExpiryDate(detail.getProductionDate().plusYears(1));
                    }
                    stockInDetailMapper.insert(detail);
                }
            }
        }
        // 如果 details 为 null，不处理明细，保持原样
    }

    /**
     * 部分更新入库单（仅更新非null字段）
     * <p>使用UpdateWrapper实现动态字段更新，避免将null值覆盖已有数据</p>
     *
     * @param stockIn 入库单实体，仅非null字段会被更新
     */
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
        
        // 执行更新
        boolean updated = this.update(updateWrapper);
        if (!updated) {
            throw new RuntimeException("入库单不存在或未被更新");
        }
    }

    /**
     * 删除入库单（含明细）
     * <p>先删除关联的入库明细，再删除入库单主表</p>
     *
     * @param stockInId 入库单ID
     */
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

    /**
     * 确认入库：草稿 → 已入库
     * <p>校验入库单为草稿状态且有明细，随后调用公共库存服务联动库存表并写入库存流水</p>
     *
     * @param stockInId 入库单ID
     */
    @Override
    @Transactional
    public void confirmStockIn(Long stockInId) {
        StockIn stockIn = stockInMapper.selectById(stockInId);
        if (stockIn == null) {
            throw new RuntimeException("入库单不存在");
        }
        if (!"草稿".equals(stockIn.getInStatus())) {
            throw new RuntimeException("仅草稿状态的入库单可确认");
        }
        List<StockInDetail> details = getStockInDetailsByStockInId(stockInId);
        if (details.isEmpty()) {
            throw new RuntimeException("入库单没有明细，无法确认");
        }
        // 库存联动：更新库存批次并写流水
        stockService.applyInbound(stockIn, details);
        // 更新入库单状态为已入库
        stockIn.setInStatus("已入库");
        stockInMapper.updateById(stockIn);
    }

    /**
     * 取消入库：已入库 → 已取消
     * <p>校验入库单为已入库状态，随后回滚库存（扣减对应库存批次）并写入调整流水</p>
     *
     * @param stockInId 入库单ID
     */
    @Override
    @Transactional
    public void cancelStockIn(Long stockInId) {
        StockIn stockIn = stockInMapper.selectById(stockInId);
        if (stockIn == null) {
            throw new RuntimeException("入库单不存在");
        }
        if (!"已入库".equals(stockIn.getInStatus())) {
            throw new RuntimeException("仅已入库状态的入库单可取消");
        }
        List<StockInDetail> details = getStockInDetailsByStockInId(stockInId);
        if (details.isEmpty()) {
            throw new RuntimeException("入库单没有明细，无法取消");
        }
        // 库存回滚：扣减库存批次并写调整流水
        stockService.rollbackInbound(stockIn, details);
        // 更新入库单状态为已取消
        stockIn.setInStatus("已取消");
        stockInMapper.updateById(stockIn);
    }

    // endregion

    // region 查询操作
    // ===================================
    // 查询操作
    // ===================================

    /**
     * 根据入库单号查询入库单
     *
     * @param stockInCode 入库单号
     * @return 入库单实体，不存在则返回null
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

    /**
     * 根据ID查询入库单
     *
     * @param stockInId 入库单ID
     * @return 入库单实体，不存在则返回null
     */
    @Override
    public StockIn getStockInById(Long stockInId) {
        return stockInMapper.selectById(stockInId);
    }

    /**
     * 根据入库单ID查询所有入库明细
     *
     * @param stockInId 入库单ID
     * @return 该入库单下所有明细的集合
     */
    @Override
    public List<StockInDetail> getStockInDetailsByStockInId(Long stockInId) {
        QueryWrapper<StockInDetail> wrapper = new QueryWrapper<>();
        wrapper.eq("in_id", stockInId);
        return stockInDetailMapper.selectList(wrapper);
    }

    /**
     * 新增单条入库明细
     * <p>自动设置生产日期为当前日期（如果未提供）</p>
     *
     * @param detail 入库明细实体
     */
    @Override
    @Transactional
    public void addStockInDetail(StockInDetail detail) {
        // 确保 production_date 不为空
        if (detail.getProductionDate() == null) {
            detail.setProductionDate(java.time.LocalDate.now());
        }
        stockInDetailMapper.insert(detail);
    }

    /**
     * 批量新增入库明细
     * <p>自动为每条明细设置生产日期为当前日期（如果未提供）</p>
     *
     * @param details 入库明细列表
     */
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

    /**
     * 更新入库明细
     * <p>如果更新的生产日期为null，则保留原有值</p>
     *
     * @param detail 入库明细实体
     */
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

    /**
     * 删除入库明细
     *
     * @param detailId 入库明细ID
     */
    @Override
    public void deleteStockInDetail(Long detailId) {
        stockInDetailMapper.deleteById(detailId);
    }

    // endregion

    // region 单号生成
    // ===================================
    // 单号生成
    // ===================================

    /**
     * 生成入库单号
     *
     * @return 自动生成的唯一入库单号
     */
    @Override
    public String generateStockInCode() {
        // 调用序列号生成服务获取入库单号
        return sequenceService.generateStockInCode();
    }

    // endregion

    // region 高级查询
    // ===================================
    // 高级查询
    // ===================================

    /**
     * 高级查询入库单（支持多条件组合查询和时间范围筛选）
     *
     * @param stockIn           查询条件实体
     * @param createdTimeStart  创建时间起始值（含）
     * @param createdTimeEnd    创建时间结束值（含）
     * @param updatedTimeStart  更新时间起始值（含）
     * @param updatedTimeEnd    更新时间结束值（含）
     * @param startDate         入库日期起始值（含）
     * @param endDate           入库日期结束值（含）
     * @param pageIndex         页码，从0开始
     * @param pageSize          每页数量
     * @return 入库单分页结果
     */
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
        if (stockIn.getCreatedBy() != null) {
            wrapper.eq("created_by", stockIn.getCreatedBy());
        }
        if (stockIn.getUpdatedBy() != null) {
            wrapper.eq("updated_by", stockIn.getUpdatedBy());
        }
        // 入库类型查询
        if (stockIn.getInType() != null) {
            wrapper.eq("in_type", stockIn.getInType());
        }
        // 入库状态查询
        if (StringUtils.hasText(stockIn.getInStatus())) {
            wrapper.eq("in_status", stockIn.getInStatus());
        }
        // 入库日期范围查询（按整天含端点：起始00:00:00，结束23:59:59）
        if (startDate != null) {
            wrapper.ge("in_date", startDate.atStartOfDay());
        }
        if (endDate != null) {
            wrapper.le("in_date", endDate.atTime(23, 59, 59));
        }
        
        // 按编号倒序排列
        wrapper.orderByDesc("in_code");

        return stockInMapper.selectPage(page, wrapper);
    }

    // endregion

    // region 带子表关联查询
    // ===================================
    // 带子表关联查询
    // ===================================

    /**
     * 查询入库单列表并关联入库明细信息
     * <p>先分页查询入库单主表数据，再批量查询关联的入库明细</p>
     *
     * @param stockIn           查询条件实体
     * @param createdTimeStart  创建时间起始值（含）
     * @param createdTimeEnd    创建时间结束值（含）
     * @param updatedTimeStart  更新时间起始值（含）
     * @param updatedTimeEnd    更新时间结束值（含）
     * @param startDate         入库日期起始值（含）
     * @param endDate           入库日期结束值（含）
     * @param pageNum           页码，从0开始
     * @param pageSize          每页数量
     * @return 带子表关联数据的入库单分页结果
     */
    @Override
    public PagedResult<StockInWithDetailsDto> searchWithDetails(StockIn stockIn, LocalDateTime createdTimeStart, LocalDateTime createdTimeEnd, LocalDateTime updatedTimeStart, LocalDateTime updatedTimeEnd, LocalDate startDate, LocalDate endDate, int pageNum, int pageSize) {
        // 查询入库单主表分页数据
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

        // 批量查询关联的入库明细
        List<Long> parentIds = parents.stream().map(StockIn::getInId).collect(Collectors.toList());
        QueryWrapper<StockInDetail> wrapper = new QueryWrapper<>();
        wrapper.in("in_id", parentIds);
        List<StockInDetail> allDetails = stockInDetailMapper.selectList(wrapper);
        Map<Long, List<StockInDetail>> detailsMap = allDetails.stream()
                .collect(Collectors.groupingBy(StockInDetail::getInId));

        // 组装带子表数据的DTO
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

    // endregion
}
