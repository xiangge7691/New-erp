package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.Dto.Warehouse.AvailableOutOrderDto;
import com.tonghui.erp.Common.Dto.Warehouse.OutOrderMaterialDto;
import com.tonghui.erp.Common.Dto.Warehouse.ReturnItemRequest;
import com.tonghui.erp.Common.Dto.Warehouse.ReturnOrderCreateDto;
import com.tonghui.erp.Common.Dto.Warehouse.ReturnOrderDetailDto;
import com.tonghui.erp.Common.Dto.Warehouse.ReturnOrderDetailItemDto;
import com.tonghui.erp.Common.utils.EntityUtils;
import com.tonghui.erp.Data.Entity.ProductionPlan;
import com.tonghui.erp.Data.Entity.ProductionUnit;
import com.tonghui.erp.Data.Entity.ReturnOrder;
import com.tonghui.erp.Data.Entity.ReturnOrderDetail;
import com.tonghui.erp.Data.Entity.Stock;
import com.tonghui.erp.Data.Entity.StockOut;
import com.tonghui.erp.Data.Entity.StockOutDetail;
import com.tonghui.erp.Data.Entity.User;
import com.tonghui.erp.Data.mapper.ProductionPlanMapper;
import com.tonghui.erp.Data.mapper.ProductionUnitMapper;
import com.tonghui.erp.Data.mapper.ReturnOrderDetailMapper;
import com.tonghui.erp.Data.mapper.ReturnOrderMapper;
import com.tonghui.erp.Data.mapper.StockMapper;
import com.tonghui.erp.Data.mapper.StockOutDetailMapper;
import com.tonghui.erp.Data.mapper.StockOutMapper;
import com.tonghui.erp.Data.mapper.UserMapper;
import com.tonghui.erp.Service.ReturnOrderService;
import com.tonghui.erp.Service.StockService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 退库单服务实现类
 * <p>
 * 实现退库单查询、可退库出库单/出库明细查询及新增退库业务：
 * 新增退库时在同事务内校验退库数量不超过可退数量（出库数量-已退数量），
 * 回增库存（原库存行不存在时按出库明细重建），生成退库流水并保存主表与明细
 * </p>
 */
@Service
public class ReturnOrderServiceImpl extends ServiceImpl<ReturnOrderMapper, ReturnOrder>
        implements ReturnOrderService {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    @Autowired
    private ReturnOrderDetailMapper returnOrderDetailMapper;

    @Autowired
    private StockOutMapper stockOutMapper;

    @Autowired
    private StockOutDetailMapper stockOutDetailMapper;

    @Autowired
    private ProductionUnitMapper productionUnitMapper;

    @Autowired
    private ProductionPlanMapper productionPlanMapper;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private StockService stockService;

    @Autowired
    private UserMapper userMapper;

    /** 退库单号生成最大重试次数（处理并发编号冲突） */
    private static final int MAX_RETRY = 3;

    /** 可退库出库类型（兼容新旧枚举值） */
    private static final List<String> RETURNABLE_OUT_TYPES = List.of("生产领料出库", "production");

    // endregion

    // region 查询操作
    // ===================================
    // 查询操作
    // ===================================

    /**
     * 分页查询退库单列表
     *
     * @param keyword   搜索关键词（退库单号/出库单号/物料名称，可选）
     * @param startTime 创建时间起始（可选）
     * @param endTime   创建时间结束（可选）
     * @param pageIndex 页码（从0开始）
     * @param pageSize  每页数量
     * @return 分页结果（主表信息列表）
     */
    @Override
    public Page<ReturnOrder> queryReturnOrders(String keyword, String startTime, String endTime, int pageIndex, int pageSize) {
        Page<ReturnOrder> page = new Page<>(pageIndex + 1, pageSize);
        QueryWrapper<ReturnOrder> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like("return_no", keyword)
                    .or().like("out_order_no", keyword)
                    .or().exists("SELECT 1 FROM return_order_detail d WHERE d.return_order_id = return_order.id "
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
        Page<ReturnOrder> result = this.page(page, wrapper);

        // 批量解析生产计划名称（按生产计划编号关联 production_plan 表）
        List<ReturnOrder> records = result.getRecords();
        if (!records.isEmpty()) {
            List<String> planNos = records.stream()
                    .map(ReturnOrder::getProductionPlanNo)
                    .filter(StringUtils::hasText)
                    .distinct()
                    .collect(Collectors.toList());
            Map<String, String> planNameMap = resolvePlanNames(planNos);
            records.forEach(r -> r.setProductionPlanName(planNameMap.get(r.getProductionPlanNo())));
        }
        return result;
    }

    /**
     * 获取可退库的出库单列表
     * <p>
     * 仅展示"生产领料出库"且仍有可退额度的出库单
     * </p>
     *
     * @return 可退库出库单列表（含可退总量）
     */
    @Override
    public List<AvailableOutOrderDto> getAvailableOutOrders() {
        // 查询生产领料出库单
        QueryWrapper<StockOut> outWrapper = new QueryWrapper<>();
        outWrapper.in("out_type", RETURNABLE_OUT_TYPES).orderByDesc("out_date");
        List<StockOut> outOrders = stockOutMapper.selectList(outWrapper);
        if (outOrders.isEmpty()) {
            return List.of();
        }

        // 批量查询出库明细与已退数量
        List<Long> outIds = outOrders.stream().map(StockOut::getOutId).toList();
        List<StockOutDetail> allDetails = stockOutDetailMapper.selectList(
                new QueryWrapper<StockOutDetail>().in("out_id", outIds));
        Map<Long, BigDecimal> returnedMap = sumReturnedByOutDetailId();
        Map<String, String> planNameMap = resolvePlanNames(
                outOrders.stream().map(StockOut::getPlanNumber).filter(StringUtils::hasText).distinct().toList());

        // 按出库单聚合可退额度
        List<AvailableOutOrderDto> result = new ArrayList<>();
        for (StockOut out : outOrders) {
            BigDecimal totalAvailable = BigDecimal.ZERO;
            int materialCount = 0;
            for (StockOutDetail detail : allDetails) {
                if (!detail.getOutId().equals(out.getOutId())) {
                    continue;
                }
                BigDecimal available = calcAvailable(detail.getQuantity(), returnedMap.get(detail.getOutDetailId()));
                if (available.compareTo(BigDecimal.ZERO) > 0) {
                    totalAvailable = totalAvailable.add(available);
                    materialCount++;
                }
            }
            if (materialCount == 0) {
                continue;
            }
            AvailableOutOrderDto dto = new AvailableOutOrderDto();
            dto.setOutOrderNo(out.getOutCode());
            dto.setProductionPlanNo(out.getPlanNumber());
            dto.setProductionPlanName(planNameMap.get(out.getPlanNumber()));
            dto.setMaterialCount(materialCount);
            dto.setTotalAvailableQuantity(totalAvailable);
            result.add(dto);
        }
        return result;
    }

    /**
     * 获取出库单物料明细（含可退数量）
     *
     * @param outOrderNo 出库单号
     * @return 物料明细列表（含出库数量、已退数量、可退数量）
     */
    @Override
    public List<OutOrderMaterialDto> getOutOrderMaterials(String outOrderNo) {
        if (!StringUtils.hasText(outOrderNo)) {
            throw new RuntimeException("出库单号不能为空");
        }
        StockOut out = stockOutMapper.selectOne(
                new QueryWrapper<StockOut>().eq("out_code", outOrderNo));
        if (out == null) {
            throw new RuntimeException("出库单不存在: " + outOrderNo);
        }
        List<StockOutDetail> details = stockOutDetailMapper.selectList(
                new QueryWrapper<StockOutDetail>().eq("out_id", out.getOutId()).orderByAsc("out_detail_id"));
        if (details.isEmpty()) {
            return List.of();
        }

        String warehouseName = resolveWarehouseName(out.getProdUnitId());
        Map<Long, BigDecimal> returnedMap = sumReturnedByOutDetailId();

        return details.stream().map(detail -> {
            OutOrderMaterialDto dto = new OutOrderMaterialDto();
            dto.setInventoryKey(detail.getItemCode() + "_" + warehouseName + "_" + detail.getBatchNumber());
            dto.setMaterialCode(detail.getItemCode());
            dto.setMaterialName(detail.getItemName());
            dto.setCategory(detail.getCategoryName());
            dto.setBatchNo(detail.getBatchNumber());
            dto.setWarehouse(warehouseName);
            dto.setUnit(detail.getUnitName());
            dto.setOutQuantity(detail.getQuantity());
            BigDecimal returned = returnedMap.getOrDefault(detail.getOutDetailId(), BigDecimal.ZERO);
            dto.setReturnedQuantity(returned);
            dto.setAvailableQuantity(calcAvailable(detail.getQuantity(), returned));
            dto.setUnitPrice(detail.getUnitPrice());
            return dto;
        }).toList();
    }

    // endregion

    // region 新增退库
    // ===================================
    // 新增退库
    // ===================================

    /**
     * 新增退库单
     * <p>
     * 校验退库数量不超过可退数量后回增库存（无同批次库存则新增库存记录），
     * 生成退库流水，并保存退库单主表与明细
     * </p>
     *
     * @param dto 退库请求（出库单号+明细）
     * @return 创建后的退库单（含单号与汇总数据）
     */
    @Override
    @Transactional
    public ReturnOrder createReturnOrder(ReturnOrderCreateDto dto) {
        // 参数校验
        if (!StringUtils.hasText(dto.getOutOrderNo())) {
            throw new RuntimeException("出库单号不能为空");
        }
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new RuntimeException("退库物料不能为空");
        }

        // 定位出库单与仓库
        StockOut out = stockOutMapper.selectOne(
                new QueryWrapper<StockOut>().eq("out_code", dto.getOutOrderNo()));
        if (out == null) {
            throw new RuntimeException("出库单不存在: " + dto.getOutOrderNo());
        }
        String warehouseName = resolveWarehouseName(out.getProdUnitId());

        // 已退数量汇总（按出库明细ID）
        Map<Long, BigDecimal> returnedMap = sumReturnedByOutDetailId();

        // 逐项校验并预计算汇总（校验失败整体回滚）
        List<ReturnOrderDetail> details = new ArrayList<>();
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (ReturnItemRequest item : dto.getItems()) {
            if (item.getReturnQuantity() == null || item.getReturnQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("退库数量必须大于0: " + item.getInventoryKey());
            }
            StockOutDetail outDetail = resolveOutDetail(out.getOutId(), item.getInventoryKey(), warehouseName);
            BigDecimal returned = returnedMap.getOrDefault(outDetail.getOutDetailId(), BigDecimal.ZERO);
            BigDecimal available = calcAvailable(outDetail.getQuantity(), returned);
            if (item.getReturnQuantity().compareTo(available) > 0) {
                throw new RuntimeException("退库数量不能超过可退数量: " + outDetail.getItemName()
                        + "（可退 " + available.toPlainString()
                        + "，本次退库 " + item.getReturnQuantity().toPlainString() + "）");
            }

            BigDecimal amount = item.getReturnQuantity().multiply(
                    outDetail.getUnitPrice() != null ? outDetail.getUnitPrice() : BigDecimal.ZERO)
                    .setScale(2, RoundingMode.HALF_UP);
            totalQuantity = totalQuantity.add(item.getReturnQuantity());
            totalAmount = totalAmount.add(amount);

            ReturnOrderDetail detail = new ReturnOrderDetail();
            detail.setOutDetailId(outDetail.getOutDetailId());
            detail.setInventoryKey(item.getInventoryKey());
            detail.setMaterialCode(outDetail.getItemCode());
            detail.setMaterialName(outDetail.getItemName());
            detail.setCategory(outDetail.getCategoryName());
            detail.setBatchNo(outDetail.getBatchNumber());
            detail.setWarehouse(warehouseName);
            detail.setOutQuantity(outDetail.getQuantity());
            detail.setReturnedQuantity(returned);
            detail.setReturnQuantity(item.getReturnQuantity());
            detail.setUnitPrice(outDetail.getUnitPrice());
            detail.setAmount(amount);
            detail.setUnit(outDetail.getUnitName());
            details.add(detail);
        }

        // 保存主表（带编号冲突重试）
        ReturnOrder order = new ReturnOrder();
        order.setOutOrderNo(dto.getOutOrderNo());
        order.setProductionPlanNo(out.getPlanNumber());
        order.setMaterialCount(details.size());
        order.setTotalQuantity(totalQuantity);
        order.setTotalAmount(totalAmount);
        order.setRemark(dto.getRemark());
        Long currentUserId = EntityUtils.getCurrentUserId();
        order.setOperatorId(currentUserId);
        order.setOperatorName(resolveUserName(currentUserId));

        for (int i = 0; i < MAX_RETRY; i++) {
            order.setReturnNo(generateReturnNo());
            order.setId(null);
            try {
                this.save(order);
                break;
            } catch (DuplicateKeyException e) {
                if (i == MAX_RETRY - 1) {
                    throw new RuntimeException("创建失败: 退库单号生成冲突，请稍后重试", e);
                }
            }
        }

        // 逐项回增库存并写流水
        for (ReturnOrderDetail detail : details) {
            applyReturn(order.getId(), dto.getOutOrderNo(), detail);
        }

        return order;
    }

    /**
     * 执行单条退库库存回增并写流水
     * <p>
     * 按出库明细的库存ID回增库存：原库存行存在则数量累加，
     * 已因出库清零被删除则按出库明细信息重建库存行，并生成退库流水（正变动）
     * </p>
     *
     * @param orderId    退库单主表ID
     * @param outOrderNo 出库单号（流水备注用）
     * @param detail     退库明细（含出库明细ID与回增数量）
     */
    private void applyReturn(Long orderId, String outOrderNo, ReturnOrderDetail detail) {
        StockOutDetail outDetail = stockOutDetailMapper.selectById(detail.getOutDetailId());
        if (outDetail == null) {
            throw new RuntimeException("出库明细不存在: " + detail.getMaterialName());
        }

        BigDecimal before;
        Stock stock = stockMapper.selectById(outDetail.getStockId());
        if (stock != null) {
            // 原库存行存在：数量累加
            before = stock.getQuantity();
            stock.setQuantity(before.add(detail.getReturnQuantity()));
            stockMapper.updateById(stock);
        } else {
            // 原库存行已因出库清零被删除：按出库明细信息重建
            stock = new Stock();
            stock.setProdUnitId(outDetail.getProdUnitId());
            stock.setItemType(outDetail.getItemType() != null ? String.valueOf(outDetail.getItemType()) : "material");
            stock.setItemId(outDetail.getItemId());
            stock.setItemCode(outDetail.getItemCode());
            stock.setItemName(outDetail.getItemName());
            stock.setCategoryName(outDetail.getCategoryName());
            stock.setUnitName(outDetail.getUnitName());
            stock.setBatchNumber(outDetail.getBatchNumber());
            stock.setQuantity(detail.getReturnQuantity());
            stock.setUnitPrice(outDetail.getUnitPrice());
            stock.setRemark("退库回增: " + outOrderNo);
            stockMapper.insert(stock);
            before = BigDecimal.ZERO;
        }

        // 写入退库流水（正变动）
        stockService.insertTransaction(stock, "退库", "return", orderId,
                "退库单: " + orderId + "，出库单: " + outOrderNo, before, detail.getReturnQuantity());

        // 落库明细
        detail.setReturnOrderId(orderId);
        returnOrderDetailMapper.insert(detail);
    }

    /**
     * 查询退库单详情（含明细）
     *
     * @param id 退库单ID
     * @return 退库单详情（主表+明细）
     */
    @Override
    public ReturnOrderDetailDto getReturnOrderDetail(Long id) {
        ReturnOrder order = this.getById(id);
        if (order == null) {
            throw new RuntimeException("退库单不存在");
        }
        List<ReturnOrderDetail> details = returnOrderDetailMapper.selectList(
                new QueryWrapper<ReturnOrderDetail>().eq("return_order_id", id));

        ReturnOrderDetailDto dto = new ReturnOrderDetailDto();
        dto.setId(order.getId());
        dto.setReturnNo(order.getReturnNo());
        dto.setOutOrderNo(order.getOutOrderNo());
        dto.setProductionPlanNo(order.getProductionPlanNo());
        dto.setProductionPlanName(resolvePlanName(order.getProductionPlanNo()));
        dto.setMaterialCount(order.getMaterialCount());
        dto.setTotalQuantity(order.getTotalQuantity());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setRemark(order.getRemark());
        dto.setOperatorName(order.getOperatorName());
        dto.setCreatedAt(order.getCreatedTime());

        dto.setItems(details.stream().map(d -> {
            ReturnOrderDetailItemDto item = new ReturnOrderDetailItemDto();
            item.setMaterialCode(d.getMaterialCode());
            item.setMaterialName(d.getMaterialName());
            item.setCategory(d.getCategory());
            item.setBatchNo(d.getBatchNo());
            item.setWarehouse(d.getWarehouse());
            item.setOutQuantity(d.getOutQuantity());
            item.setReturnQuantity(d.getReturnQuantity());
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
     * 汇总各出库明细的已退数量
     * <p>
     * 按出库明细ID汇总所有退库单的退库数量（仅统计未删除的退库明细）
     * </p>
     *
     * @return 出库明细ID → 已退数量
     */
    private Map<Long, BigDecimal> sumReturnedByOutDetailId() {
        List<ReturnOrderDetail> all = returnOrderDetailMapper.selectList(null);
        Map<Long, BigDecimal> map = new HashMap<>();
        for (ReturnOrderDetail detail : all) {
            map.merge(detail.getOutDetailId(), detail.getReturnQuantity(), BigDecimal::add);
        }
        return map;
    }

    /**
     * 计算可退数量（出库数量 - 已退数量，不为负）
     *
     * @param outQuantity 出库数量
     * @param returned    已退数量（可为null）
     * @return 可退数量
     */
    private BigDecimal calcAvailable(BigDecimal outQuantity, BigDecimal returned) {
        BigDecimal available = outQuantity.subtract(returned != null ? returned : BigDecimal.ZERO);
        return available.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : available;
    }

    /**
     * 根据出库单ID与库存标识定位出库明细
     * <p>
     * 库存标识格式：物料编码_仓库名_批号，按 出库单+编码+批号 定位明细行
     * </p>
     *
     * @param outId         出库单ID
     * @param inventoryKey  库存标识
     * @param warehouseName 仓库名称
     * @return 出库明细记录
     */
    private StockOutDetail resolveOutDetail(Long outId, String inventoryKey, String warehouseName) {
        String[] parts = parseInventoryKey(inventoryKey);
        List<StockOutDetail> details = stockOutDetailMapper.selectList(new QueryWrapper<StockOutDetail>()
                .eq("out_id", outId)
                .eq("item_code", parts[0])
                .eq("batch_number", parts[2]));
        if (details.isEmpty()) {
            throw new RuntimeException("出库明细不存在: " + inventoryKey);
        }
        return details.get(0);
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
     * 根据生产单位ID解析仓库名称
     *
     * @param prodUnitId 生产单位ID
     * @return 仓库名称，无匹配返回空字符串
     */
    private String resolveWarehouseName(Long prodUnitId) {
        if (prodUnitId == null) {
            return "";
        }
        ProductionUnit unit = productionUnitMapper.selectById(prodUnitId);
        return unit != null ? unit.getProdUnitName() : "";
    }

    /**
     * 批量解析生产计划编号对应的计划名称（制剂名称）
     *
     * @param planNumbers 生产计划编号集合（可能为空）
     * @return 生产计划编号 → 计划名称
     */
    private Map<String, String> resolvePlanNames(List<String> planNumbers) {
        if (planNumbers == null || planNumbers.isEmpty()) {
            return Map.of();
        }
        List<ProductionPlan> plans = productionPlanMapper.selectList(
                new QueryWrapper<ProductionPlan>().in("plan_number", planNumbers));
        return plans.stream().collect(Collectors.toMap(
                ProductionPlan::getPlanNumber,
                p -> StringUtils.hasText(p.getPlanName()) ? p.getPlanName() : "",
                (a, b) -> a));
    }

    /**
     * 解析单个生产计划编号对应的计划名称
     *
     * @param planNumber 生产计划编号（可为空）
     * @return 计划名称，无匹配返回null
     */
    private String resolvePlanName(String planNumber) {
        if (!StringUtils.hasText(planNumber)) {
            return null;
        }
        return resolvePlanNames(List.of(planNumber)).getOrDefault(planNumber, null);
    }

    /**
     * 生成退库单号（TK-YYYYMMDD-NNN）
     * <p>查询最大单号时绕过软删除过滤，避免与已软删除单号冲突</p>
     *
     * @return 退库单号
     */
    private String generateReturnNo() {
        String prefix = "TK-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String lastNo = baseMapper.selectMaxReturnNoByPrefix(prefix);
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