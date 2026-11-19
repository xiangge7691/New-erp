package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Constants.OperationType;
import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.Pda.PdaLabelDto;
import com.tonghui.erp.Common.Dto.Pda.PdaLabelPrintRequest;
import com.tonghui.erp.Common.Dto.Pda.PdaLabelReprintRequest;
import com.tonghui.erp.Common.Dto.Pda.PdaStockConfirmRequest;
import com.tonghui.erp.Common.Dto.Pda.PdaStockLineDto;
import com.tonghui.erp.Common.Dto.Pda.PdaStockRecordDto;
import com.tonghui.erp.Common.Dto.Pda.PdaStockScanRequest;
import com.tonghui.erp.Common.Dto.Pda.PdaStockTaskDetailDto;
import com.tonghui.erp.Common.Dto.Pda.PdaStockTaskDto;
import com.tonghui.erp.Common.annotation.OperationLog;
import com.tonghui.erp.Service.PdaStockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * PDA 仓储扫码控制器
 * <p>
 * 仅提供移动端页面所需接口；不做接口级权限校验。
 * </p>
 */
@RestController
@RequestMapping("/api/pda/stock")
public class PdaStockController extends BaseController {

    @Autowired
    private PdaStockService pdaStockService;

    /**
     * 仓储任务列表
     *
     * 示例请求：
     * GET /api/pda/stock/tasks?type=INBOUND&status=待扫码&keyword=IN2026
     *
     * @param type    INBOUND/OUTBOUND/RETURN
     * @param status  状态
     * @param keyword 单号关键字
     * @return 任务列表
     */
    @GetMapping("/tasks")
    public ApiResponse<List<PdaStockTaskDto>> tasks(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        try {
            return success(pdaStockService.listTasks(type, status, keyword));
        } catch (Exception ex) {
            return exception(ex, "查询PDA仓储任务");
        }
    }

    /**
     * 任务详情
     *
     * 示例请求：
     * GET /api/pda/stock/tasks/INBOUND/1
     *
     * @param type 类型
     * @param id   单据ID
     * @return 详情
     */
    @GetMapping("/tasks/{type}/{id}")
    public ApiResponse<PdaStockTaskDetailDto> task(@PathVariable String type,
                                                   @PathVariable Long id) {
        try {
            return success(pdaStockService.getTask(type, id));
        } catch (Exception ex) {
            return exception(ex, "查询PDA仓储任务详情");
        }
    }

    /**
     * 生成入库标签
     *
     * 示例请求：
     * POST /api/pda/stock/tasks/INBOUND/1/labels/generate
     *
     * @param id 入库单ID
     * @return 详情
     */
    @OperationLog(module = "PDA仓储", type = OperationType.CREATE, desc = "生成入库标签",
            businessType = "STOCK_IN", businessId = "#id")
    @PostMapping("/tasks/INBOUND/{id}/labels/generate")
    public ApiResponse<PdaStockTaskDetailDto> generateLabels(@PathVariable Long id) {
        try {
            return success(pdaStockService.generateLabels(id), "标签已生成");
        } catch (Exception ex) {
            return exception(ex, "生成入库标签");
        }
    }

    /**
     * 标记标签已打印
     *
     * 示例请求：
     * POST /api/pda/stock/tasks/INBOUND/1/labels/print
     * {"lineIds":[10,11],"deviceNo":"PDA-A03"}
     *
     * @param id      入库单ID
     * @param request 请求
     * @return 详情
     */
    @OperationLog(module = "PDA仓储", type = OperationType.UPDATE, desc = "打印入库标签",
            businessType = "STOCK_IN", businessId = "#id")
    @PostMapping("/tasks/INBOUND/{id}/labels/print")
    public ApiResponse<PdaStockTaskDetailDto> printLabels(@PathVariable Long id,
                                                          @RequestBody(required = false) PdaLabelPrintRequest request) {
        try {
            return success(pdaStockService.printLabels(id, request), "已记录打印");
        } catch (Exception ex) {
            return exception(ex, "打印入库标签");
        }
    }

    /**
     * 补打标签
     *
     * 示例请求：
     * POST /api/pda/stock/tasks/INBOUND/1/labels/10/reprint
     * {"reason":"标签破损","deviceNo":"PDA-A03"}
     *
     * @param id      入库单ID
     * @param lineId  明细ID
     * @param request 请求
     * @return 标签
     */
    @OperationLog(module = "PDA仓储", type = OperationType.UPDATE, desc = "补打入库标签",
            businessType = "STOCK_IN", businessId = "#id")
    @PostMapping("/tasks/INBOUND/{id}/labels/{lineId}/reprint")
    public ApiResponse<PdaLabelDto> reprint(@PathVariable Long id,
                                            @PathVariable Long lineId,
                                            @RequestBody PdaLabelReprintRequest request) {
        try {
            return success(pdaStockService.reprint(id, lineId, request), "已记录补打");
        } catch (Exception ex) {
            return exception(ex, "补打入库标签");
        }
    }

    /**
     * 补打候选标签（支持关键词、仓库、状态筛选）
     *
     * 示例请求：
     * GET /api/pda/stock/labels?keyword=当归&warehouse=原料仓库&status=可补打
     *
     * @param keyword   关键字
     * @param warehouse 仓库名称
     * @param status    可补打/已作废
     * @return 标签列表
     */
    @GetMapping("/labels")
    public ApiResponse<List<PdaLabelDto>> labels(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String warehouse,
            @RequestParam(required = false) String status) {
        try {
            return success(pdaStockService.listLabels(keyword, warehouse, status));
        } catch (Exception ex) {
            return exception(ex, "查询PDA补打标签");
        }
    }

    /**
     * 单条扫码
     *
     * 示例请求：
     * POST /api/pda/stock/tasks/INBOUND/1/scan
     * {"code":"in1001","deviceNo":"PDA-A03","requestId":"uuid"}
     *
     * @param type    类型
     * @param id      单据ID
     * @param request 请求
     * @return 明细行
     */
    @OperationLog(module = "PDA仓储", type = OperationType.UPDATE, desc = "仓储扫码",
            businessType = "STOCK_SCAN", businessId = "#id")
    @PostMapping("/tasks/{type}/{id}/scan")
    public ApiResponse<PdaStockLineDto> scan(@PathVariable String type,
                                             @PathVariable Long id,
                                             @RequestBody PdaStockScanRequest request) {
        try {
            return success(pdaStockService.scan(type, id, request), "扫码成功");
        } catch (Exception ex) {
            return exception(ex, "仓储扫码");
        }
    }

    /**
     * 手工录入（主管授权）
     *
     * 示例请求：
     * POST /api/pda/stock/tasks/INBOUND/1/manual-scan
     * {"code":"Z046","authorizer":"王主任","reason":"标签破损","deviceNo":"PDA-A03"}
     *
     * @param type    类型
     * @param id      单据ID
     * @param request 请求
     * @return 明细行
     */
    @OperationLog(module = "PDA仓储", type = OperationType.UPDATE, desc = "手工录入",
            businessType = "STOCK_SCAN", businessId = "#id")
    @PostMapping("/tasks/{type}/{id}/manual-scan")
    public ApiResponse<PdaStockLineDto> manualScan(@PathVariable String type,
                                                   @PathVariable Long id,
                                                   @RequestBody PdaStockScanRequest request) {
        try {
            return success(pdaStockService.manualScan(type, id, request), "手工录入成功");
        } catch (Exception ex) {
            return exception(ex, "手工录入");
        }
    }

    /**
     * 撤销单条扫码
     *
     * 示例请求：
     * DELETE /api/pda/stock/tasks/INBOUND/1/lines/10/scan
     *
     * @param type   类型
     * @param id     单据ID
     * @param lineId 明细ID
     * @return 明细行
     */
    @OperationLog(module = "PDA仓储", type = OperationType.UPDATE, desc = "撤销扫码",
            businessType = "STOCK_SCAN", businessId = "#id")
    @DeleteMapping("/tasks/{type}/{id}/lines/{lineId}/scan")
    public ApiResponse<PdaStockLineDto> clearScan(@PathVariable String type,
                                                  @PathVariable Long id,
                                                  @PathVariable Long lineId) {
        try {
            return success(pdaStockService.clearScan(type, id, lineId), "已撤销扫码");
        } catch (Exception ex) {
            return exception(ex, "撤销扫码");
        }
    }

    /**
     * 整单确认（作业签名）
     *
     * 示例请求：
     * POST /api/pda/stock/tasks/INBOUND/1/confirm
     * {"password":"pda123","deviceNo":"PDA-A03","requestId":"uuid"}
     *
     * @param type    类型
     * @param id      单据ID
     * @param request 请求
     * @return 详情
     */
    @OperationLog(module = "PDA仓储", type = OperationType.UPDATE, desc = "仓储整单确认",
            businessType = "STOCK_SCAN", businessId = "#id")
    @PostMapping("/tasks/{type}/{id}/confirm")
    public ApiResponse<PdaStockTaskDetailDto> confirm(@PathVariable String type,
                                                      @PathVariable Long id,
                                                      @RequestBody PdaStockConfirmRequest request) {
        try {
            return success(pdaStockService.confirm(type, id, request), "确认成功");
        } catch (Exception ex) {
            return exception(ex, "仓储整单确认");
        }
    }

    /**
     * 作业记录
     *
     * 示例请求：
     * GET /api/pda/stock/records?keyword=IN2026&type=入库&warehouse=原料仓库&from=2026-11-01&to=2026-11-30
     *
     * @param keyword   关键字
     * @param type      类型
     * @param warehouse 仓库
     * @param from      日期起
     * @param to        日期止
     * @return 记录
     */
    @GetMapping("/records")
    public ApiResponse<List<PdaStockRecordDto>> records(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String warehouse,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        try {
            return success(pdaStockService.listRecords(keyword, type, warehouse, from, to));
        } catch (Exception ex) {
            return exception(ex, "查询PDA作业记录");
        }
    }
}
