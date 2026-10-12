package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 销毁记录主表实体类
 * <p>
 * 记录不合格品销毁与留样及稳定性考察产品销毁两类纸质表单的电子台账，
 * 包括销毁单号、记录类型、销毁合计数量、销毁原因/方法/地点、
 * 申请人/批准人/销毁人/监销人四方签字文本及作废状态
 * </p>
 * <p>
 * 作废语义：不提供删除接口，作废通过 status=作废 + void_time + void_remark 实现，作废后禁止修改
 * </p>
 *
 * @TableName destruction_record
 */
@TableName(value = "destruction_record")
@Data
@EqualsAndHashCode(callSuper = true)
public class DestructionRecord extends AuditEntity {

    // region 常量定义
    // ===================================
    // 常量定义
    // ===================================

    /**
     * 记录类型：不合格品销毁（单号前缀BPXH）
     */
    public static final String TYPE_UNQUALIFIED = "不合格品销毁";

    /**
     * 记录类型：留样及稳定性考察产品销毁（单号前缀LYXH）
     */
    public static final String TYPE_RETAINED = "留样及稳定性考察产品销毁";

    /**
     * 状态：正常
     */
    public static final String STATUS_NORMAL = "正常";

    /**
     * 状态：作废
     */
    public static final String STATUS_VOID = "作废";

    // endregion

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 销毁记录唯一标识（自增主键）
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 销毁单号（唯一，不合格品销毁BPXH-YYYYMMDD-NNN/留样销毁LYXH-YYYYMMDD-NNN，系统自动生成，不可修改）
     */
    @TableField(value = "destruction_code")
    private String destructionCode;

    /**
     * 记录类型：不合格品销毁/留样及稳定性考察产品销毁
     */
    @TableField(value = "record_type")
    private String recordType;

    /**
     * 销毁合计数量（服务端按明细数量求和计算）
     */
    @TableField(value = "total_quantity")
    private BigDecimal totalQuantity;

    // endregion

    // region 销毁详情字段
    // ===================================
    // 销毁详情字段
    // ===================================

    /**
     * 销毁原因（可选，最长1000字符）
     */
    @TableField(value = "destroy_reason")
    private String destroyReason;

    /**
     * 销毁方法（可选，最长1000字符）
     */
    @TableField(value = "destroy_method")
    private String destroyMethod;

    /**
     * 销毁地点（可选，最长200字符）
     */
    @TableField(value = "destroy_location")
    private String destroyLocation;

    /**
     * 备注说明（可选，最长1000字符）
     */
    @TableField(value = "remark")
    private String remark;

    // endregion

    // region 四方签字字段（存姓名文本）
    // ===================================
    // 四方签字字段（存姓名文本）
    // ===================================

    /**
     * 申请人姓名（存姓名文本，最长50字符）
     */
    @TableField(value = "applicant_name")
    private String applicantName;

    /**
     * 申请日期
     */
    @TableField(value = "apply_date")
    private LocalDate applyDate;

    /**
     * 批准人姓名（存姓名文本，最长50字符）
     */
    @TableField(value = "approver_name")
    private String approverName;

    /**
     * 批准日期
     */
    @TableField(value = "approve_date")
    private LocalDate approveDate;

    /**
     * 销毁人姓名（存姓名文本，最长50字符）
     */
    @TableField(value = "destroyer_name")
    private String destroyerName;

    /**
     * 销毁日期
     */
    @TableField(value = "destroy_date")
    private LocalDate destroyDate;

    /**
     * 监销人姓名（存姓名文本，最长50字符）
     */
    @TableField(value = "supervisor_name")
    private String supervisorName;

    /**
     * 监销日期
     */
    @TableField(value = "supervise_date")
    private LocalDate superviseDate;

    // endregion

    // region 作废状态字段
    // ===================================
    // 作废状态字段
    // ===================================

    /**
     * 状态：正常/作废
     */
    @TableField(value = "status")
    private String status;

    /**
     * 作废时间（status=作废时由服务端填写）
     */
    @TableField(value = "void_time")
    private LocalDateTime voidTime;

    /**
     * 作废原因（status=作废时填写，最长500字符）
     */
    @TableField(value = "void_remark")
    private String voidRemark;

    // endregion

    // region 审计与软删除字段
    // ===================================
    // 审计与软删除字段
    // ===================================

    /**
     * 是否已删除：0否/1是（保留字段，业务不走删除接口）
     */
    @TableField(value = "is_deleted")
    private Integer isDeleted;

    // endregion
}
