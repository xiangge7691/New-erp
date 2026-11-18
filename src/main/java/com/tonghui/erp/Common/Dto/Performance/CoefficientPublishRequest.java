package com.tonghui.erp.Common.Dto.Performance;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.List;

/**
 * 发布绩效系数版本请求
 * <p>系数既可传 id -> 系数 的映射，也可传明细数组（前端按表格提交时）</p>
 */
@Data
public class CoefficientPublishRequest {

    /** 版本备注（选填） */
    private String note;

    /** 工序系数：支持 {"工序类型ID": 系数} 或 [{processTypeId, coefficient}] */
    private JsonNode processCoefficients;

    /** 人员系数：支持 {"人员档案ID": 系数} 或 [{personnelId, coefficient}] */
    private JsonNode personCoefficients;

    /** 工序系数明细（与 processCoefficients 二选一或并存，数组形式） */
    private List<ProcessCoefficientItem> processItems;

    /** 人员系数明细（与 personCoefficients 二选一或并存，数组形式） */
    private List<PersonCoefficientItem> personItems;
}
