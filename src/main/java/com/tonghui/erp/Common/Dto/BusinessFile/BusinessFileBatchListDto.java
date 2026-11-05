package com.tonghui.erp.Common.Dto.BusinessFile;

import lombok.Data;

import java.util.List;

/**
 * 批量查询业务附件请求
 */
@Data
public class BusinessFileBatchListDto {

    /** 业务类型编码 */
    private String bizType;

    /** 业务ID列表 */
    private List<Long> bizIds;

    /** 文件槽位（可为空） */
    private String fileKind;
}
