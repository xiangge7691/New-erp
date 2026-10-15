package com.tonghui.erp.Common.Dto.Maintenance;

import com.tonghui.erp.Data.Entity.MaintenanceRecordDetail;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 库存检查明细详情视图对象
 * <p>
 * 继承明细实体，补充异常判定标记与明细附件列表
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MaintenanceRecordDetailVo extends MaintenanceRecordDetail {

    /**
     * 是否异常明细（任一检查项为发现异常或填写了异常描述）
     */
    private boolean abnormal;

    /**
     * 明细附件列表
     */
    private List<com.tonghui.erp.Data.Entity.FileInfo> attachments;
}
