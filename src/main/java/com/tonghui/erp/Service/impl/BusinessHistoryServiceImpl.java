package com.tonghui.erp.Service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tonghui.erp.Common.Dto.Log.BusinessHistoryChangeDto;
import com.tonghui.erp.Common.Dto.Log.BusinessHistoryDto;
import com.tonghui.erp.Common.Dto.Log.BusinessHistoryEventDto;
import com.tonghui.erp.Common.Dto.Log.BusinessHistoryFieldStatDto;
import com.tonghui.erp.Common.Dto.Log.BusinessHistoryRecordStatDto;
import com.tonghui.erp.Common.Dto.Log.BusinessHistoryScope;
import com.tonghui.erp.Common.Dto.Log.FieldChange;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Data.Entity.SysDataChangeLog;
import com.tonghui.erp.Data.Entity.SysOperationLog;
import com.tonghui.erp.Data.mapper.SysDataChangeLogMapper;
import com.tonghui.erp.Data.mapper.SysOperationLogMapper;
import com.tonghui.erp.Service.BusinessHistoryService;
import com.tonghui.erp.Service.businesshistory.BusinessHistoryProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 业务变更历史服务实现类
 * <p>
 * 统一处理表范围解析、变更日志加载、按操作归组、操作人补全与事件分页，
 * 各业务对象只需要提供 {@link BusinessHistoryProvider} 解析查询范围
 * </p>
 */
@Service
public class BusinessHistoryServiceImpl implements BusinessHistoryService {

    // region 常量与依赖
    // ===================================
    // 常量与依赖
    // ===================================

    private static final Logger log = LoggerFactory.getLogger(BusinessHistoryServiceImpl.class);

    /** 默认每页事件数 */
    private static final int DEFAULT_PAGE_SIZE = 20;

    /** 单页最大事件数 */
    private static final int MAX_PAGE_SIZE = 200;

    /** 单表单次最大加载变更数，避免大记录拖慢详情页 */
    private static final int MAX_HISTORY_CHANGES = 1000;

    /** 字段差异JSON类型 */
    private static final TypeReference<List<FieldChange>> FIELD_CHANGE_TYPE = new TypeReference<>() {
    };

    private final Map<String, BusinessHistoryProvider> providers;
    private final SysDataChangeLogMapper dataChangeLogMapper;
    private final SysOperationLogMapper operationLogMapper;
    private final ObjectMapper objectMapper;

    /**
     * 构造业务变更历史服务
     *
     * @param providers          业务历史 Provider 列表
     * @param dataChangeLogMapper 数据变更日志Mapper
     * @param operationLogMapper  操作日志Mapper
     * @param objectMapper        Jackson对象映射器
     */
    public BusinessHistoryServiceImpl(List<BusinessHistoryProvider> providers,
                                      SysDataChangeLogMapper dataChangeLogMapper,
                                      SysOperationLogMapper operationLogMapper,
                                      ObjectMapper objectMapper) {
        this.providers = new LinkedHashMap<>();
        if (providers != null) {
            for (BusinessHistoryProvider provider : providers) {
                this.providers.put(normalize(provider.bizType()), provider);
            }
        }
        this.dataChangeLogMapper = dataChangeLogMapper;
        this.operationLogMapper = operationLogMapper;
        this.objectMapper = objectMapper;
    }

    // endregion

    // region 查询入口
    // ===================================
    // 查询入口
    // ===================================

    /**
     * 查询业务对象的变更历史
     *
     * @param bizType     业务类型
     * @param bizId       业务对象主键
     * @param field       字段筛选
     * @param source      来源筛选
     * @param startTime   起始时间
     * @param endTime     截止时间
     * @param pageRequest 分页参数
     * @return 业务变更历史
     */
    @Override
    public BusinessHistoryDto query(String bizType, String bizId, String field, String source,
                                    LocalDateTime startTime, LocalDateTime endTime,
                                    PageRequestDto pageRequest) {
        BusinessHistoryProvider provider = providers.get(normalize(bizType));
        if (provider == null) {
            throw new IllegalArgumentException("不支持的业务类型：" + bizType);
        }
        if (!hasText(bizId)) {
            throw new IllegalArgumentException("业务ID不能为空");
        }
        BusinessHistoryScope scope = provider.resolve(bizId.trim());
        if (scope == null) {
            throw new IllegalArgumentException("业务对象不存在：" + bizId);
        }

        int pageIndex = pageIndex(pageRequest);
        int pageSize = pageSize(pageRequest);

        List<ChangeView> views = new ArrayList<>();
        boolean truncated = loadViews(scope, startTime, endTime, views);

        BusinessHistoryDto result = new BusinessHistoryDto();
        result.setBizType(normalize(bizType));
        result.setBizId(bizId.trim());
        result.setTitle(scope.getTitle());
        result.setTruncated(truncated);

        fillStats(result, views, scope);

        String fieldFilter = hasText(field) ? field.trim() : null;
        String sourceFilter = hasText(source) ? source.trim().toUpperCase(Locale.ROOT) : null;

        List<BusinessHistoryEventDto> events = buildEvents(views, fieldFilter, sourceFilter, scope);
        result.setTotalCount(events.size());
        result.setPageIndex(pageIndex);
        result.setPageSize(pageSize);
        result.setEvents(pageSlice(events, pageIndex, pageSize));
        return result;
    }

    // endregion

    // region 变更日志加载
    // ===================================
    // 变更日志加载
    // ===================================

    /**
     * 加载业务对象各表的变更日志
     *
     * @param scope     查询范围
     * @param startTime 起始时间
     * @param endTime   截止时间
     * @param views     变更视图输出
     * @return 是否因为单次上限被截断
     */
    private boolean loadViews(BusinessHistoryScope scope, LocalDateTime startTime,
                              LocalDateTime endTime, List<ChangeView> views) {
        boolean truncated = false;
        for (BusinessHistoryScope.TableScope table : scope.getTables()) {
            if (table == null || !hasText(table.getTableName())
                    || table.getBusinessIds() == null || table.getBusinessIds().isEmpty()) {
                continue;
            }
            List<SysDataChangeLog> rows = dataChangeLogMapper.selectHistory(
                    table.getTableName(), table.getBusinessIds(), startTime, endTime, MAX_HISTORY_CHANGES + 1);
            if (rows == null || rows.isEmpty()) {
                continue;
            }
            if (rows.size() > MAX_HISTORY_CHANGES) {
                truncated = true;
                rows = new ArrayList<>(rows.subList(0, MAX_HISTORY_CHANGES));
            }
            for (SysDataChangeLog row : rows) {
                views.add(new ChangeView(row, parseFields(row.getChangedFields())));
            }
        }
        views.sort(this::compareChangeDesc);
        return truncated;
    }

    /**
     * 解析字段差异JSON
     *
     * @param json 字段差异JSON
     * @return 字段差异列表
     */
    private List<FieldChange> parseFields(String json) {
        if (!hasText(json)) {
            return List.of();
        }
        try {
            List<FieldChange> fields = objectMapper.readValue(json, FIELD_CHANGE_TYPE);
            return fields == null ? List.of() : fields;
        } catch (Exception e) {
            log.debug("解析业务变更字段差异失败：{}", e.getMessage());
            return List.of();
        }
    }

    // endregion

    // region 统计
    // ===================================
    // 统计
    // ===================================

    /**
     * 填充字段级与记录级变更统计
     *
     * @param result 返回结果
     * @param views  变更视图
     * @param scope  查询范围
     */
    private void fillStats(BusinessHistoryDto result, List<ChangeView> views, BusinessHistoryScope scope) {
        Map<String, BusinessHistoryFieldStatDto> fieldStats = new LinkedHashMap<>();
        Map<String, BusinessHistoryRecordStatDto> recordStats = new LinkedHashMap<>();

        for (ChangeView view : views) {
            SysDataChangeLog entity = view.entity;
            for (FieldChange change : view.fields) {
                if (change == null || !hasText(change.getField())) {
                    continue;
                }
                BusinessHistoryFieldStatDto stat = fieldStats.get(change.getField());
                if (stat == null) {
                    stat = new BusinessHistoryFieldStatDto();
                    stat.setField(change.getField());
                    stat.setLabel(hasText(change.getLabel()) ? change.getLabel() : change.getField());
                    fieldStats.put(change.getField(), stat);
                }
                stat.setCount(stat.getCount() + 1);
            }
            if (!view.fields.isEmpty() && hasText(entity.getTableName()) && hasText(entity.getBusinessId())) {
                String key = entity.getTableName() + "#" + entity.getBusinessId();
                BusinessHistoryRecordStatDto stat = recordStats.get(key);
                if (stat == null) {
                    stat = new BusinessHistoryRecordStatDto();
                    stat.setTableName(entity.getTableName());
                    stat.setRecordId(entity.getBusinessId());
                    String label = scope.recordLabel(entity.getTableName(), entity.getBusinessId());
                    stat.setLabel(hasText(label) ? label : entity.getBusinessId());
                    recordStats.put(key, stat);
                }
                stat.setCount(stat.getCount() + 1);
            }
        }
        result.setFieldStats(new ArrayList<>(fieldStats.values()));
        result.setRecordStats(new ArrayList<>(recordStats.values()));
    }

    // endregion

    // region 事件组装
    // ===================================
    // 事件组装
    // ===================================

    /**
     * 组装变更事件并补全操作信息
     *
     * @param views        变更视图
     * @param fieldFilter  字段筛选
     * @param sourceFilter 来源筛选
     * @param scope        查询范围
     * @return 事件列表（按时间倒序）
     */
    private List<BusinessHistoryEventDto> buildEvents(List<ChangeView> views, String fieldFilter,
                                                      String sourceFilter, BusinessHistoryScope scope) {
        Map<String, EventGroup> groups = new LinkedHashMap<>();
        for (ChangeView view : views) {
            SysDataChangeLog entity = view.entity;
            if (!matchesSource(entity, sourceFilter)) {
                continue;
            }
            List<FieldChange> matched = view.fields;
            if (fieldFilter != null) {
                matched = view.fields.stream()
                        .filter(change -> change != null && fieldFilter.equals(change.getField()))
                        .toList();
                if (matched.isEmpty()) {
                    continue;
                }
            }
            EventGroup group = groups.get(groupKey(entity));
            if (group == null) {
                String key = groupKey(entity);
                group = new EventGroup();
                groups.put(key, group);
            }
            group.add(entity, matched);
        }

        Map<Long, SysOperationLog> operationLogs = loadOperationLogs(groups);
        List<BusinessHistoryEventDto> events = new ArrayList<>();
        for (EventGroup group : groups.values()) {
            events.add(toEvent(group, operationLogs, scope));
        }
        events.sort(this::compareEventDesc);
        return events;
    }

    /**
     * 批量查询操作日志摘要
     *
     * @param groups 事件分组
     * @return 操作日志映射
     */
    private Map<Long, SysOperationLog> loadOperationLogs(Map<String, EventGroup> groups) {
        Set<Long> ids = new LinkedHashSet<>();
        for (EventGroup group : groups.values()) {
            if (group.operationLogId != null) {
                ids.add(group.operationLogId);
            }
        }
        Map<Long, SysOperationLog> result = new HashMap<>();
        if (ids.isEmpty()) {
            return result;
        }
        List<SysOperationLog> logs = operationLogMapper.selectBriefByIds(ids);
        if (logs == null) {
            return result;
        }
        for (SysOperationLog log : logs) {
            if (log.getId() != null) {
                result.put(log.getId(), log);
            }
        }
        return result;
    }

    /**
     * 分组转换为事件
     *
     * @param group          事件分组
     * @param operationLogs  操作日志映射
     * @param scope          查询范围
     * @return 事件
     */
    private BusinessHistoryEventDto toEvent(EventGroup group, Map<Long, SysOperationLog> operationLogs,
                                            BusinessHistoryScope scope) {
        SysDataChangeLog first = group.first;
        SysOperationLog operationLog = group.operationLogId == null ? null : operationLogs.get(group.operationLogId);

        BusinessHistoryEventDto event = new BusinessHistoryEventDto();
        event.setOperationLogId(group.operationLogId);
        event.setTraceId(hasText(group.traceId) ? group.traceId : first.getTraceId());

        if (operationLog != null) {
            event.setOperationBusinessType(operationLog.getBusinessType());
            event.setModule(operationLog.getModule());
            event.setOperationType(operationLog.getOperationType());
            event.setOperationDesc(operationLog.getOperationDesc());
            event.setUserName(hasText(operationLog.getUserName()) ? operationLog.getUserName() : first.getUserName());
            event.setUserAccount(hasText(operationLog.getUserAccount())
                    ? operationLog.getUserAccount() : first.getUserAccount());
            event.setClientIp(operationLog.getClientIp());
            event.setIpLocation(operationLog.getIpLocation());
            event.setMacAddress(operationLog.getMacAddress());
            event.setCreatedTime(operationLog.getCreatedTime() != null
                    ? operationLog.getCreatedTime() : group.time);
        } else {
            event.setOperationType(first.getChangeType());
            event.setOperationDesc(defaultDesc(first.getChangeType()));
            event.setUserName(first.getUserName());
            event.setUserAccount(first.getUserAccount());
            event.setCreatedTime(group.time);
        }

        List<BusinessHistoryChangeDto> changes = new ArrayList<>();
        for (ChangeView view : group.changes) {
            SysDataChangeLog entity = view.entity;
            String recordLabel = scope.recordLabel(entity.getTableName(), entity.getBusinessId());
            for (FieldChange change : view.fields) {
                if (change == null) {
                    continue;
                }
                BusinessHistoryChangeDto dto = new BusinessHistoryChangeDto();
                dto.setDataChangeId(entity.getId());
                dto.setTableName(entity.getTableName());
                dto.setBusinessId(entity.getBusinessId());
                dto.setRecordLabel(recordLabel);
                dto.setChangeType(entity.getChangeType());
                dto.setChangeSource(entity.getChangeSource());
                dto.setField(change.getField());
                dto.setLabel(hasText(change.getLabel()) ? change.getLabel() : change.getField());
                dto.setBefore(change.getBefore());
                dto.setAfter(change.getAfter());
                changes.add(dto);
            }
        }
        event.setChanges(changes);
        return event;
    }

    // endregion

    // region 辅助方法
    // ===================================
    // 辅助方法
    // ===================================

    /**
     * 判断变更是否匹配来源筛选
     *
     * @param entity       变更日志
     * @param sourceFilter 来源筛选
     * @return 是否匹配
     */
    private boolean matchesSource(SysDataChangeLog entity, String sourceFilter) {
        if (sourceFilter == null) {
            return true;
        }
        return entity.getChangeSource() != null && sourceFilter.equalsIgnoreCase(entity.getChangeSource());
    }

    /**
     * 生成事件分组键
     *
     * @param entity 变更日志
     * @return 分组键
     */
    private String groupKey(SysDataChangeLog entity) {
        if (entity.getOperationLogId() != null) {
            return "op:" + entity.getOperationLogId();
        }
        if (hasText(entity.getTraceId())) {
            return "trace:" + entity.getTraceId();
        }
        return "row:" + entity.getId();
    }

    /**
     * 变更视图倒序比较
     *
     * @param left  左值
     * @param right 右值
     * @return 比较结果
     */
    private int compareChangeDesc(ChangeView left, ChangeView right) {
        int timeCompare = compareTimeDesc(left.entity.getCreatedTime(), right.entity.getCreatedTime());
        if (timeCompare != 0) {
            return timeCompare;
        }
        Long leftId = left.entity.getId();
        Long rightId = right.entity.getId();
        return Long.compare(rightId == null ? 0 : rightId, leftId == null ? 0 : leftId);
    }

    /**
     * 事件倒序比较
     *
     * @param left  左值
     * @param right 右值
     * @return 比较结果
     */
    private int compareEventDesc(BusinessHistoryEventDto left, BusinessHistoryEventDto right) {
        int timeCompare = compareTimeDesc(left.getCreatedTime(), right.getCreatedTime());
        if (timeCompare != 0) {
            return timeCompare;
        }
        Long leftId = left.getOperationLogId();
        Long rightId = right.getOperationLogId();
        return Long.compare(rightId == null ? 0 : rightId, leftId == null ? 0 : leftId);
    }

    /**
     * 时间倒序比较
     *
     * @param left  左时间
     * @param right 右时间
     * @return 比较结果
     */
    private int compareTimeDesc(LocalDateTime left, LocalDateTime right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return right.compareTo(left);
    }

    /**
     * 默认操作描述
     *
     * @param changeType 变更类型
     * @return 操作描述
     */
    private String defaultDesc(String changeType) {
        if ("INSERT".equalsIgnoreCase(changeType)) {
            return "新增记录";
        }
        if ("DELETE".equalsIgnoreCase(changeType)) {
            return "删除记录";
        }
        return "字段变更";
    }

    /**
     * 事件分页
     *
     * @param events    事件列表
     * @param pageIndex 页码
     * @param pageSize  每页数量
     * @return 当前页事件
     */
    private List<BusinessHistoryEventDto> pageSlice(List<BusinessHistoryEventDto> events, int pageIndex, int pageSize) {
        int total = events.size();
        int from = (int) Math.min((long) pageIndex * pageSize, total);
        int to = Math.min(from + pageSize, total);
        return new ArrayList<>(events.subList(from, to));
    }

    /**
     * 规范化页码
     *
     * @param pageRequest 分页参数
     * @return 页码
     */
    private int pageIndex(PageRequestDto pageRequest) {
        return pageRequest == null ? 0 : Math.max(0, pageRequest.getPageIndex());
    }

    /**
     * 规范化每页数量
     *
     * @param pageRequest 分页参数
     * @return 每页数量
     */
    private int pageSize(PageRequestDto pageRequest) {
        if (pageRequest == null) {
            return DEFAULT_PAGE_SIZE;
        }
        int size = pageRequest.getPageSize();
        return size <= 0 || size > MAX_PAGE_SIZE ? DEFAULT_PAGE_SIZE : size;
    }

    /**
     * 规范化业务类型
     *
     * @param bizType 业务类型
     * @return 大写业务类型
     */
    private String normalize(String bizType) {
        return bizType == null ? "" : bizType.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 判断字符串是否有内容
     *
     * @param text 字符串
     * @return 是否有内容
     */
    private boolean hasText(String text) {
        return text != null && !text.trim().isEmpty();
    }

    // endregion

    // region 内部数据结构
    // ===================================
    // 内部数据结构
    // ===================================

    /**
     * 变更视图：日志实体 + 解析后的字段差异
     */
    private static class ChangeView {

        private final SysDataChangeLog entity;
        private final List<FieldChange> fields;

        private ChangeView(SysDataChangeLog entity, List<FieldChange> fields) {
            this.entity = entity;
            this.fields = fields;
        }
    }

    /**
     * 事件分组
     */
    private static class EventGroup {

        private Long operationLogId;
        private String traceId;
        private LocalDateTime time;
        private SysDataChangeLog first;
        private final List<ChangeView> changes = new ArrayList<>();

        private void add(SysDataChangeLog entity, List<FieldChange> fields) {
            if (first == null) {
                first = entity;
            }
            if (entity.getCreatedTime() != null && (time == null || entity.getCreatedTime().isAfter(time))) {
                time = entity.getCreatedTime();
            }
            if (operationLogId == null) {
                operationLogId = entity.getOperationLogId();
            }
            if (traceId == null || traceId.trim().isEmpty()) {
                traceId = entity.getTraceId();
            }
            changes.add(new ChangeView(entity, fields));
        }
    }

    // endregion
}
