package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 销毁记录明细表实体类
 * <p>
 * 记录销毁单的物料明细行，包括品名、规格、批号、数量及可选的留样记录引用，
 * 更新主表时明细整体重建（按 destruction_id 物理删除旧明细再重插）
 * </p>
 *
 * @TableName destruction_record_detail
 */
@TableName(value = "destruction_record_detail")
@Data
@EqualsAndHashCode(callSuper = true)
public class DestructionRecordDetail extends AuditEntity {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 销毁明细唯一标识（自增主键）
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联销毁记录主表ID
     */
    @TableField(value = "destruction_id")
    private Long destructionId;

    /**
     * 品名/物料名称（必填，最长100字符）
     */
    @TableField(value = "material_name")
    private String materialName;

    /**
     * 规格（可选，最长100字符）
     */
    @TableField(value = "spec")
    private String spec;

    /**
     * 批号（可选，最长50字符）
     */
    @TableField(value = "batch_no")
    private String batchNo;

    /**
     * 数量（可选，默认0，主表合计数量由本字段求和）
     */
    @TableField(value = "quantity")
    private BigDecimal quantity;

    /**
     * 关联留样记录ID（可空，仅作选人回填与追溯引用，保存销毁记录不联动留样状态）
     */
    @TableField(value = "retained_sample_id")
    private Long retainedSampleId;

    /**
     * 关联留样编号（非表字段，查询时按retainedSampleId关联留样表回填，用于明细展示）
     */
    @TableField(exist = false)
    private String retainedCode;

    // endregion

    // region 审计与软删除字段
    // ===================================
    // 审计与软删除字段
    // ===================================

    /**
     * 是否已删除：0否/1是（保留字段，重建时物理删除）
     */
    @TableField(value = "is_deleted")
    private Integer isDeleted;

    // endregion
}
