package com.tonghui.erp.Common.utils;

import java.util.function.BiFunction;

/**
 * 编号唯一性校验工具类
 * <p>
 * 提供通用的编号唯一性校验逻辑，避免在各 Service 实现中重复编写相同代码。
 * 调用方只需提供实际的计数查询回调（绕过软删除过滤）即可。
 * </p>
 */
public final class CodeUniqueChecker {

    // region 构造函数
    // ===================================
    // 构造函数
    // ===================================

    /** 私有构造函数，防止实例化 */
    private CodeUniqueChecker() {}

    // endregion

    // region 唯一性校验方法
    // ===================================
    // 唯一性校验方法
    // ===================================

    /**
     * 校验编号是否唯一
     * <p>
     * 通过传入的计数查询函数判断指定编号是否已存在（包含已软删除的记录）。
     * 计数函数需要绕过 MyBatis-Plus 的全局软删除过滤，确保检测到所有记录。
     * </p>
     *
     * @param code          待校验的编号
     * @param excludeId     需要排除的记录ID（修改时传入当前记录ID，新增时传null）
     * @param countFunction 实际的计数查询函数，接收(code, excludeId)返回匹配记录数
     * @return 唯一返回true，否则返回false
     */
    public static boolean isCodeUnique(String code, Long excludeId,
                                       BiFunction<String, Long, Long> countFunction) {
        return countFunction.apply(code, excludeId) == 0;
    }

    // endregion
}
