package com.tonghui.erp.Common.Dto.Stock;

import com.tonghui.erp.Data.Entity.StockIn;
import com.tonghui.erp.Data.Entity.StockInDetail;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class StockInWithDetailsDto extends StockIn {
    private List<StockInDetail> details;
}
