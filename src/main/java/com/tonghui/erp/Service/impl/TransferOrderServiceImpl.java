package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.Dto.Warehouse.MaterialBatchDto;
import com.tonghui.erp.Common.Dto.Warehouse.TransferItemRequest;
import com.tonghui.erp.Common.Dto.Warehouse.TransferOrderCreateDto;
import com.tonghui.erp.Common.Dto.Warehouse.TransferOrderDetailDto;
import com.tonghui.erp.Common.Dto.Warehouse.TransferOrderDetailItemDto;
import com.tonghui.erp.Common.Dto.Warehouse.TransferOrderListItemDto;
import com.tonghui.erp.Common.Dto.Warehouse.WarehouseMaterialDto;
import com.tonghui.erp.Common.utils.EntityUtils;
import com.tonghui.erp.Data.Entity.ProductionUnit;
import com.tonghui.erp.Data.Entity.Stock;
import com.tonghui.erp.Data.Entity.TransferOrder;
import com.tonghui.erp.Data.Entity.TransferOrderDetail;
import com.tonghui.erp.Data.Entity.User;
import com.tonghui.erp.Data.mapper.ProductionUnitMapper;
import com.tonghui.erp.Data.mapper.StockMapper;
import com.tonghui.erp.Data.mapper.TransferOrderDetailMapper;
import com.tonghui.erp.Data.mapper.TransferOrderMapper;
import com.tonghui.erp.Data.mapper.UserMapper;
import com.tonghui.erp.Service.StockService;
import com.tonghui.erp.Service.TransferOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 调拨单服务实现类
 * <p>
 * 实现调拨单查询、仓库/物料/批次信息查询及新增调拨业务：
 * 新增调拨时在同事务内扣减调出库存、增加调入库存（不存在同批次则新增库存行），
 * 生成调拨出库/调拨入库两条库存流水，并保存主表与明细
 * </p>
 */
@Service
public class TransferOrderServiceImpl extends ServiceImpl<TransferOrderMapper, TransferOrder>
        implements TransferOrderService {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    @Autowired
    private TransferOrderDetailMapper transferOrderDetailMapper;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private ProductionUnitMapper productionUnitMapper;

    @Autowired
    private StockService stockService;

    @Autowired
    private UserMapper userMapper;

    /** 调拨单号生成最大重试次数（处理并发编号冲突） */
    private static final int MAX_RETRY = 3;

    // endregion

    // region 查询操作
    // ===================================
    // 查询操作
    // ===================================

    /**
     * 分页查询调拨单列表
     *
     * @param type      类型筛选：调拨出库（按调出仓库模糊匹配keyword）/调拨入库（按调入仓库模糊匹配keyword），可选
     * @param keyword   搜索关键词（调拨单号/物料名称），可选
     * @param startTime 创建时间起始（可选）
     * @param endTime   创建时间结束（可选）
     * @param pageIndex 页码（从0开始）
     * @param pageSize  每页数量
     * @return 分页结果（主表信息列表）
     */
    @Override
    public Page<TransferOrder> queryTransferOrders(String type, String keyword, String startTime, String endTime, int pageIndex, int pageSize) {
        Page<TransferOrder> page = new Page<>(pageIndex + 1, pageSize);
        QueryWrapper<TransferOrder> wrapper = new QueryWrapper<>();

        // 类型筛选：调拨出库按调出仓库、调拨入库按调入仓库模糊匹配（keyword 为空时传仓库名筛选）
        if (StringUtils.hasText(type)) {
            if ("调拨出库".equals(type)) {
                if (StringUtils.hasText(keyword)) {
                    wrapper.like("from_warehouse", keyword);
                }
            } else if ("调拨入库".equals(type)) {
                if (StringUtils.hasText(keyword)) {
                    wrapper.like("to_warehouse", keyword);
                }
            }
        }

        // 关键词：调拨单号模糊匹配 + 明细物料名称模糊匹配
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like("transfer_no", keyword)
                    .or().exists("SELECT 1 FROM transfer_order_detail d WHERE d.transfer_order_id = transfer_order.id "
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
     * 获取指定仓库的可用物料列表（含批次数量）
     *
     * @param warehouse 仓库名称
     * @param keyword   搜索关键词（按物料编码/物料名称模糊匹配，可选）
     * @return 物料列表（按物料编码分组）
     */
    @Override
    public List<WarehouseMaterialDto> getWarehouseMaterials(String warehouse, String keyword) {
        Long prodUnitId = resolveProdUnitId(warehouse);
        QueryWrapper<Stock> wrapper = new QueryWrapper<Stock>()
                .eq("prod_unit_id", prodUnitId);
        // 关键词对物料编码、物料名称进行模糊匹配
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like("item_code", keyword).or().like("item_name", keyword));
        }
        wrapper.orderByAsc("item_code");
        List<Stock> stocks = stockMapper.selectList(wrapper);

        // 按物料编码分组统计批次数量
        Map<String, WarehouseMaterialDto> grouped = new LinkedHashMap<>();
        for (Stock stock : stocks) {
            WarehouseMaterialDto dto = grouped.computeIfAbsent(stock.getItemCode(), code -> {
                WarehouseMaterialDto d = new WarehouseMaterialDto();
                d.setMaterialCode(stock.getItemCode());
                d.setMaterialName(stock.getItemName());
                d.setCategory(stock.getCategoryName());
                d.setBatchCount(0);
                return d;
            });
            dto.setBatchCount(dto.getBatchCount() + 1);
        }
        return new ArrayList<>(grouped.values());
    }

    /**
     * 获取指定仓库某物料的批次库存详情
     *
     * @param warehouse    仓库名称
     * @param materialCode 物料编码
     * @return 批次详情列表
     */
    @Override
    public List<MaterialBatchDto> getMaterialBatches(String warehouse, String materialCode) {
        if (!StringUtils.hasText(warehouse)) {
            throw new RuntimeException("仓库不能为空");
        }
        if (!StringUtils.hasText(materialCode)) {
            throw new RuntimeException("物料编码不能为空");
        }
        Long prodUnitId = resolveProdUnitId(warehouse);
        List<Stock> stocks = stockMapper.selectList(new QueryWrapper<Stock>()
                .eq("prod_unit_id", prodUnitId)
                .eq("item_code", materialCode)
                .orderByAsc("batch_number"));

        return stocks.stream().map(stock -> {
            MaterialBatchDto dto = new MaterialBatchDto();
            dto.setInventoryKey(buildInventoryKey(stock.getItemCode(), warehouse, stock.getBatchNumber()));
            dto.setMaterialCode(stock.getItemCode());
            dto.setMaterialName(stock.getItemName());
            dto.setCategory(stock.getCategoryName());
            dto.setBatchNo(stock.getBatchNumber());
            dto.setWarehouse(warehouse);
            dto.setStatus(stock.getStockStatus() != null ? String.valueOf(stock.getStockStatus()) : null);
            dto.setStock(stock.getQuantity());
            dto.setUnit(stock.getUnitName());
            dto.setUnitPrice(stock.getUnitPrice());
            return dto;
        }).toList();
    }

    // endregion

    // region 新增调拨
    // ===================================
    // 新增调拨
    // ===================================

    /**
     * 新增调拨单
     * <p>
     * 校验仓库与库存后，扣减调出库存、增加调入库存（无同批次则新增库存记录），
     * 生成调拨出库与调拨入库两条流水，并保存调拨单主表与明细
     * </p>
     *
     * @param dto 调拨单请求（仓库+明细）
     * @return 创建后的调拨单（含单号与汇总数据）
     */
    @Override
    @Transactional
    public TransferOrder createTransferOrder(TransferOrderCreateDto dto) {
        // 参数校验
        if (!StringUtils.hasText(dto.getFromWarehouse())) {
            throw new RuntimeException("调出仓库不能为空");
        }
        if (!StringUtils.hasText(dto.getToWarehouse())) {
            throw new RuntimeException("调入仓库不能为空");
        }
        if (dto.getFromWarehouse().equals(dto.getToWarehouse())) {
            throw new RuntimeException("调入仓库不能与调出仓库相同");
        }
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new RuntimeException("调拨物料不能为空");
        }

        Long fromUnitId = resolveProdUnitId(dto.getFromWarehouse());
        Long toUnitId = resolveProdUnitId(dto.getToWarehouse());

        // 逐项校验并预计算汇总（校验失败整体回滚）
        List<TransferOrderDetail> details = new ArrayList<>();
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (TransferItemRequest item : dto.getItems()) {
            if (item.getTransferQuantity() == null || item.getTransferQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("调拨数量必须大于0: " + item.getInventoryKey());
            }
            Stock src = resolveSrcStock(item.getInventoryKey(), dto.getFromWarehouse(), fromUnitId);
            if (item.getTransferQuantity().compareTo(src.getQuantity()) > 0) {
                throw new RuntimeException("调拨数量不能超过调出仓库库存: " + src.getItemName()
                        + "（当前 " + src.getQuantity().toPlainString()
                        + "，需调拨 " + item.getTransferQuantity().toPlainString() + "）");
            }

            BigDecimal amount = item.getTransferQuantity().multiply(
                    src.getUnitPrice() != null ? src.getUnitPrice() : BigDecimal.ZERO)
                    .setScale(2, RoundingMode.HALF_UP);
            totalQuantity = totalQuantity.add(item.getTransferQuantity());
            totalAmount = totalAmount.add(amount);

            TransferOrderDetail detail = new TransferOrderDetail();
            detail.setSrcStockId(src.getStockId());
            detail.setSrcInventoryKey(item.getInventoryKey());
            detail.setDstInventoryKey(buildInventoryKey(src.getItemCode(), dto.getToWarehouse(), src.getBatchNumber()));
            detail.setMaterialCode(src.getItemCode());
            detail.setMaterialName(src.getItemName());
            detail.setCategory(src.getCategoryName());
            detail.setBatchNo(src.getBatchNumber());
            detail.setSrcStock(src.getQuantity());
            detail.setTransferQuantity(item.getTransferQuantity());
            detail.setUnitPrice(src.getUnitPrice());
            detail.setAmount(amount);
            detail.setUnit(src.getUnitName());
            details.add(detail);
        }

        // 保存主表（带编号冲突重试）
        TransferOrder order = new TransferOrder();
        order.setFromWarehouse(dto.getFromWarehouse());
        order.setToWarehouse(dto.getToWarehouse());
        order.setMaterialCount(details.size());
        order.setTotalQuantity(totalQuantity);
        order.setTotalAmount(totalAmount);
        order.setRemark(dto.getRemark());
        Long currentUserId = EntityUtils.getCurrentUserId();
        order.setOperatorId(currentUserId);
        order.setOperatorName(resolveUserName(currentUserId));

        for (int i = 0; i < MAX_RETRY; i++) {
            order.setTransferNo(generateTransferNo());
            order.setId(null);
            try {
                this.save(order);
                break;
            } catch (DuplicateKeyException e) {
                if (i == MAX_RETRY - 1) {
                    throw new RuntimeException("创建失败: 调拨单号生成冲突，请稍后重试", e);
                }
            }
        }

        // 逐项执行库存变更 + 流水 + 明细
        for (TransferOrderDetail detail : details) {
            applyTransfer(dto, fromUnitId, toUnitId, order.getId(), detail);
        }

        return order;
    }

    /**
     * 执行单条物料批次的调拨库存变更并写流水
     * <p>
     * 扣减调出库存（调空则软删除库存行），增加调入库存（无同批次则新增库存行），
     * 生成调拨出库（负变动）与调拨入库（正变动）两条流水
     * </p>
     *
     * @param dto        调拨请求（取仓库名称）
     * @param fromUnitId 调出生产单位ID
     * @param toUnitId   调入生产单位ID
     * @param orderId    调拨单主表ID
     * @param detail     调拨明细（含调出库存信息）
     */
    private void applyTransfer(TransferOrderCreateDto dto, Long fromUnitId, Long toUnitId,
                               Long orderId, TransferOrderDetail detail) {
        // 1. 扣减调出库存
        Stock src = stockMapper.selectById(detail.getSrcStockId());
        if (src == null) {
            throw new RuntimeException("调出库存记录不存在: " + detail.getMaterialName());
        }
        BigDecimal srcAfter = src.getQuantity().subtract(detail.getTransferQuantity());
        if (srcAfter.compareTo(BigDecimal.ZERO) == 0) {
            // 调空后软删除该库存行
            // 注意：全局软删除配置下 updateById 不会将 is_deleted 放入 SET 子句，
            // 必须使用 deleteById 才会生成 UPDATE ... SET is_deleted=1
            stockMapper.deleteById(src.getStockId());
        } else {
            src.setQuantity(srcAfter);
            stockMapper.updateById(src);
        }

        // 2. 增加调入库存（同批次累加，无则新增）
        QueryWrapper<Stock> dstWrapper = new QueryWrapper<>();
        dstWrapper.eq("item_code", detail.getMaterialCode());
        dstWrapper.eq("prod_unit_id", toUnitId);
        dstWrapper.eq("batch_number", detail.getBatchNo());
        Stock dst = stockMapper.selectOne(dstWrapper);
        BigDecimal dstBefore;
        if (dst != null) {
            dstBefore = dst.getQuantity();
            dst.setQuantity(dstBefore.add(detail.getTransferQuantity()));
            stockMapper.updateById(dst);
        } else {
            dst = new Stock();
            dst.setProdUnitId(toUnitId);
            dst.setItemType(src.getItemType());
            dst.setItemId(src.getItemId());
            dst.setItemCode(src.getItemCode());
            dst.setItemName(src.getItemName());
            dst.setCategoryName(src.getCategoryName());
            dst.setUnitName(src.getUnitName());
            dst.setBatchNumber(src.getBatchNumber());
            dst.setQuantity(detail.getTransferQuantity());
            dst.setUnitPrice(src.getUnitPrice());
            dst.setProductionDate(src.getProductionDate());
            dst.setExpiryDate(src.getExpiryDate());
            dst.setStorageLocation(src.getStorageLocation());
            dst.setStockStatus(src.getStockStatus());
            dst.setRemark("调拨入库: " + dto.getFromWarehouse() + "→" + dto.getToWarehouse());
            stockMapper.insert(dst);
            dstBefore = BigDecimal.ZERO;
        }

        // 3. 写入两条库存流水（调拨出库为负变动、调拨入库为正变动）
        String remark = "调拨单: " + orderId;
        stockService.insertTransaction(src, "调拨出库", "transfer", orderId, remark,
                detail.getSrcStock(), detail.getTransferQuantity().negate());
        stockService.insertTransaction(dst, "调拨入库", "transfer", orderId, remark,
                dstBefore, detail.getTransferQuantity());

        // 4. 落库明细（补充调入前库存与调入库存ID）
        detail.setTransferOrderId(orderId);
        detail.setDstStockId(dst.getStockId());
        detail.setDstStock(dstBefore);
        transferOrderDetailMapper.insert(detail);
    }

    /**
     * 查询调拨单详情（含明细）
     *
     * @param id 调拨单ID
     * @return 调拨单详情（主表+明细）
     */
    @Override
    public TransferOrderDetailDto getTransferOrderDetail(Long id) {
        TransferOrder order = this.getById(id);
        if (order == null) {
            throw new RuntimeException("调拨单不存在");
        }
        List<TransferOrderDetail> details = transferOrderDetailMapper.selectList(
                new QueryWrapper<TransferOrderDetail>().eq("transfer_order_id", id));

        TransferOrderDetailDto dto = new TransferOrderDetailDto();
        dto.setId(order.getId());
        dto.setTransferNo(order.getTransferNo());
        dto.setFromWarehouse(order.getFromWarehouse());
        dto.setToWarehouse(order.getToWarehouse());
        dto.setMaterialCount(order.getMaterialCount());
        dto.setTotalQuantity(order.getTotalQuantity());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setRemark(order.getRemark());
        dto.setOperatorName(order.getOperatorName());
        dto.setCreatedAt(order.getCreatedTime());

        dto.setItems(details.stream().map(d -> {
            TransferOrderDetailItemDto item = new TransferOrderDetailItemDto();
            item.setMaterialCode(d.getMaterialCode());
            item.setMaterialName(d.getMaterialName());
            item.setCategory(d.getCategory());
            item.setBatchNo(d.getBatchNo());
            item.setSrcWarehouse(order.getFromWarehouse());
            item.setDstWarehouse(order.getToWarehouse());
            item.setSrcStock(d.getSrcStock());
            item.setDstStock(d.getDstStock());
            item.setTransferQuantity(d.getTransferQuantity());
            item.setUnitPrice(d.getUnitPrice());
            item.setAmount(d.getAmount());
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
     * 生成调拨单号（DB-YYYYMMDD-NNN）
     * <p>查询最大单号时绕过软删除过滤，避免与已软删除单号冲突</p>
     *
     * @return 调拨单号
     */
    private String generateTransferNo() {
        String prefix = "DB-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String lastNo = baseMapper.selectMaxTransferNoByPrefix(prefix);
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
     * 根据库存标识反查调出库存记录
     * <p>
     * 库存标识格式：物料编码_仓库名_批号，按 编码+仓库+批号 三要素定位库存行
     * </p>
     *
     * @param inventoryKey 库存标识
     * @param warehouse    调出仓库名称
     * @param prodUnitId   调出生产单位ID
     * @return 调出库存记录
     */
    private Stock resolveSrcStock(String inventoryKey, String warehouse, Long prodUnitId) {
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
     * 构建库存标识（物料编码_仓库名_批号）
     *
     * @param materialCode 物料编码
     * @param warehouse    仓库名称
     * @param batchNo      批号
     * @return 库存标识
     */
    private String buildInventoryKey(String materialCode, String warehouse, String batchNo) {
        return materialCode + "_" + warehouse + "_" + batchNo;
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