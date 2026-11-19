package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tonghui.erp.Data.Entity.StockIn;
import com.tonghui.erp.Data.Entity.StockInDetail;
import com.tonghui.erp.Data.Entity.StockInLabel;
import com.tonghui.erp.Data.mapper.StockInDetailMapper;
import com.tonghui.erp.Data.mapper.StockInLabelMapper;
import com.tonghui.erp.Data.mapper.StockInMapper;
import com.tonghui.erp.Service.StockLabelService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 入库逐条标签服务实现
 */
@Service
public class StockLabelServiceImpl implements StockLabelService {

    private final StockInMapper stockInMapper;
    private final StockInDetailMapper stockInDetailMapper;
    private final StockInLabelMapper stockInLabelMapper;

    public StockLabelServiceImpl(StockInMapper stockInMapper,
                                 StockInDetailMapper stockInDetailMapper,
                                 StockInLabelMapper stockInLabelMapper) {
        this.stockInMapper = stockInMapper;
        this.stockInDetailMapper = stockInDetailMapper;
        this.stockInLabelMapper = stockInLabelMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<StockInLabel> ensureLabels(Long inId) {
        StockIn stockIn = stockInMapper.selectById(inId);
        if (stockIn == null) {
            throw new IllegalArgumentException("入库单不存在");
        }
        List<StockInDetail> details = stockInDetailMapper.selectList(
                new QueryWrapper<StockInDetail>()
                        .eq("in_id", inId)
                        .eq("is_deleted", 0)
                        .orderByAsc("in_detail_id"));
        if (details.isEmpty()) {
            return List.of();
        }
        List<StockInLabel> existing = stockInLabelMapper.selectList(
                new QueryWrapper<StockInLabel>()
                        .eq("in_id", inId)
                        .eq("is_deleted", 0));
        Map<Long, StockInLabel> byDetail = existing.stream()
                .collect(Collectors.toMap(StockInLabel::getInDetailId, Function.identity(), (a, b) -> a));

        List<StockInLabel> result = new ArrayList<>(existing);
        for (int i = 0; i < details.size(); i++) {
            StockInDetail detail = details.get(i);
            if (byDetail.containsKey(detail.getInDetailId())) {
                continue;
            }
            StockInLabel label = buildLabel(inId, detail, i + 1);
            for (int retry = 0; retry < 5; retry++) {
                try {
                    stockInLabelMapper.insert(label);
                    result.add(label);
                    break;
                } catch (DuplicateKeyException e) {
                    // 编码冲突时追加明细ID后重试
                    label.setId(null);
                    label.setLabelCode("in" + inId + "D" + detail.getInDetailId()
                            + (retry == 0 ? "" : "R" + retry));
                    label.setQrContent(buildQrContent(label.getLabelCode()));
                }
            }
        }
        return result;
    }

    @Override
    public List<StockInLabel> listByInId(Long inId) {
        return stockInLabelMapper.selectByInIdWithDetail(inId);
    }

    @Override
    public String buildQrContent(String labelCode) {
        return "ERP1|IC|" + labelCode + "|" + crc16(labelCode);
    }

    private StockInLabel buildLabel(Long inId, StockInDetail detail, int index) {
        StockInLabel label = new StockInLabel();
        label.setInId(inId);
        label.setInDetailId(detail.getInDetailId());
        label.setLabelCode("in" + inId + String.format("%03d", index));
        label.setQrContent(buildQrContent(label.getLabelCode()));
        label.setPrintStatus("待打印");
        label.setReprintCount(0);
        label.setScanStatus("待扫");
        label.setIsDeleted(0);
        label.setVersion(0);
        return label;
    }

    /**
     * CRC16-CCITT（与 PDA 前端二维码内容校验一致）
     */
    private String crc16(String str) {
        int crc = 0xFFFF;
        for (int i = 0; i < str.length(); i++) {
            crc ^= str.charAt(i) << 8;
            for (int j = 0; j < 8; j++) {
                crc = (crc & 0x8000) != 0 ? (crc << 1) ^ 0x1021 : crc << 1;
                crc &= 0xFFFF;
            }
        }
        return String.format("%04X", crc);
    }
}
