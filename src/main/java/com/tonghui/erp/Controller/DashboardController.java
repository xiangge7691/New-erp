package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.Dashboard.*;
import com.tonghui.erp.Data.Entity.*;
import com.tonghui.erp.Service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 首页仪表盘控制器
 * 提供首页统计数据查询
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController extends BaseController {

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

    /**
     * 获取首页汇总数据（保留旧接口兼容）
     */
    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryDto> getSummary() {
        try {
            DashboardSummaryDto summary = new DashboardSummaryDto();

            ProductionStatsDto productionStats = new ProductionStatsDto();
            productionStats.setTotalPlans(productionPlanService.count());
            productionStats.setInProgress(productionPlanService.count(
                new QueryWrapper<ProductionPlan>().eq("current_status", "IN_PRODUCTION")));
            productionStats.setCompleted(productionPlanService.count(
                new QueryWrapper<ProductionPlan>().eq("current_status", "OUTBOUND")));
            productionStats.setPending(productionPlanService.count(
                new QueryWrapper<ProductionPlan>().eq("current_status", "PLAN_ISSUED")));
            summary.setProductionStats(productionStats);

            StockWarningStatsDto stockWarnings = new StockWarningStatsDto();
            List<Stock> allStocks = stockService.list(
                new QueryWrapper<Stock>().isNotNull("min_quantity"));
            long lowStock = allStocks.stream()
                .filter(s -> s.getQuantity() != null && s.getMinQuantity() != null
                    && s.getQuantity().compareTo(s.getMinQuantity()) <= 0)
                .count();
            LocalDate today = LocalDate.now();
            long expiringSoon = stockService.count(
                new QueryWrapper<Stock>()
                    .ge("expiry_date", today)
                    .le("expiry_date", today.plusDays(30)));
            long expired = stockService.count(
                new QueryWrapper<Stock>().lt("expiry_date", today));
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
     * @param startMonth 起始月份（格式：2026-01）
     * @param endMonth   结束月份（格式：2026-06）
     */
    @GetMapping("/metrics")
    public ApiResponse<DashboardMetricsDto> getMetrics(
            @RequestParam(required = false) String startMonth,
            @RequestParam(required = false) String endMonth) {
        try {
            DashboardMetricsDto metrics = new DashboardMetricsDto();
            QueryWrapper<ProductionPlan> wrapper = buildTimeWrapper(startMonth, endMonth);

            // 预估产值：总金额求和
            List<ProductionPlan> plans = productionPlanService.list(wrapper);
            double estimatedValue = plans.stream()
                .filter(p -> p.getTotalAmount() != null)
                .mapToDouble(p -> p.getTotalAmount().doubleValue())
                .sum();
            metrics.setEstimatedOutputValue(Math.round(estimatedValue * 100.0) / 100.0);

            // 总订单量
            metrics.setTotalOrders((long) plans.size());

            // 总交付量：有出库时间的
            long deliveries = plans.stream()
                .filter(p -> p.getOutboundTime() != null)
                .count();
            metrics.setTotalDeliveries(deliveries);

            // 总采购额：入库金额求和
            QueryWrapper<StockIn> stockInWrapper = buildStockInTimeWrapper(startMonth, endMonth);
            List<StockIn> stockIns = stockInService.list(stockInWrapper);
            double purchaseAmount = stockIns.stream()
                .filter(s -> s.getTotalAmount() != null)
                .mapToDouble(s -> s.getTotalAmount().doubleValue())
                .sum();
            metrics.setTotalPurchaseAmount(Math.round(purchaseAmount * 100.0) / 100.0);

            // 待生产数量：草稿+已确认
            long pending = productionPlanService.count(
                new QueryWrapper<ProductionPlan>()
                    .in("current_status", "PLAN_ISSUED", "MATERIAL_PREP"));
            metrics.setPendingProduction(pending);

            return success(metrics);
        } catch (Exception e) {
            return exception(e, "操作");
        }
    }

    /**
     * 待办事项列表
     */
    @GetMapping("/todos")
    public ApiResponse<TodoListDto> getTodos() {
        try {
            List<TodoItemDto> allTodos = new ArrayList<>();
            Map<String, Long> typeCounts = new LinkedHashMap<>();
            LocalDate today = LocalDate.now();

            // 1. 设备维保提醒
            List<EquipmentMaintenance> upcomingMaintenance = equipmentMaintenanceService.findUpcomingMaintenance(30);
            for (EquipmentMaintenance m : upcomingMaintenance) {
                TodoItemDto todo = new TodoItemDto();
                todo.setId(m.getMaintenanceId());
                todo.setTodoType("设备维保");
                String equipmentName = m.getEquipmentName() != null ? m.getEquipmentName() : "设备" + m.getEquipmentId();
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
                    .isNotNull("expiry_date")
                    .le("expiry_date", today.plusDays(30))
                    .orderByAsc("expiry_date"));
            for (Stock s : expiringStocks) {
                TodoItemDto todo = new TodoItemDto();
                todo.setId(s.getStockId());
                todo.setTodoType("库存预警");
                long days = java.time.temporal.ChronoUnit.DAYS.between(today, s.getExpiryDate());
                String itemName = s.getItemName() != null ? s.getItemName() : "物料" + s.getItemId();
                if (days < 0) {
                    todo.setContent(itemName + "已过期" + Math.abs(days) + "天");
                } else {
                    todo.setContent(itemName + "将在" + days + "天后过期");
                }
                todo.setDueDate(s.getExpiryDate().toString());
                todo.setSourceModule("库存管理");
                todo.setLink("库存有效期预警.html");
                allTodos.add(todo);
            }
            typeCounts.put("库存", (long) expiringStocks.size());

            // 2.5 待入库
            List<StockIn> pendingStockIns = stockInService.list(
                new QueryWrapper<StockIn>()
                    .eq("in_status", "草稿")
                    .orderByDesc("created_time"));
            for (StockIn si : pendingStockIns) {
                TodoItemDto todo = new TodoItemDto();
                todo.setId(si.getInId());
                todo.setTodoType("待入库");
                todo.setContent((si.getInCode() != null ? si.getInCode() : "入库单" + si.getInId()) + "已完成检验，待入库");
                todo.setDueDate(si.getInDate() != null ? si.getInDate().toString() : "");
                todo.setSourceModule("库存管理");
                todo.setLink("入库管理.html");
                allTodos.add(todo);
            }
            typeCounts.put("待入库", (long) pendingStockIns.size());

            // 2.6 待确认
            List<StockIn> unconfirmedStockIns = stockInService.list(
                new QueryWrapper<StockIn>()
                    .eq("in_status", "已确认")
                    .orderByDesc("created_time"));
            for (StockIn si : unconfirmedStockIns) {
                TodoItemDto todo = new TodoItemDto();
                todo.setId(si.getInId());
                todo.setTodoType("待确认");
                todo.setContent((si.getInCode() != null ? si.getInCode() : "入库单" + si.getInId()) + "已到货，待确认入库");
                todo.setDueDate(si.getInDate() != null ? si.getInDate().toString() : "");
                todo.setSourceModule("库存管理");
                todo.setLink("入库管理.html");
                allTodos.add(todo);
            }
            typeCounts.put("待确认", (long) unconfirmedStockIns.size());

            // 3. 人员健康证到期
            List<PersonnelFile> expiringCerts = personnelFileService.findExpiringHealthCerts(30);
            for (PersonnelFile p : expiringCerts) {
                TodoItemDto todo = new TodoItemDto();
                todo.setId(p.getPersonnelFileId());
                todo.setTodoType("人员管理");
                String name = p.getName() != null ? p.getName() : "人员" + p.getPersonnelFileId();
                if (p.getHealthCertExpire() != null) {
                    long days = java.time.temporal.ChronoUnit.DAYS.between(today, p.getHealthCertExpire());
                    todo.setContent(name + "健康证还有" + days + "天到期");
                    todo.setDueDate(p.getHealthCertExpire().toString());
                }
                todo.setSourceModule("人员管理");
                todo.setLink("人员档案.html");
                allTodos.add(todo);
            }
            typeCounts.put("人员管理", (long) expiringCerts.size());

            // 5. 环境管理（消毒到期提醒）
            List<DisinfectionRecord> upcomingDisinfection = disinfectionRecordService.findUpcomingDisinfection(30);
            for (DisinfectionRecord d : upcomingDisinfection) {
                TodoItemDto todo = new TodoItemDto();
                todo.setId(d.getId());
                todo.setTodoType("环境管理");
                String roomName = d.getRoomName() != null ? d.getRoomName() : "车间" + d.getRoomId();
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

            typeCounts.put("全部", (long) allTodos.size());

            TodoListDto result = new TodoListDto();
            result.setItems(allTodos);
            result.setTypeCounts(typeCounts);
            return success(result);
        } catch (Exception e) {
            return exception(e, "操作");
        }
    }

    /**
     * 订单跟踪看板
     */
    @GetMapping("/order-tracking")
    public ApiResponse<List<OrderTrackingDto>> getOrderTracking(
            @RequestParam(required = false) String startMonth,
            @RequestParam(required = false) String endMonth,
            @RequestParam(required = false) String status) {
        try {
            QueryWrapper<ProductionPlan> wrapper = buildTimeWrapper(startMonth, endMonth);
            if (status != null && !status.isEmpty()) {
                wrapper.eq("current_status", status);
            }
            wrapper.orderByDesc("create_time");

            List<ProductionPlan> plans = productionPlanService.list(wrapper);
            List<OrderTrackingDto> trackingList = plans.stream().map(plan -> {
                OrderTrackingDto dto = new OrderTrackingDto();
                dto.setId(plan.getId().longValue());
                dto.setOrderName(plan.getPreparationName());
                dto.setQuantity(plan.getPlanQuantity() != null ? plan.getPlanQuantity() + "" : "");
                dto.setBatchNo(plan.getPlanNumber());
                dto.setHospital(plan.getUnitName());
                dto.setCurrentStatus(plan.getCurrentStatus());
                if (plan.getCreateTime() != null) {
                    dto.setOrderDate(plan.getCreateTime().format(DateTimeFormatter.ofPattern("MM-dd")));
                }
                if (plan.getProductionStartTime() != null) {
                    dto.setProductionDate(plan.getProductionStartTime().format(DateTimeFormatter.ofPattern("MM-dd")));
                }
                if (plan.getInspectionStartTime() != null) {
                    dto.setInspectionDate(plan.getInspectionStartTime().format(DateTimeFormatter.ofPattern("MM-dd")));
                }
                if (plan.getOutboundTime() != null) {
                    dto.setOutboundDate(plan.getOutboundTime().format(DateTimeFormatter.ofPattern("MM-dd")));
                }
                if (plan.getArchiveTime() != null) {
                    dto.setArchiveDate(plan.getArchiveTime().format(DateTimeFormatter.ofPattern("MM-dd")));
                }
                return dto;
            }).collect(Collectors.toList());

            return success(trackingList);
        } catch (Exception e) {
            return exception(e, "操作");
        }
    }

    /**
     * 图表数据
     */
    @GetMapping("/charts")
    public ApiResponse<ChartDataDto> getCharts(
            @RequestParam(required = false) String startMonth,
            @RequestParam(required = false) String endMonth) {
        try {
            ChartDataDto chartData = new ChartDataDto();

            // 按月统计交付数量和产值
            QueryWrapper<ProductionPlan> wrapper = buildTimeWrapper(startMonth, endMonth);
            List<ProductionPlan> plans = productionPlanService.list(wrapper);

            // 交付数量按剂型（简化：按制剂名称分组）
            Map<String, Map<String, Long>> deliveryByMonth = new LinkedHashMap<>();
            Map<String, Map<String, Double>> revenueByMonth = new LinkedHashMap<>();

            for (ProductionPlan plan : plans) {
                String month = plan.getCreateTime() != null
                    ? plan.getCreateTime().format(DateTimeFormatter.ofPattern("M月"))
                    : "未知";

                deliveryByMonth.computeIfAbsent(month, k -> new LinkedHashMap<>())
                    .merge(plan.getPreparationName() != null ? plan.getPreparationName() : "其他",
                        plan.getOutboundTime() != null ? 1L : 0L, Long::sum);

                revenueByMonth.computeIfAbsent(month, k -> new LinkedHashMap<>())
                    .merge(plan.getPreparationName() != null ? plan.getPreparationName() : "其他",
                        plan.getTotalAmount() != null ? plan.getTotalAmount().doubleValue() : 0.0, Double::sum);
            }

            List<Map<String, Object>> deliveryList = new ArrayList<>();
            deliveryByMonth.forEach((month, data) -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("月份", month);
                item.putAll(data);
                deliveryList.add(item);
            });
            chartData.setDeliveryByDosageForm(deliveryList);

            List<Map<String, Object>> revenueList = new ArrayList<>();
            revenueByMonth.forEach((month, data) -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("月份", month);
                data.forEach((k, v) -> item.put(k, Math.round(v * 100.0) / 100.0));
                revenueList.add(item);
            });
            chartData.setRevenueByMonth(revenueList);

            // 库存资金占用（按分类分组，计算金额=数量*单价）
            List<Stock> allStocks = stockService.list();
            Map<String, Double> fundOccupation = allStocks.stream()
                .collect(Collectors.groupingBy(
                    s -> s.getCategoryName() != null ? s.getCategoryName() : "其他",
                    Collectors.summingDouble(s -> {
                        double qty = s.getQuantity() != null ? s.getQuantity().doubleValue() : 0.0;
                        double price = s.getUnitPrice() != null ? s.getUnitPrice().doubleValue() : 0.0;
                        return qty * price;
                    })
                ));
            chartData.setInventoryFundOccupation(fundOccupation);

            return success(chartData);
        } catch (Exception e) {
            return exception(e, "操作");
        }
    }

    private QueryWrapper<ProductionPlan> buildTimeWrapper(String startMonth, String endMonth) {
        QueryWrapper<ProductionPlan> wrapper = new QueryWrapper<>();
        if (startMonth != null && !startMonth.isEmpty()) {
            wrapper.ge("create_time", startMonth + "-01 00:00:00");
        }
        if (endMonth != null && !endMonth.isEmpty()) {
            LocalDate end = LocalDate.parse(endMonth + "-01").plusMonths(1).minusDays(1);
            wrapper.le("create_time", end.atTime(23, 59, 59));
        }
        return wrapper;
    }

    private QueryWrapper<StockIn> buildStockInTimeWrapper(String startMonth, String endMonth) {
        QueryWrapper<StockIn> wrapper = new QueryWrapper<>();
        if (startMonth != null && !startMonth.isEmpty()) {
            wrapper.ge("in_date", startMonth + "-01");
        }
        if (endMonth != null && !endMonth.isEmpty()) {
            LocalDate end = LocalDate.parse(endMonth + "-01").plusMonths(1).minusDays(1);
            wrapper.le("in_date", end.toString());
        }
        return wrapper;
    }
}
