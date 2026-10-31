package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.ProductionPlanWithRecordsDto;
import com.tonghui.erp.Common.Dto.Dashboard.*;
import com.tonghui.erp.Common.Dto.System.DashboardExpiryConfigDto;
import com.tonghui.erp.Common.utils.DashboardCalcUtils;
import com.tonghui.erp.Data.Entity.*;
import com.tonghui.erp.Data.mapper.ProductionUnitMapper;
import com.tonghui.erp.Service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 首页仪表盘控制器
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────────────────┬────────┬─────────────────────────────────────┐
 * │ #  │ 接口                                         │ 方法   │ 说明                                │
 * ├────┼──────────────────────────────────────────────┼────────┼─────────────────────────────────────┤
 * │ 1  │ /api/dashboard/summary                       │ GET   │ 获取首页汇总数据                    │
 * │ 2  │ /api/dashboard/metrics                       │ GET   │ 核心指标卡片（成本三卡 + 生产三卡）  │
 * │ 3  │ /api/dashboard/metrics/details               │ GET   │ 卡片明细（全量不分页，兼容入口）     │
 * │ 4  │ /api/dashboard/metrics/details/estimated-cost│ GET   │ 预估成本明细（totals 强类型）        │
 * │ 5  │ /api/dashboard/metrics/details/actual-cost   │ GET   │ 实际成本明细（totals 强类型）        │
 * │ 6  │ /api/dashboard/metrics/details/output-value  │ GET   │ 预估产值明细（totals 强类型）        │
 * │ 7  │ /api/dashboard/metrics/details/total-plan    │ GET   │ 总计划量明细（totals 强类型）        │
 * │ 8  │ /api/dashboard/metrics/details/delivery      │ GET   │ 总交付量明细（totals 强类型）        │
 * │ 9  │ /api/dashboard/metrics/details/pending       │ GET   │ 待生产明细（totals 强类型）          │
 * │ 10 │ /api/dashboard/todos                         │ GET   │ 待办事项列表                        │
 * │ 11 │ /api/dashboard/order-tracking                │ GET   │ 订单跟踪看板                        │
 * │ 12 │ /api/dashboard/charts                        │ GET   │ 图表数据                            │
 * │ 13 │ /api/dashboard/material-funds/details        │ GET   │ 原辅包物料级明细                    │
 * │ 14 │ /api/dashboard/details/tree                  │ GET   │ 明细下钻（月份→制剂→批次）           │
 * └────┴──────────────────────────────────────────────┴────────┴─────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController extends BaseController {

    /**
     * 日期时间格式化器（yyyy-MM-dd HH:mm:ss）
     */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 原辅包资金明细支持的查询类型
     */
    private static final List<String> MATERIAL_FUND_TYPES = List.of("inbound", "outbound", "stock");

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /** 成本核算统计服务 */
    @Autowired
    private DashboardCostService dashboardCostService;

    /** 生产统计服务 */
    @Autowired
    private DashboardProductionService dashboardProductionService;

    /** 原辅包资金统计服务 */
    @Autowired
    private DashboardMaterialFundService dashboardMaterialFundService;

    /** 明细下钻服务（月份 → 制剂 → 批次） */
    @Autowired
    private DashboardTreeService dashboardTreeService;

    @Autowired
    private ProductionPlanService productionPlanService;

    @Autowired
    private StockService stockService;

    @Autowired
    private ApprovalInstanceService approvalInstanceService;

    @Autowired
    private EquipmentMaintenanceService equipmentMaintenanceService;

    @Autowired
    private PersonnelFileService personnelFileService;

    @Autowired
    private StockInService stockInService;

    @Autowired
    private DisinfectionRecordService disinfectionRecordService;

    @Autowired
    private PreparationService preparationService;

    @Autowired
    private PersonnelCertificateService personnelCertificateService;

    @Autowired
    private EquipmentService equipmentService;

    @Autowired
    private RoomInfoService roomInfoService;

    @Autowired
    private WorkOrderService workOrderService;

    @Autowired
    private PurchaseOrdersService purchaseOrdersService;

    @Autowired
    private AcceptanceOrderService acceptanceOrderService;

    @Autowired
    private StockOutService stockOutService;

    @Autowired
    private OrganizationService organizationService;

    /** 生产单位数据访问层，用于库存预警仓库名称映射 */
    @Autowired
    private ProductionUnitMapper productionUnitMapper;

    /** 系统配置服务，用于读取首页到期提醒时间范围等配置 */
    @Autowired
    private SystemConfigService systemConfigService;

    /** 留样记录服务，用于留样到期提醒 */
    @Autowired
    private RetainedSampleService retainedSampleService;

    // endregion

    // region 汇总和统计接口
    // ===================================
    // 汇总和统计接口
    // ===================================

    /**
     * 获取首页汇总数据（保留旧接口兼容）
     *
     * 示例请求：
     * GET /api/dashboard/summary
     *
     * @return 汇总数据，包含生产统计、库存预警、审批统计等
     */
// @GetMapping("/summary")
    public ApiResponse<DashboardSummaryDto> getSummary() {
        try {
            DashboardSummaryDto summary = new DashboardSummaryDto();

            ProductionStatsDto productionStats = new ProductionStatsDto();
            productionStats.setTotalPlans(productionPlanService.count());
            // 生产计划状态已落库（待生产/生产中/已完成），直接按状态列统计
            // 进行中：生产中
            productionStats.setInProgress(productionPlanService.count(
                new QueryWrapper<ProductionPlan>().eq("current_status", "生产中")));
            // 已完成：已完成
            productionStats.setCompleted(productionPlanService.count(
                new QueryWrapper<ProductionPlan>().eq("current_status", "已完成")));
            // 待处理：待生产
            productionStats.setPending(productionPlanService.count(
                new QueryWrapper<ProductionPlan>().eq("current_status", "待生产")));
            summary.setProductionStats(productionStats);

            StockWarningStatsDto stockWarnings = new StockWarningStatsDto();
            List<Stock> allStocks = stockService.list(
                new QueryWrapper<Stock>().isNotNull("min_quantity").gt("quantity", 0));
            long lowStock = allStocks.stream()
                .filter(s -> s.getQuantity() != null && s.getMinQuantity() != null
                    && s.getQuantity().compareTo(s.getMinQuantity()) <= 0)
                .count();
            LocalDate today = LocalDate.now();
            int stockWarningDays = systemConfigService.getInt(SystemConfigService.KEY_STOCK_DAYS,
                SystemConfigService.DEFAULT_EXPIRY_DAYS);
            long expiringSoon = stockService.count(
                new QueryWrapper<Stock>()
                    .gt("quantity", 0)
                    .ge("expiry_date", today)
                    .le("expiry_date", today.plusDays(stockWarningDays)));
            long expired = stockService.count(
                new QueryWrapper<Stock>().gt("quantity", 0).lt("expiry_date", today));
            stockWarnings.setLowStock(lowStock);
            stockWarnings.setExpiringSoon(expiringSoon);
            stockWarnings.setExpired(expired);
            summary.setStockWarnings(stockWarnings);

            ApprovalStatsDto approvalStats = new ApprovalStatsDto();
            approvalStats.setPendingApproval(approvalInstanceService.count(
                new QueryWrapper<ApprovalInstance>().eq("status", "PENDING")));
            approvalStats.setMyPending(0);
            summary.setApprovalStats(approvalStats);

            return success(summary);
        } catch (Exception e) {
            return exception(e, "操作");
        }
    }

    /**
     * 核心指标卡片数据
     *
     * 示例请求：
     * GET /api/dashboard/metrics?startMonth=2026-01&endMonth=2026-06
     *
     * @param startMonth 起始月份（格式：2026-01）
     * @param endMonth   结束月份（格式：2026-06）
     * @return 核心指标数据，包含成本核算三卡与生产三卡
     */
    @GetMapping("/metrics")
    public ApiResponse<DashboardMetricsDto> getMetrics(
            @RequestParam(required = false) String startMonth,
            @RequestParam(required = false) String endMonth) {
        try {
            DashboardMetricsDto metrics = new DashboardMetricsDto();

            // 查询区间
            DashboardMetricsDto.RangeDto range = new DashboardMetricsDto.RangeDto();
            range.setStartMonth(startMonth);
            range.setEndMonth(endMonth);
            range.setMonths(DashboardCalcUtils.expandMonths(startMonth, endMonth));
            metrics.setRange(range);

            // 成本核算三卡
            metrics.setCost(dashboardCostService.getCostMetrics(startMonth, endMonth));

            // 生产三卡
            metrics.setProduction(dashboardProductionService.getProductionMetrics(startMonth, endMonth));

            return success(metrics);
        } catch (Exception e) {
            return exception(e, "操作");
        }
    }

    /**
     * 卡片明细（全量，不分页）——兼容入口
     *
     * 注意：本接口为一张接口服务六张卡片，totals 只能声明为通配符类型，
     * 接口文档工具无法展开其字段（会显示为 null）；如需完整结构，
     * 请使用下方六个 /metrics/details/xxx 具体接口。
     *
     * 示例请求：
     * GET /api/dashboard/metrics/details?card=estimatedCost&startMonth=2026-05&endMonth=2026-10
     * GET /api/dashboard/metrics/details?card=actualCost&startMonth=2026-05&endMonth=2026-10
     * GET /api/dashboard/metrics/details?card=outputValue&startMonth=2026-05&endMonth=2026-10
     * GET /api/dashboard/metrics/details?card=totalPlan&startMonth=2026-05&endMonth=2026-10
     * GET /api/dashboard/metrics/details?card=delivery&startMonth=2026-05&endMonth=2026-10
     * GET /api/dashboard/metrics/details?card=pending&startMonth=2026-05&endMonth=2026-10
     *
     * @param card       卡片标识：estimatedCost / actualCost / outputValue / totalPlan / delivery / pending
     * @param startMonth 起始月份（格式：2026-05）
     * @param endMonth   结束月份（格式：2026-10）
     * @return 对应卡片的汇总与全量明细
     */
    @GetMapping("/metrics/details")
    public ApiResponse<CardDetailDto<?, DashboardDetailRowDto>> getMetricsDetails(
            @RequestParam String card,
            @RequestParam(required = false) String startMonth,
            @RequestParam(required = false) String endMonth) {
        try {
            if (!StringUtils.hasText(card)) {
                return error("card 不能为空");
            }
            // 各卡片 totals 类型不同，统一收敛到通配符类型；只调用一次 success，便于接口文档插件解析返回结构
            CardDetailDto<?, DashboardDetailRowDto> detail;
            if ("estimatedCost".equals(card)) {
                detail = dashboardCostService.getEstimatedCostDetails(startMonth, endMonth);
            } else if ("actualCost".equals(card)) {
                detail = dashboardCostService.getActualCostDetails(startMonth, endMonth);
            } else if ("outputValue".equals(card)) {
                detail = dashboardCostService.getOutputValueDetails(startMonth, endMonth);
            } else if ("totalPlan".equals(card)) {
                detail = dashboardProductionService.getTotalPlanDetails(startMonth, endMonth);
            } else if ("delivery".equals(card)) {
                detail = dashboardProductionService.getDeliveryDetails(startMonth, endMonth);
            } else if ("pending".equals(card)) {
                detail = dashboardProductionService.getPendingDetails(startMonth, endMonth);
            } else {
                return error("不支持的 card 类型：" + card);
            }
            return success(detail);
        } catch (Exception e) {
            return exception(e, "操作");
        }
    }

    /**
     * 预估成本明细（工单粒度）
     *
     * 示例请求：
     * GET /api/dashboard/metrics/details/estimated-cost?startMonth=2026-08&endMonth=2026-10
     *
     * totals 结构与 /api/dashboard/metrics 的 cost.estimatedCost 完全一致。
     *
     * @param startMonth 起始月份（格式：2026-08，可空）
     * @param endMonth   结束月份（格式：2026-10，可空）
     * @return 汇总（totals）+ 全量明细（details，不分页）
     */
    @GetMapping("/metrics/details/estimated-cost")
    public ApiResponse<CardDetailDto<DashboardMetricsDto.EstimatedCostCardDto, DashboardDetailRowDto>>
    getEstimatedCostDetails(@RequestParam(required = false) String startMonth,
                            @RequestParam(required = false) String endMonth) {
        try {
            return success(dashboardCostService.getEstimatedCostDetails(startMonth, endMonth));
        } catch (Exception e) {
            return exception(e, "操作");
        }
    }

    /**
     * 实际成本明细（月份 + 工单，含未关联制剂 / 未分摊水电气特殊行）
     *
     * 示例请求：
     * GET /api/dashboard/metrics/details/actual-cost?startMonth=2026-08&endMonth=2026-10
     *
     * totals 结构与 /api/dashboard/metrics 的 cost.actualCost 完全一致。
     *
     * @param startMonth 起始月份（格式：2026-08，可空）
     * @param endMonth   结束月份（格式：2026-10，可空）
     * @return 汇总（totals）+ 全量明细（details，不分页）
     */
    @GetMapping("/metrics/details/actual-cost")
    public ApiResponse<CardDetailDto<DashboardMetricsDto.ActualCostCardDto, DashboardDetailRowDto>>
    getActualCostDetails(@RequestParam(required = false) String startMonth,
                         @RequestParam(required = false) String endMonth) {
        try {
            return success(dashboardCostService.getActualCostDetails(startMonth, endMonth));
        } catch (Exception e) {
            return exception(e, "操作");
        }
    }

    /**
     * 预估产值明细（工单粒度）
     *
     * 示例请求：
     * GET /api/dashboard/metrics/details/output-value?startMonth=2026-08&endMonth=2026-10
     *
     * totals 结构与 /api/dashboard/metrics 的 cost.outputValue 完全一致。
     *
     * @param startMonth 起始月份（格式：2026-08，可空）
     * @param endMonth   结束月份（格式：2026-10，可空）
     * @return 汇总（totals）+ 全量明细（details，不分页）
     */
    @GetMapping("/metrics/details/output-value")
    public ApiResponse<CardDetailDto<DashboardMetricsDto.OutputValueCardDto, DashboardDetailRowDto>>
    getOutputValueDetails(@RequestParam(required = false) String startMonth,
                          @RequestParam(required = false) String endMonth) {
        try {
            return success(dashboardCostService.getOutputValueDetails(startMonth, endMonth));
        } catch (Exception e) {
            return exception(e, "操作");
        }
    }

    /**
     * 总计划量明细（生产计划单粒度）
     *
     * 示例请求：
     * GET /api/dashboard/metrics/details/total-plan?startMonth=2026-08&endMonth=2026-10
     *
     * totals 结构与 /api/dashboard/metrics 的 production.totalPlan 完全一致。
     *
     * @param startMonth 起始月份（格式：2026-08，可空）
     * @param endMonth   结束月份（格式：2026-10，可空）
     * @return 汇总（totals）+ 全量明细（details，不分页）
     */
    @GetMapping("/metrics/details/total-plan")
    public ApiResponse<CardDetailDto<DashboardMetricsDto.TotalPlanCardDto, DashboardDetailRowDto>>
    getTotalPlanDetails(@RequestParam(required = false) String startMonth,
                        @RequestParam(required = false) String endMonth) {
        try {
            return success(dashboardProductionService.getTotalPlanDetails(startMonth, endMonth));
        } catch (Exception e) {
            return exception(e, "操作");
        }
    }

    /**
     * 总交付量明细（工单粒度）
     *
     * 示例请求：
     * GET /api/dashboard/metrics/details/delivery?startMonth=2026-08&endMonth=2026-10
     *
     * totals 结构与 /api/dashboard/metrics 的 production.delivery 完全一致。
     *
     * @param startMonth 起始月份（格式：2026-08，可空）
     * @param endMonth   结束月份（格式：2026-10，可空）
     * @return 汇总（totals）+ 全量明细（details，不分页）
     */
    @GetMapping("/metrics/details/delivery")
    public ApiResponse<CardDetailDto<DashboardMetricsDto.DeliveryCardDto, DashboardDetailRowDto>>
    getDeliveryDetails(@RequestParam(required = false) String startMonth,
                       @RequestParam(required = false) String endMonth) {
        try {
            return success(dashboardProductionService.getDeliveryDetails(startMonth, endMonth));
        } catch (Exception e) {
            return exception(e, "操作");
        }
    }

    /**
     * 待生产明细（待生产计划单粒度）
     *
     * 示例请求：
     * GET /api/dashboard/metrics/details/pending?startMonth=2026-08&endMonth=2026-10
     *
     * totals 结构与 /api/dashboard/metrics 的 production.pending 完全一致。
     *
     * @param startMonth 起始月份（格式：2026-08，可空）
     * @param endMonth   结束月份（格式：2026-10，可空）
     * @return 汇总（totals）+ 全量明细（details，不分页）
     */
    @GetMapping("/metrics/details/pending")
    public ApiResponse<CardDetailDto<DashboardMetricsDto.PendingCardDto, DashboardDetailRowDto>>
    getPendingDetails(@RequestParam(required = false) String startMonth,
                      @RequestParam(required = false) String endMonth) {
        try {
            return success(dashboardProductionService.getPendingDetails(startMonth, endMonth));
        } catch (Exception e) {
            return exception(e, "操作");
        }
    }

    /**
     * 成本与生产明细树（月份 → 制剂 → 批次，一次返回整棵树）
     *
     * 示例请求：
     * GET /api/dashboard/details/tree?startMonth=2026-08&endMonth=2026-10
     *
     * 各层级金额单位均为「元」（前端按万元展示时除以 10000），交付量单位「件」。
     * 月份级合计与 /api/dashboard/metrics 对应卡片数值一致，可直接作为合计行使用。
     *
     * @param startMonth 起始月份（格式：2026-08，可空）
     * @param endMonth   结束月份（格式：2026-10，可空）
     * @return 区间合计 + 月份节点 + 制剂节点 + 批次节点
     */
    @GetMapping("/details/tree")
    public ApiResponse<DashboardTreeDto> getDetailTree(
            @RequestParam(required = false) String startMonth,
            @RequestParam(required = false) String endMonth) {
        try {
            return success(dashboardTreeService.getDetailTree(startMonth, endMonth));
        } catch (Exception e) {
            return exception(e, "操作");
        }
    }

    // endregion

    // region 待办事项接口
    // ===================================
    // 待办事项接口
    // ===================================

    /**
     * 待办事项列表
     *
     * 示例请求：
     * GET /api/dashboard/todos
     *
     * @return 待办事项列表，包含设备维保、库存预警、人员健康证、环境管理等提醒
     */
    @GetMapping("/todos")
    public ApiResponse<TodoListDto> getTodos() {
        try {
            List<TodoItemDto> allTodos = new ArrayList<>();
            Map<String, Long> typeCounts = new LinkedHashMap<>();
            LocalDate today = LocalDate.now();

            // 读取各类到期提醒时间范围配置（系统配置决定，默认30天）
            DashboardExpiryConfigDto expiryCfg = systemConfigService.getDashboardExpiryConfig();

            // 1. 设备维保提醒
            List<EquipmentMaintenance> upcomingMaintenance = equipmentMaintenanceService.findUpcomingMaintenance(expiryCfg.getEquipmentDays());
            // 批量查询设备名称
            Set<Long> equipmentIds = upcomingMaintenance.stream()
                .map(EquipmentMaintenance::getEquipmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            Map<Long, String> equipmentNameMap = new HashMap<>();
            if (!equipmentIds.isEmpty()) {
                equipmentService.listByIds(equipmentIds).forEach(e ->
                    equipmentNameMap.put(e.getEquipmentId().longValue(), e.getEquipmentName()));
            }
            for (EquipmentMaintenance m : upcomingMaintenance) {
                TodoItemDto todo = new TodoItemDto();
                todo.setId(m.getMaintenanceId());
                todo.setTodoType("设备维保");
                String equipmentName = equipmentNameMap.getOrDefault(m.getEquipmentId(), "设备" + m.getEquipmentId());
                if (m.getNextMaintenanceDate() != null) {
                    long days = java.time.temporal.ChronoUnit.DAYS.between(today, m.getNextMaintenanceDate());
                    if (days < 0) {
                        todo.setContent(equipmentName + "已超期维保" + Math.abs(days) + "天！");
                    } else {
                        todo.setContent(equipmentName + "距离下次维保还有" + days + "天");
                    }
                    todo.setDueDate(m.getNextMaintenanceDate().toString());
                }
                todo.setSourceModule("设备管理");
                todo.setLink("设备.html?openMaintenance=" + m.getEquipmentId());
                allTodos.add(todo);
            }
            typeCounts.put("设备维保", (long) upcomingMaintenance.size());

            // 2. 库存预警
            List<Stock> expiringStocks = stockService.list(
                new QueryWrapper<Stock>()
                    .eq("is_deleted", 0)
                    .gt("quantity", 0)
                    .isNotNull("expiry_date")
                    .le("expiry_date", today.plusDays(expiryCfg.getStockDays()))
                    .orderByAsc("expiry_date"));

            // 批量加载仓库名称映射
            List<Long> warningWarehouseIds = expiringStocks.stream()
                    .map(Stock::getProdUnitId)
                    .filter(id -> id != null)
                    .distinct()
                    .collect(Collectors.toList());
            Map<Long, String> warningWarehouseNameMap = new HashMap<>();
            if (!warningWarehouseIds.isEmpty()) {
                warningWarehouseNameMap = productionUnitMapper.selectList(
                        new QueryWrapper<ProductionUnit>().in("prod_unit_id", warningWarehouseIds))
                    .stream()
                    .collect(Collectors.toMap(
                        ProductionUnit::getProdUnitId,
                        ProductionUnit::getProdUnitName,
                        (a, b) -> a));
            }

            for (Stock s : expiringStocks) {
                TodoItemDto todo = new TodoItemDto();
                todo.setId(s.getStockId());
                todo.setTodoType("库存预警");
                long days = java.time.temporal.ChronoUnit.DAYS.between(today, s.getExpiryDate());
                String warehouseName = warningWarehouseNameMap.getOrDefault(s.getProdUnitId(), "未知仓库");
                String itemName = s.getItemName() != null ? s.getItemName() : "未知药品";
                String batchInfo = s.getBatchNumber() != null ? s.getBatchNumber() : "未知批号";
                if (days < 0) {
                    todo.setContent(warehouseName + "-" + itemName + "-" + batchInfo + "已过期" + Math.abs(days) + "天");
                } else {
                    todo.setContent(warehouseName + "-" + itemName + "-" + batchInfo + "将在" + days + "天后过期");
                }
                todo.setDueDate(s.getExpiryDate().toString());
                todo.setSourceModule("库存管理");
                todo.setLink("库存有效期预警.html");
                allTodos.add(todo);
            }
            typeCounts.put("库存预警", (long) expiringStocks.size());

            // 2.5 待入库
            List<StockIn> pendingStockIns = stockInService.list(
                new QueryWrapper<StockIn>()
                    .eq("is_deleted", 0)
                    .eq("in_status", "草稿")
                    .orderByDesc("created_time"));
            for (StockIn si : pendingStockIns) {
                TodoItemDto todo = new TodoItemDto();
                todo.setId(si.getInId());
                todo.setTodoType("待入库");
                todo.setContent((si.getInCode() != null ? si.getInCode() : "入库单" + si.getInId()) + "待审核");
                todo.setDueDate(si.getInDate() != null ? si.getInDate().format(DATE_TIME_FORMATTER) : "");
                todo.setSourceModule("库存管理");
                todo.setLink("入库管理.html");
                allTodos.add(todo);
            }
            typeCounts.put("待入库", (long) pendingStockIns.size());

            // 2.6 待确认
            List<StockIn> unconfirmedStockIns = stockInService.list(
                new QueryWrapper<StockIn>()
                    .eq("is_deleted", 0)
                    .eq("in_status", "已到货")
                    .orderByDesc("created_time"));
            for (StockIn si : unconfirmedStockIns) {
                TodoItemDto todo = new TodoItemDto();
                todo.setId(si.getInId());
                todo.setTodoType("待确认");
                todo.setContent((si.getInCode() != null ? si.getInCode() : "入库单" + si.getInId()) + "已到货，待确认入库");
                todo.setDueDate(si.getInDate() != null ? si.getInDate().format(DATE_TIME_FORMATTER) : "");
                todo.setSourceModule("库存管理");
                todo.setLink("入库管理.html");
                allTodos.add(todo);
            }
            typeCounts.put("待确认", (long) unconfirmedStockIns.size());

            // 3. 人员健康证到期
            List<PersonnelFile> expiringCerts = personnelFileService.findExpiringHealthCerts(expiryCfg.getHealthCertDays());
            for (PersonnelFile p : expiringCerts) {
                TodoItemDto todo = new TodoItemDto();
                todo.setId(p.getPersonnelFileId());
                todo.setTodoType("人员管理");
                String name = p.getName() != null ? p.getName() : "人员" + p.getPersonnelFileId();
                if (p.getHealthCertExpire() != null) {
                    long days = java.time.temporal.ChronoUnit.DAYS.between(today, p.getHealthCertExpire());
                    if (days < 0) {
                        todo.setContent(name + "健康证已过期" + Math.abs(days) + "天");
                    } else {
                        todo.setContent(name + "健康证还有" + days + "天到期");
                    }
                    todo.setDueDate(p.getHealthCertExpire().toString());
                }
                todo.setSourceModule("人员管理");
                todo.setLink("人员档案.html");
                allTodos.add(todo);
            }

            // 3.5 人员证书到期（从证书子表查询）
            List<PersonnelCertificate> expiringCertificates = personnelCertificateService.findExpiringCertificates(expiryCfg.getPersonnelCertDays());
            // 批量查询人员名称
            Set<Long> certPersonnelIds = expiringCertificates.stream()
                .map(PersonnelCertificate::getPersonnelFileId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            Map<Long, String> certPersonnelNameMap = new HashMap<>();
            if (!certPersonnelIds.isEmpty()) {
                personnelFileService.listByIds(certPersonnelIds).forEach(p ->
                    certPersonnelNameMap.put(p.getPersonnelFileId(), p.getName()));
            }
            for (PersonnelCertificate cert : expiringCertificates) {
                TodoItemDto todo = new TodoItemDto();
                todo.setId(cert.getCertificateId());
                todo.setTodoType("人员管理");
                String personName = certPersonnelNameMap.getOrDefault(cert.getPersonnelFileId(), "人员" + cert.getPersonnelFileId());
                String certName = cert.getCertificateName() != null ? cert.getCertificateName() : "证书";
                if (cert.getExpiryDate() != null) {
                    long days = java.time.temporal.ChronoUnit.DAYS.between(today, cert.getExpiryDate());
                    if (days < 0) {
                        todo.setContent(personName + "的" + certName + "已过期" + Math.abs(days) + "天");
                    } else {
                        todo.setContent(personName + "的" + certName + "还有" + days + "天到期");
                    }
                    todo.setDueDate(cert.getExpiryDate().toString());
                }
                todo.setSourceModule("人员管理");
                todo.setLink("人员档案.html");
                allTodos.add(todo);
            }
            // 更新人员管理计数（健康证 + 证书子表）
            typeCounts.put("人员管理", (long) expiringCerts.size() + expiringCertificates.size());

            // 5. 环境管理（消毒到期提醒）
            List<DisinfectionRecord> upcomingDisinfection = disinfectionRecordService.findUpcomingDisinfection(expiryCfg.getDisinfectionDays());
            // 批量查询房间名称
            Set<Integer> roomIds = upcomingDisinfection.stream()
                .map(DisinfectionRecord::getRoomId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            Map<Integer, String> roomNameMap = new HashMap<>();
            if (!roomIds.isEmpty()) {
                roomInfoService.listByIds(roomIds).forEach(r ->
                    roomNameMap.put(r.getRoomId(), r.getRoomName()));
            }
            for (DisinfectionRecord d : upcomingDisinfection) {
                TodoItemDto todo = new TodoItemDto();
                todo.setId(d.getId());
                todo.setTodoType("环境管理");
                String roomName = roomNameMap.getOrDefault(d.getRoomId(), "车间" + d.getRoomId());
                if (d.getNextDisinfectionDate() != null) {
                    long days = java.time.temporal.ChronoUnit.DAYS.between(today, d.getNextDisinfectionDate());
                    if (days < 0) {
                        todo.setContent(roomName + "已超期消毒" + Math.abs(days) + "天！");
                    } else {
                        todo.setContent(roomName + "距离下次消毒还有" + days + "天");
                    }
                    todo.setDueDate(d.getNextDisinfectionDate().toString());
                }
                todo.setSourceModule("环境管理");
                todo.setLink("车间详情.html?id=" + d.getRoomId());
                allTodos.add(todo);
            }
            typeCounts.put("环境管理", (long) upcomingDisinfection.size());

            // 6. 低库存预警（库存数量低于安全库存，全员可见）
            List<Stock> lowStocks = stockService.list(
                new QueryWrapper<Stock>()
                    .eq("is_deleted", 0)
                    .gt("quantity", 0)
                    .isNotNull("min_quantity")
                    .apply("quantity <= min_quantity")
                    .orderByDesc("updated_time"));
            for (Stock s : lowStocks) {
                TodoItemDto todo = new TodoItemDto();
                todo.setId(s.getStockId());
                todo.setTodoType("低库存");
                todo.setContent(s.getItemName() + "库存" + s.getQuantity() + s.getUnitName()
                    + "，低于安全库存" + s.getMinQuantity());
                todo.setDueDate(s.getUpdatedTime() != null ? s.getUpdatedTime().format(DATE_TIME_FORMATTER) : "");
                todo.setSourceModule("库存管理");
                todo.setLink("/stock_info");
                allTodos.add(todo);
            }
            typeCounts.put("低库存", (long) lowStocks.size());

            // 7. 采购订单待采购（审批通过生成但未执行的采购订单）
            List<PurchaseOrders> pendingPurchases = purchaseOrdersService.list(
                new QueryWrapper<PurchaseOrders>()
                    .eq("is_deleted", 0)
                    .eq("status", "待采购")
                    .orderByDesc("created_time"));
            for (PurchaseOrders po : pendingPurchases) {
                TodoItemDto todo = new TodoItemDto();
                todo.setId(po.getId());
                todo.setTodoType("采购订单");
                todo.setContent((po.getPurchaseNumber() != null ? po.getPurchaseNumber() : "采购订单" + po.getId()) + "待采购");
                todo.setDueDate(po.getProcessingDate() != null ? po.getProcessingDate().toString() : "");
                todo.setSourceModule("采购管理");
                todo.setLink("/inbound_info");
                allTodos.add(todo);
            }
            typeCounts.put("采购订单", (long) pendingPurchases.size());

            // 8. 验收流程待办（运输中/验收中）
            List<AcceptanceOrder> pendingAcceptances = acceptanceOrderService.list(
                new QueryWrapper<AcceptanceOrder>()
                    .eq("is_deleted", 0)
                    .in("status", Arrays.asList("运输中", "验收中"))
                    .orderByDesc("created_time"));
            Map<String, String> acceptanceContentMap = new HashMap<>();
            acceptanceContentMap.put("运输中", "已发货，待确认到货");
            acceptanceContentMap.put("验收中", "验收处理中");
            for (AcceptanceOrder a : pendingAcceptances) {
                TodoItemDto todo = new TodoItemDto();
                todo.setId(a.getAcceptanceId());
                todo.setTodoType("验收");
                todo.setContent((a.getAcceptanceCode() != null ? a.getAcceptanceCode() : "验收单" + a.getAcceptanceId())
                    + acceptanceContentMap.getOrDefault(a.getStatus(), "待处理"));
                todo.setDueDate("");
                todo.setSourceModule("验收管理");
                todo.setLink("/inbound_info");
                allTodos.add(todo);
            }
            typeCounts.put("验收", (long) pendingAcceptances.size());

            // 9. 出库单待确认（草稿状态的出库单）
            List<StockOut> pendingStockOuts = stockOutService.list(
                new QueryWrapper<StockOut>()
                    .eq("is_deleted", 0)
                    .eq("out_status", "草稿")
                    .orderByDesc("created_time"));
            for (StockOut so : pendingStockOuts) {
                TodoItemDto todo = new TodoItemDto();
                todo.setId(so.getOutId());
                todo.setTodoType("出库单");
                todo.setContent((so.getOutCode() != null ? so.getOutCode() : "出库单" + so.getOutId()) + "待确认出库");
                todo.setDueDate(so.getOutDate() != null ? so.getOutDate().format(DATE_TIME_FORMATTER) : "");
                todo.setSourceModule("库存管理");
                todo.setLink("/warehouse_management/stock");
                allTodos.add(todo);
            }
            typeCounts.put("出库单", (long) pendingStockOuts.size());

            // 10. 机构许可证到期（配置提醒天数内到期或已过期）
            List<Organization> expiringOrgs = organizationService.list(
                new QueryWrapper<Organization>()
                    .eq("is_deleted", 0)
                    .isNotNull("expiry_date")
                    .le("expiry_date", today.plusDays(expiryCfg.getOrganizationDays()))
                    .orderByAsc("expiry_date"));
            for (Organization org : expiringOrgs) {
                TodoItemDto todo = new TodoItemDto();
                todo.setId(org.getId());
                todo.setTodoType("机构证照");
                long days = java.time.temporal.ChronoUnit.DAYS.between(today, org.getExpiryDate());
                if (days < 0) {
                    todo.setContent(org.getOrgName() + "许可证已过期" + Math.abs(days) + "天");
                } else {
                    todo.setContent(org.getOrgName() + "许可证还有" + days + "天到期");
                }
                todo.setDueDate(org.getExpiryDate().toString());
                todo.setSourceModule("机构管理");
                todo.setLink("/organization");
                allTodos.add(todo);
            }
            typeCounts.put("机构证照", (long) expiringOrgs.size());

            // 11. 留样到期提醒（留样中且留样期限临近/已到期）
            List<RetainedSample> expiringSamples = retainedSampleService.list(
                new QueryWrapper<RetainedSample>()
                    .eq("is_deleted", 0)
                    .eq("status", "留样中")
                    .isNotNull("expiry_date")
                    .le("expiry_date", today.plusDays(expiryCfg.getRetainedSampleDays()))
                    .orderByAsc("expiry_date"));
            for (RetainedSample rs : expiringSamples) {
                TodoItemDto todo = new TodoItemDto();
                todo.setId(rs.getId());
                todo.setTodoType("留样到期");
                long days = java.time.temporal.ChronoUnit.DAYS.between(today, rs.getExpiryDate());
                String sampleName = (rs.getMaterialName() != null ? rs.getMaterialName() : "留样")
                        + (rs.getBatchNo() != null ? "-" + rs.getBatchNo() : "");
                if (days < 0) {
                    todo.setContent(sampleName + "留样已超期" + Math.abs(days) + "天");
                } else {
                    todo.setContent(sampleName + "留样还有" + days + "天到期");
                }
                todo.setDueDate(rs.getExpiryDate().toString());
                todo.setSourceModule("质量检验");
                todo.setLink("/retained_sample");
                allTodos.add(todo);
            }
            typeCounts.put("留样到期", (long) expiringSamples.size());

            // 12. 制剂批件过期提醒（启用且批件过期时间临近/已过期）
            List<Preparation> expiringApprovals = preparationService.list(
                new QueryWrapper<Preparation>()
                    .eq("is_deleted", 0)
                    .eq("status", 1)
                    .isNotNull("approval_expiry_date")
                    .le("approval_expiry_date", today.plusDays(expiryCfg.getApprovalDays()))
                    .orderByAsc("approval_expiry_date"));
            for (Preparation p : expiringApprovals) {
                TodoItemDto todo = new TodoItemDto();
                todo.setId(p.getPreparationId());
                todo.setTodoType("批件过期");
                long days = java.time.temporal.ChronoUnit.DAYS.between(today, p.getApprovalExpiryDate());
                String prepName = p.getPreparationName() != null ? p.getPreparationName() : "制剂" + p.getPreparationId();
                if (days < 0) {
                    todo.setContent(prepName + "批件已过期" + Math.abs(days) + "天");
                } else {
                    todo.setContent(prepName + "批件还有" + days + "天到期");
                }
                todo.setDueDate(p.getApprovalExpiryDate().toString());
                todo.setSourceModule("产品管理");
                todo.setLink("/preparation");
                allTodos.add(todo);
            }
            typeCounts.put("批件过期", (long) expiringApprovals.size());

            typeCounts.put("全部", (long) allTodos.size());

            TodoListDto result = new TodoListDto();
            result.setItems(allTodos);
            result.setTypeCounts(typeCounts);
            return success(result);
        } catch (Exception e) {
            return exception(e, "操作");
        }
    }

    // endregion

    // region 订单跟踪接口
    // ===================================
    // 订单跟踪接口
    // ===================================

    /**
     * 订单跟踪看板
     *
     * 示例请求：
     * GET /api/dashboard/order-tracking?startMonth=2026-01&endMonth=2026-06&status=生产中&pageIndex=0&pageSize=20
     *
     * @param startMonth 起始月份（格式：2026-01，可选）
     * @param endMonth   结束月份（格式：2026-06，可选）
     * @param status     订单状态（可选），如待生产、生产中、已完成
     * @param pageIndex  页码（从0开始）
     * @param pageSize   每页数量
     * @return 订单跟踪分页列表
     */
    @GetMapping("/order-tracking")
    public ApiResponse<PagedResult<OrderTrackingDto>> getOrderTracking(
            @RequestParam(required = false) String startMonth,
            @RequestParam(required = false) String endMonth,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "-1") int pageIndex,
            @RequestParam(defaultValue = "-1") int pageSize) {
        try {
            // -1 表示不分页，返回全部数据
            boolean returnAll = pageIndex == -1 || pageSize == -1;
            int safePageIndex = returnAll ? 0 : Math.max(0, pageIndex);
            int safePageSize = returnAll ? Integer.MAX_VALUE : (pageSize <= 0 ? 20 : Math.max(1, pageSize));

            // 构建创建时间范围
            LocalDateTime createdTimeStart = null;
            LocalDateTime createdTimeEnd = null;
            if (startMonth != null && !startMonth.isEmpty()) {
                createdTimeStart = LocalDate.parse(startMonth + "-01").atStartOfDay();
            }
            if (endMonth != null && !endMonth.isEmpty()) {
                LocalDate end = LocalDate.parse(endMonth + "-01").plusMonths(1).minusDays(1);
                createdTimeEnd = end.atTime(23, 59, 59);
            }

            // 构建状态过滤条件
            ProductionPlan planFilter = new ProductionPlan();
            if (status != null && !status.isEmpty()) {
                planFilter.setCurrentStatus(status);
            }

            // 调用分页查询，获取带工单关联的生产计划
            PagedResult<ProductionPlanWithRecordsDto> planResult = productionPlanService.searchWithDetails(
                    planFilter, null,
                    createdTimeStart, createdTimeEnd, null, null,
                    null, null, null, null,
                    null, null,
                    null, null, null, null,
                    null, null, null, null,
                    null, null, null,
                    safePageIndex, safePageSize);

            // 将 ProductionPlanWithRecordsDto 映射为 OrderTrackingDto，日期取最晚工单时间
            List<OrderTrackingDto> trackingList = planResult.getItems().stream().map(plan -> {
                OrderTrackingDto dto = new OrderTrackingDto();
                dto.setId(plan.getId().longValue());
                dto.setOrderName(plan.getPreparationName());
                dto.setQuantity(plan.getPlanQuantity() != null ? plan.getPlanQuantity() + "" : "");
                dto.setBatchNo(plan.getPlanNumber());
                dto.setHospital(plan.getUnitName());
                dto.setCurrentStatus(plan.getCurrentStatus());

                // 下单日期取计划创建时间
                if (plan.getCreatedTime() != null) {
                    dto.setOrderDate(plan.getCreatedTime().format(DateTimeFormatter.ofPattern("MM-dd")));
                }

                // 从关联工单中取最晚完成时间
                List<WorkOrder> workOrders = plan.getWorkOrders();
                if (workOrders != null && !workOrders.isEmpty()) {
                    // 生产日期：取最晚工单的 configCompleteTime（生产完成时间）
                    workOrders.stream()
                            .map(WorkOrder::getConfigCompleteTime)
                            .filter(Objects::nonNull)
                            .max(LocalDateTime::compareTo)
                            .ifPresent(time -> dto.setProductionDate(time.format(DateTimeFormatter.ofPattern("MM-dd"))));

                    // 检验日期：取最晚工单的 inspectionEnd（检验完成时间）
                    workOrders.stream()
                            .map(WorkOrder::getInspectionEnd)
                            .filter(Objects::nonNull)
                            .max(LocalDateTime::compareTo)
                            .ifPresent(time -> dto.setInspectionDate(time.format(DateTimeFormatter.ofPattern("MM-dd"))));

                    // 出库日期：取最晚工单的 outboundTime
                    workOrders.stream()
                            .map(WorkOrder::getOutboundTime)
                            .filter(Objects::nonNull)
                            .max(LocalDateTime::compareTo)
                            .ifPresent(time -> dto.setOutboundDate(time.format(DateTimeFormatter.ofPattern("MM-dd"))));

                    // 归档日期：取最晚工单的 archiveTime
                    workOrders.stream()
                            .map(WorkOrder::getArchiveTime)
                            .filter(Objects::nonNull)
                            .max(LocalDateTime::compareTo)
                            .ifPresent(time -> dto.setArchiveDate(time.format(DateTimeFormatter.ofPattern("MM-dd"))));
                }

                return dto;
            }).collect(Collectors.toList());

            // 组装分页结果
            PagedResult<OrderTrackingDto> result = new PagedResult<>();
            result.setItems(trackingList);
            result.setTotalCount(planResult.getTotalCount());
            result.setPageIndex(returnAll ? 0 : planResult.getPageIndex());
            result.setPageSize(returnAll ? 0 : planResult.getPageSize());

            return success(result);
        } catch (Exception e) {
            return exception(e, "操作");
        }
    }

    // endregion

    // region 图表数据接口
    // ===================================
    // 图表数据接口
    // ===================================
    /**
     * 首页图表数据
     *
     * 示例请求：
     * GET /api/dashboard/charts?startMonth=2026-05&endMonth=2026-10
     *
     * @param startMonth 起始月份（格式：2026-05，可选）
     * @param endMonth   结束月份（格式：2026-10，可选）
     * @return 图表数据：成本趋势、成本构成、原辅包资金、生产分析
     */
    @GetMapping("/charts")
    public ApiResponse<ChartDataDto> getCharts(
            @RequestParam(required = false) String startMonth,
            @RequestParam(required = false) String endMonth) {
        try {
            ChartDataDto chartData = new ChartDataDto();

            // 成本趋势与月度构成
            List<ChartDataDto.CostTrendItem> costTrend =
                dashboardCostService.getCostTrend(startMonth, endMonth);
            List<ChartDataDto.CostStructureItem> costStructure =
                dashboardCostService.getCostStructure(startMonth, endMonth);
            chartData.setCostTrend(costTrend);
            chartData.setCostStructure(costStructure);

            // 新增图表：批次成本偏差排行、按品种拆分的成本构成
            chartData.setBatchCostVarianceRank(
                dashboardCostService.getBatchCostVarianceRank(startMonth, endMonth));
            chartData.setCostStructureByPreparation(
                dashboardCostService.getCostStructureByPreparation(startMonth, endMonth));

            // 原辅包资金三项（入库 / 出库净额 / 当前库存）
            chartData.setMaterialFunds(dashboardMaterialFundService.getMaterialFunds(startMonth, endMonth));

            // 生产分析（计划 vs 交付 / 待生产 / 品种排名）
            ChartDataDto.ProductionCharts productionCharts =
                dashboardProductionService.getProductionCharts(startMonth, endMonth);
            chartData.setProduction(productionCharts);

            // 月份列表：查询区间优先，否则合并各图表实际存在的月份
            List<String> months = DashboardCalcUtils.expandMonths(startMonth, endMonth);
            if (months.isEmpty()) {
                List<String> trendMonths = new ArrayList<>();
                for (ChartDataDto.CostTrendItem item : costTrend) {
                    trendMonths.add(item.getMonth());
                }
                List<String> planMonths = new ArrayList<>();
                if (productionCharts.getPlanVsDelivery() != null) {
                    for (ChartDataDto.PlanVsDeliveryItem item : productionCharts.getPlanVsDelivery()) {
                        planMonths.add(item.getMonth());
                    }
                }
                List<String> fundMonths = new ArrayList<>();
                ChartDataDto.MaterialFunds funds = chartData.getMaterialFunds();
                if (funds != null) {
                    if (funds.getInbound() != null && funds.getInbound().getMonths() != null) {
                        for (ChartDataDto.MaterialFundMonth item : funds.getInbound().getMonths()) {
                            fundMonths.add(item.getMonth());
                        }
                    }
                    if (funds.getOutbound() != null && funds.getOutbound().getMonths() != null) {
                        for (ChartDataDto.MaterialFundMonth item : funds.getOutbound().getMonths()) {
                            fundMonths.add(item.getMonth());
                        }
                    }
                }
                months = DashboardCalcUtils.mergeMonths(trendMonths, planMonths, fundMonths);
            }
            chartData.setMonths(months);

            return success(chartData);
        } catch (Exception e) {
            return exception(e, "操作");
        }
    }

    /**
     * 原辅包物料级明细（点击色块下钻，全量不分页）
     *
     * 示例请求：
     * GET /api/dashboard/material-funds/details?type=inbound&category=原料&month=2026-08
     * GET /api/dashboard/material-funds/details?type=outbound&category=包材
     * GET /api/dashboard/material-funds/details?type=stock&category=原料
     *
     * @param type     类型：inbound 入库 / outbound 出库净额 / stock 当前库存
     * @param category 分类：原料 / 辅料 / 包材（可空表示全部）
     * @param month    月份（格式：2026-08，仅 inbound/outbound 生效）
     * @return 物料级明细
     */
    @GetMapping("/material-funds/details")
    public ApiResponse<DashboardMaterialFundDetailDto> getMaterialFundDetails(
            @RequestParam(required = false, defaultValue = "inbound") String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String month) {
        try {
            if (!MATERIAL_FUND_TYPES.contains(type)) {
                return error("不支持的 type：" + type);
            }
            return success(dashboardMaterialFundService.getMaterialFundDetails(type, category, month));
        } catch (Exception e) {
            return exception(e, "操作");
        }
    }

    // endregion

    // region 私有辅助方法
    // ===================================
    // 私有辅助方法
    // ===================================

    /**
     * 格式化日期时间为字符串（null安全）
     *
     * @param dateTime 日期时间
     * @return 格式化后的字符串，null输入返回null
     */
    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_TIME_FORMATTER) : null;
    }
    // endregion
}
