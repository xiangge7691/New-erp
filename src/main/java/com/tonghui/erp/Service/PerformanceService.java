package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Performance.CoefficientPublishRequest;
import com.tonghui.erp.Common.Dto.Performance.CoefficientVersionDetail;
import com.tonghui.erp.Common.Dto.Performance.PerformanceManualRequest;
import com.tonghui.erp.Common.Dto.Performance.PerformanceModifyRequest;
import com.tonghui.erp.Data.Entity.PerformanceCoefficientVersion;
import com.tonghui.erp.Data.Entity.PerformanceRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 人员绩效服务接口
 */
public interface PerformanceService {

    /**
     * 分页查询绩效记录
     *
     * @param from           绩效归属日期起
     * @param to             绩效归属日期止
     * @param personnelId    人员档案ID
     * @param workOrderId    工单ID
     * @param teamId         班组ID
     * @param source         记录来源
     * @param modifiedStatus 修改状态
     * @param recordStatus   记录状态
     * @param keyword        工单号/工单名称/制剂名称/批号/计划名称/计划编号关键字
     * @param pageRequest    分页参数
     * @return 绩效记录分页
     */
    PagedResult<PerformanceRecord> query(LocalDate from, LocalDate to, Long personnelId, Long workOrderId,
                                         Long teamId, String source, Integer modifiedStatus, Integer recordStatus,
                                         String keyword, PageRequestDto pageRequest);

    /**
     * 修改绩效时长
     */
    PerformanceRecord modify(Long id, PerformanceModifyRequest request);

    /**
     * 手动添加绩效
     */
    PerformanceRecord manual(PerformanceManualRequest request);

    /**
     * 作废手动记录
     */
    PerformanceRecord voidRecord(Long id, String note);

    /**
     * 查询系数版本列表
     */
    List<PerformanceCoefficientVersion> listVersions();

    /**
     * 发布新系数版本
     */
    PerformanceCoefficientVersion publish(CoefficientPublishRequest request);

    /**
     * 查询系数版本详情
     */
    CoefficientVersionDetail versionDetail(Long id);

    /**
     * 汇总查询
     *
     * @param groupBy team/product/date
     */
    List<Map<String, Object>> aggregate(LocalDate from, LocalDate to, String groupBy);
}
