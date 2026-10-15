package com.tonghui.erp.Common.Dto.Maintenance;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 养护台账导出行数据传输对象
 * <p>
 * 台账按「主单+明细」平铺导出一行一物料：主单信息随明细重复展示，
 * 检查项目结果以中文四态文本输出，实测含水量为空输出「未测」
 * </p>
 */
@Data
public class MaintenanceExportRowDto {

    /**
     * 养护记录编号
     */
    private String maintenanceCode;

    /**
     * 实际检查时间
     */
    private LocalDateTime checkTime;

    /**
     * 检查类型
     */
    private String checkType;

    /**
     * 仓库
     */
    private String warehouseName;

    /**
     * 检查人员
     */
    private String checkerName;

    /**
     * 环境观测时间及位置
     */
    private String envObserveInfo;

    /**
     * 温度（℃）
     */
    private BigDecimal temperature;

    /**
     * 相对湿度（%）
     */
    private BigDecimal humidity;

    /**
     * 总体说明
     */
    private String summary;

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
     * 分类
     */
    private String categoryName;

    /**
     * 批号
     */
    private String batchNumber;

    /**
     * 入库编号
     */
    private String inCode;

    /**
     * 计量单位
     */
    private String unitName;

    /**
     * 检查时库存数量
     */
    private BigDecimal stockQuantity;

    /**
     * 货架位置
     */
    private String shelfLocation;

    /**
     * 检查范围或数量
     */
    private String checkScope;

    /**
     * 检查项目结果汇总（项名：四态，分号分隔）
     */
    private String checkResultSummary;

    /**
     * 实测含水量（%），空表示未测
     */
    private BigDecimal moistureContent;

    /**
     * 实测垛温（℃）
     */
    private BigDecimal stackTemperature;

    /**
     * 异常描述
     */
    private String abnormalDescription;

    /**
     * 养护措施
     */
    private String measures;

    /**
     * 操作人员
     */
    private String operatorName;

    /**
     * 操作时间
     */
    private LocalDateTime operateTime;

    /**
     * 处理结果
     */
    private String handleResult;

    /**
     * 备注
     */
    private String remark;

    /**
     * 最近养护日期
     */
    private java.time.LocalDate lastMaintenanceDate;

    /**
     * 下次养护日期
     */
    private java.time.LocalDate nextMaintenanceDate;
}
