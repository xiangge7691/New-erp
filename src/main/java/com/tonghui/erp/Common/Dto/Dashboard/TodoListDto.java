package com.tonghui.erp.Common.Dto.Dashboard;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 待办事项列表DTO
 */
@Data
public class TodoListDto {
    /**
     * 待办事项列表
     */
    private List<TodoItemDto> items;

    /**
     * 各类型计数
     */
    private Map<String, Long> typeCounts;
}
