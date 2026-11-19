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

    /**
     * 解析扫码内容：兼容纯标签码与 ERP1|IC|标签码|CRC16 完整二维码
     *
     * @param qrContent 扫码枪读到的原始内容
     * @return 纯标签码
     */
    String parseLabelCode(String qrContent);
}
