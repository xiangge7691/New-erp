package com.tonghui.erp.Common.Dto.System;

import com.tonghui.erp.Data.Entity.ProdUnitInvoice;
import com.tonghui.erp.Data.Entity.ProdUnitMaterialFile;
import com.tonghui.erp.Data.Entity.ProductionUnit;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 生产单元包含发票和物料文件的扩展数据传输对象
 * <p>
 * 在生产单元基础上扩展了发票和物料文件列表，用于展示完整的生产单元详情及关联资料
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductionUnitWithDetailsDto extends ProductionUnit {

    /**
     * 该生产单元的发票列表
     */
    private List<ProdUnitInvoice> invoices;

    /**
     * 该生产单元的物料文件列表
     */
    private List<ProdUnitMaterialFile> materialFiles;
}
