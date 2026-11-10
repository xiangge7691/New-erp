package com.tonghui.erp.Common.Dto.QualityPages;

import com.tonghui.erp.Data.Entity.AcceptanceOrder;
import com.tonghui.erp.Data.Entity.DeviationRecord;
import com.tonghui.erp.Data.Entity.InspectionRecord;
import com.tonghui.erp.Data.Entity.InspectionRequest;
import com.tonghui.erp.Data.Entity.Preparation;
import com.tonghui.erp.Data.Entity.ProductionPick;
import com.tonghui.erp.Data.Entity.ProductionPlan;
import com.tonghui.erp.Data.Entity.PurchaseOrders;
import com.tonghui.erp.Data.Entity.ReleaseReview;
import com.tonghui.erp.Data.Entity.ReturnOrder;
import com.tonghui.erp.Data.Entity.RoomInfo;
import com.tonghui.erp.Data.Entity.SalesOrder;
import com.tonghui.erp.Data.Entity.SamplingRecord;
import com.tonghui.erp.Data.Entity.StockIn;
import com.tonghui.erp.Data.Entity.StockOut;
import com.tonghui.erp.Data.Entity.WorkOrder;
import com.tonghui.erp.Data.Entity.WorkOrderProcessCheck;
import com.tonghui.erp.Data.Entity.WorkOrderProcessExecution;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 批次聚合上下文
 * <p>
 * 一次加载一个工单/批次在全链路中需要的生产、采购、验收、领料、退库、
 * 出入库、检验、放行、偏差、工序执行和工序要求数据，供产品信息汇总和审核放行共用。
 * </p>
 */
@Data
public class BatchContext {

    private WorkOrder workOrder;
    private ProductionPlan plan;
    private Preparation preparation;

    private List<PurchaseOrders> purchases = new ArrayList<>();
    private List<AcceptanceOrder> acceptances = new ArrayList<>();
    private List<ProductionPick> picks = new ArrayList<>();
    private List<ReturnOrder> returns = new ArrayList<>();
    /** 成品入库单（stock_in.work_order_id 关联，in_type=成品入库） */
    private List<StockIn> stockIns = new ArrayList<>();
    /** 成品出库单（销售出库/成品出库，按销售台账或批号关联） */
    private List<StockOut> stockOuts = new ArrayList<>();
    /** 物料入库单（采购入库/直接入库/领料入库，按验收单或生产计划关联） */
    private List<StockIn> materialStockIns = new ArrayList<>();
    /** 生产领料出库单（领料单关联的出库单） */
    private List<StockOut> materialStockOuts = new ArrayList<>();
    /** 成品出库对应的销售台账 */
    private List<SalesOrder> salesOrders = new ArrayList<>();
    private List<InspectionRequest> requests = new ArrayList<>();
    private List<SamplingRecord> samplings = new ArrayList<>();
    private List<InspectionRecord> inspections = new ArrayList<>();
    private ReleaseReview release;
    private List<DeviationRecord> deviations = new ArrayList<>();
    private List<WorkOrderProcessExecution> executions = new ArrayList<>();
    private List<WorkOrderProcessCheck> processChecks = new ArrayList<>();

    /** 工序类型ID -> 要求（品种工序模板 keyProcessParams） */
    private Map<Long, String> processRequirements;
    /** 工序类型ID -> 工序名称 */
    private Map<Long, String> processNames;
    /** 房间ID -> 房间信息 */
    private Map<Integer, RoomInfo> rooms;

    /** 采购订单ID -> 明细 */
    private Map<Long, List<com.tonghui.erp.Data.Entity.PurchaseOrderItems>> purchaseItems;
    /** 采购明细物料ID -> 物料主数据名称 */
    private Map<Long, String> purchaseMaterialNames;
    /** 领料单ID -> 明细 */
    private Map<Long, List<com.tonghui.erp.Data.Entity.ProductionPickDetail>> pickDetails;
    /** 成品入库单ID -> 明细 */
    private Map<Long, List<com.tonghui.erp.Data.Entity.StockInDetail>> stockInDetails;
    /** 成品出库单ID -> 明细 */
    private Map<Long, List<com.tonghui.erp.Data.Entity.StockOutDetail>> stockOutDetails;
    /** 物料入库单ID -> 明细 */
    private Map<Long, List<com.tonghui.erp.Data.Entity.StockInDetail>> materialStockInDetails;
    /** 生产领料出库单ID -> 明细 */
    private Map<Long, List<com.tonghui.erp.Data.Entity.StockOutDetail>> materialStockOutDetails;
}
