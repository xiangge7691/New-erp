package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.Dto.Warehouse.CheckItemRequest;
import com.tonghui.erp.Common.Dto.Warehouse.CheckOrderCreateDto;
import com.tonghui.erp.Common.Dto.Warehouse.CheckOrderDetailDto;
import com.tonghui.erp.Common.Dto.Warehouse.CheckOrderDetailItemDto;
import com.tonghui.erp.Common.Dto.Warehouse.StockDetailItemDto;
import com.tonghui.erp.Common.utils.EntityUtils;
import com.tonghui.erp.Data.Entity.CheckOrder;
import com.tonghui.erp.Data.Entity.CheckOrderDetail;
import com.tonghui.erp.Data.Entity.ProductionUnit;
import com.tonghui.erp.Data.Entity.Stock;
import com.tonghui.erp.Data.Entity.User;
import com.tonghui.erp.Data.mapper.CheckOrderDetailMapper;
import com.tonghui.erp.Data.mapper.CheckOrderMapper;
import com.tonghui.erp.Data.mapper.ProductionUnitMapper;
import com.tonghui.erp.Data.mapper.StockMapper;
import com.tonghui.erp.Data.mapper.UserMapper;
import com.tonghui.erp.Service.CheckOrderService;
import com.tonghui.erp.Service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 盘点单服务实现类
 * <p>
 * 实现盘点单查询、盘点仓库/库存明细查询及提交盘点业务：
 * 提交盘点时在同事务内计算差异（实盘-系统），对有差异的库存行调整数量
 * （盘盈加正差异、盘亏加负差异），生成盘盈入库/盘亏出库流水，并保存主表与明细
 * </p>
 */
@Service
public class CheckOrderServiceImpl extends ServiceImpl<CheckOrderMapper, CheckOrder>
        implements CheckOrderService {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    @Autowired
    private CheckOrderDetailMapper checkOrderDetailMapper;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private ProductionUnitMapper productionUnitMapper;

    @Autowired
    private StockService stockService;

    @Autowired
    private UserMapper userMapper;

    /** 盘点单号生成最大重试次数（处理并发编号冲突） */
    private static final int MAX_RETRY = 3;

    // endregion

    // region 查询操作
    // ===================================
    // 查询操作
    // ===================================

    /**
     * 分页查询盘点单列表
     *
     * @param warehouse 仓库名称筛选（可选）
     * @param keyword   搜索关键词（盘点单号/物料名称，可选）
     * @param startTime 创建时间起始（可选）
     * @param endTime   创建时间结束（可选）
     * @param pageIndex 页码（从0开始）
     * @param pageSize  每页数量
     * @return 分页结果（主表信息列表）
     */
    @Override
    public Page<CheckOrder> queryCheckOrders(String warehouse, String keyword, String startTime, String endTime, int pageIndex, int pageSize) {
        Page<CheckOrder> page = new Page<>(pageIndex + 1, pageSize);
        QueryWrapper<CheckOrder> wrapper = new QueryWrapper<>();

        if (StringUtils.hasText(warehouse)) {
            wrapper.eq("warehouse", warehouse);
        }
        // 关键词：盘点单号模糊匹配 + 明细物料名称模糊匹配
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like("check_no", keyword)
                    .or().exists("SELECT 1 FROM check_order_detail d WHERE d.check_order_id = check_order.id "
                            + "AND d.material_name LIKE CONCAT('%', {0}, '%')", keyword));
        }
        // 创建时间范围查询
        if (StringUtils.hasText(startTime)) {
            wrapper.ge("created_time", startTime);
        }
        if (StringUtils.hasText(endTime)) {
            wrapper.le("created_time", endTime);
        }
        wrapper.orderByDesc("created_time");
        return this.page(page, wrapper);
    }

    /**
     * 获取所有仓库名称列表（生产单位名称）
     *
     * @return 仓库名称列表
     */
    @Override
    public List<String> getWarehouseList() {
        List<ProductionUnit> units = productionUnitMapper.selectList(
                new QueryWrapper<ProductionUnit>().orderByAsc("prod_unit_id"));
        return units.stream().map(ProductionUnit::getProdUnitName).toList();
    }

    /**
     * 获取仓库库存明细（盘点用）
     *
     * @param warehouse 仓库名称（必填）
     * @param showZero  是否显示零库存（默认false）
     * @param keyword   搜索关键词（物料名称/批号/编码，可选）
     * @return 库存明细列表
     */
    @Override
    public List<StockDetailItemDto> getStockDetails(String warehouse, Boolean showZero, String keyword) {
        if (!StringUtils.hasText(warehouse)) {
            throw new RuntimeException("仓库不能为空");
        }
        Long prodUnitId = resolveProdUnitId(warehouse);

        QueryWrapper<Stock> wrapper = new QueryWrapper<>();
        wrapper.eq("prod_unit_id", prodUnitId);
        if (!Boolean.TRUE.equals(showZero)) {
            wrapper.gt("quantity", BigDecimal.ZERO);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like("item_name", keyword)
                    .or().like("batch_number", keyword)
                    .or().like("item_code", keyword));
        }
        wrapper.orderByAsc("item_code", "batch_number");
        List<Stock> stocks = stockMapper.selectList(wrapper);

        return stocks.stream().map(stock -> {
            StockDetailItemDto dto = new StockDetailItemDto();
            dto.setInventoryKey(stock.getItemCode() + "_" + warehouse + "_" + stock.getBatchNumber());
            dto.setMaterialCode(stock.getItemCode());
            dto.setMaterialName(stock.getItemName());
            dto.setCategory(stock.getCategoryName());
            dto.setBatchNo(stock.getBatchNumber());
            dto.setWarehouse(warehouse);
            dto.setStatus(stock.getStockStatus() != null ? String.valueOf(stock.getStockStatus()) : null);
            dto.setSystemStock(stock.getQuantity());
            dto.setUnit(stock.getUnitName());
            return dto;
        }).toList();
    }

    // endregion

    // region 提交盘点
    // ===================================
    // 提交盘点
    // ===================================

    /**
     * 提交盘点
     * <p>
     * 自动计算差异（实盘-系统）并生成盘点结果（盘盈/盘亏/盘平），
     * 对有差异的物料调整库存并生成盘点调整流水，保存盘点单主表与明细
     * </p>
     *
     * @param dto 盘点请求（仓库+明细）
     * @return 创建后的盘点单（含单号与统计结果）
     */
    @Override
    @Transactional
    public CheckOrder createCheckOrder(CheckOrderCreateDto dto) {
        // 参数校验
        if (!StringUtils.hasText(dto.getWarehouse())) {
            throw new RuntimeException("盘点仓库不能为空");
        }
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new RuntimeException("盘点数据不能为空");
        }
        Long prodUnitId = resolveProdUnitId(dto.getWarehouse());

        // 逐项校验并计算差异与统计
        List<CheckOrderDetail> details = new ArrayList<>();
        int profitCount = 0;
        int lossCount = 0;
        int matchCount = 0;
        for (CheckItemRequest item : dto.getItems()) {
            if (item.getActualStock() == null || item.getActualStock().compareTo(BigDecimal.ZERO) < 0) {
                throw new RuntimeException("实盘数量不能为负数: " + item.getInventoryKey());
            }
            Stock stock = resolveStock(item.getInventoryKey(), dto.getWarehouse(), prodUnitId);
            BigDecimal systemStock = stock.getQuantity();
            BigDecimal difference = item.getActualStock().subtract(systemStock);
            int cmp = difference.compareTo(BigDecimal.ZERO);
            String result = cmp > 0 ? "盘盈" : (cmp < 0 ? "盘亏" : "盘平");
            if (cmp > 0) {
                profitCount++;
            } else if (cmp < 0) {
                lossCount++;
            } else {
                matchCount++;
            }

            CheckOrderDetail detail = new CheckOrderDetail();
            detail.setStockId(stock.getStockId());
            detail.setInventoryKey(item.getInventoryKey());
            detail.setMaterialCode(stock.getItemCode());
            detail.setMaterialName(stock.getItemName());
            detail.setCategory(stock.getCategoryName());
            detail.setBatchNo(stock.getBatchNumber());
            detail.setWarehouse(dto.getWarehouse());
            detail.setStatus(stock.getStockStatus() != null ? String.valueOf(stock.getStockStatus()) : "合格");
            detail.setSystemStock(systemStock);
            detail.setActualStock(item.getActualStock());
            detail.setDifference(difference);
            detail.setResult(result);
            detail.setUnit(stock.getUnitName());
            details.add(detail);
        }

        // 保存主表（带编号冲突重试）
        CheckOrder order = new CheckOrder();
        order.setWarehouse(dto.getWarehouse());
        order.setMaterialCount(details.size());
        order.setProfitCount(profitCount);
        order.setLossCount(lossCount);
        order.setMatchCount(matchCount);
        Long currentUserId = EntityUtils.getCurrentUserId();
        order.setOperatorId(currentUserId);
        order.setOperatorName(resolveUserName(currentUserId));

        for (int i = 0; i < MAX_RETRY; i++) {
            order.setCheckNo(generateCheckNo());
            order.setId(null);
            try {
                this.save(order);
                break;
            } catch (DuplicateKeyException e) {
                if (i == MAX_RETRY - 1) {
                    throw new RuntimeException("创建失败: 盘点单号生成冲突，请稍后重试", e);
                }
            }
        }

        // 逐项调整库存并写流水
        for (CheckOrderDetail detail : details) {
            applyCheckAdjustment(order.getId(), detail);
        }

        return order;
    }

    /**
     * 执行单条盘点库存调整并写流水
     * <p>
     * 差异不为0时调整库存数量（盘盈加正差异、盘亏加负差异），
     * 生成盘盈入库（正变动）或盘亏出库（负变动）流水
     * </p>
     *
     * @param orderId 盘点单主表ID
     * @param detail  盘点明细（含差异与结果）
     */
    private void applyCheckAdjustment(Long orderId, CheckOrderDetail detail) {
        int cmp = detail.getDifference().compareTo(BigDecimal.ZERO);
        if (cmp != 0) {
            Stock stock = stockMapper.selectById(detail.getStockId());
            if (stock == null) {
                throw new RuntimeException("库存记录不存在: " + detail.getMaterialName());
            }
            BigDecimal before = stock.getQuantity();
            BigDecimal after = before.add(detail.getDifference());
            // 盘亏不会导致负库存（差异 >= -系统库存），兜底钳制到0
            if (after.compareTo(BigDecimal.ZERO) < 0) {
                after = BigDecimal.ZERO;
            }
            stock.setQuantity(after);
            stockMapper.updateById(stock);

            // 写入盘点调整流水（盘盈为正变动、盘亏为负变动）
            String transactionType = cmp > 0 ? "盘盈入库" : "盘亏出库";
            stockService.insertTransaction(stock, transactionType, "check", orderId,
                    "盘点单: " + orderId, before, detail.getDifference());
        }

        // 落库明细
        detail.setCheckOrderId(orderId);
        checkOrderDetailMapper.insert(detail);
    }

    /**
     * 查询盘点单详情（含明细）
     *
     * @param id 盘点单ID
     * @return 盘点单详情（主表+明细）
     */
    @Override
    public CheckOrderDetailDto getCheckOrderDetail(Long id) {
        CheckOrder order = this.getById(id);
        if (order == null) {
            throw new RuntimeException("盘点单不存在");
        }
        List<CheckOrderDetail> details = checkOrderDetailMapper.selectList(
                new QueryWrapper<CheckOrderDetail>().eq("check_order_id", id));

        CheckOrderDetailDto dto = new CheckOrderDetailDto();
        dto.setId(order.getId());
        dto.setCheckNo(order.getCheckNo());
        dto.setWarehouse(order.getWarehouse());
        dto.setMaterialCount(order.getMaterialCount());
        dto.setProfitCount(order.getProfitCount());
        dto.setLossCount(order.getLossCount());
        dto.setMatchCount(order.getMatchCount());
        dto.setOperatorName(order.getOperatorName());
        dto.setCreatedAt(order.getCreatedTime());

        dto.setItems(details.stream().map(d -> {
            CheckOrderDetailItemDto item = new CheckOrderDetailItemDto();
            item.setMaterialCode(d.getMaterialCode());
            item.setMaterialName(d.getMaterialName());
            item.setCategory(d.getCategory());
            item.setBatchNo(d.getBatchNo());
            item.setWarehouse(d.getWarehouse());
            item.setStatus(d.getStatus());
            item.setSystemStock(d.getSystemStock());
            item.setActualStock(d.getActualStock());
            item.setDifference(d.getDifference());
            item.setResult(d.getResult());
            item.setUnit(d.getUnit());
            return item;
        }).toList());
        return dto;
    }

    // endregion

    // region 私有工具方法
    // ===================================
    // 私有工具方法
    // ===================================

    /**
     * 生成盘点单号（PD-YYYYMMDD-NNN）
     * <p>查询最大单号时绕过软删除过滤，避免与已软删除单号冲突</p>
     *
     * @return 盘点单号
     */
    private String generateCheckNo() {
        String prefix = "PD-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String lastNo = baseMapper.selectMaxCheckNoByPrefix(prefix);
        int sequence = 1;
        if (StringUtils.hasText(lastNo) && lastNo.length() > prefix.length()) {
            try {
                sequence = Integer.parseInt(lastNo.substring(prefix.length())) + 1;
            } catch (Exception e) {
                sequence = 1;
            }
        }
        return prefix + String.format("%03d", sequence);
    }

    /**
     * 根据仓库名称解析生产单位ID
     *
     * @param warehouse 仓库名称
     * @return 生产单位ID
     */
    private Long resolveProdUnitId(String warehouse) {
        List<ProductionUnit> units = productionUnitMapper.selectList(
                new QueryWrapper<ProductionUnit>().eq("prod_unit_name", warehouse));
        if (units.isEmpty()) {
            throw new RuntimeException("仓库不存在: " + warehouse);
        }
        return units.get(0).getProdUnitId();
    }

    /**
     * 根据库存标识反查库存记录
     * <p>
     * 库存标识格式：物料编码_仓库名_批号，按 编码+仓库+批号 三要素定位库存行
     * </p>
     *
     * @param inventoryKey 库存标识
     * @param warehouse    仓库名称
     * @param prodUnitId   生产单位ID
     * @return 库存记录
     */
    private Stock resolveStock(String inventoryKey, String warehouse, Long prodUnitId) {
        String[] parts = parseInventoryKey(inventoryKey);
        List<Stock> stocks = stockMapper.selectList(new QueryWrapper<Stock>()
                .eq("item_code", parts[0])
                .eq("prod_unit_id", prodUnitId)
                .eq("batch_number", parts[2]));
        if (stocks.isEmpty()) {
            throw new RuntimeException("库存记录不存在: " + inventoryKey);
        }
        return stocks.get(0);
    }

    /**
     * 解析库存标识（物料编码_仓库名_批号）
     *
     * @param inventoryKey 库存标识
     * @return [物料编码, 仓库名, 批号]
     */
    private String[] parseInventoryKey(String inventoryKey) {
        if (!StringUtils.hasText(inventoryKey)) {
            throw new RuntimeException("库存标识不能为空");
        }
        int lastSep = inventoryKey.lastIndexOf('_');
        if (lastSep <= 0) {
            throw new RuntimeException("库存标识格式错误: " + inventoryKey);
        }
        String batch = inventoryKey.substring(lastSep + 1);
        String rest = inventoryKey.substring(0, lastSep);
        int firstSep = rest.indexOf('_');
        if (firstSep <= 0) {
            throw new RuntimeException("库存标识格式错误: " + inventoryKey);
        }
        return new String[]{rest.substring(0, firstSep), rest.substring(firstSep + 1), batch};
    }

    /**
     * 根据用户ID解析用户显示名称（真实姓名，无则回退登录账号）
     *
     * @param userId 用户ID
     * @return 用户显示名称
     */
    private String resolveUserName(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        return StringUtils.hasText(user.getUserName()) ? user.getUserName() : user.getUserAccount();
    }

    // endregion
}