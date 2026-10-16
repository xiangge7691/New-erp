package com.tonghui.erp.Common.Dto.Maintenance;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 库存检查明细保存请求数据传输对象
 * <p>
 * 一条明细对应一条库存明细（stock）的检查与养护结果。
 * 物料信息、养护要求、批号、入库编号、检查时库存数量由服务端按 stockId 快照，
 * 前端只需填写货架位置、检查范围、检查项目、实测数据、异常描述、
 * 养护措施、操作人员、处理结果等人工登记内容
 * </p>
 */
@Data
public class MaintenanceRecordDetailSaveDto {

    // region 库存关联字段
    // ===================================
    // 库存关联字段
    // ===================================

    /**
     * 明细行ID（修改已有明细行时回传，服务端按ID原地更新保留ID；
     * 新增行不传，由数据库自增生成）
     */
    private Long id;

    /**
     * 关联库存明细ID（必填，stock.stock_id）
     */
    private Long stockId;

    // endregion

    // region 人工登记字段
    // ===================================
    // 人工登记字段
    // ===================================

    /**
     * 货架位置（可选，手动填写）
     */
    private String shelfLocation;

    /**
     * 检查范围或数量（可选，如全部/1~3货位）
     */
    private String checkScope;

    // endregion

    // region 检查项目字段（四态，默认未见异常）
    // ===================================
    // 检查项目字段（四态，默认未见异常）
    // ===================================

    /**
     * 包装与标签：1未检查/2未见异常/3发现异常/4不适用
     */
    private Integer packagingStatus;

    /**
     * 受潮及干湿：1未检查/2未见异常/3发现异常/4不适用
     */
    private Integer moistureStatus;

    /**
     * 虫蛀情况：1未检查/2未见异常/3发现异常/4不适用
     */
    private Integer pestStatus;

    /**
     * 霉变及腐败：1未检查/2未见异常/3发现异常/4不适用
     */
    private Integer moldStatus;

    /**
     * 泛油情况：1未检查/2未见异常/3发现异常/4不适用
     */
    private Integer oilStatus;

    /**
     * 色泽及外观：1未检查/2未见异常/3发现异常/4不适用
     */
    private Integer colorStatus;

    /**
     * 气味：1未检查/2未见异常/3发现异常/4不适用
     */
    private Integer odorStatus;

    /**
     * 破碎及杂质：1未检查/2未见异常/3发现异常/4不适用
     */
    private Integer breakageStatus;

    /**
     * 堆放及储存条件：1未检查/2未见异常/3发现异常/4不适用
     */
    private Integer stackingStatus;

    /**
     * 其他异常：1未检查/2未见异常/3发现异常/4不适用
     */
    private Integer otherStatus;

    /**
     * 其他异常具体内容（其他异常勾选发现异常时填写）
     */
    private String otherDescription;

    // endregion

    // region 实测与异常字段
    // ===================================
    // 实测与异常字段
    // ===================================

    /**
     * 实测含水量（%），为空表示未测（不得填0冒充实测）
     */
    private BigDecimal moistureContent;

    /**
     * 实测垛温（℃，可选）
     */
    private BigDecimal stackTemperature;

    /**
     * 异常描述（可选，说明实际发现的情况）
     */
    private String abnormalDescription;

    // endregion

    // region 养护措施与处理字段
    // ===================================
    // 养护措施与处理字段
    // ===================================

    /**
     * 养护措施（多选，逗号分隔）
     */
    private String measures;

    /**
     * 养护措施其他（补充说明）
     */
    private String measuresOther;

    /**
     * 操作人员ID（可选，引用系统用户，可与检查人员不同）
     */
    private Long operatorId;

    /**
     * 操作时间（可选，可与检查时间不同）
     */
    private LocalDateTime operateTime;

    /**
     * 处理结果（可选，尚未完成时允许后续补充）
     */
    private String handleResult;

    /**
     * 备注（可选）
     */
    private String remark;

    // endregion

    // region 附件字段
    // ===================================
    // 附件字段
    // ===================================

    /**
     * 明细附件文件ID列表（可选，照片或报告对应到具体物料批次）
     */
    private List<Long> attachmentFileIds;

    // endregion
}
