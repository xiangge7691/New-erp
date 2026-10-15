package com.tonghui.erp.Common.Dto.Maintenance;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 养护记录用库存选择项数据传输对象
 * <p>
 * 新增养护记录时「选择库存物料」弹窗的数据项：
 * 带出库存明细稳定标识（stockId+入库编号）及物料养护要求（只读参考）
 * </p>
 */
@Data
public class MaintenanceStockItemDto {

    /**
     * 库存明细ID（stock.stock_id，明细关联的稳定标识）
     */
    private Long stockId;

    /**
     * 物料编码
     */
    private String materialCode;

    /**
     * 物料名称
     */
    private String materialName;

    /**
     * 规格
     */
    private String spec;

    /**
     * 物料分类
     */
    private String categoryName;

    /**
     * 批号
     */
    private String batchNumber;

    /**
     * 入库单ID
     */
    private Long stockInId;

    /**
     * 入库编号（stock_in.in_code，如IN202512010001）
     */
    private String inCode;

    /**
     * 仓库名称
     */
    private String warehouseName;

    /**
     * 仓库ID
     */
    private Long warehouseId;

    /**
     * 库存数量
     */
    private BigDecimal stockQuantity;

    /**
     * 计量单位
     */
    private String unitName;

    /**
     * 库存状态（合格/待检/不合格）
     */
    private String stockStatus;

    /**
     * 养护注意事项（引用物料信息，只读参考）
     */
    private String maintenanceNotes;

    /**
     * 养护周期-天（引用物料信息，用于计算下次养护日期）
     */
    private Integer maintenanceCycleDays;

    /**
     * 是否重点养护物料：1是/0否
     */
    private Integer keyMaintenance;
}
