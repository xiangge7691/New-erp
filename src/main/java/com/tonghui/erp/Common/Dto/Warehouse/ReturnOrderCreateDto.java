package com.tonghui.erp.Common.Dto.Warehouse;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 新增退库单请求DTO
 * <p>
 * 退库单创建请求体：指定关联出库单号及退库物料明细（库存标识+退库数量）
 * </p>
 */
@Data
public class ReturnOrderCreateDto {

    /**
     * 关联出库单号
     */
    private String outOrderNo;

    /**
     * 备注
     */
    private String remark;

    /**
     * 外包装检查结果：合格/不合格（可选，为空默认合格）
     */
    private String outerPackageCheck;

    /**
     * 性状检查结果：合格/不合格（可选，为空默认合格）
     */
    private String appearanceCheck;

    /**
     * 退库人姓名（前端下拉选择，存姓名文本）
     */
    private String returnPersonName;

    /**
     * 验收人姓名（前端下拉选择，存姓名文本）
     */
    private String acceptorName;

    /**
     * 审核人姓名（前端下拉选择，存姓名文本）
     */
    private String reviewerName;

    /**
     * 退库时间（可选，为空默认当前时间）
     */
    private LocalDateTime returnTime;

    /**
     * 退库物料明细列表（库存标识+退库数量）
     */
    private List<ReturnItemRequest> items;
}