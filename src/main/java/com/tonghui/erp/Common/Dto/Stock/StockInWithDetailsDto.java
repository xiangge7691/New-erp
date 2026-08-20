package com.tonghui.erp.Common.Dto.Stock;

import com.tonghui.erp.Data.Entity.StockIn;
import com.tonghui.erp.Data.Entity.StockInDetail;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 入库单包含明细的扩展数据传输对象
 * <p>
 * 在入库单基础上扩展了入库明细列表以及操作人姓名、仓库名称，
 * 用于展示完整的入库单及其明细信息
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StockInWithDetailsDto extends StockIn {

    /**
     * 入库明细列表
     */
    private List<StockInDetail> details;

    /**
     * 本次入库成功的明细条数（接口返回时填充）
     */
    private Integer successCount;

    /**
     * 操作人姓名（创建人，查询时回填）
     */
    private String createdByName;

    /**
     * 更新人姓名（查询时回填）
     */
    private String updatedByName;

    /**
     * 仓库名称（入库生产单位名称，查询时回填）
     */
    private String warehouseName;
}
