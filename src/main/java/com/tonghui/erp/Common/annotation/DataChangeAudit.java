package com.tonghui.erp.Common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义 SQL 数据变更审计注解
 * <p>
 * 用于 {@code @Update} / {@code @Delete} 等自定义 Mapper 方法，告诉数据变更拦截器
 * 如何定位受影响的行。普通 MyBatis-Plus 方法无需该注解。
 * </p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataChangeAudit {

    /**
     * 物理表名
     */
    String table();

    /**
     * 自定义 SQL 的 WHERE 列名，如 prod_unit_id
     */
    String whereColumn();

    /**
     * WHERE 列对应的 Mapper 方法参数名，如 prodUnitId
     */
    String param();

    /**
     * 表主键列名，用于定位每条变更，如 stock_id
     */
    String primaryKeyColumn();

    /**
     * 变更类型：UPDATE/DELETE
     */
    String changeType() default "UPDATE";
}
