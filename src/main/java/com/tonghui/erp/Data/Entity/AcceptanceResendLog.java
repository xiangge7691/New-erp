package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 验收明细重新发货操作留痕表
 * <p>
 * 物料退货管理页执行「重新发货」时逐条写入留痕：
 * 记录来源验收单、退货物料、重新发货数量、生成的新验收单号与操作时间。
 * 列表页「重发时间」列取该物料最近一条记录的 operationTime
 * </p>
 * @TableName acceptance_resend_log
 */
@TableName(value = "acceptance_resend_log")
@Data
public class AcceptanceResendLog {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 来源验收单ID（退货明细所属验收单）
     */
    @TableField(value = "acceptance_id")
    private Long acceptanceId;

    /**
     * 来源验收明细ID
     */
    @TableField(value = "detail_id")
    private Long detailId;

    // endregion

    // region 业务字段
    // ===================================
    // 业务字段
    // ===================================

    /**
     * 物料名称（冗余存储，便于留痕直接展示）
     */
    @TableField(value = "material_name")
    private String materialName;

    /**
     * 重新发货数量
     */
    @TableField(value = "resend_qty")
    private BigDecimal resendQty;

    /**
     * 重新发货生成的新验收单号
     */
    @TableField(value = "new_acceptance_code")
    private String newAcceptanceCode;

    /**
     * 重新发货操作时间
     */
    @TableField(value = "operation_time")
    private LocalDateTime operationTime;

    // endregion

    // region 审计字段
    // ===================================
    // 审计字段
    // ===================================

    /**
     * 操作人ID
     */
    @TableField(value = "created_by")
    private Long createdBy;

    /**
     * 创建时间
     */
    @TableField(value = "created_time")
    private LocalDateTime createdTime;

    // endregion
}
