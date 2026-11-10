package com.tonghui.erp.Common.Dto.Log;

import lombok.Data;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

/**
 * 数据变更 Trace 上下文
 * <p>
 * 一次 Controller 写操作创建一个 Trace，同一请求内主表和级联表的所有 Mapper 写入
 * 都记录到同一个 Trace 中；使用 ThreadLocal 保存，业务方法结束后取出并清理
 * </p>
 */
public final class DataChangeTraceContext {

    // region ThreadLocal
    // ===================================
    // ThreadLocal
    // ===================================

    /**
     * 使用栈保存 Trace，兼容极少见的嵌套注解调用
     */
    private static final ThreadLocal<Deque<Trace>> STACK = ThreadLocal.withInitial(ArrayDeque::new);

    // endregion

    // region 构造方法
    // ===================================
    // 构造方法
    // ===================================

    /**
     * 工具类禁止实例化
     */
    private DataChangeTraceContext() {
    }

    // endregion

    // region Trace 生命周期
    // ===================================
    // Trace 生命周期
    // ===================================

    /**
     * 开始一个变更 Trace
     *
     * @return 新建的 Trace
     */
    public static Trace begin() {
        Trace trace = new Trace(UUID.randomUUID().toString().replace("-", ""));
        STACK.get().push(trace);
        return trace;
    }

    /**
     * 获取当前 Trace
     *
     * @return 当前 Trace；没有则返回 null
     */
    public static Trace current() {
        Deque<Trace> stack = STACK.get();
        return stack.isEmpty() ? null : stack.peek();
    }

    /**
     * 结束当前 Trace 并清理 ThreadLocal
     *
     * @return 被结束的 Trace；没有则返回 null
     */
    public static Trace end() {
        Deque<Trace> stack = STACK.get();
        if (stack.isEmpty()) {
            STACK.remove();
            return null;
        }
        Trace trace = stack.pop();
        if (stack.isEmpty()) {
            STACK.remove();
        }
        return trace;
    }

    /**
     * 记录一条变更
     *
     * @param record     变更记录
     * @param maxChanges 单次操作最大变更条数
     */
    public static void record(DataChangeRecord record, int maxChanges) {
        Trace trace = current();
        if (trace == null || record == null) {
            return;
        }
        if (maxChanges > 0 && trace.getChanges().size() >= maxChanges) {
            trace.setTruncated(true);
            return;
        }
        trace.getChanges().add(record);
    }

    // endregion

    // region Trace 对象
    // ===================================
    // Trace 对象
    // ===================================

    /**
     * 一次请求的变更 Trace
     */
    @Data
    public static class Trace {

        /**
         * TraceID
         */
        private final String traceId;

        /**
         * 变更记录
         */
        private List<DataChangeRecord> changes = new ArrayList<>();

        /**
         * 是否因为超过条数上限被截断
         */
        private boolean truncated;

        /**
         * 构造 Trace
         *
         * @param traceId TraceID
         */
        public Trace(String traceId) {
            this.traceId = traceId;
        }
    }

    // endregion
}
