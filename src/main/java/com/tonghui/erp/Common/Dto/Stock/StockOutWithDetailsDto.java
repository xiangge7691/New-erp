package com.tonghui.erp.Common.Dto.Stock;

import com.tonghui.erp.Data.Entity.StockOut;
import com.tonghui.erp.Data.Entity.StockOutDetail;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class StockOutWithDetailsDto extends StockOut {
    private List<StockOutDetail> details;
}
