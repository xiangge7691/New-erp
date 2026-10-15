package com.tonghui.erp.Common.Dto.Maintenance;

import com.tonghui.erp.Data.Entity.MaintenanceRecord;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 养护记录详情数据传输对象
 * <p>
 * 继承主单实体，补充库存检查明细（含明细附件）、主单附件、创建人/更新人姓名回填
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MaintenanceRecordWithDetailsDto extends MaintenanceRecord {

    /**
     * 库存检查明细列表（全部检查项目及结果，非只列异常项）
     */
    private List<MaintenanceRecordDetailVo> details;

    /**
     * 主单附件列表
     */
    private List<com.tonghui.erp.Data.Entity.FileInfo> attachments;

    /**
     * 检查明细数
     */
    private int detailCount;

    /**
     * 异常明细数
     */
    private int abnormalCount;

    /**
     * 创建人姓名（登记人）
     */
    private String createdByName;

    /**
     * 更新人姓名
     */
    private String updatedByName;
}
