package com.tonghui.erp.Service;

import com.tonghui.erp.Data.Entity.StockInLabel;

import java.util.List;

/**
 * 入库逐条标签服务
 */
public interface StockLabelService {

    /**
     * 确保指定入库单的每条明细都有标签，缺失时生成
     *
     * @param inId 入库单ID
     * @return 标签列表
     */
    List<StockInLabel> ensureLabels(Long inId);

    /**
     * 查询入库单标签
     *
     * @param inId 入库单ID
     * @return 标签列表
     */
    List<StockInLabel> listByInId(Long inId);

    /**
     * 构造二维码内容：ERP1|IC|labelCode|CRC16
     *
     * @param labelCode 标签码
     * @return 二维码内容
     */
    String buildQrContent(String labelCode);
}
