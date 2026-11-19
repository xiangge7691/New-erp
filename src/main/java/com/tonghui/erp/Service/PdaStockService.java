package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Dto.Pda.PdaLabelDto;
import com.tonghui.erp.Common.Dto.Pda.PdaLabelPrintRequest;
import com.tonghui.erp.Common.Dto.Pda.PdaLabelReprintRequest;
import com.tonghui.erp.Common.Dto.Pda.PdaStockConfirmRequest;
import com.tonghui.erp.Common.Dto.Pda.PdaStockLineDto;
import com.tonghui.erp.Common.Dto.Pda.PdaStockRecordDto;
import com.tonghui.erp.Common.Dto.Pda.PdaStockScanRequest;
import com.tonghui.erp.Common.Dto.Pda.PdaStockTaskDetailDto;
import com.tonghui.erp.Common.Dto.Pda.PdaStockTaskDto;

import java.time.LocalDate;
import java.util.List;

/**
 * PDA 仓储扫码服务
 */
public interface PdaStockService {

    /**
     * 仓储任务列表
     *
     * @param type    INBOUND/OUTBOUND/RETURN，空表示全部
     * @param status  状态
     * @param keyword 单号/物料关键字
     * @return 任务列表
     */
    List<PdaStockTaskDto> listTasks(String type, String status, String keyword);

    /**
     * 任务详情
     *
     * @param type 类型
     * @param id   单据ID
     * @return 详情
     */
    PdaStockTaskDetailDto getTask(String type, Long id);

    /**
     * 确保入库单标签已生成
     *
     * @param inId 入库单ID
     * @return 详情
     */
    PdaStockTaskDetailDto generateLabels(Long inId);

    /**
     * 标记标签已打印
     *
     * @param inId    入库单ID
     * @param request 请求
     * @return 详情
     */
    PdaStockTaskDetailDto printLabels(Long inId, PdaLabelPrintRequest request);

    /**
     * 补打标签
     *
     * @param inId    入库单ID
     * @param lineId  明细ID
     * @param request 请求
     * @return 标签
     */
    PdaLabelDto reprint(Long inId, Long lineId, PdaLabelReprintRequest request);

    /**
     * 补打候选标签
     *
     * @param keyword   关键字
     * @param warehouse 仓库名称
     * @param status    可补打/已作废
     * @return 标签列表
     */
    List<PdaLabelDto> listLabels(String keyword, String warehouse, String status);

    /**
     * 单条扫码
     *
     * @param type    类型
     * @param id      单据ID
     * @param request 请求
     * @return 更新的明细行
     */
    PdaStockLineDto scan(String type, Long id, PdaStockScanRequest request);

    /**
     * 手工录入（主管授权）
     *
     * @param type    类型
     * @param id      单据ID
     * @param request 请求
     * @return 更新的明细行
     */
    PdaStockLineDto manualScan(String type, Long id, PdaStockScanRequest request);

    /**
     * 撤销单条扫码
     *
     * @param type   类型
     * @param id     单据ID
     * @param lineId 明细ID
     * @return 更新的明细行
     */
    PdaStockLineDto clearScan(String type, Long id, Long lineId);

    /**
     * 整单确认（作业签名）
     *
     * @param type    类型
     * @param id      单据ID
     * @param request 请求
     * @return 详情
     */
    PdaStockTaskDetailDto confirm(String type, Long id, PdaStockConfirmRequest request);

    /**
     * 作业记录
     *
     * @param keyword   关键字
     * @param type      类型
     * @param warehouse 仓库名称
     * @param from      日期起
     * @param to        日期止
     * @return 记录
     */
    List<PdaStockRecordDto> listRecords(String keyword, String type, String warehouse,
                                        LocalDate from, LocalDate to);
}
