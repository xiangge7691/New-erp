package com.tonghui.erp.Common.interceptor;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.tonghui.erp.Common.Config.LogProperties;
import com.tonghui.erp.Common.Dto.Log.DataChangeRecord;
import com.tonghui.erp.Common.Dto.Log.DataChangeTraceContext;
import com.tonghui.erp.Common.annotation.DataChangeAudit;
import com.tonghui.erp.Common.utils.DataChangeDiffUtils;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.GenericTypeResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 数据变更拦截器
 * <p>
 * 拦截 MyBatis Mapper 的 insert/update/delete 语句，在请求 Trace 内抓取修改前后的整行数据，
 * 计算字段差异并缓存，最终随操作日志异步写入 sys_data_change_log。
 * 采样失败只记录应用日志，绝不影响业务执行。
 * </p>
 */
@Component
@Intercepts({
        @Signature(type = Executor.class, method = "update",
                args = {MappedStatement.class, Object.class})
})
public class DataChangeInterceptor implements Interceptor {

    // region 常量与依赖
    // ===================================
    // 常量与依赖
    // ===================================

    private static final Logger log = LoggerFactory.getLogger(DataChangeInterceptor.class);

    /** 合法表名/列名 */
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[A-Za-z0-9_]+$");

    private final JdbcTemplate jdbcTemplate;
    private final LogProperties logProperties;
    private final DataChangeDiffUtils diffUtils;

    /** 自定义 SQL 审计注解缓存 */
    private final Map<String, Optional<DataChangeAudit>> auditAnnotationCache = new ConcurrentHashMap<>();

    /**
     * 构造数据变更拦截器
     *
     * @param jdbcTemplate  JDBC模板
     * @param logProperties 日志配置
     * @param diffUtils     差异工具
     */
    public DataChangeInterceptor(JdbcTemplate jdbcTemplate,
                                 LogProperties logProperties,
                                 DataChangeDiffUtils diffUtils) {
        this.jdbcTemplate = jdbcTemplate;
        this.logProperties = logProperties;
        this.diffUtils = diffUtils;
    }

    // endregion

    // region 拦截入口
    // ===================================
    // 拦截入口
    // ===================================

    /**
     * 拦截 Mapper 写入
     *
     * @param invocation 调用信息
     * @return 原方法返回值
     * @throws Throwable 原方法异常
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (!logProperties.getChange().isEnabled() || DataChangeTraceContext.current() == null) {
            return invocation.proceed();
        }

        MappedStatement statement = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs()[1];
        Capture capture = null;
        try {
            capture = prepare(statement, parameter, (Executor) invocation.getTarget());
        } catch (Exception e) {
            log.debug("数据变更采集准备失败：{}", e.getMessage(), e);
        }

        Object result = invocation.proceed();

        if (capture != null) {
            try {
                recordChanges(capture, result);
            } catch (Exception e) {
                log.warn("数据变更采集失败：表={}，原因={}", capture.tableName, e.getMessage());
            }
        }
        return result;
    }

    // endregion

    // region 采集准备
    // ===================================
    // 采集准备
    // ===================================

    /**
     * 解析语句并抓取修改前数据
     *
     * @param statement  MappedStatement
     * @param parameter  参数
     * @param executor   Executor
     * @return 采集上下文；不支持或不需采集时返回 null
     */
    private Capture prepare(MappedStatement statement, Object parameter, Executor executor) {
        String statementId = statement.getId();
        int dot = statementId.lastIndexOf('.');
        if (dot <= 0) {
            return null;
        }
        String mapperClassName = statementId.substring(0, dot);
        String methodName = statementId.substring(dot + 1);
        log.debug("数据变更拦截：语句={}，参数类型={}",
                statementId, parameter == null ? null : parameter.getClass().getName());

        Class<?> mapperClass;
        try {
            mapperClass = Class.forName(mapperClassName);
        } catch (ClassNotFoundException e) {
            return null;
        }
        if (!BaseMapper.class.isAssignableFrom(mapperClass)) {
            return null;
        }
        Class<?> entityClass = GenericTypeResolver.resolveTypeArgument(mapperClass, BaseMapper.class);
        if (entityClass == null) {
            return null;
        }
        TableInfo tableInfo = TableInfoHelper.getTableInfo(entityClass);
        if (tableInfo == null || !hasText(tableInfo.getTableName()) || !hasText(tableInfo.getKeyColumn())) {
            return null;
        }
        String tableName = tableInfo.getTableName();
        if (isExcludedTable(tableName)
                || !SAFE_IDENTIFIER.matcher(tableName).matches()
                || !SAFE_IDENTIFIER.matcher(tableInfo.getKeyColumn()).matches()) {
            return null;
        }

        Capture capture = new Capture();
        capture.mapperClassName = mapperClassName;
        capture.methodName = methodName;
        capture.entityClass = entityClass;
        capture.tableName = tableName;
        capture.keyColumn = tableInfo.getKeyColumn();
        capture.keyProperty = tableInfo.getKeyProperty();

        SqlCommandType commandType = statement.getSqlCommandType();

        // 自定义 SQL：优先使用 @DataChangeAudit 指定的表/条件/主键采集
        DataChangeAudit audit = findAuditAnnotation(mapperClass, methodName);
        if (audit != null) {
            return prepareCustom(capture, parameter, audit);
        }

        if (commandType == SqlCommandType.INSERT) {
            capture.insert = true;
            capture.entity = entityOf(parameter);
            return capture;
        }
        // 逻辑删除在 SQL 层会被改写成 UPDATE，必须以方法名优先识别删除
        if (commandType == SqlCommandType.DELETE || isDeleteMethod(methodName)) {
            return prepareDelete(capture, parameter, executor, statement);
        }
        if (commandType == SqlCommandType.UPDATE) {
            return prepareUpdate(capture, parameter, executor, statement);
        }
        return null;
    }

    /**
     * 判断是否为删除方法
     *
     * @param methodName 方法名
     * @return 是否删除方法
     */
    private boolean isDeleteMethod(String methodName) {
        return "delete".equals(methodName)
                || "deleteById".equals(methodName)
                || "deleteBatchIds".equals(methodName)
                || "removeById".equals(methodName);
    }

    /**
     * 查找自定义 SQL 变更审计注解
     *
     * @param mapperClass Mapper接口
     * @param methodName  方法名
     * @return 注解；不存在返回 null
     */
    private DataChangeAudit findAuditAnnotation(Class<?> mapperClass, String methodName) {
        String cacheKey = mapperClass.getName() + "#" + methodName;
        return auditAnnotationCache.computeIfAbsent(cacheKey, key -> {
            for (Method method : mapperClass.getMethods()) {
                if (method.getName().equals(methodName)) {
                    DataChangeAudit audit = method.getAnnotation(DataChangeAudit.class);
                    if (audit != null) {
                        return Optional.of(audit);
                    }
                }
            }
            return Optional.empty();
        }).orElse(null);
    }

    /**
     * 准备自定义 SQL 采集
     * <p>
     * 按注解指定的 WHERE 列先查出受影响行，再按主键抓取修改前后快照
     * </p>
     *
     * @param capture   采集上下文
     * @param parameter 参数
     * @param audit     审计注解
     * @return 采集上下文；无法定位时返回 null
     */
    private Capture prepareCustom(Capture capture, Object parameter, DataChangeAudit audit) {
        String tableName = audit.table();
        if (!hasText(tableName) || isExcludedTable(tableName)
                || !SAFE_IDENTIFIER.matcher(tableName).matches()
                || !SAFE_IDENTIFIER.matcher(audit.whereColumn()).matches()
                || !SAFE_IDENTIFIER.matcher(audit.primaryKeyColumn()).matches()) {
            return null;
        }
        Object whereValue = extract(parameter, audit.param());
        if (whereValue == null) {
            whereValue = extract(parameter, "param1");
        }
        if (whereValue == null) {
            return null;
        }

        capture.custom = true;
        capture.customChangeType = audit.changeType();
        capture.tableName = tableName;
        capture.keyColumn = audit.primaryKeyColumn();

        String sql = "SELECT * FROM `" + tableName + "` WHERE `" + audit.whereColumn() + "` = ?";
        List<Map<String, Object>> rows;
        try {
            rows = jdbcTemplate.queryForList(sql, whereValue);
        } catch (Exception e) {
            log.debug("自定义SQL变更采集查询失败：表={}，原因={}", tableName, e.getMessage());
            return null;
        }
        int max = maxRows();
        capture.truncated = rows.size() > max;
        int limit = Math.min(rows.size(), max);
        for (int i = 0; i < limit; i++) {
            Map<String, Object> row = rows.get(i);
            if (DataChangeDiffUtils.TYPE_DELETE.equalsIgnoreCase(audit.changeType())
                    && diffUtils.isDeletedRow(row)) {
                continue;
            }
            Object id = getIgnoreCase(row, audit.primaryKeyColumn());
            if (id != null) {
                capture.ids.add(id);
                capture.beforeRows.put(id, row);
            }
        }
        return capture.ids.isEmpty() ? null : capture;
    }

    /**
     * 准备 UPDATE 采集
     *
     * @param capture   采集上下文
     * @param parameter 参数
     * @param executor  Executor
     * @param statement MappedStatement
     * @return 采集上下文；不支持时返回 null
     */
    private Capture prepareUpdate(Capture capture, Object parameter, Executor executor,
                                  MappedStatement statement) {
        if ("updateById".equals(capture.methodName)) {
            capture.entity = entityOf(parameter);
            Object id = readProperty(capture.entity, capture.keyProperty);
            if (id == null) {
                return null;
            }
            capture.ids.add(id);
        } else if ("update".equals(capture.methodName)) {
            Object entity = extract(parameter, "et");
            Object wrapper = resolveWrapper(parameter);
            capture.entity = entityOf(entity);
            if (wrapper != null) {
                IdCollectResult idResult = collectIds(capture, wrapper, executor, statement);
                capture.ids.addAll(idResult.ids);
                capture.truncated = idResult.truncated;
            } else if (capture.entity != null) {
                Object id = readProperty(capture.entity, capture.keyProperty);
                if (id != null) {
                    capture.ids.add(id);
                }
            }
        } else {
            // 自定义 UPDATE：尝试从参数中解析主键，解析不到则跳过
            Object id = resolveId(parameter, capture.keyProperty);
            if (id != null) {
                capture.ids.add(id);
            }
        }
        if (capture.ids.isEmpty()) {
            return null;
        }
        loadBeforeRows(capture);
        return capture;
    }

    /**
     * 准备 DELETE 采集
     *
     * @param capture   采集上下文
     * @param parameter 参数
     * @param executor  Executor
     * @param statement MappedStatement
     * @return 采集上下文；不支持时返回 null
     */
    private Capture prepareDelete(Capture capture, Object parameter, Executor executor,
                                  MappedStatement statement) {
        capture.delete = true;
        if ("deleteById".equals(capture.methodName) || "removeById".equals(capture.methodName)) {
            Object id = resolveId(parameter, capture.keyProperty);
            if (id != null) {
                capture.ids.add(id);
            }
        } else if ("deleteBatchIds".equals(capture.methodName)) {
            Object ids = extract(parameter, "coll");
            if (ids == null) {
                ids = extract(parameter, "collection");
            }
            if (ids == null) {
                ids = extract(parameter, "param1");
            }
            if (ids == null) {
                ids = parameter;
            }
            if (ids instanceof Collection<?> collection) {
                capture.ids.addAll(collection);
            }
        } else if ("delete".equals(capture.methodName)) {
            Object wrapper = resolveWrapper(parameter);
            log.debug("数据变更删除：参数类型={}，wrapper类型={}", parameter == null ? null
                    : parameter.getClass().getName(), wrapper == null ? null : wrapper.getClass().getName());
            if (wrapper != null) {
                IdCollectResult idResult = collectIds(capture, wrapper, executor, statement);
                capture.ids.addAll(idResult.ids);
                capture.truncated = idResult.truncated;
            }
        }
        if (capture.ids.isEmpty()) {
            return null;
        }
        loadBeforeRows(capture);
        return capture;
    }

    /**
     * 按主键加载修改前整行
     *
     * @param capture 采集上下文
     */
    private void loadBeforeRows(Capture capture) {
        for (Object id : capture.ids) {
            Map<String, Object> row = selectRow(capture.tableName, capture.keyColumn, id);
            if (row != null) {
                capture.beforeRows.put(id, row);
            }
        }
    }

    // endregion

    // region 变更记录
    // ===================================
    // 变更记录
    // ===================================

    /**
     * 执行完成后抓取修改后数据并生成变更记录
     *
     * @param capture 采集上下文
     * @param result  原方法返回值（影响行数）
     */
    private void recordChanges(Capture capture, Object result) {
        int affected = result instanceof Number number ? number.intValue() : 1;
        if (affected <= 0) {
            return;
        }
        if (capture.insert) {
            recordInsert(capture);
            return;
        }
        if (capture.truncated) {
            recordSummary(capture);
            return;
        }
        DataChangeTraceContext.Trace trace = DataChangeTraceContext.current();
        for (Object id : capture.ids) {
            if (trace != null && trace.getChanges().size() >= maxChanges()) {
                trace.setTruncated(true);
                break;
            }
            Map<String, Object> before = capture.beforeRows.get(id);
            if (before == null) {
                continue;
            }
            Map<String, Object> after = selectRow(capture.tableName, capture.keyColumn, id);
            DataChangeDiffUtils.Result diff;
            if (capture.custom) {
                if (DataChangeDiffUtils.TYPE_DELETE.equalsIgnoreCase(capture.customChangeType)) {
                    diff = diffUtils.delete(capture.tableName, before);
                } else {
                    diff = diffUtils.diff(capture.tableName, before, after);
                }
            } else if (capture.delete) {
                if (after != null && !diffUtils.isDeletedRow(after)) {
                    continue;
                }
                diff = diffUtils.delete(capture.tableName, before);
            } else {
                diff = diffUtils.diff(capture.tableName, before, after);
            }
            if (diff == null) {
                continue;
            }
            DataChangeTraceContext.record(buildRecord(capture, id, diff), maxChanges());
        }
    }

    /**
     * 记录新增
     *
     * @param capture 采集上下文
     */
    private void recordInsert(Capture capture) {
        Object id = readProperty(capture.entity, capture.keyProperty);
        if (id == null) {
            return;
        }
        Map<String, Object> after = selectRow(capture.tableName, capture.keyColumn, id);
        DataChangeDiffUtils.Result diff = diffUtils.insert(capture.tableName, after);
        if (diff != null) {
            DataChangeTraceContext.record(buildRecord(capture, id, diff), maxChanges());
        }
    }

    /**
     * 记录批量操作摘要
     *
     * @param capture 采集上下文
     */
    private void recordSummary(Capture capture) {
        DataChangeTraceContext.Trace trace = DataChangeTraceContext.current();
        if (trace != null) {
            trace.setTruncated(true);
        }
        DataChangeRecord record = new DataChangeRecord();
        record.setTableName(capture.tableName);
        record.setEntityClass(capture.entityClass.getName());
        record.setChangeType(capture.custom && capture.customChangeType != null
                ? capture.customChangeType : DataChangeDiffUtils.TYPE_UPDATE);
        record.setChangeSource("RELATED");
        record.setFieldCount(0);
        record.setAfterJson("{\"_summary\":\"批量更新超过 " + maxRows() + " 行，已跳过逐行快照\"}");
        DataChangeTraceContext.record(record, maxChanges());
    }

    /**
     * 构建变更记录
     *
     * @param capture 采集上下文
     * @param id      主键
     * @param diff    差异结果
     * @return 变更记录
     */
    private DataChangeRecord buildRecord(Capture capture, Object id, DataChangeDiffUtils.Result diff) {
        DataChangeRecord record = new DataChangeRecord();
        record.setTableName(capture.tableName);
        record.setEntityClass(capture.entityClass.getName());
        record.setBusinessId(String.valueOf(id));
        record.setChangeType(diff.getChangeType());
        record.setChangeSource("RELATED");
        record.setBeforeJson(diff.getBeforeJson());
        record.setAfterJson(diff.getAfterJson());
        record.setChangedFields(diff.getChangedFields());
        record.setFieldCount(diff.getFieldCount());
        return record;
    }

    // endregion

    // region 查询辅助
    // ===================================
    // 查询辅助
    // ===================================

    /**
     * 按 wrapper 查询受影响行主键
     *
     * @param capture   采集上下文
     * @param wrapper   wrapper
     * @param executor  Executor
     * @param statement MappedStatement
     * @return 主键集合与是否截断
     */
    private IdCollectResult collectIds(Capture capture, Object wrapper, Executor executor,
                                       MappedStatement statement) {
        IdCollectResult result = new IdCollectResult();
        String selectListId = capture.mapperClassName + ".selectList";
        try {
            if (!statement.getConfiguration().hasStatement(selectListId)) {
                return result;
            }
            MappedStatement selectStatement = statement.getConfiguration().getMappedStatement(selectListId);
            // selectList 的 Mapper 方法使用 @Param("ew")，直接调用 Executor 时需要自行封装参数
            Map<String, Object> selectParam = new HashMap<>();
            selectParam.put("ew", wrapper);
            selectParam.put("param1", wrapper);
            List<?> rows = executor.query(selectStatement, selectParam,
                    RowBounds.DEFAULT, Executor.NO_RESULT_HANDLER);
            int max = maxRows();
            result.truncated = rows.size() > max;
            int limit = Math.min(rows.size(), max);
            Set<Object> ids = new LinkedHashSet<>();
            for (int i = 0; i < limit; i++) {
                Object id = readProperty(rows.get(i), capture.keyProperty);
                if (id != null) {
                    ids.add(id);
                }
            }
            result.ids.addAll(ids);
        } catch (Exception e) {
            log.debug("按 wrapper 采集数据变更失败：表={}，原因={}", capture.tableName, e.getMessage());
        }
        return result;
    }

    /**
     * 按主键查询整行
     *
     * @param tableName 表名
     * @param keyColumn 主键列
     * @param id        主键值
     * @return 整行数据；不存在返回 null
     */
    private Map<String, Object> selectRow(String tableName, String keyColumn, Object id) {
        if (id == null) {
            return null;
        }
        String sql = "SELECT * FROM `" + tableName + "` WHERE `" + keyColumn + "` = ? LIMIT 1";
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, id);
            return rows.isEmpty() ? null : rows.get(0);
        } catch (Exception e) {
            log.debug("查询数据变更快照失败：表={}，主键={}，原因={}", tableName, id, e.getMessage());
            return null;
        }
    }

    // endregion

    // region 参数与属性辅助
    // ===================================
    // 参数与属性辅助
    // ===================================

    /**
     * 从参数中提取实体
     *
     * @param parameter 参数
     * @return 实体
     */
    private Object entityOf(Object parameter) {
        if (parameter instanceof Map<?, ?> map) {
            Object entity = mapGet(map, "et");
            return entity != null ? entity : mapGet(map, "param1");
        }
        return parameter;
    }

    /**
     * 从参数中提取指定键
     *
     * @param parameter 参数
     * @param key       键
     * @return 值
     */
    private Object extract(Object parameter, String key) {
        return parameter instanceof Map<?, ?> map ? mapGet(map, key) : null;
    }

    /**
     * 安全读取 Map 值
     * <p>
     * MyBatis 的 MapperMethod.ParamMap 在 key 不存在时会抛 BindingException，
     * 这里统一做安全读取
     * </p>
     *
     * @param map 参数Map
     * @param key 键
     * @return 值；不存在返回 null
     */
    private Object mapGet(Map<?, ?> map, String key) {
        if (map == null || key == null) {
            return null;
        }
        try {
            return map.containsKey(key) ? map.get(key) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 解析 wrapper 参数
     * <p>
     * MyBatis-Plus 的单参数 Mapper 方法会把 wrapper 包成 ParamMap（key=ew），
     * 双参数方法也使用 ew；这里统一兼容直接传 wrapper 的情况
     * </p>
     *
     * @param parameter 参数
     * @return wrapper；不存在返回 null
     */
    private Object resolveWrapper(Object parameter) {
        Object wrapper = extract(parameter, "ew");
        if (wrapper == null && parameter instanceof Wrapper<?>) {
            wrapper = parameter;
        }
        return wrapper instanceof Wrapper<?> ? wrapper : null;
    }

    /**
     * 解析主键
     *
     * @param parameter   参数
     * @param keyProperty 主键属性
     * @return 主键值；无法解析返回 null
     */
    private Object resolveId(Object parameter, String keyProperty) {
        if (parameter == null) {
            return null;
        }
        if (parameter instanceof Map<?, ?> map) {
            Object id = mapGet(map, keyProperty);
            if (id == null) {
                id = mapGet(map, "id");
            }
            if (id == null) {
                Object entity = mapGet(map, "et");
                id = entity == null ? null : readProperty(entity, keyProperty);
            }
            return id;
        }
        if (parameter instanceof Number || parameter instanceof String) {
            return parameter;
        }
        return readProperty(parameter, keyProperty);
    }

    /**
     * 读取对象属性
     *
     * @param target   对象
     * @param property 属性名
     * @return 属性值
     */
    private Object readProperty(Object target, String property) {
        if (target == null || property == null) {
            return null;
        }
        if (target instanceof Map<?, ?> map) {
            Object value = mapGet(map, property);
            if (value == null) {
                value = mapGet(map, "id");
            }
            return value;
        }
        try {
            return SystemMetaObject.forObject(target).getValue(property);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 忽略大小写读取行数据列
     *
     * @param row    行数据
     * @param column 列名
     * @return 列值
     */
    private Object getIgnoreCase(Map<String, Object> row, String column) {
        if (row == null || column == null) {
            return null;
        }
        if (row.containsKey(column)) {
            return row.get(column);
        }
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(column)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 判断表是否排除
     *
     * @param tableName 表名
     * @return 是否排除
     */
    private boolean isExcludedTable(String tableName) {
        if (tableName == null) {
            return true;
        }
        String lower = tableName.toLowerCase(Locale.ROOT);
        for (String excluded : logProperties.getChange().getExcludeTables()) {
            if (excluded != null && lower.equals(excluded.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return lower.startsWith("sys_") && lower.endsWith("_log");
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

    /**
     * 单次操作最大变更条数
     *
     * @return 最大条数
     */
    private int maxChanges() {
        int configured = logProperties.getChange().getMaxChanges();
        return configured > 0 ? configured : 100;
    }

    /**
     * 批量更新最大行数
     *
     * @return 最大行数
     */
    private int maxRows() {
        int configured = logProperties.getChange().getMaxRows();
        return configured > 0 ? configured : 1000;
    }

    // endregion

    // region 内部对象
    // ===================================
    // 内部对象
    // ===================================

    /**
     * 采集上下文
     */
    private static class Capture {

        private String mapperClassName;
        private String methodName;
        private Class<?> entityClass;
        private String tableName;
        private String keyColumn;
        private String keyProperty;
        private boolean insert;
        private boolean delete;
        private boolean custom;
        private String customChangeType;
        private boolean truncated;
        private Object entity;
        private final List<Object> ids = new ArrayList<>();
        private final Map<Object, Map<String, Object>> beforeRows = new LinkedHashMap<>();
    }

    /**
     * 主键收集结果
     */
    private static class IdCollectResult {

        private final List<Object> ids = new ArrayList<>();
        private boolean truncated;
    }

    // endregion
}
