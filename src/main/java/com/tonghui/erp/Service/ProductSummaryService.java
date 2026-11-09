package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.QualityPages.ProductSummaryDtos;

/**
 * 产品信息汇总服务
 * <p>
 * 按品种/时间查询计划 → 采购/领料 → 工单 → 生产 → 入库 → 出库 → 检验全链路，
 * 返回槽位统计、单据明细、文件分组和缺件清单。
 * </p>
 */
public interface ProductSummaryService {

    /**
     * 分页查询批次汇总
     *
     * @param varietyCode 品种编码
     * @param dateType    日期类型：planDate/workOrderDate/inboundDate/inspectionDate
     * @param startDate   开始日期
     * @param endDate     结束日期
     * @param keyword     关键字
     * @param onlyMissing 只看缺件
     * @param pageIndex   页码，从0开始
     * @param pageSize    每页数量
     * @return 分页结果
     */
    PagedResult<ProductSummaryDtos.BatchSummary> queryBatches(String varietyCode, String dateType,
                                                              String startDate, String endDate,
                                                              String keyword, boolean onlyMissing,
                                                              int pageIndex, int pageSize);

    /**
     * 查询批次全链路详情
     *
     * @param workOrderId 工单ID
     * @return 批次详情
     */
    ProductSummaryDtos.BatchDetail getBatchDetail(Long workOrderId);


    /**
     * 查询页面聚合概览
     *
     * @param varietyCode 品种编码
     * @param dateType    日期类型
     * @param startDate   开始日期
     * @param endDate     结束日期
     * @param keyword     关键字
     * @param onlyMissing 只看缺件
     * @return 结论、KPI、待办、文件健康度、制剂卡片
     */
    ProductSummaryDtos.Overview getOverview(String varietyCode, String dateType,
                                            String startDate, String endDate,
                                            String keyword, boolean onlyMissing);

    /**
     * 查询品种卡片视图数据（不分页）
     *
     * @param varietyCode 品种编码
     * @param dateType    日期类型
     * @param startDate   开始日期
     * @param endDate     结束日期
     * @param keyword     关键字
     * @param onlyMissing 只看缺件
     * @return 批次汇总列表
     */
    java.util.List<ProductSummaryDtos.BatchSummary> queryCards(String varietyCode, String dateType,
                                                               String startDate, String endDate,
                                                               String keyword, boolean onlyMissing);

    /**
     * 查询未下达工单的计划详情
     *
     * @param planId 生产计划ID
     * @return 计划详情
     */
    ProductSummaryDtos.BatchDetail getPlanDetail(Long planId);

    /**
     * 导出批次文件 zip
     * <p>
     * 按 品种/批次/环节 组织目录，附文件清单.csv；产品资料可按开关统一放 00_产品资料。
     * </p>
     *
     * @param request 导出请求
     * @return zip 字节
     */
    byte[] export(ProductSummaryDtos.ExportRequest request);
}
