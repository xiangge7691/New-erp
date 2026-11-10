package com.tonghui.erp.Common.Constants;

/**
 * 操作日志类型
 * <p>
 * 用于 {@code @OperationLog} 注解标注业务动作，前端按类型展示不同颜色标签
 * </p>
 */
public enum OperationType {

    /** 新增 */
    CREATE,

    /** 修改 */
    UPDATE,

    /** 删除 */
    DELETE,

    /** 审批（提交、通过、驳回） */
    APPROVE,

    /** 导出 */
    EXPORT,

    /** 导入 */
    IMPORT,

    /** 上传 */
    UPLOAD,

    /** 下载 */
    DOWNLOAD,

    /** 启用 */
    ENABLE,

    /** 停用 */
    DISABLE,

    /** 其他 */
    OTHER
}
