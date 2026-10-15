package com.tonghui.erp.Common.Dto.Maintenance;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 待养护库存明细数据传输对象
 * <p>
 * 按库存检查明细的下次养护日期统计的待养护列表项，逾期优先、再按下次养护日期升序。
 * 数量按待检查的库存明细数统计（同一物料多批库存分别计算，不互相覆盖）
 * </p>
 */
@Data
public class MaintenancePendingDto {

    /**
     * 库存检查明细ID（maintenance_record_detail.id，最近一次检查对应的明细）
     */
    private Long detailId;

    /**
     * 关联库存明细ID（stock.stock_id）
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
     * 批号
     */
    private String batchNumber;

    /**
     * 入库编号
     */
    private String inCode;

    /**
     * 仓库名称
     */
    private String warehouseName;

    /**
     * 计量单位
     */
    private String unitName;

    /**
     * 最近养护日期
     */
    private LocalDate lastMaintenanceDate;

    /**
     * 下次养护日期
     */
    private LocalDate nextMaintenanceDate;

    /**
     * 逾期天数（已逾期>今日，未逾期为0）
     */
    private long overdueDays;

    /**
     * 提醒状态：OVERDUE已逾期/TODAY今日到期/SOON即将到期
     */
    private String pendingStatus;

    /**
     * 最近一次养护记录编号（可查看原始记录）
     */
    private String lastMaintenanceCode;

    /**
     * 最近一次实际检查时间
     */
    private java.time.LocalDateTime lastCheckTime;

    /**
     * 当前库存数量（来自库存明细快照，仅作参考）
     */
    private BigDecimal stockQuantity;

    /**
     * 是否重点养护物料：1是/0否
     */
    private Integer keyMaintenance;
}
