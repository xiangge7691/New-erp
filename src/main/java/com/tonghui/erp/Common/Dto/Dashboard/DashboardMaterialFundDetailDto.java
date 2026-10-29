package com.tonghui.erp.Common.Dto.Dashboard;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 原辅包物料级明细 DTO
 * <p>
 * 对应原辅包图表色块下钻：按分类（原料/辅料/包材）与月份（入库/出库）筛选后，
 * 返回物料级聚合明细（同一物料编码 + 名称合并为一行）。
 * </p>
 */
@Data
public class DashboardMaterialFundDetailDto {

    // region 查询条件回显
    // ===================================
    // 查询条件回显
    // ===================================

    /** 类型：inbound 入库 / outbound 出库净额 / stock 当前库存 */
    private String type;

    /** 分类：原料 / 辅料 / 包材；为空表示全部 */
    private String category;

    /** 月份（yyyy-MM）；仅 inbound/outbound 生效，stock 忽略 */
    private String month;

    /** 库存快照日期（yyyy-MM-dd，仅 stock 返回） */
    private String snapshotDate;

    /** 合计金额（元；出库为净额） */
    private BigDecimal totalAmount;

    /** 合计数量（各分类单位独立，仅供单分类筛选时参考） */
    private BigDecimal totalQuantity;

    /** 物料明细列表（全量，不分页） */
    private List<Item> items;

    // endregion

    // region 明细行
    // ===================================
    // 明细行
    // ===================================

    /**
     * 物料级明细行
     */
    @Data
    public static class Item {

        /** 分类：原料 / 辅料 / 包材 */
        private String category;

        /** 物料编码 */
        private String materialCode;

        /** 物料名称 */
        private String materialName;

        /** 规格（取自物料档案，取不到为 null） */
        private String spec;

        /** 数量（单位见 unit） */
        private BigDecimal quantity;

        /** 单位（kg / 个 等） */
        private String unit;

        /** 批次数量（涉及的不同批号个数） */
        private Integer batchCount;

        /** 金额（元；出库为出库金额，库存为数量 × 单价） */
        private BigDecimal amount;

        /** 退库冲减金额（元，仅出库返回） */
        private BigDecimal returnAmount;

        /** 退库冲减数量（仅出库返回） */
        private BigDecimal returnQuantity;

        /** 净金额（元）= 金额 − 退库冲减 */
        private BigDecimal netAmount;

        /** 占该次查询合计金额比例（%） */
        private BigDecimal ratio;

        /** 库龄（天）= 今天 − 入库日期；仅库存返回，取不到为 null */
        private Long stockAgeDays;
    }

    // endregion
}
