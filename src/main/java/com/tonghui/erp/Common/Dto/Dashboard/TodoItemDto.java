package com.tonghui.erp.Common.Dto.Dashboard;

import lombok.Data;

/**
 * 待办事项项DTO
 */
@Data
public class TodoItemDto {
    /**
     * 唯一标识
     */
    private Long id;

    /**
     * 提醒类型：设备维保/库存预警/环境管理/人员管理
     */
    private String todoType;

    /**
     * 提醒内容
     */
    private String content;

    /**
     * 到期时间
     */
    private String dueDate;

    /**
     * 来源模块
     */
    private String sourceModule;

    /**
     * 跳转链接
     */
    private String link;
}
