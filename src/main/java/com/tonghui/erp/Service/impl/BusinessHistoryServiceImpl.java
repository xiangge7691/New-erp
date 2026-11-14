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
import com.tonghui.erp.Data.Entity.FileInfo;
import com.tonghui.erp.Data.Entity.FileOperationLog;
import com.tonghui.erp.Data.Entity.SysDataChangeLog;
import com.tonghui.erp.Data.Entity.SysOperationLog;
import com.tonghui.erp.Data.mapper.FileInfoMapper;
import com.tonghui.erp.Data.mapper.FileOperationLogMapper;
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

    /** 附件变更字段名 */
    private static final String FILE_FIELD = "file_name";

    /** 附件变更字段中文名 */
    private static final String FILE_LABEL = "附件文件";

    /** 附件变更来源标记 */
    private static final String FILE_SOURCE = "FILE";

    /** 字段差异JSON类型 */
    private static final TypeReference<List<FieldChange>> FIELD_CHANGE_TYPE = new TypeReference<>() {
    };

    private final Map<String, BusinessHistoryProvider> providers;
    private final SysDataChangeLogMapper dataChangeLogMapper;
    private final SysOperationLogMapper operationLogMapper;
    private final FileOperationLogMapper fileOperationLogMapper;
    private final FileInfoMapper fileInfoMapper;
    private final ObjectMapper objectMapper;

    /**
     * 构造业务变更历史服务
     *
     * @param providers            业务历史 Provider 列表
     * @param dataChangeLogMapper  数据变更日志Mapper
     * @param operationLogMapper   操作日志Mapper
     * @param fileOperationLogMapper 文件操作日志Mapper
     * @param fileInfoMapper       文件信息Mapper
     * @param objectMapper         Jackson对象映射器
     */
    public BusinessHistoryServiceImpl(List<BusinessHistoryProvider> providers,
                                      SysDataChangeLogMapper dataChangeLogMapper,
                                      SysOperationLogMapper operationLogMapper,
                                      FileOperationLogMapper fileOperationLogMapper,
                                      FileInfoMapper fileInfoMapper,
                                      ObjectMapper objectMapper) {
        this.providers = new LinkedHashMap<>();
        if (providers != null) {
            for (BusinessHistoryProvider provider : providers) {
                this.providers.put(normalize(provider.bizType()), provider);
            }
        }
        this.dataChangeLogMapper = dataChangeLogMapper;
        this.operationLogMapper = operationLogMapper;
        this.fileOperationLogMapper = fileOperationLogMapper;
        this.fileInfoMapper = fileInfoMapper;
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
     * @param table       表筛选
     * @param startTime   起始时间
     * @param endTime     截止时间
     * @param pageRequest 分页参数
     * @return 业务变更历史
     */
    @Override
    public BusinessHistoryDto query(String bizType, String bizId, String field, String source, String table,
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
        String tableFilter = hasText(table) ? table.trim() : null;

        List<BusinessHistoryEventDto> events = buildEvents(views, fieldFilter, sourceFilter, tableFilter, scope);
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
        truncated |= loadFileViews(scope, startTime, endTime, views);
        views.sort(this::compareChangeDesc);
        return truncated;
    }

    /**
     * 加载业务对象的附件上传 / 删除记录
     * <p>
     * 文件模块不写数据变更日志，附件历史取自 file_operation_log；
     * 通过日志 detail 中的 bizType + bizId 归到主表或具体工序行
     * </p>
     *
     * @param scope     查询范围
     * @param startTime 起始时间
     * @param endTime   截止时间
     * @param views     变更视图输出
     * @return 是否因为单次上限被截断
     */
    private boolean loadFileViews(BusinessHistoryScope scope, LocalDateTime startTime,
                                  LocalDateTime endTime, List<ChangeView> views) {
        List<BusinessHistoryScope.FileScope> fileScopes = scope.getFileScopes();
        if (fileScopes == null || fileScopes.isEmpty()) {
            return false;
        }
        List<FileOperationLog> logs = fileOperationLogMapper.selectBusinessFileHistory(
                fileScopes, startTime, endTime, MAX_HISTORY_CHANGES + 1);
        if (logs == null || logs.isEmpty()) {
            return false;
        }
        boolean truncated = false;
        if (logs.size() > MAX_HISTORY_CHANGES) {
            truncated = true;
            logs = new ArrayList<>(logs.subList(0, MAX_HISTORY_CHANGES));
        }

        Map<String, BusinessHistoryScope.FileScope> scopeIndex = new LinkedHashMap<>();
        for (BusinessHistoryScope.FileScope fileScope : fileScopes) {
            scopeIndex.put(fileScope.getBizType() + "#" + fileScope.getBizId(), fileScope);
        }
        Set<Long> fileIds = new LinkedHashSet<>();
        for (FileOperationLog fileLog : logs) {
            if (fileLog.getFileId() != null) {
                fileIds.add(fileLog.getFileId());
            }
        }
        Map<Long, FileInfo> fileInfos = loadFileInfos(fileIds);

        for (FileOperationLog fileLog : logs) {
            String bizType = detailValue(fileLog.getDetail(), "bizType");
            String bizId = detailValue(fileLog.getDetail(), "bizId");
            BusinessHistoryScope.FileScope fileScope = scopeIndex.get(bizType + "#" + bizId);
            if (fileScope == null) {
                continue;
            }
            boolean upload = "UPLOAD".equalsIgnoreCase(fileLog.getOperationType());

            SysDataChangeLog entity = new SysDataChangeLog();
            entity.setId(fileLog.getId());
            entity.setTableName(fileScope.getTableName());
            entity.setBusinessId(fileScope.getBizId());
            entity.setChangeType(upload ? "INSERT" : "DELETE");
            entity.setChangeSource(FILE_SOURCE);
            entity.setUserName(fileLog.getUserName());
            entity.setCreatedTime(fileLog.getCreatedTime());

            String before = upload ? "" : fileLog.getFileName();
            String after = upload ? fileLog.getFileName() : "";
            FieldChange fieldChange = new FieldChange(FILE_FIELD, FILE_LABEL, before, after);
            views.add(new ChangeView(entity, List.of(fieldChange), fileLog,
                    fileInfos.get(fileLog.getFileId())));
        }
        return truncated;
    }

    /**
     * 批量加载文件信息，用于展示附件大小
     *
     * @param fileIds 文件ID集合
     * @return 文件ID到文件信息的映射
     */
    private Map<Long, FileInfo> loadFileInfos(Set<Long> fileIds) {
        Map<Long, FileInfo> result = new HashMap<>();
        if (fileIds == null || fileIds.isEmpty()) {
            return result;
        }
        List<FileInfo> infos = fileInfoMapper.selectByIdsIgnoreDeleted(new ArrayList<>(fileIds));
        if (infos == null) {
            return result;
        }
        for (FileInfo info : infos) {
            if (info.getFileId() != null) {
                result.put(info.getFileId(), info);
            }
        }
        return result;
    }

    /**
     * 解析文件日志 detail 中的业务标记
     * <p>
     * detail 形如：bizType=WORK_ORDER, bizId=880, refId=0, kind=BATCH_RECORD
     * </p>
     *
     * @param detail 操作详情
     * @param key    标记名
     * @return 标记值；不存在时返回 null
     */
    private String detailValue(String detail, String key) {
        if (!hasText(detail)) {
            return null;
        }
        String marker = key + "=";
        int start = detail.indexOf(marker);
        if (start < 0) {
            return null;
        }
        start += marker.length();
        int end = detail.indexOf(',', start);
        String value = end < 0 ? detail.substring(start) : detail.substring(start, end);
        value = value.trim();
        return value.isEmpty() ? null : value;
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
     * @param tableFilter  表筛选
     * @param scope        查询范围
     * @return 事件列表（按时间倒序）
     */
    private List<BusinessHistoryEventDto> buildEvents(List<ChangeView> views, String fieldFilter,
                                                      String sourceFilter, String tableFilter,
                                                      BusinessHistoryScope scope) {
        Map<String, EventGroup> groups = new LinkedHashMap<>();
        for (ChangeView view : views) {
            SysDataChangeLog entity = view.entity;
            if (!matchesSource(entity, sourceFilter) || !matchesTable(entity, tableFilter)) {
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
            group.add(view.withFields(matched));
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
        } else if (FILE_SOURCE.equalsIgnoreCase(first.getChangeSource())) {
            event.setOperationType("UPDATE");
            event.setOperationDesc(fileEventDesc(group));
            event.setUserName(first.getUserName());
            event.setUserAccount(first.getUserAccount());
            event.setCreatedTime(group.time);
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
                dto.setDataChangeId(view.fileLog == null ? entity.getId() : null);
                dto.setTableName(entity.getTableName());
                dto.setBusinessId(entity.getBusinessId());
                dto.setRecordLabel(recordLabel);
                dto.setChangeType(entity.getChangeType());
                dto.setChangeSource(entity.getChangeSource());
                dto.setField(change.getField());
                dto.setLabel(hasText(change.getLabel()) ? change.getLabel() : change.getField());
                dto.setBefore(change.getBefore());
                dto.setAfter(change.getAfter());
                if (view.fileLog != null) {
                    fillFileChange(dto, view);
                } else {
                    dto.setChangeKind("FIELD");
                }
                changes.add(dto);
            }
        }
        event.setChanges(changes);
        return event;
    }

    /**
     * 填充附件变更的文件信息
     *
     * @param dto  变更返回对象
     * @param view 变更视图
     */
    private void fillFileChange(BusinessHistoryChangeDto dto, ChangeView view) {
        FileOperationLog fileLog = view.fileLog;
        boolean upload = "UPLOAD".equalsIgnoreCase(fileLog.getOperationType());
        dto.setChangeKind(FILE_SOURCE);
        dto.setFileAction(upload ? "ADD" : "DELETE");
        dto.setFileId(fileLog.getFileId());
        dto.setFileName(fileLog.getFileName());
        dto.setFileKind(detailValue(fileLog.getDetail(), "kind"));
        if (view.fileInfo != null) {
            dto.setFileSize(view.fileInfo.getFileSize());
        }
    }

    /**
     * 附件事件的默认描述
     *
     * @param group 事件分组
     * @return 操作描述
     */
    private String fileEventDesc(EventGroup group) {
        boolean hasUpload = false;
        boolean hasDelete = false;
        for (ChangeView view : group.changes) {
            if (view.fileLog == null) {
                continue;
            }
            if ("UPLOAD".equalsIgnoreCase(view.fileLog.getOperationType())) {
                hasUpload = true;
            } else {
                hasDelete = true;
            }
        }
        if (hasUpload && hasDelete) {
            return "变更工序附件";
        }
        return hasUpload ? "上传工序附件" : "删除工序附件";
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
     * 判断变更是否匹配表筛选
     *
     * @param entity      变更日志
     * @param tableFilter 表筛选
     * @return 是否匹配
     */
    private boolean matchesTable(SysDataChangeLog entity, String tableFilter) {
        if (tableFilter == null) {
            return true;
        }
        return tableFilter.equalsIgnoreCase(entity.getTableName());
    }

    /**
     * 生成事件分组键
     *
     * @param entity 变更日志
     * @return 分组键
     */
    private String groupKey(SysDataChangeLog entity) {
        if (FILE_SOURCE.equalsIgnoreCase(entity.getChangeSource())) {
            return "file:" + entity.getId();
        }
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
     * <p>
     * 附件变更额外携带文件操作日志与文件信息
     * </p>
     */
    private static class ChangeView {

        private final SysDataChangeLog entity;
        private final List<FieldChange> fields;
        private final FileOperationLog fileLog;
        private final FileInfo fileInfo;

        private ChangeView(SysDataChangeLog entity, List<FieldChange> fields) {
            this(entity, fields, null, null);
        }

        private ChangeView(SysDataChangeLog entity, List<FieldChange> fields,
                           FileOperationLog fileLog, FileInfo fileInfo) {
            this.entity = entity;
            this.fields = fields;
            this.fileLog = fileLog;
            this.fileInfo = fileInfo;
        }

        /**
         * 生成字段筛选后的视图，保留附件上下文
         *
         * @param fields 筛选后的字段差异
         * @return 新视图
         */
        private ChangeView withFields(List<FieldChange> fields) {
            return new ChangeView(entity, fields, fileLog, fileInfo);
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

        private void add(ChangeView view) {
            SysDataChangeLog entity = view.entity;
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
            changes.add(view);
        }
    }

    // endregion
}
