package com.tonghui.erp.Common.annotation;

import com.tonghui.erp.Common.Constants.OperationType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解
 * <p>
 * 标注在 Controller 的写操作入口方法上，由 OperationLogAspect 记录操作人、
 * 模块、参数摘要、结果、耗时与客户端信息。
 * 注意：注解必须加在通过 Spring 代理调用的入口方法上，同类内部自调用不会生效。
 * </p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /**
     * 业务模块，如 客户管理
     */
    String module();

    /**
     * 操作类型
     */
    OperationType type();

    /**
     * 操作描述，如 新增客户
     */
    String desc() default "";

    /**
     * 业务对象类型，如 CUSTOMER
     */
    String businessType() default "";

    /**
     * 业务对象ID的 SpEL 表达式，如 #id、#request.customerId、#result.data.customerId
     * <p>解析失败时留空，不影响业务</p>
     */
    String businessId() default "";

    /**
     * 是否记录请求参数，默认记录
     */
    boolean recordParams() default true;
}
