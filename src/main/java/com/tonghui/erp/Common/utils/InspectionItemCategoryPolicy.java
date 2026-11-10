package com.tonghui.erp.Common.utils;

/**
 * 被检品属性与检验类型映射策略
 * <p>
 * 检验计划的检验类型由任务的被检品属性唯一确定：
 * 生产原料→生产原料检验，中间产品→中间产品检验，成品→成品检验。
 * 未知属性按成品检验兜底，保持与历史默认值一致。
 * </p>
 */
public final class InspectionItemCategoryPolicy {

    private InspectionItemCategoryPolicy() {
    }

    /**
     * 根据被检品属性推导检验类型
     *
     * @param itemCategory 被检品属性：生产原料/中间产品/成品
     * @return 检验类型
     */
    public static String inspectionTypeOf(String itemCategory) {
        if ("生产原料".equals(itemCategory)) {
            return "生产原料检验";
        }
        if ("中间产品".equals(itemCategory)) {
            return "中间产品检验";
        }
        return "成品检验";
    }
}
