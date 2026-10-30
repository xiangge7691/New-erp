package com.tonghui.erp.Common.utils;

import com.tonghui.erp.Data.Entity.StockInDetail;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 入库单明细合并工具类
 * <p>
 * 入库单明细在以下场景容易出现重复行：同一次验收的多条明细指向同一物料同一批号、
 * 手工录入重复行、后续单独追加明细等。重复行会向下游扩散——库存流水按明细逐条写入、
 * 近效期预警按入库明细出行、取消入库按明细逐条回滚。
 * </p>
 * <p>
 * 合并规则（同一入库单内生效，入库单ID由调用方保证一致）：
 * <ul>
 *   <li>合并键：物品编码（为空回退物品ID） + 批号 + 明细仓库 + 库存状态 + 生产日期 + 有效期 + 存放位置</li>
 *   <li>仓库、库存状态、日期、存放位置参与合并键，避免合并后丢失库存语义（如不同仓库/待检与合格）</li>
 *   <li>数量累加；金额累加（各行金额之和，单价不同时保留首行单价）；单价、物品信息、备注仅在首行为空时用后续行补齐</li>
 *   <li>物品编码与物品ID均为空时不做合并，避免把无关行错误合并</li>
 * </ul>
 * </p>
 */
public final class StockInDetailMergeUtils {

    // region 常量
    // ===================================
    // 常量
    // ===================================

    /** 库存状态缺省值（与库存联动默认值保持一致） */
    private static final String DEFAULT_STOCK_STATUS = "合格";

    // endregion

    // region 构造方法
    // ===================================
    // 构造方法
    // ===================================

    /**
     * 工具类禁止实例化
     */
    private StockInDetailMergeUtils() {
    }

    // endregion

    // region 合并入口
    // ===================================
    // 合并入口
    // ===================================

    /**
     * 合并入库明细列表中合并键相同的行（保持首次出现顺序）
     *
     * @param details 入库明细列表，允许为null或空
     * @return 合并后的明细列表；入参为null或空时原样返回
     */
    public static List<StockInDetail> merge(List<StockInDetail> details) {
        if (details == null || details.isEmpty()) {
            return details;
        }
        Map<MergeKey, StockInDetail> mergedMap = new LinkedHashMap<>();
        for (StockInDetail detail : details) {
            if (detail == null) {
                continue;
            }
            MergeKey key = MergeKey.of(detail);
            StockInDetail target = mergedMap.get(key);
            if (target == null) {
                mergedMap.put(key, detail);
            } else {
                accumulate(target, detail);
            }
        }
        return new ArrayList<>(mergedMap.values());
    }

    /**
     * 判断两条入库明细是否属于同一合并组
     *
     * @param first  第一条明细
     * @param second 第二条明细
     * @return 合并键相同返回true
     */
    public static boolean isSameMergeKey(StockInDetail first, StockInDetail second) {
        if (first == null || second == null) {
            return false;
        }
        // 同一对象直接视为同组（物品标识缺失时每次构建的占位标识不同）
        if (first == second) {
            return true;
        }
        return MergeKey.of(first).equals(MergeKey.of(second));
    }

    /**
     * 将来源明细累加到目标明细上
     * <p>
     * 数量、金额累加；单价及物品信息仅在目标为空时补齐；备注保留首个非空值。
     * 目标明细的主键、入库单ID等标识字段保持不变。
     * </p>
     *
     * @param target 合并目标明细（保留该行）
     * @param source 被合并的明细（其数量金额累加到目标）
     */
    public static void accumulate(StockInDetail target, StockInDetail source) {
        if (target == null || source == null) {
            return;
        }
        // 数量与金额累加，空值按不影响另一侧处理
        target.setQuantity(add(target.getQuantity(), source.getQuantity()));
        target.setAmount(add(target.getAmount(), source.getAmount()));
        // 单价保留首个非空值，避免后续行覆盖已确定的入库单价
        if (target.getUnitPrice() == null) {
            target.setUnitPrice(source.getUnitPrice());
        }
        // 标识与展示字段仅在目标缺失时补齐
        if (target.getItemId() == null) {
            target.setItemId(source.getItemId());
        }
        if (target.getItemType() == null) {
            target.setItemType(source.getItemType());
        }
        if (!hasText(target.getItemCode()) && hasText(source.getItemCode())) {
            target.setItemCode(source.getItemCode());
        }
        if (!hasText(target.getItemName()) && hasText(source.getItemName())) {
            target.setItemName(source.getItemName());
        }
        if (!hasText(target.getCategoryName()) && hasText(source.getCategoryName())) {
            target.setCategoryName(source.getCategoryName());
        }
        if (!hasText(target.getUnitName()) && hasText(source.getUnitName())) {
            target.setUnitName(source.getUnitName());
        }
        if (!hasText(target.getRemark()) && hasText(source.getRemark())) {
            target.setRemark(source.getRemark());
        }
    }

    // endregion

    // region 私有工具方法
    // ===================================
    // 私有工具方法
    // ===================================

    /**
     * BigDecimal 空安全相加
     *
     * @param left  左值
     * @param right 右值
     * @return 相加结果；两者均为null时返回null
     */
    private static BigDecimal add(BigDecimal left, BigDecimal right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.add(right);
    }

    /**
     * 判断字符串是否有实际内容（去除首尾空白后非空）
     *
     * @param value 待判断字符串
     * @return 有内容返回true
     */
    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * 字符串规范化：去除首尾空白，空串统一转为null，便于合并键判等
     *
     * @param value 原始字符串
     * @return 规范化后的字符串
     */
    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // endregion

    // region 合并键
    // ===================================
    // 合并键
    // ===================================

    /**
     * 入库明细合并键
     *
     * @param itemCode        物品编码（规范化后）
     * @param itemId          物品ID
     * @param batchNumber     批号（规范化后）
     * @param prodUnitId      明细仓库ID
     * @param stockStatus     库存状态（空值按默认“合格”处理）
     * @param productionDate  生产日期
     * @param expiryDate      有效期至
     * @param storageLocation 存放位置（规范化后）
     * @param identityToken   物品标识缺失时的占位对象（保证每行独立、不误合并）
     */
    private record MergeKey(String itemCode, Long itemId, String batchNumber, Long prodUnitId,
                            String stockStatus, LocalDate productionDate, LocalDate expiryDate,
                            String storageLocation, Object identityToken) {

        /**
         * 根据入库明细构建合并键
         *
         * @param detail 入库明细
         * @return 合并键
         */
        static MergeKey of(StockInDetail detail) {
            String itemCode = normalize(detail.getItemCode());
            Long itemId = detail.getItemId();
            // 物品编码与物品ID均缺失时使用独立占位对象，避免不同行被误判为同一物料（此类行不参与合并）
            Object identityToken = (itemCode == null && itemId == null) ? new Object() : null;
            String stockStatus = normalize(detail.getStockStatus());
            if (stockStatus == null) {
                stockStatus = DEFAULT_STOCK_STATUS;
            }
            return new MergeKey(
                    itemCode,
                    itemId,
                    normalize(detail.getBatchNumber()),
                    detail.getProdUnitId(),
                    stockStatus,
                    detail.getProductionDate(),
                    detail.getExpiryDate(),
                    normalize(detail.getStorageLocation()),
                    identityToken
            );
        }
    }

    // endregion
}
