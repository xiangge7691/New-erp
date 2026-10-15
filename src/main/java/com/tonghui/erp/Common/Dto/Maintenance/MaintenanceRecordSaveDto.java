package com.tonghui.erp.Common.Dto.Maintenance;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 养护记录保存请求数据传输对象
 * <p>
 * 用于新增/修改养护记录的统一请求体，一次请求同时保存主单与库存检查明细。
 * 保存时服务端按实际检查日期自动生成养护记录编号（YH-YYYYMMDD-NNN）；
 * 明细需引用库存明细（stockId），登记本身不产生库存增减
 * </p>
 */
@Data
public class MaintenanceRecordSaveDto {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 实际检查时间（必填，人工填写，支持事后补录）
     */
    private LocalDateTime checkTime;

    /**
     * 检查类型（必填：日常检查/定期检查/特殊天气检查/复查）
     */
    private String checkType;

    // endregion

    // region 仓库与人员字段
    // ===================================
    // 仓库与人员字段
    // ===================================

    /**
     * 仓库ID（必填，引用生产单位production_unit.prod_unit_id）
     */
    private Long warehouseId;

    /**
     * 检查人员ID（必填，引用系统用户user.user_id）
     */
    private Long checkerId;

    // endregion

    // region 环境数据字段
    // ===================================
    // 环境数据字段
    // ===================================

    /**
     * 环境观测时间及位置（可选）
     */
    private String envObserveInfo;

    /**
     * 温度（℃，可选）
     */
    private java.math.BigDecimal temperature;

    /**
     * 相对湿度（%，可选）
     */
    private java.math.BigDecimal humidity;

    /**
     * 总体说明（可选，最长1000字符）
     */
    private String summary;

    // endregion

    // region 关联复查字段
    // ===================================
    // 关联复查字段
    // ===================================

    /**
     * 关联复查记录ID（可选，检查类型为复查时关联原始记录编号对应的记录）
     */
    private Long relatedMaintenanceId;

    // endregion

    // region 明细与附件字段
    // ===================================
    // 明细与附件字段
    // ===================================

    /**
     * 库存检查明细列表（必填，至少1行）
     */
    private List<MaintenanceRecordDetailSaveDto> details;

    /**
     * 附件文件ID列表（可选，文件本体已由文件上传接口存储，此处仅建立业务关联）
     */
    private List<Long> attachmentFileIds;

    // endregion
}
