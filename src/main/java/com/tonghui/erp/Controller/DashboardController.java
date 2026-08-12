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
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────────┬────────┬─────────────────────────────────────┐
 * │ #  │ 接口                                 │ 方法   │ 说明                                │
 * ├────┼──────────────────────────────────────┼────────┼─────────────────────────────────────┤
 * │ 1  │ /api/dashboard/summary               │ GET   │ 获取首页汇总数据                    │
 * │ 2  │ /api/dashboard/metrics               │ GET   │ 核心指标卡片数据                    │
 * │ 3  │ /api/dashboard/todos                 │ GET   │ 待办事项列表                        │
 * │ 4  │ /api/dashboard/order-tracking        │ GET   │ 订单跟踪看板                        │
 * │ 5  │ /api/dashboard/charts                │ GET   │ 图表数据                            │
 * └────┴──────────────────────────────────────┴────────┴─────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController extends BaseController {

    /**
     * 日期时间格式化器（yyyy-MM-dd HH:mm:ss）
     */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

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
     * 示例请求：
     * GET /api/dashboard/metrics?startMonth=2026-01&endMonth=2026-06
     *
     * @param startMonth 起始月份（格式：2026-01）
     * @param endMonth   结束月份（格式：2026-06）
     * @return 核心指标数据，包含预估产值、总订单量、总交付量、总采购额、待生产数量
     */
    @GetMapping("/metrics")
    public ApiResponse<DashboardMetricsDto> getMetrics(
            @RequestParam(required = false) String startMonth,
            @RequestParam(required = false) String endMonth) {
        try {
            DashboardMetricsDto metrics = new DashboardMetricsDto();

            // 预估产值/总订单量：查 work_order 表（按 created_time 筛选）
            QueryWrapper<WorkOrder> woWrapper = buildWorkOrderTimeWrapper(startMonth, endMonth);
            List<WorkOrder> workOrders = workOrderService.list(woWrapper);

            // 预估产值：SUM(batch_qty * settlement_price)
            double estimatedValue = workOrders.stream()
                .filter(wo -> wo.getBatchQty() != null && wo.getSettlementPrice() != null)
                .mapToDouble(wo -> wo.getBatchQty().doubleValue() * wo.getSettlementPrice().doubleValue())
                .sum();
            metrics.setEstimatedOutputValue(Math.round(estimatedValue * 100.0) / 100.0);

            // 总订单量：COUNT(*)
            metrics.setTotalOrders((long) workOrders.size());

            // 总交付量：查 work_order 表（按 delivery_time 筛选）
            QueryWrapper<WorkOrder> deliveryWrapper = buildWorkOrderDeliveryTimeWrapper(startMonth, endMonth);
            long deliveries = workOrderService.count(deliveryWrapper);
            metrics.setTotalDeliveries(deliveries);

            // 总采购额：入库金额求和
            QueryWrapper<StockIn> stockInWrapper = buildStockInTimeWrapper(startMonth, endMonth);
            List<StockIn> stockIns = stockInService.list(stockInWrapper);
            double purchaseAmount = stockIns.stream()
                .filter(s -> s.getTotalAmount() != null)
                .mapToDouble(s -> s.getTotalAmount().doubleValue())
                .sum();
            metrics.setTotalPurchaseAmount(Math.round(purchaseAmount * 100.0) / 100.0);

            // 待生产数量：current_status = '待生产' 且在时间范围内
            QueryWrapper<ProductionPlan> pendingWrapper = new QueryWrapper<>();
            pendingWrapper.eq("is_deleted", 0)
                          .eq("current_status", "待生产");
            if (startMonth != null && !startMonth.isEmpty()) {
                pendingWrapper.ge("created_time", startMonth + "-01 00:00:00");
            }
            if (endMonth != null && !endMonth.isEmpty()) {
                LocalDate end = LocalDate.parse(endMonth + "-01").plusMonths(1).minusDays(1);
                pendingWrapper.le("created_time", end.atTime(23, 59, 59));
            }
            long pending = productionPlanService.count(pendingWrapper);
            metrics.setPendingProduction(pending);

            return success(metrics);
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

            // 1. 设备维保提醒
            List<EquipmentMaintenance> upcomingMaintenance = equipmentMaintenanceService.findUpcomingMaintenance(30);
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
            List<PersonnelFile> expiringCerts = personnelFileService.findExpiringHealthCerts(30);
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
            List<PersonnelCertificate> expiringCertificates = personnelCertificateService.findExpiringCertificates(30);
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
            List<DisinfectionRecord> upcomingDisinfection = disinfectionRecordService.findUpcomingDisinfection(30);
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
     * GET /api/dashboard/order-tracking?startMonth=2026-01&endMonth=2026-06&status=生产中
     *
     * @param startMonth 起始月份（格式：2026-01，可选）
     * @param endMonth   结束月份（格式：2026-06，可选）
     * @param status     订单状态（可选），如待生产、生产中、已完成
     * @return 订单跟踪列表
     */
    @GetMapping("/order-tracking")
    public ApiResponse<List<OrderTrackingDto>> getOrderTracking(
            @RequestParam(required = false) String startMonth,
            @RequestParam(required = false) String endMonth,
            @RequestParam(required = false) String status) {
        try {
            QueryWrapper<ProductionPlan> wrapper = buildTimeWrapper(startMonth, endMonth);
            if (status != null && !status.isEmpty()) {
                // 状态已落库，直接按中文状态值精确匹配（待生产/生产中/已完成）
                wrapper.eq("current_status", status);
            }
            wrapper.orderByDesc("created_time");

            List<ProductionPlan> plans = productionPlanService.list(wrapper);
            List<OrderTrackingDto> trackingList = plans.stream().map(plan -> {
                OrderTrackingDto dto = new OrderTrackingDto();
                dto.setId(plan.getId().longValue());
                dto.setOrderName(plan.getPreparationName());
                dto.setQuantity(plan.getPlanQuantity() != null ? plan.getPlanQuantity() + "" : "");
                dto.setBatchNo(plan.getPlanNumber());
                dto.setHospital(plan.getUnitName());
                dto.setCurrentStatus(plan.getCurrentStatus());
                if (plan.getCreatedTime() != null) {
                    dto.setOrderDate(plan.getCreatedTime().format(DateTimeFormatter.ofPattern("MM-dd")));
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

    // endregion

    // region 图表数据接口
    // ===================================
    // 图表数据接口
    // ===================================

    /**
     * 图表数据
     *
     * 示例请求：
     * GET /api/dashboard/charts?startMonth=2026-01&endMonth=2026-06
     *
     * @param startMonth 起始月份（格式：2026-01，可选）
     * @param endMonth   结束月份（格式：2026-06，可选）
     * @return 图表数据，包含交付数量、收入趋势、库存资金占用等
     */
    @GetMapping("/charts")
    public ApiResponse<ChartDataDto> getCharts(
            @RequestParam(required = false) String startMonth,
            @RequestParam(required = false) String endMonth) {
        try {
            ChartDataDto chartData = new ChartDataDto();

            // 预加载制剂→剂型大类映射
            Map<String, String> preparationDosageMap = new HashMap<>();
            preparationService.list().forEach(p ->
                preparationDosageMap.put(p.getPreparationCode(), p.getDosageCategory() != null ? p.getDosageCategory() : "其他")
            );

            // === 交付数量按剂型（月度）：查 work_order（按 delivery_time 筛选） ===
            QueryWrapper<WorkOrder> deliveryWrapper = buildWorkOrderDeliveryTimeWrapper(startMonth, endMonth);
            List<WorkOrder> deliveryOrders = workOrderService.list(deliveryWrapper);

            Map<String, Map<String, Long>> deliveryByMonth = new LinkedHashMap<>();
            for (WorkOrder wo : deliveryOrders) {
                String month = wo.getDeliveryTime() != null
                    ? wo.getDeliveryTime().format(DateTimeFormatter.ofPattern("M月"))
                    : "未知";
                String dosageForm = preparationDosageMap.getOrDefault(wo.getPreparationCode(), "其他");
                deliveryByMonth.computeIfAbsent(month, k -> new LinkedHashMap<>())
                    .merge(dosageForm, 1L, Long::sum);
            }

            // 组装交付数量结果（含总计）
            List<Map<String, Object>> deliveryList = new ArrayList<>();
            deliveryByMonth.forEach((month, data) -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("月份", month);
                long total = 0;
                for (Long v : data.values()) total += v;
                item.putAll(data);
                item.put("总计", total);
                deliveryList.add(item);
            });
            chartData.setDeliveryByDosageForm(deliveryList);

            // === 预估产值按剂型（月度）：查 work_order（按 created_time 筛选） ===
            QueryWrapper<WorkOrder> revenueWrapper = buildWorkOrderTimeWrapper(startMonth, endMonth);
            List<WorkOrder> revenueOrders = workOrderService.list(revenueWrapper);

            Map<String, Map<String, Double>> revenueByMonth = new LinkedHashMap<>();
            for (WorkOrder wo : revenueOrders) {
                String month = wo.getCreatedTime() != null
                    ? wo.getCreatedTime().format(DateTimeFormatter.ofPattern("M月"))
                    : "未知";
                String dosageForm = preparationDosageMap.getOrDefault(wo.getPreparationCode(), "其他");
                double amount = (wo.getBatchQty() != null && wo.getSettlementPrice() != null)
                    ? wo.getBatchQty().doubleValue() * wo.getSettlementPrice().doubleValue()
                    : 0.0;
                revenueByMonth.computeIfAbsent(month, k -> new LinkedHashMap<>())
                    .merge(dosageForm, amount, Double::sum);
            }

            // 组装预估产值结果（含总计）
            List<Map<String, Object>> revenueList = new ArrayList<>();
            revenueByMonth.forEach((month, data) -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("月份", month);
                double total = 0;
                for (Double v : data.values()) total += v;
                data.forEach((k, v) -> item.put(k, Math.round(v * 100.0) / 100.0));
                item.put("总计", Math.round(total * 100.0) / 100.0);
                revenueList.add(item);
            });
            chartData.setRevenueByMonth(revenueList);

            // === 库存资金占用（按分类分组，计算金额=数量*单价） ===
            List<Stock> allStocks = stockService.list();
            Map<String, Double> fundOccupation = allStocks.stream()
                .filter(s -> s.getIsDeleted() == null || s.getIsDeleted() == 0)
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

    // endregion

    // region 私有辅助方法
    // ===================================
    // 私有辅助方法
    // ===================================

    /**
     * 构建生产计划时间范围查询条件
     *
     * @param startMonth 起始月份（格式：2026-01）
     * @param endMonth   结束月份（格式：2026-06）
     * @return 查询条件
     */
    private QueryWrapper<ProductionPlan> buildTimeWrapper(String startMonth, String endMonth) {
        QueryWrapper<ProductionPlan> wrapper = new QueryWrapper<>();
        if (startMonth != null && !startMonth.isEmpty()) {
            wrapper.ge("created_time", startMonth + "-01 00:00:00");
        }
        if (endMonth != null && !endMonth.isEmpty()) {
            LocalDate end = LocalDate.parse(endMonth + "-01").plusMonths(1).minusDays(1);
            wrapper.le("created_time", end.atTime(23, 59, 59));
        }
        return wrapper;
    }

    /**
     * 构建入库单时间范围查询条件
     *
     * @param startMonth 起始月份（格式：2026-01）
     * @param endMonth   结束月份（格式：2026-06）
     * @return 查询条件
     */
    private QueryWrapper<StockIn> buildStockInTimeWrapper(String startMonth, String endMonth) {
        QueryWrapper<StockIn> wrapper = new QueryWrapper<>();
        if (startMonth != null && !startMonth.isEmpty()) {
            wrapper.ge("in_date", LocalDate.parse(startMonth + "-01").atStartOfDay());
        }
        if (endMonth != null && !endMonth.isEmpty()) {
            LocalDate end = LocalDate.parse(endMonth + "-01").plusMonths(1).minusDays(1);
            wrapper.le("in_date", end.atTime(23, 59, 59));
        }
        return wrapper;
    }

    /**
     * 构建工单时间范围查询条件
     *
     * @param startMonth 起始月份（格式：2026-01）
     * @param endMonth   结束月份（格式：2026-06）
     * @return 查询条件
     */
    private QueryWrapper<WorkOrder> buildWorkOrderTimeWrapper(String startMonth, String endMonth) {
        QueryWrapper<WorkOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("is_deleted", 0);
        if (startMonth != null && !startMonth.isEmpty()) {
            wrapper.ge("created_time", startMonth + "-01 00:00:00");
        }
        if (endMonth != null && !endMonth.isEmpty()) {
            LocalDate end = LocalDate.parse(endMonth + "-01").plusMonths(1).minusDays(1);
            wrapper.le("created_time", end.atTime(23, 59, 59));
        }
        return wrapper;
    }

    /**
     * 构建工单交付时间范围查询条件
     *
     * @param startMonth 起始月份（格式：2026-01）
     * @param endMonth   结束月份（格式：2026-06）
     * @return 查询条件
     */
    private QueryWrapper<WorkOrder> buildWorkOrderDeliveryTimeWrapper(String startMonth, String endMonth) {
        QueryWrapper<WorkOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("is_deleted", 0);
        if (startMonth != null && !startMonth.isEmpty()) {
            wrapper.ge("delivery_time", startMonth + "-01 00:00:00");
        }
        if (endMonth != null && !endMonth.isEmpty()) {
            LocalDate end = LocalDate.parse(endMonth + "-01").plusMonths(1).minusDays(1);
            wrapper.le("delivery_time", end.atTime(23, 59, 59));
        }
        return wrapper;
    }

    // endregion
}