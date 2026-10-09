package com.tonghui.erp.Common.Dto.System;

import lombok.Data;

/**
 * 首页到期提醒天数配置数据传输对象
 * <p>
 * 汇总各类到期提醒的时间范围（天），供配置页与首页统一读取
 * </p>
 */
@Data
public class DashboardExpiryConfigDto {

    /**
     * 库存效期提醒天数
     */
    private Integer stockDays;

    /**
     * 设备维保提醒天数
     */
    private Integer equipmentDays;

    /**
     * 人员健康证提醒天数
     */
    private Integer healthCertDays;

    /**
     * 人员证书提醒天数
     */
    private Integer personnelCertDays;

    /**
     * 环境消毒提醒天数
     */
    private Integer disinfectionDays;

    /**
     * 机构证照提醒天数
     */
    private Integer organizationDays;

    /**
     * 清洁提醒天数
     */
    private Integer cleaningDays;

    /**
     * 供应商审核提醒天数
     */
    private Integer supplierAuditDays;

    /**
     * 培训提醒天数
     */
    private Integer trainingDays;

    /**
     * 验证提醒天数
     */
    private Integer verificationDays;

    /**
     * 留样到期提醒天数
     */
    private Integer retainedSampleDays;

    /**
     * 制剂批件过期提醒天数
     */
    private Integer approvalDays;
}
