package com.tonghui.erp.Common.Dto.Quality;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 销毁记录保存请求数据传输对象
 * <p>
 * 用于新增/修改销毁记录的统一请求体，一次请求同时保存主表与明细。
 * 保存时服务端按记录类型自动生成单号（BPXH-/LYXH-）并按明细求和计算合计数量
 * </p>
 */
@Data
public class DestructionRecordSaveDto {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 记录类型（必填：不合格品销毁/留样及稳定性考察产品销毁，决定单号前缀）
     */
    private String recordType;

    // endregion

    // region 销毁详情字段
    // ===================================
    // 销毁详情字段
    // ===================================

    /**
     * 销毁原因（可选，最长1000字符）
     */
    private String destroyReason;

    /**
     * 销毁方法（可选，最长1000字符）
     */
    private String destroyMethod;

    /**
     * 销毁地点（可选，最长200字符）
     */
    private String destroyLocation;

    /**
     * 备注说明（可选，最长1000字符）
     */
    private String remark;

    // endregion

    // region 四方签字字段（存姓名文本）
    // ===================================
    // 四方签字字段（存姓名文本）
    // ===================================

    /**
     * 申请人姓名（可选，存姓名文本，最长50字符）
     */
    private String applicantName;

    /**
     * 申请日期（可选）
     */
    private LocalDate applyDate;

    /**
     * 批准人姓名（可选，存姓名文本，最长50字符）
     */
    private String approverName;

    /**
     * 批准日期（可选）
     */
    private LocalDate approveDate;

    /**
     * 销毁人姓名（可选，存姓名文本，最长50字符）
     */
    private String destroyerName;

    /**
     * 销毁日期（可选）
     */
    private LocalDate destroyDate;

    /**
     * 监销人姓名（可选，存姓名文本，最长50字符）
     */
    private String supervisorName;

    /**
     * 监销日期（可选）
     */
    private LocalDate superviseDate;

    // endregion

    // region 明细与附件字段
    // ===================================
    // 明细与附件字段
    // ===================================

    /**
     * 销毁明细列表（必填，至少1行）
     */
    private List<DestructionRecordDetailSaveDto> details;

    /**
     * 附件文件ID列表（可选，文件本体已由文件上传接口存储，此处仅建立业务关联）
     */
    private List<Long> attachmentFileIds;

    // endregion
}
