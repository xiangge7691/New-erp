package com.tonghui.erp.Common.Dto.Quality;

import com.tonghui.erp.Data.Entity.DeviationRecord;
import com.tonghui.erp.Data.Entity.FileInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 偏差记录详情数据传输对象
 * <p>
 * 在偏差记录实体基础上扩展附件列表与创建人姓名，
 * 用于详情回显及列表展示完整的偏差台账信息
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DeviationRecordWithAttachmentsDto extends DeviationRecord {

    /**
     * 附件列表（查询详情时回填）
     */
    private List<FileInfo> attachments;

    /**
     * 创建人姓名（列表/详情展示回填）
     */
    private String createdByName;

    /**
     * 更新人姓名（详情展示回填）
     */
    private String updatedByName;
}
