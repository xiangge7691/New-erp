package com.tonghui.erp.Controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.ProductionPlanWithRecordsDto;
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

    @Autowired
    private PurchaseOrdersService purchaseOrdersService;

    @Autowired
    private AcceptanceOrderService acceptanceOrderService;

    @Autowired
    private StockOutService stockOutService;

    @Autowired
    private OrganizationService organizationService;

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

            // 8. 验收流程待办（运输中/到货初验/物料检验/待退货）
            List<AcceptanceOrder> pendingAcceptances = acceptanceOrderService.list(
                new QueryWrapper<AcceptanceOrder>()
                    .eq("is_deleted", 0)
                    .in("status", Arrays.asList("运输中", "到货初验", "物料检验", "待退货"))
                    .orderByDesc("created_time"));
            Map<String, String> acceptanceContentMap = new HashMap<>();
            acceptanceContentMap.put("运输中", "已发货，待确认到货");
            acceptanceContentMap.put("到货初验", "待到货初验");
            acceptanceContentMap.put("物料检验", "待检验");
            acceptanceContentMap.put("待退货", "检验不合格，待退货");
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

            // 10. 机构许可证到期（30天内到期或已过期）
            List<Organization> expiringOrgs = organizationService.list(
                new QueryWrapper<Organization>()
                    .eq("is_deleted", 0)
                    .isNotNull("expiry_date")
                    .le("expiry_date", today.plusDays(30))
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
            @RequestParam int pageIndex,
            @RequestParam int pageSize) {
        try {
            int safePageIndex = Math.max(0, pageIndex);
            int safePageSize = pageSize <= 0 ? 20 : Math.max(1, pageSize);

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
            result.setPageIndex(planResult.getPageIndex());
            result.setPageSize(planResult.getPageSize());

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