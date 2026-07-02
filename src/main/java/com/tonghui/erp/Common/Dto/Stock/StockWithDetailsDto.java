package com.tonghui.erp.Common.Dto.Stock;

import com.tonghui.erp.Data.Entity.Stock;
import com.tonghui.erp.Data.Entity.StockOutDetail;
import com.tonghui.erp.Data.Entity.StockTransaction;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class StockWithDetailsDto extends Stock {
    private List<StockTransaction> transactions;
    private List<StockOutDetail> outDetails;
}
