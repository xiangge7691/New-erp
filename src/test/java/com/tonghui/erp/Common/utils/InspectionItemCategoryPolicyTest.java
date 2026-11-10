package com.tonghui.erp.Common.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 被检品属性与检验类型映射测试
 */
public class InspectionItemCategoryPolicyTest {

    @Test
    public void shouldMapItemCategoryToInspectionType() {
        Assertions.assertEquals("生产原料检验", InspectionItemCategoryPolicy.inspectionTypeOf("生产原料"));
        Assertions.assertEquals("中间产品检验", InspectionItemCategoryPolicy.inspectionTypeOf("中间产品"));
        Assertions.assertEquals("成品检验", InspectionItemCategoryPolicy.inspectionTypeOf("成品"));
        Assertions.assertEquals("成品检验", InspectionItemCategoryPolicy.inspectionTypeOf(null),
                "未知属性应按成品检验兜底");
    }
}
