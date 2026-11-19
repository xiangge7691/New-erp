package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tonghui.erp.Common.Dto.Pda.PdaLabelDto;
import com.tonghui.erp.Common.Dto.Pda.PdaLabelPrintRequest;
import com.tonghui.erp.Common.Dto.Pda.PdaLabelReprintRequest;
import com.tonghui.erp.Common.Dto.Pda.PdaStockConfirmRequest;
import com.tonghui.erp.Common.Dto.Pda.PdaStockLineDto;
import com.tonghui.erp.Common.Dto.Pda.PdaStockRecordDto;
import com.tonghui.erp.Common.Dto.Pda.PdaStockScanRequest;
import com.tonghui.erp.Common.Dto.Pda.PdaStockTaskDetailDto;
import com.tonghui.erp.Common.Dto.Pda.PdaStockTaskDto;
import com.tonghui.erp.Common.utils.EntityUtils;
import com.tonghui.erp.Common.utils.PasswordHasher;
import com.tonghui.erp.Data.Entity.ProductionUnit;
import com.tonghui.erp.Data.Entity.ReturnOrder;
import com.tonghui.erp.Data.Entity.ReturnOrderDetail;
import com.tonghui.erp.Data.Entity.Stock;
import com.tonghui.erp.Data.Entity.StockIn;
import com.tonghui.erp.Data.Entity.StockInDetail;
import com.tonghui.erp.Data.Entity.StockInLabel;
import com.tonghui.erp.Data.Entity.StockLabelPrintLog;
import com.tonghui.erp.Data.Entity.StockOut;
import com.tonghui.erp.Data.Entity.StockOutDetail;
import com.tonghui.erp.Data.Entity.StockScanRecord;
import com.tonghui.erp.Data.Entity.User;
import com.tonghui.erp.Data.mapper.ProductionUnitMapper;
import com.tonghui.erp.Data.mapper.ReturnOrderDetailMapper;
import com.tonghui.erp.Data.mapper.ReturnOrderMapper;
import com.tonghui.erp.Data.mapper.StockInDetailMapper;
import com.tonghui.erp.Data.mapper.StockInLabelMapper;
import com.tonghui.erp.Data.mapper.StockInMapper;
import com.tonghui.erp.Data.mapper.StockLabelPrintLogMapper;
import com.tonghui.erp.Data.mapper.StockMapper;
import com.tonghui.erp.Data.mapper.StockOutDetailMapper;
import com.tonghui.erp.Data.mapper.StockOutMapper;
import com.tonghui.erp.Data.mapper.StockScanRecordMapper;
import com.tonghui.erp.Data.mapper.UserMapper;
import com.tonghui.erp.Service.PdaStockService;
import com.tonghui.erp.Service.StockInService;
import com.tonghui.erp.Service.StockLabelService;
import com.tonghui.erp.Service.StockOutService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * PDA 仓储扫码服务实现
 */
@Service
public class PdaStockServiceImpl implements PdaStockService {

    private static final String INBOUND = "INBOUND";
    private static final String OUTBOUND = "OUTBOUND";
    private static final String RETURN = "RETURN";
    private static final String PENDING_PRINT = "待打印";
    private static final String PENDING_SCAN = "待扫码";
    private static final String DONE_IN = "已入库";
    private static final String DONE_OUT = "已出库";

    private final StockInMapper stockInMapper;
    private final StockInDetailMapper stockInDetailMapper;
    private final StockOutMapper stockOutMapper;
    private final StockOutDetailMapper stockOutDetailMapper;
    private final StockMapper stockMapper;
    private final ReturnOrderMapper returnOrderMapper;
    private final ReturnOrderDetailMapper returnOrderDetailMapper;
    private final StockInLabelMapper stockInLabelMapper;
    private final StockScanRecordMapper stockScanRecordMapper;
    private final StockLabelPrintLogMapper stockLabelPrintLogMapper;
    private final ProductionUnitMapper productionUnitMapper;
    private final UserMapper userMapper;
    private final StockInService stockInService;
    private final StockOutService stockOutService;
    private final StockLabelService stockLabelService;

    public PdaStockServiceImpl(StockInMapper stockInMapper,
                               StockInDetailMapper stockInDetailMapper,
                               StockOutMapper stockOutMapper,
                               StockOutDetailMapper stockOutDetailMapper,
                               StockMapper stockMapper,
                               ReturnOrderMapper returnOrderMapper,
                               ReturnOrderDetailMapper returnOrderDetailMapper,
                               StockInLabelMapper stockInLabelMapper,
                               StockScanRecordMapper stockScanRecordMapper,
                               StockLabelPrintLogMapper stockLabelPrintLogMapper,
                               ProductionUnitMapper productionUnitMapper,
                               UserMapper userMapper,
                               StockInService stockInService,
                               StockOutService stockOutService,
                               StockLabelService stockLabelService) {
        this.stockInMapper = stockInMapper;
        this.stockInDetailMapper = stockInDetailMapper;
        this.stockOutMapper = stockOutMapper;
        this.stockOutDetailMapper = stockOutDetailMapper;
        this.stockMapper = stockMapper;
        this.returnOrderMapper = returnOrderMapper;
        this.returnOrderDetailMapper = returnOrderDetailMapper;
        this.stockInLabelMapper = stockInLabelMapper;
        this.stockScanRecordMapper = stockScanRecordMapper;
        this.stockLabelPrintLogMapper = stockLabelPrintLogMapper;
        this.productionUnitMapper = productionUnitMapper;
        this.userMapper = userMapper;
        this.stockInService = stockInService;
        this.stockOutService = stockOutService;
        this.stockLabelService = stockLabelService;
    }

    // region 任务查询

    @Override
    public List<PdaStockTaskDto> listTasks(String type, String status, String keyword) {
        String normalized = normalizeType(type);
        List<PdaStockTaskDto> result = new ArrayList<>();
        if (normalized == null || INBOUND.equals(normalized)) {
            result.addAll(listInboundTasks(keyword));
        }
        if (normalized == null || OUTBOUND.equals(normalized)) {
            result.addAll(listOutboundTasks(keyword));
        }
        if (normalized == null || RETURN.equals(normalized)) {
            result.addAll(listReturnTasks(keyword));
        }
        if (StringUtils.hasText(status)) {
            result = result.stream()
                    .filter(t -> status.equals(t.getScanStatus()))
                    .collect(Collectors.toList());
        }
        result.sort(Comparator.comparing(PdaStockTaskDto::getDocDate,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    private List<PdaStockTaskDto> listInboundTasks(String keyword) {
        LambdaQueryWrapper<StockIn> wrapper = new LambdaQueryWrapper<StockIn>()
                .eq(StockIn::getIsDeleted, 0)
                .eq(StockIn::getInStatus, "草稿")
                .and(q -> q.eq(StockIn::getScanStatus, PENDING_PRINT)
                        .or().eq(StockIn::getScanStatus, PENDING_SCAN));
        if (StringUtils.hasText(keyword)) {
            wrapper.like(StockIn::getInCode, keyword);
        }
        return stockInMapper.selectList(wrapper).stream()
                .map(this::toInboundTask)
                .collect(Collectors.toList());
    }

    private List<PdaStockTaskDto> listOutboundTasks(String keyword) {
        LambdaQueryWrapper<StockOut> wrapper = new LambdaQueryWrapper<StockOut>()
                .eq(StockOut::getIsDeleted, 0)
                .eq(StockOut::getOutStatus, "草稿");
        if (StringUtils.hasText(keyword)) {
            wrapper.like(StockOut::getOutCode, keyword);
        }
        return stockOutMapper.selectList(wrapper).stream()
                .map(this::toOutboundTask)
                .collect(Collectors.toList());
    }

    private List<PdaStockTaskDto> listReturnTasks(String keyword) {
        LambdaQueryWrapper<ReturnOrder> wrapper = new LambdaQueryWrapper<ReturnOrder>()
                .eq(ReturnOrder::getIsDeleted, 0)
                .eq(ReturnOrder::getScanStatus, PENDING_SCAN);
        if (StringUtils.hasText(keyword)) {
            wrapper.like(ReturnOrder::getReturnNo, keyword);
        }
        return returnOrderMapper.selectList(wrapper).stream()
                .map(this::toReturnTask)
                .collect(Collectors.toList());
    }

    private PdaStockTaskDto toInboundTask(StockIn stockIn) {
        List<StockInLabel> labels = ensureInboundLabels(stockIn);
        PdaStockTaskDto dto = baseTask(stockIn.getInId(), stockIn.getInCode(), INBOUND,
                stockIn.getInStatus(), stockIn.getScanStatus(), stockIn.getProdUnitId(),
                stockIn.getInDate(), stockIn.getRemark());
        dto.setLineCount(labels.size());
        dto.setPrintedCount((int) labels.stream()
                .filter(l -> "已打印".equals(l.getPrintStatus())).count());
        dto.setScannedCount((int) labels.stream()
                .filter(l -> "已扫".equals(l.getScanStatus())).count());
        dto.setCanConfirm(dto.getLineCount() > 0
                && dto.getPrintedCount().equals(dto.getLineCount())
                && dto.getScannedCount().equals(dto.getLineCount()));
        return dto;
    }

    private PdaStockTaskDto toOutboundTask(StockOut stockOut) {
        List<StockOutDetail> details = outboundDetails(stockOut.getOutId());
        int scanned = 0;
        for (StockOutDetail detail : details) {
            resolveExpectedLabel(detail);
            if ("已扫".equals(detail.getScanStatus())) {
                scanned++;
            }
        }
        PdaStockTaskDto dto = baseTask(stockOut.getOutId(), stockOut.getOutCode(), OUTBOUND,
                stockOut.getOutStatus(), outboundScanStatus(stockOut, details, scanned),
                stockOut.getProdUnitId(), stockOut.getOutDate(), stockOut.getRemark());
        dto.setLineCount(details.size());
        dto.setPrintedCount(0);
        dto.setScannedCount(scanned);
        dto.setCanConfirm(!details.isEmpty() && scanned == details.size());
        return dto;
    }

    private String outboundScanStatus(StockOut stockOut, List<StockOutDetail> details, int scanned) {
        if (!"草稿".equals(stockOut.getOutStatus())) {
            return defaultIfBlank(stockOut.getScanStatus(), DONE_OUT);
        }
        if (details.isEmpty() || scanned == 0) {
            return PENDING_SCAN;
        }
        return scanned < details.size() ? "扫码中" : "待确认";
    }

    private PdaStockTaskDto toReturnTask(ReturnOrder order) {
        List<ReturnOrderDetail> details = returnDetails(order.getId());
        int scanned = (int) details.stream().filter(d -> "已扫".equals(d.getScanStatus())).count();
        PdaStockTaskDto dto = baseTask(order.getId(), order.getReturnNo(), RETURN,
                order.getScanStatus(), defaultIfBlank(order.getScanStatus(), PENDING_SCAN),
                null, order.getReturnTime(), order.getRemark());
        dto.setLineCount(details.size());
        dto.setPrintedCount(0);
        dto.setScannedCount(scanned);
        dto.setCanConfirm(!details.isEmpty() && scanned == details.size());
        return dto;
    }

    private PdaStockTaskDto baseTask(Long id, String code, String type, String status,
                                     String scanStatus, Long warehouseId,
                                     LocalDateTime docDate, String remark) {
        PdaStockTaskDto dto = new PdaStockTaskDto();
        dto.setId(id);
        dto.setDocCode(code);
        dto.setType(type);
        dto.setStatus(status);
        dto.setScanStatus(scanStatus);
        dto.setWarehouseId(warehouseId);
        dto.setWarehouseName(warehouseName(warehouseId));
        dto.setDocDate(docDate);
        dto.setRemark(remark);
        return dto;
    }

    @Override
    public PdaStockTaskDetailDto getTask(String type, Long id) {
        String normalized = normalizeType(type);
        if (INBOUND.equals(normalized)) {
            StockIn stockIn = requireStockIn(id);
            PdaStockTaskDetailDto dto = new PdaStockTaskDetailDto();
            dto.setTask(toInboundTask(stockIn));
            dto.setLines(inboundLines(stockIn.getInId()));
            return dto;
        }
        if (OUTBOUND.equals(normalized)) {
            StockOut stockOut = requireStockOut(id);
            PdaStockTaskDetailDto dto = new PdaStockTaskDetailDto();
            dto.setTask(toOutboundTask(stockOut));
            dto.setLines(outboundLines(stockOut.getOutId()));
            return dto;
        }
        if (RETURN.equals(normalized)) {
            ReturnOrder order = requireReturnOrder(id);
            PdaStockTaskDetailDto dto = new PdaStockTaskDetailDto();
            dto.setTask(toReturnTask(order));
            dto.setLines(returnLines(order.getId()));
            return dto;
        }
        throw new IllegalArgumentException("不支持的单据类型: " + type);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PdaStockTaskDetailDto generateLabels(Long inId) {
        StockIn stockIn = requireStockIn(inId);
        stockLabelService.ensureLabels(inId);
        if ("草稿".equals(stockIn.getInStatus())
                && !StringUtils.hasText(stockIn.getScanStatus())) {
            stockIn.setScanStatus(PENDING_PRINT);
            stockInMapper.updateById(stockIn);
        }
        return getTask(INBOUND, inId);
    }

    // endregion

    // region 标签

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PdaStockTaskDetailDto printLabels(Long inId, PdaLabelPrintRequest request) {
        StockIn stockIn = requireStockIn(inId);
        if (!"草稿".equals(stockIn.getInStatus())) {
            throw new IllegalArgumentException("仅草稿入库单可以打印待入库标签");
        }
        ensureInboundLabels(stockIn);
        List<StockInLabel> labels = stockInLabelMapper.selectByInIdWithDetail(inId);
        Set<Long> selected = request != null && !CollectionUtils.isEmpty(request.getLineIds())
                ? Set.copyOf(request.getLineIds()) : null;
        LocalDateTime now = LocalDateTime.now();
        Long userId = EntityUtils.getCurrentUserId();
        for (StockInLabel label : labels) {
            if (selected != null && !selected.contains(label.getInDetailId())) {
                continue;
            }
            if (!"已打印".equals(label.getPrintStatus())) {
                label.setPrintStatus("已打印");
                label.setPrintedBy(userId);
                label.setPrintedTime(now);
                stockInLabelMapper.updateById(label);
            }
            insertPrintLog(label, "PRINT", null, request == null ? null : request.getDeviceNo());
        }
        long printed = stockInLabelMapper.selectCount(new QueryWrapper<StockInLabel>()
                .eq("in_id", inId).eq("print_status", "已打印").eq("is_deleted", 0));
        if (printed > 0) {
            stockIn.setScanStatus(PENDING_SCAN);
            stockInMapper.updateById(stockIn);
        }
        return getTask(INBOUND, inId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PdaLabelDto reprint(Long inId, Long lineId, PdaLabelReprintRequest request) {
        if (request == null || !StringUtils.hasText(request.getReason())) {
            throw new IllegalArgumentException("请填写补打原因");
        }
        StockIn stockIn = requireStockIn(inId);
        if ("已取消".equals(stockIn.getInStatus())) {
            throw new IllegalArgumentException("已作废的入库单不能补打");
        }
        StockInLabel label = stockInLabelMapper.selectByInDetailIdWithDetail(lineId);
        if (label == null || !Objects.equals(label.getInId(), inId)) {
            throw new IllegalArgumentException("未找到对应标签");
        }
        label.setReprintCount((label.getReprintCount() == null ? 0 : label.getReprintCount()) + 1);
        stockInLabelMapper.updateById(label);
        insertPrintLog(label, "REPRINT", request.getReason(), request.getDeviceNo());
        return toLabelDto(label, stockIn);
    }

    @Override
    public List<PdaLabelDto> listLabels(String keyword, String warehouse, String status) {
        Map<Long, String> warehouseMap = new HashMap<>();
        List<PdaLabelDto> result = new ArrayList<>();
        for (StockInLabel label : stockInLabelMapper.selectAllWithDetail()) {
            StockIn stockIn = stockInMapper.selectById(label.getInId());
            if (stockIn == null) {
                continue;
            }
            PdaLabelDto dto = toLabelDto(label, stockIn);
            if (StringUtils.hasText(warehouse) && !warehouse.equals(dto.getWarehouseName())) {
                continue;
            }
            if (StringUtils.hasText(status) && !status.equals(dto.getStatus())) {
                continue;
            }
            if (StringUtils.hasText(keyword)) {
                String text = joinText(dto.getMaterialName(), dto.getMaterialCode(),
                        dto.getBatchNumber(), dto.getLabelCode(), dto.getInCode());
                if (!text.toLowerCase().contains(keyword.toLowerCase())) {
                    continue;
                }
            }
            result.add(dto);
        }
        result.sort(Comparator.comparing(PdaLabelDto::getId,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    private PdaLabelDto toLabelDto(StockInLabel label, StockIn stockIn) {
        PdaLabelDto dto = new PdaLabelDto();
        dto.setId(label.getId());
        dto.setInId(label.getInId());
        dto.setInCode(stockIn.getInCode());
        dto.setLineId(label.getInDetailId());
        dto.setLabelCode(label.getLabelCode());
        dto.setQrContent(label.getQrContent());
        dto.setMaterialCode(label.getMaterialCode());
        dto.setMaterialName(label.getMaterialName());
        dto.setBatchNumber(label.getBatchNumber());
        dto.setQuantity(label.getQuantity());
        dto.setUnitName(label.getUnitName());
        dto.setWarehouseName(warehouseName(stockIn.getProdUnitId()));
        dto.setDocDate(stockIn.getInDate());
        dto.setPrintStatus(label.getPrintStatus());
        dto.setScanStatus(label.getScanStatus());
        dto.setReprintCount(label.getReprintCount());
        dto.setStatus("已取消".equals(stockIn.getInStatus()) ? "已作废" : "可补打");
        return dto;
    }

    private void insertPrintLog(StockInLabel label, String action, String reason, String deviceNo) {
        StockLabelPrintLog log = new StockLabelPrintLog();
        log.setInId(label.getInId());
        log.setInDetailId(label.getInDetailId());
        log.setLabelCode(label.getLabelCode());
        log.setAction(action);
        log.setReason(reason);
        log.setDeviceNo(deviceNo);
        log.setOperatorId(EntityUtils.getCurrentUserId());
        log.setOperatorName(currentUserName());
        log.setIsDeleted(0);
        log.setVersion(0);
        stockLabelPrintLogMapper.insert(log);
    }

    // endregion

    // region 扫码

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PdaStockLineDto scan(String type, Long id, PdaStockScanRequest request) {
        return doScan(type, id, request, "SCAN");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PdaStockLineDto manualScan(String type, Long id, PdaStockScanRequest request) {
        if (request == null || !StringUtils.hasText(request.getAuthorizer())
                || !StringUtils.hasText(request.getReason())) {
            throw new IllegalArgumentException("手工录入必须填写主管授权与原因");
        }
        return doScan(type, id, request, "MANUAL");
    }

    private PdaStockLineDto doScan(String type, Long id, PdaStockScanRequest request, String action) {
        if (request == null || !StringUtils.hasText(request.getCode())) {
            throw new IllegalArgumentException("请扫描或输入条码");
        }
        String normalized = normalizeType(type);
        if (StringUtils.hasText(request.getRequestId())) {
            StockScanRecord existing = stockScanRecordMapper.selectOne(
                    new QueryWrapper<StockScanRecord>()
                            .eq("request_id", request.getRequestId())
                            .eq("is_deleted", 0), false);
            if (existing != null && "SUCCESS".equals(existing.getResult())) {
                return findLine(normalized, id, existing.getDetailId());
            }
        }
        if (INBOUND.equals(normalized)) {
            return scanInbound(id, request, action);
        }
        if (OUTBOUND.equals(normalized)) {
            return scanOutbound(id, request, action);
        }
        if (RETURN.equals(normalized)) {
            return scanReturn(id, request, action);
        }
        throw new IllegalArgumentException("不支持的单据类型: " + type);
    }

    private PdaStockLineDto scanInbound(Long inId, PdaStockScanRequest request, String action) {
        StockIn stockIn = requireStockIn(inId);
        if ("已取消".equals(stockIn.getInStatus())) {
            fail(INBOUND, inId, null, request, action, "入库单已取消");
        }
        StockInLabel label = stockInLabelMapper.selectByCodeWithDetail(request.getCode());
        if (label == null || !Objects.equals(label.getInId(), inId)) {
            // 手工录入允许按物料编码/批号定位未扫标签
            if ("MANUAL".equals(action)) {
                label = stockInLabelMapper.selectByInIdWithDetail(inId).stream()
                        .filter(l -> !"已扫".equals(l.getScanStatus()))
                        .filter(l -> request.getCode().equalsIgnoreCase(l.getMaterialCode())
                                || request.getCode().equalsIgnoreCase(l.getBatchNumber())
                                || request.getCode().equalsIgnoreCase(l.getMaterialName()))
                        .findFirst().orElse(null);
            }
            if (label == null) {
                fail(INBOUND, inId, null, request, action, "扫码不属于当前入库单");
            }
        }
        if ("已扫".equals(label.getScanStatus())) {
            throw new IllegalArgumentException("该标签已扫描，不重复计数");
        }
        if (!"已打印".equals(label.getPrintStatus())) {
            throw new IllegalArgumentException("标签未打印，不能扫码");
        }
        if (PENDING_PRINT.equals(stockIn.getScanStatus())) {
            throw new IllegalArgumentException("请先打印入库标签");
        }
        label.setScanStatus("已扫");
        label.setScannedBy(EntityUtils.getCurrentUserId());
        label.setScannedTime(LocalDateTime.now());
        stockInLabelMapper.updateById(label);
        recordScan(INBOUND, inId, label.getInDetailId(), label.getLabelCode(), action,
                "SUCCESS", null, request, null);
        return findLine(INBOUND, inId, label.getInDetailId());
    }

    private PdaStockLineDto scanOutbound(Long outId, PdaStockScanRequest request, String action) {
        StockOut stockOut = requireStockOut(outId);
        if (!"草稿".equals(stockOut.getOutStatus())) {
            throw new IllegalArgumentException("单据已出库，不能再扫码");
        }
        List<StockOutDetail> details = outboundDetails(outId);
        StockOutDetail target = null;
        for (StockOutDetail detail : details) {
            StockInLabel label = resolveExpectedLabel(detail);
            boolean match = label != null && request.getCode().equalsIgnoreCase(label.getLabelCode());
            if (!match && "MANUAL".equals(action)) {
                match = request.getCode().equalsIgnoreCase(detail.getItemCode())
                        || request.getCode().equalsIgnoreCase(detail.getBatchNumber())
                        || request.getCode().equalsIgnoreCase(detail.getItemName());
            }
            if (match) {
                target = detail;
                break;
            }
        }
        if (target == null) {
            fail(OUTBOUND, outId, null, request, action, "扫码不属于当前出库单");
        }
        if ("已扫".equals(target.getScanStatus())) {
            throw new IllegalArgumentException("该明细已扫码，不重复计数");
        }
        target.setScanStatus("已扫");
        target.setScannedBy(EntityUtils.getCurrentUserId());
        target.setScannedTime(LocalDateTime.now());
        stockOutDetailMapper.updateById(target);
        recordScan(OUTBOUND, outId, target.getOutDetailId(),
                defaultIfBlank(target.getExpectedLabelCode(), request.getCode()), action,
                "SUCCESS", null, request, null);
        return findLine(OUTBOUND, outId, target.getOutDetailId());
    }

    private PdaStockLineDto scanReturn(Long returnId, PdaStockScanRequest request, String action) {
        ReturnOrder order = requireReturnOrder(returnId);
        if ("已确认".equals(order.getScanStatus())) {
            throw new IllegalArgumentException("退库单已确认");
        }
        List<ReturnOrderDetail> details = returnDetails(returnId);
        ReturnOrderDetail target = null;
        for (ReturnOrderDetail detail : details) {
            StockInLabel label = resolveReturnExpectedLabel(detail);
            boolean match = label != null && request.getCode().equalsIgnoreCase(label.getLabelCode());
            if (!match && "MANUAL".equals(action)) {
                match = request.getCode().equalsIgnoreCase(detail.getMaterialCode())
                        || request.getCode().equalsIgnoreCase(detail.getBatchNo())
                        || request.getCode().equalsIgnoreCase(detail.getMaterialName());
            }
            if (match) {
                target = detail;
                break;
            }
        }
        if (target == null) {
            fail(RETURN, returnId, null, request, action, "扫码不属于当前退库单");
        }
        if ("已扫".equals(target.getScanStatus())) {
            throw new IllegalArgumentException("该明细已扫码，不重复计数");
        }
        target.setScanStatus("已扫");
        target.setScannedBy(EntityUtils.getCurrentUserId());
        target.setScannedTime(LocalDateTime.now());
        returnOrderDetailMapper.updateById(target);
        recordScan(RETURN, returnId, target.getId(),
                defaultIfBlank(target.getExpectedLabelCode(), request.getCode()), action,
                "SUCCESS", null, request, null);
        return findLine(RETURN, returnId, target.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PdaStockLineDto clearScan(String type, Long id, Long lineId) {
        String normalized = normalizeType(type);
        if (INBOUND.equals(normalized)) {
            StockInLabel label = stockInLabelMapper.selectByInDetailIdWithDetail(lineId);
            if (label == null || !Objects.equals(label.getInId(), id)) {
                throw new IllegalArgumentException("未找到对应标签");
            }
            if (!"已扫".equals(label.getScanStatus())) {
                throw new IllegalArgumentException("该标签尚未扫码");
            }
            label.setScanStatus("待扫");
            label.setScannedBy(null);
            label.setScannedTime(null);
            stockInLabelMapper.updateById(label);
            recordScan(INBOUND, id, lineId, label.getLabelCode(), "CLEAR",
                    "SUCCESS", null, null, "撤销扫码");
            return findLine(INBOUND, id, lineId);
        }
        if (OUTBOUND.equals(normalized)) {
            StockOutDetail detail = requireOutboundDetail(id, lineId);
            detail.setScanStatus("待扫码");
            detail.setScannedBy(null);
            detail.setScannedTime(null);
            stockOutDetailMapper.updateById(detail);
            recordScan(OUTBOUND, id, lineId, detail.getExpectedLabelCode(), "CLEAR",
                    "SUCCESS", null, null, "撤销扫码");
            return findLine(OUTBOUND, id, lineId);
        }
        if (RETURN.equals(normalized)) {
            ReturnOrderDetail detail = requireReturnDetail(id, lineId);
            detail.setScanStatus("待扫码");
            detail.setScannedBy(null);
            detail.setScannedTime(null);
            returnOrderDetailMapper.updateById(detail);
            recordScan(RETURN, id, lineId, detail.getExpectedLabelCode(), "CLEAR",
                    "SUCCESS", null, null, "撤销扫码");
            return findLine(RETURN, id, lineId);
        }
        throw new IllegalArgumentException("不支持的单据类型: " + type);
    }

    private void fail(String type, Long id, Long lineId, PdaStockScanRequest request,
                      String action, String reason) {
        recordScan(type, id, lineId, request == null ? null : request.getCode(), action,
                "FAIL", reason, request, null);
        throw new IllegalArgumentException(reason);
    }

    private void recordScan(String type, Long id, Long lineId, String code, String action,
                            String result, String failReason, PdaStockScanRequest request,
                            String remark) {
        StockScanRecord record = new StockScanRecord();
        record.setBizType(type);
        record.setBizId(id);
        record.setDetailId(lineId == null ? 0L : lineId);
        record.setLabelCode(code);
        record.setAction(action);
        record.setResult(result);
        record.setFailReason(failReason);
        record.setOperatorId(EntityUtils.getCurrentUserId());
        record.setOperatorName(currentUserName());
        record.setDeviceNo(request == null ? null : request.getDeviceNo());
        record.setRequestId(request == null ? null : request.getRequestId());
        record.setRemark(remark != null ? remark
                : request != null && "MANUAL".equals(action)
                ? "授权:" + request.getAuthorizer() + " 原因:" + request.getReason() : null);
        record.setScanTime(LocalDateTime.now());
        record.setIsDeleted(0);
        record.setVersion(0);
        stockScanRecordMapper.insert(record);
    }

    // endregion

    // region 确认

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PdaStockTaskDetailDto confirm(String type, Long id, PdaStockConfirmRequest request) {
        verifyPassword(request);
        String normalized = normalizeType(type);
        if (INBOUND.equals(normalized)) {
            StockIn stockIn = requireStockIn(id);
            if (DONE_IN.equals(stockIn.getScanStatus()) && "已入库".equals(stockIn.getInStatus())) {
                return getTask(INBOUND, id);
            }
            List<StockInLabel> labels = ensureInboundLabels(stockIn);
            List<StockInLabel> unscanned = labels.stream()
                    .filter(l -> !"已扫".equals(l.getScanStatus()))
                    .collect(Collectors.toList());
            if (!unscanned.isEmpty()) {
                throw new IllegalArgumentException("还有 " + unscanned.size()
                        + " 条明细未扫码，不能确认入库");
            }
            if (!"草稿".equals(stockIn.getInStatus())) {
                throw new IllegalArgumentException("当前入库单状态不能确认");
            }
            stockInService.confirmStockIn(id);
            StockIn fresh = stockInMapper.selectById(id);
            fresh.setScanStatus(DONE_IN);
            fresh.setConfirmBy(EntityUtils.getCurrentUserId());
            fresh.setConfirmTime(LocalDateTime.now());
            fresh.setConfirmDeviceNo(request == null ? null : request.getDeviceNo());
            stockInMapper.updateById(fresh);
            return getTask(INBOUND, id);
        }
        if (OUTBOUND.equals(normalized)) {
            StockOut stockOut = requireStockOut(id);
            if (DONE_OUT.equals(stockOut.getScanStatus()) && "已出库".equals(stockOut.getOutStatus())) {
                return getTask(OUTBOUND, id);
            }
            List<StockOutDetail> details = outboundDetails(id);
            List<StockOutDetail> unscanned = details.stream()
                    .filter(d -> !"已扫".equals(d.getScanStatus()))
                    .collect(Collectors.toList());
            if (!unscanned.isEmpty()) {
                throw new IllegalArgumentException("还有 " + unscanned.size()
                        + " 条明细未扫码，不能确认出库");
            }
            if (!"草稿".equals(stockOut.getOutStatus())) {
                throw new IllegalArgumentException("当前出库单状态不能确认");
            }
            stockOutService.confirmStockOut(id);
            StockOut fresh = stockOutMapper.selectById(id);
            fresh.setScanStatus(DONE_OUT);
            fresh.setConfirmBy(EntityUtils.getCurrentUserId());
            fresh.setConfirmTime(LocalDateTime.now());
            fresh.setConfirmDeviceNo(request == null ? null : request.getDeviceNo());
            stockOutMapper.updateById(fresh);
            return getTask(OUTBOUND, id);
        }
        if (RETURN.equals(normalized)) {
            throw new IllegalArgumentException("退库扫码确认流程尚未接入");
        }
        throw new IllegalArgumentException("不支持的单据类型: " + type);
    }

    private void verifyPassword(PdaStockConfirmRequest request) {
        if (request == null || !StringUtils.hasText(request.getPassword())) {
            throw new IllegalArgumentException("请输入签名密码");
        }
        User user = userMapper.selectById(EntityUtils.getCurrentUserId());
        if (user == null || !PasswordHasher.verifyPassword(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("签名密码错误");
        }
    }

    // endregion

    // region 记录

    @Override
    public List<PdaStockRecordDto> listRecords(String keyword, String type, String warehouse,
                                               LocalDate from, LocalDate to) {
        LocalDateTime fromTime = from == null ? null : from.atStartOfDay();
        LocalDateTime toTime = to == null ? null : to.plusDays(1).atStartOfDay();
        List<PdaStockRecordDto> records = new ArrayList<>();

        List<StockIn> stockIns = stockInMapper.selectList(new LambdaQueryWrapper<StockIn>()
                .eq(StockIn::getIsDeleted, 0)
                .eq(StockIn::getInStatus, "已入库")
                .orderByDesc(StockIn::getInDate));
        for (StockIn stockIn : stockIns) {
            PdaStockRecordDto dto = new PdaStockRecordDto();
            dto.setType("入库");
            dto.setDocCode(stockIn.getInCode());
            dto.setWarehouseName(warehouseName(stockIn.getProdUnitId()));
            dto.setDocDate(stockIn.getInDate());
            long lineCount = stockInDetailMapper.selectCount(new QueryWrapper<StockInDetail>()
                    .eq("in_id", stockIn.getInId()).eq("is_deleted", 0));
            dto.setSummary(defaultIfBlank(stockIn.getInType(), "入库") + " · "
                    + lineCount + " 种物料");
            dto.setOperatorName(userName(stockIn.getConfirmBy() != null
                    ? stockIn.getConfirmBy() : stockIn.getUpdatedBy()));
            dto.setDeviceNo(stockIn.getConfirmDeviceNo());
            dto.setRecordTime(stockIn.getConfirmTime() == null
                    ? stockIn.getInDate() : stockIn.getConfirmTime());
            dto.setStatus("已入库");
            records.add(dto);
        }

        List<StockOut> stockOuts = stockOutMapper.selectList(new LambdaQueryWrapper<StockOut>()
                .eq(StockOut::getIsDeleted, 0)
                .eq(StockOut::getOutStatus, "已出库")
                .orderByDesc(StockOut::getOutDate));
        for (StockOut stockOut : stockOuts) {
            PdaStockRecordDto dto = new PdaStockRecordDto();
            dto.setType("出库");
            dto.setDocCode(stockOut.getOutCode());
            dto.setWarehouseName(warehouseName(stockOut.getProdUnitId()));
            dto.setDocDate(stockOut.getOutDate());
            long lineCount = stockOutDetailMapper.selectCount(new QueryWrapper<StockOutDetail>()
                    .eq("out_id", stockOut.getOutId()).eq("is_deleted", 0));
            dto.setSummary(defaultIfBlank(stockOut.getOutType(), "出库") + " · "
                    + lineCount + " 种物料");
            dto.setOperatorName(userName(stockOut.getConfirmBy() != null
                    ? stockOut.getConfirmBy() : stockOut.getUpdatedBy()));
            dto.setDeviceNo(stockOut.getConfirmDeviceNo());
            dto.setRecordTime(stockOut.getConfirmTime() == null
                    ? stockOut.getOutDate() : stockOut.getConfirmTime());
            dto.setStatus("已出库");
            records.add(dto);
        }

        // 补打记录
        List<StockLabelPrintLog> reprints = stockLabelPrintLogMapper.selectList(
                new QueryWrapper<StockLabelPrintLog>()
                        .eq("action", "REPRINT")
                        .eq("is_deleted", 0)
                        .orderByDesc("created_time"));
        for (StockLabelPrintLog log : reprints) {
            StockIn stockIn = stockInMapper.selectById(log.getInId());
            StockInLabel label = log.getLabelCode() == null ? null
                    : stockInLabelMapper.selectByCodeWithDetail(log.getLabelCode());
            PdaStockRecordDto dto = new PdaStockRecordDto();
            dto.setType("补打");
            dto.setDocCode(log.getLabelCode());
            dto.setWarehouseName(stockIn == null ? null : warehouseName(stockIn.getProdUnitId()));
            dto.setDocDate(log.getCreatedTime());
            dto.setSummary(label == null ? "补打标签"
                    : label.getMaterialName() + " · " + label.getBatchNumber());
            dto.setOperatorName(log.getOperatorName());
            dto.setDeviceNo(log.getDeviceNo());
            dto.setRecordTime(log.getCreatedTime());
            dto.setStatus("补打");
            records.add(dto);
        }

        // 手工录入记录
        List<StockScanRecord> manuals = stockScanRecordMapper.selectList(
                new QueryWrapper<StockScanRecord>()
                        .eq("action", "MANUAL")
                        .eq("result", "SUCCESS")
                        .eq("is_deleted", 0)
                        .orderByDesc("scan_time"));
        for (StockScanRecord scan : manuals) {
            StockInLabel label = scan.getLabelCode() == null ? null
                    : stockInLabelMapper.selectByCodeWithDetail(scan.getLabelCode());
            PdaStockRecordDto dto = new PdaStockRecordDto();
            dto.setType("手工录入");
            dto.setDocCode(scan.getLabelCode());
            dto.setWarehouseName(label == null ? null : warehouseName(label.getProdUnitId()));
            dto.setDocDate(scan.getScanTime());
            dto.setSummary(label == null ? "手工录入"
                    : label.getMaterialName() + " · " + label.getBatchNumber());
            dto.setOperatorName(scan.getOperatorName());
            dto.setDeviceNo(scan.getDeviceNo());
            dto.setRecordTime(scan.getScanTime());
            dto.setStatus("授权");
            records.add(dto);
        }

        return records.stream()
                .filter(r -> !StringUtils.hasText(type) || type.equals(r.getType()))
                .filter(r -> !StringUtils.hasText(warehouse) || warehouse.equals(r.getWarehouseName()))
                .filter(r -> fromTime == null || (r.getRecordTime() != null && !r.getRecordTime().isBefore(fromTime)))
                .filter(r -> toTime == null || (r.getRecordTime() != null && r.getRecordTime().isBefore(toTime)))
                .filter(r -> {
                    if (!StringUtils.hasText(keyword)) {
                        return true;
                    }
                    String text = joinText(r.getDocCode(), r.getSummary(), r.getWarehouseName(),
                            r.getOperatorName(), r.getType());
                    return text.toLowerCase().contains(keyword.toLowerCase());
                })
                .sorted(Comparator.comparing(PdaStockRecordDto::getRecordTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    // endregion

    // region 明细映射

    private List<PdaStockLineDto> inboundLines(Long inId) {
        StockIn stockIn = requireStockIn(inId);
        Map<Long, StockInDetail> detailMap = stockInDetailMapper.selectList(
                        new QueryWrapper<StockInDetail>()
                                .eq("in_id", inId)
                                .eq("is_deleted", 0))
                .stream().collect(Collectors.toMap(StockInDetail::getInDetailId, Function.identity()));
        List<PdaStockLineDto> lines = new ArrayList<>();
        for (StockInLabel label : ensureInboundLabels(stockIn)) {
            StockInDetail detail = detailMap.get(label.getInDetailId());
            PdaStockLineDto line = new PdaStockLineDto();
            line.setLineId(label.getInDetailId());
            line.setMaterialCode(detail != null ? detail.getItemCode() : label.getMaterialCode());
            line.setMaterialName(detail != null ? detail.getItemName() : label.getMaterialName());
            line.setBatchNumber(detail != null ? detail.getBatchNumber() : label.getBatchNumber());
            line.setQuantity(detail != null ? detail.getQuantity() : label.getQuantity());
            line.setUnitName(detail != null ? detail.getUnitName() : label.getUnitName());
            line.setExpiryDate(detail == null ? null : detail.getExpiryDate());
            line.setLocation(detail == null ? null : detail.getStorageLocation());
            line.setLabelCode(label.getLabelCode());
            line.setQrContent(label.getQrContent());
            line.setPrintStatus(label.getPrintStatus());
            line.setScanStatus(label.getScanStatus());
            line.setScannedBy(label.getScannedBy());
            line.setScannedTime(label.getScannedTime());
            line.setReprintCount(label.getReprintCount());
            lines.add(line);
        }
        return lines;
    }

    private List<PdaStockLineDto> outboundLines(Long outId) {
        List<PdaStockLineDto> lines = new ArrayList<>();
        for (StockOutDetail detail : outboundDetails(outId)) {
            StockInLabel label = resolveExpectedLabel(detail);
            PdaStockLineDto line = new PdaStockLineDto();
            line.setLineId(detail.getOutDetailId());
            line.setMaterialCode(detail.getItemCode());
            line.setMaterialName(detail.getItemName());
            line.setBatchNumber(detail.getBatchNumber());
            line.setQuantity(detail.getQuantity());
            line.setUnitName(detail.getUnitName());
            line.setLabelCode(label == null ? detail.getExpectedLabelCode()
                    : label.getLabelCode());
            line.setExpectedLabelCode(detail.getExpectedLabelCode());
            line.setQrContent(label == null ? null : label.getQrContent());
            line.setScanStatus(detail.getScanStatus());
            line.setScannedBy(detail.getScannedBy());
            line.setScannedTime(detail.getScannedTime());
            lines.add(line);
        }
        return lines;
    }

    private List<PdaStockLineDto> returnLines(Long returnId) {
        List<PdaStockLineDto> lines = new ArrayList<>();
        for (ReturnOrderDetail detail : returnDetails(returnId)) {
            StockInLabel label = resolveReturnExpectedLabel(detail);
            PdaStockLineDto line = new PdaStockLineDto();
            line.setLineId(detail.getId());
            line.setMaterialCode(detail.getMaterialCode());
            line.setMaterialName(detail.getMaterialName());
            line.setBatchNumber(detail.getBatchNo());
            line.setQuantity(detail.getReturnQuantity());
            line.setUnitName(detail.getUnit());
            line.setLabelCode(label == null ? detail.getExpectedLabelCode()
                    : label.getLabelCode());
            line.setExpectedLabelCode(detail.getExpectedLabelCode());
            line.setQrContent(label == null ? null : label.getQrContent());
            line.setScanStatus(detail.getScanStatus());
            line.setScannedBy(detail.getScannedBy());
            line.setScannedTime(detail.getScannedTime());
            lines.add(line);
        }
        return lines;
    }

    private PdaStockLineDto findLine(String type, Long id, Long lineId) {
        List<PdaStockLineDto> lines;
        if (INBOUND.equals(type)) {
            lines = inboundLines(id);
        } else if (OUTBOUND.equals(type)) {
            lines = outboundLines(id);
        } else {
            lines = returnLines(id);
        }
        return lines.stream().filter(l -> Objects.equals(l.getLineId(), lineId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未找到对应明细"));
    }

    private List<StockInLabel> ensureInboundLabels(StockIn stockIn) {
        List<StockInLabel> labels = stockLabelService.ensureLabels(stockIn.getInId());
        if (labels.isEmpty()) {
            return List.of();
        }
        return stockInLabelMapper.selectByInIdWithDetail(stockIn.getInId()).stream()
                .sorted(Comparator.comparing(StockInLabel::getInDetailId))
                .collect(Collectors.toList());
    }

    private List<StockOutDetail> outboundDetails(Long outId) {
        return stockOutDetailMapper.selectList(new QueryWrapper<StockOutDetail>()
                .eq("out_id", outId)
                .eq("is_deleted", 0)
                .orderByAsc("out_detail_id"));
    }

    private List<ReturnOrderDetail> returnDetails(Long returnId) {
        return returnOrderDetailMapper.selectList(new QueryWrapper<ReturnOrderDetail>()
                .eq("return_order_id", returnId)
                .eq("is_deleted", 0)
                .orderByAsc("id"));
    }

    /**
     * 解析出库明细对应的入库标签，并回填 expected_label_code
     */
    private StockInLabel resolveExpectedLabel(StockOutDetail detail) {
        if (StringUtils.hasText(detail.getExpectedLabelCode())) {
            StockInLabel label = stockInLabelMapper.selectByCodeWithDetail(detail.getExpectedLabelCode());
            if (label != null) {
                return label;
            }
        }
        Stock stock = detail.getStockId() == null ? null : stockMapper.selectById(detail.getStockId());
        if (stock == null) {
            return null;
        }
        StockInLabel label = null;
        if (stock.getStockInDetailId() != null) {
            label = stockInLabelMapper.selectByInDetailIdWithDetail(stock.getStockInDetailId());
        }
        if (label == null && stock.getStockInId() != null) {
            stockLabelService.ensureLabels(stock.getStockInId());
            if (stock.getStockInDetailId() != null) {
                label = stockInLabelMapper.selectByInDetailIdWithDetail(stock.getStockInDetailId());
            }
            if (label == null) {
                label = stockInLabelMapper.selectByInIdWithDetail(stock.getStockInId()).stream()
                        .filter(l -> Objects.equals(l.getMaterialCode(), detail.getItemCode())
                                && Objects.equals(l.getBatchNumber(), detail.getBatchNumber()))
                        .findFirst().orElse(null);
            }
        }
        if (label != null && !Objects.equals(detail.getExpectedLabelCode(), label.getLabelCode())) {
            detail.setExpectedLabelCode(label.getLabelCode());
            stockOutDetailMapper.updateById(detail);
        }
        return label;
    }

    private StockInLabel resolveReturnExpectedLabel(ReturnOrderDetail detail) {
        if (StringUtils.hasText(detail.getExpectedLabelCode())) {
            StockInLabel label = stockInLabelMapper.selectByCodeWithDetail(detail.getExpectedLabelCode());
            if (label != null) {
                return label;
            }
        }
        if (detail.getOutDetailId() == null) {
            return null;
        }
        StockOutDetail outDetail = stockOutDetailMapper.selectById(detail.getOutDetailId());
        if (outDetail == null) {
            return null;
        }
        StockInLabel label = resolveExpectedLabel(outDetail);
        if (label != null && !Objects.equals(detail.getExpectedLabelCode(), label.getLabelCode())) {
            detail.setExpectedLabelCode(label.getLabelCode());
            returnOrderDetailMapper.updateById(detail);
        }
        return label;
    }

    // endregion

    // region 通用工具

    private String normalizeType(String type) {
        if (!StringUtils.hasText(type)) {
            return null;
        }
        String value = type.trim().toUpperCase();
        if (!INBOUND.equals(value) && !OUTBOUND.equals(value) && !RETURN.equals(value)) {
            throw new IllegalArgumentException("不支持的单据类型: " + type);
        }
        return value;
    }

    private StockIn requireStockIn(Long id) {
        StockIn stockIn = id == null ? null : stockInMapper.selectById(id);
        if (stockIn == null || Integer.valueOf(1).equals(stockIn.getIsDeleted())) {
            throw new IllegalArgumentException("入库单不存在");
        }
        return stockIn;
    }

    private StockOut requireStockOut(Long id) {
        StockOut stockOut = id == null ? null : stockOutMapper.selectById(id);
        if (stockOut == null || Integer.valueOf(1).equals(stockOut.getIsDeleted())) {
            throw new IllegalArgumentException("出库单不存在");
        }
        return stockOut;
    }

    private ReturnOrder requireReturnOrder(Long id) {
        ReturnOrder order = id == null ? null : returnOrderMapper.selectById(id);
        if (order == null || Integer.valueOf(1).equals(order.getIsDeleted())) {
            throw new IllegalArgumentException("退库单不存在");
        }
        return order;
    }

    private StockOutDetail requireOutboundDetail(Long outId, Long lineId) {
        StockOutDetail detail = stockOutDetailMapper.selectById(lineId);
        if (detail == null || !Objects.equals(detail.getOutId(), outId)) {
            throw new IllegalArgumentException("出库明细不存在");
        }
        return detail;
    }

    private ReturnOrderDetail requireReturnDetail(Long returnId, Long lineId) {
        ReturnOrderDetail detail = returnOrderDetailMapper.selectById(lineId);
        if (detail == null || !Objects.equals(detail.getReturnOrderId(), returnId)) {
            throw new IllegalArgumentException("退库明细不存在");
        }
        return detail;
    }

    private String warehouseName(Long prodUnitId) {
        if (prodUnitId == null) {
            return null;
        }
        ProductionUnit unit = productionUnitMapper.selectById(prodUnitId);
        return unit == null ? null : unit.getProdUnitName();
    }

    private String userName(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = userMapper.selectById(userId);
        return user == null ? null : user.getUserName();
    }

    private String currentUserName() {
        return userName(EntityUtils.getCurrentUserId());
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private String joinText(String... values) {
        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                sb.append(value).append(' ');
            }
        }
        return sb.toString();
    }

    // endregion
}
