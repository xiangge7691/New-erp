package com.tonghui.erp.Common.Dto.QualityPages;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 产品信息汇总页 DTO 集合
 * <p>
 * 按批次返回全链路节点、单据明细、槽位统计、文件分组和缺件清单。
 * </p>
 */
public final class ProductSummaryDtos {

    private ProductSummaryDtos() {
    }

    /** 批次列表项 */
    @Data
    public static class BatchSummary {
        private Long workOrderId;
        private String workOrderCode;
        private Long planId;
        private String planNumber;
        private String preparationCode;
        private String preparationName;
        private String spec;
        private String unitName;
        private String batchNo;
        private BigDecimal planQty;
        private BigDecimal finishedQty;
        private String materialSource;
        private LocalDate planDate;
        private LocalDate productionStart;
        private LocalDate productionEnd;
        private BigDecimal yieldRate;
        private String inspectionConclusion;
        private String status;
        private int fileCount;
        private int missingCount;
        private String missingSummary;
    }

    /** 批次详情 */
    @Data
    public static class BatchDetail {
        private BatchSummary batch;
        private List<SlotState> slots = new ArrayList<>();
        private List<StageItem> stages = new ArrayList<>();
        private List<FileGroup> files = new ArrayList<>();
        private List<MissingItem> missingItems = new ArrayList<>();
    }

    /** 槽位统计 */
    @Data
    public static class SlotState {
        private String stage;
        private String label;
        private String bizType;
        private String fileKind;
        private String requirement;
        private int expected;
        private int uploaded;
        private int missing;
    }

    /** 链路节点 */
    @Data
    public static class StageItem {
        private String key;
        private String label;
        private String state;
        private String main;
        private String meta;
        private List<DocumentItem> documents = new ArrayList<>();
    }

    /** 单据 */
    @Data
    public static class DocumentItem {
        private String type;
        private String code;
        private LocalDate date;
        private LocalDateTime time;
        private String status;
        private String operator;
        private String supplier;
        private String customer;
        private BigDecimal amount;
        private List<DocumentLine> lines = new ArrayList<>();
        private List<FileItem> files = new ArrayList<>();
    }

    /** 单据明细行 */
    @Data
    public static class DocumentLine {
        private String code;
        private String name;
        private String category;
        private BigDecimal qty;
        private String unit;
        private BigDecimal price;
        private BigDecimal amount;
        private String supplier;
        private String batchNo;
        private String status;
    }

    /** 文件分组 */
    @Data
    public static class FileGroup {
        private String stage;
        private String label;
        private String bizType;
        private String fileKind;
        private String requirement;
        private List<FileItem> files = new ArrayList<>();
    }

    /** 缺件项 */
    @Data
    public static class MissingItem {
        private String stage;
        private String label;
        private String doc;
        private String slotCode;
    }

    /** 文件项 */
    @Data
    public static class FileItem {
        private Long fileId;
        private String name;
        private Long size;
        private LocalDateTime time;
        private String uploader;
    }

    /** 导出请求 */
    @Data
    public static class ExportRequest {
        private List<Long> workOrderIds = new ArrayList<>();
        private String varietyCode;
        private Boolean includeProductFiles = true;
        private List<String> requirements = new ArrayList<>();
    }
}
