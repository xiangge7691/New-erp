package com.tonghui.erp.Common.Dto.Warehouse;

import lombok.Data;

/**
 * 仓库可用物料DTO
 * <p>
 * 新增调拨时展示某仓库下的物料列表（含批次数量）
 * </p>
 */
@Data
public class WarehouseMaterialDto {

    /**
     * 物料编码
     */
    private String materialCode;

    /**
     * 物料名称
     */
    private String materialName;

    /**
     * 分类（原料/辅料/包材/成品）
     */
    private String category;

    /**
     * 该物料在仓库中的批次数量
     */
    private Integer batchCount;
}