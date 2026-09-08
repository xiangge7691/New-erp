package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 请检记录表实体
 * <p>
 * 生产过程向检验部门提出检验申请的记录。针对在生产中的中药制剂，发起请检后进入检验流程。
 * 支持两种关联方式：从生产中任务选择 / 手动输入任务号。
 * </p>
 *
 * @TableName inspection_request
 */
@TableName(value = "inspection_request")
@Data
@EqualsAndHashCode(callSuper = true)
public class InspectionRequest extends AuditEntity {

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
     * 请检编号（格式 QJ-YYYYMMDD-NNN，保存时自动生成）
     */
    @TableField(value = "inspection_code")
    private String inspectionCode;

    /**
     * 关联生产任务ID
     */
    @TableField(value = "work_order_id")
    private Long workOrderId;

    /**
     * 关联生产任务编号
     */
    @TableField(value = "work_order_code")
    private String workOrderCode;

    /**
     * 关联检验计划编号（选填）
     */
    @TableField(value = "inspection_plan_code")
    private String inspectionPlanCode;

    // endregion

    // region 制剂与检验信息字段
    // ===================================
    // 制剂与检验信息字段
    // ===================================

    /**
     * 制剂编码（选择/匹配任务后自动带出）
     */
    @TableField(value = "preparation_code")
    private String preparationCode;

    /**
     * 制剂名称
     */
    @TableField(value = "preparation_name")
    private String preparationName;

    /**
     * 被检物名称（默认取制剂名称，可按被检物修改）
     */
    @TableField(value = "inspection_item_name")
    private String inspectionItemName;

    /**
     * 批号（匹配任务可自动带出，可修改）
     */
    @TableField(value = "batch_number")
    private String batchNumber;

    /**
     * 规格（按制剂编码带出）
     */
    @TableField(value = "spec")
    private String spec;

    // endregion

    // region 生产与请检信息字段
    // ===================================
    // 生产与请检信息字段
    // ===================================

    /**
     * 工序（手动输入请检所在工序，不做候选提示）
     */
    @TableField(value = "process_name")
    private String processName;

    /**
     * 配置量（文本自由输入，如2000或3000kg）
     */
    @TableField(value = "config_quantity")
    private String configQuantity;

    /**
     * 请检部门（下拉选择车间房间名称）
     */
    @TableField(value = "request_department")
    private String requestDepartment;

    /**
     * 请检人（发起请检的人）
     */
    @TableField(value = "requester")
    private String requester;

    /**
     * 请检时间
     */
    @TableField(value = "request_time")
    private LocalDateTime requestTime;

    /**
     * 请检项目（多项，顿号分隔）
     */
    @TableField(value = "inspection_items")
    private String inspectionItems;

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;

    // endregion

    // region 状态与审计字段
    // ===================================
    // 状态与审计字段
    // ===================================

    /**
     * 是否已删除
     */
    @TableField(value = "is_deleted")
    private Integer isDeleted;

    /**
     * 乐观锁版本号
     */
    @TableField(value = "version")
    private Integer version;

    // endregion
}
