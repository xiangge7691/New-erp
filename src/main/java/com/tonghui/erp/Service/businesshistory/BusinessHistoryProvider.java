package com.tonghui.erp.Service.businesshistory;

import com.tonghui.erp.Common.Dto.Log.BusinessHistoryScope;

/**
 * 业务变更历史 Provider
 * <p>
 * 每种业务对象实现一个 Provider，负责把业务主键解析为需要查询的表范围、
 * 关联记录名称与展示标题；聚合查询逻辑由 BusinessHistoryService 统一处理
 * </p>
 */
public interface BusinessHistoryProvider {

    /**
     * 业务类型
     *
     * @return 业务类型，如 WORK_ORDER / PREPARATION
     */
    String bizType();

    /**
     * 解析业务对象的查询范围
     *
     * @param bizId 业务主键
     * @return 查询范围
     */
    BusinessHistoryScope resolve(String bizId);
}
