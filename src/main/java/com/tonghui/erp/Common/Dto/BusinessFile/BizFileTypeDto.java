package com.tonghui.erp.Common.Dto.BusinessFile;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

/**
 * 业务类型注册表返回对象（供前端生成常量/校验）
 */
@Data
@AllArgsConstructor
public class BizFileTypeDto {

    /** 业务类型编码 */
    private String code;

    /** 显示名称 */
    private String displayName;

    /** 槽位 -> 最大数量（-1 表示不限） */
    private Map<String, Integer> kinds;
}
