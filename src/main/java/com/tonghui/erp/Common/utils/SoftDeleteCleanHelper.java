package com.tonghui.erp.Common.utils;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.regex.Pattern;

/**
 * 软删除统一清理工具类
 * <p>
 * 解决软删除带来的两类问题：
 * 1. 唯一约束冲突 - 新增时唯一字段与已软删除记录冲突
 * 2. 外键约束冲突 - 物理删除父表时被子表外键阻塞
 * </p>
 * <p>
 * 重要说明：本工具类必须使用原生 SQL 执行物理删除。
 * 全局开启逻辑删除（mybatis-plus logic-delete-field: isDeleted）后，
 * MyBatis-Plus 的 BaseMapper.delete(wrapper) 会被改写为
 * “UPDATE 表 SET is_deleted=1 WHERE is_deleted=0 AND (wrapper条件)”，
 * 若 wrapper 中再指定 is_deleted=1 则条件互斥，永远命中 0 行（无效操作）。
 * 因此这里通过 JdbcTemplate 直接执行原生 DELETE 绕过逻辑删除拦截器。
 * </p>
 */
@Component
public class SoftDeleteCleanHelper {

    // region 常量定义
    // ===================================
    // 常量定义
    // ===================================

    /** 字段名白名单（字母、数字、下划线），防止拼接SQL时注入 */
    private static final Pattern COLUMN_PATTERN = Pattern.compile("^[A-Za-z0-9_]+$");

    // endregion

    // region 依赖注入与构造函数
    // ===================================
    // 依赖注入与构造函数
    // ===================================

    /** JdbcTemplate，用于执行原生物理删除SQL，绕过逻辑删除拦截器 */
    private final JdbcTemplate jdbcTemplate;

    /**
     * 构造函数注入JdbcTemplate
     *
     * @param jdbcTemplate Spring JDBC模板
     */
    public SoftDeleteCleanHelper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // endregion

    // region 清理方法
    // ===================================
    // 清理方法
    // ===================================

    /**
     * 按唯一字段清理已软删除的记录
     * <p>物理删除指定表中 is_deleted=1 且唯一字段匹配的记录</p>
     *
     * @param mapper      Mapper实例
     * @param uniqueField 唯一字段名（数据库列名，如 position_code）
     * @param uniqueValue 唯一字段值
     * @return 删除的记录数
     */
    public <T> int cleanByUniqueField(BaseMapper<T> mapper, String uniqueField, Object uniqueValue) {
        String table = resolveTableName(mapper);
        assertValidColumn(uniqueField);
        return jdbcTemplate.update(
                "DELETE FROM " + table + " WHERE is_deleted = 1 AND " + uniqueField + " = ?", uniqueValue);
    }

    /**
     * 按外键字段清理子表记录
     * <p>物理删除子表中所有引用指定父表ID的记录（不区分软删除状态）</p>
     *
     * @param mapper    子表Mapper实例
     * @param foreignKey 外键字段名（数据库列名）
     * @param parentId   父表ID
     * @return 删除的记录数
     */
    public <T> int cleanChildRecords(BaseMapper<T> mapper, String foreignKey, Object parentId) {
        String table = resolveTableName(mapper);
        assertValidColumn(foreignKey);
        return jdbcTemplate.update(
                "DELETE FROM " + table + " WHERE " + foreignKey + " = ?", parentId);
    }

    // endregion

    // region 内部工具方法
    // ===================================
    // 内部工具方法
    // ===================================

    /**
     * 从Mapper实例解析实体类并获取对应表名
     * <p>Mapper为JDK代理，其实现的Mapper接口继承了 BaseMapper&lt;实体类&gt;，
     * 由此取出泛型实参即为实体类，再通过 MyBatis-Plus 的 TableInfoHelper 获取表名</p>
     *
     * @param mapper Mapper实例
     * @return 数据库表名
     */
    private <T> String resolveTableName(BaseMapper<T> mapper) {
        Class<?> entityClass = resolveEntityClass(mapper);
        TableInfo tableInfo = TableInfoHelper.getTableInfo(entityClass);
        if (tableInfo == null) {
            throw new IllegalStateException("无法获取实体对应的表信息：" + entityClass.getName());
        }
        return tableInfo.getTableName();
    }

    /**
     * 解析Mapper对应的实体类
     *
     * @param mapper Mapper实例
     * @return 实体类Class
     */
    private Class<?> resolveEntityClass(BaseMapper<?> mapper) {
        // 遍历代理对象实现的Mapper接口（如 DepartmentMapper）
        for (Class<?> mapperInterface : mapper.getClass().getInterfaces()) {
            // 在接口的泛型父接口中查找 BaseMapper<实体类>
            for (Type genericInterface : mapperInterface.getGenericInterfaces()) {
                if (genericInterface instanceof ParameterizedType parameterizedType
                        && parameterizedType.getRawType() == BaseMapper.class) {
                    Type argument = parameterizedType.getActualTypeArguments()[0];
                    if (argument instanceof Class<?> entityClass) {
                        return entityClass;
                    }
                }
            }
        }
        throw new IllegalStateException("无法从Mapper解析实体类：" + mapper.getClass().getName());
    }

    /**
     * 校验字段名合法性（白名单），防止SQL注入
     *
     * @param column 字段名
     */
    private void assertValidColumn(String column) {
        if (column == null || !COLUMN_PATTERN.matcher(column).matches()) {
            throw new IllegalArgumentException("非法字段名：" + column);
        }
    }

    // endregion
}
