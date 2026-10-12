package com.tonghui.erp.Common.Dto.Quality;

import com.tonghui.erp.Data.Entity.DestructionRecord;
import com.tonghui.erp.Data.Entity.DestructionRecordDetail;
import com.tonghui.erp.Data.Entity.FileInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 销毁记录详情数据传输对象
 * <p>
 * 在销毁记录主表实体基础上扩展明细列表与附件列表，
 * 用于详情回显及列表展示完整的销毁台账信息
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DestructionRecordWithDetailsDto extends DestructionRecord {

    /**
     * 销毁明细列表（查询详情时回填）
     */
    private List<DestructionRecordDetail> details;

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
