package com.tonghui.erp.Common.Dto.BusinessFile;

import lombok.Data;

import java.util.List;

/**
 * 业务附件同步请求（保存单据时一次性绑定附件）
 */
@Data
public class BusinessFileSyncDto {

    /** 业务类型编码 */
    private String bizType;

    /** 业务ID */
    private Long bizId;

    /** 子对象ID（审批节点ID等，可为空） */
    private Long refId;

    /** 文件槽位（可为空，默认 DEFAULT） */
    private String fileKind;

    /** 目标文件ID列表（空列表表示解绑全部） */
    private List<Long> fileIds;
}
