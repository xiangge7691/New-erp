package com.tonghui.erp.Common.Dto.Quality;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 偏差记录保存请求数据传输对象
 * <p>
 * 用于新增/修改偏差记录的统一请求体，保存时服务端自动生成偏差编号
 * 并从生产任务表快照任务编号/制剂名称/批号
 * </p>
 */
@Data
public class DeviationRecordSaveDto {

    // region 关联生产任务字段
    // ===================================
    // 关联生产任务字段
    // ===================================

    /**
     * 关联生产任务ID（必填，用于快照任务编号/制剂名称/批号）
     */
    private Long workOrderId;

    // endregion

    // region 偏差详情字段
    // ===================================
    // 偏差详情字段
    // ===================================

    /**
     * 偏差标题（必填，最长200字符）
     */
    private String deviationTitle;

    /**
     * 实际发生时间（必填）
     */
    private LocalDateTime occurTime;

    /**
     * 发生工序（可选，最长100字符）
     */
    private String processName;

    /**
     * 偏差描述（必填，最长5000字符）
     */
    private String description;

    /**
     * 处理措施（可选，最长5000字符）
     */
    private String actionDesc;

    /**
     * 处理结果（可选，最长5000字符）
     */
    private String resultDesc;

    /**
     * 处理人姓名（可选，存姓名文本，最长100字符）
     */
    private String handlerName;

    /**
     * 处理时间（可选）
     */
    private LocalDateTime handleTime;

    /**
     * 备注说明（可选，最长1000字符）
     */
    private String remark;

    // endregion

    // region 附件字段
    // ===================================
    // 附件字段
    // ===================================

    /**
     * 附件文件ID列表（可选，文件本体已由文件上传接口存储，此处仅建立业务关联）
     */
    private List<Long> attachmentFileIds;

    // endregion
}
