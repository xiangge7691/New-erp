package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志表
 * <p>
 * 记录业务写操作、审批、导入导出等动作的操作人、模块、参数摘要、结果与客户端信息。
 * 日志只增不改，不继承审计基类，也不参与全局软删除。
 * </p>
 *
 * @TableName sys_operation_log
 */
@TableName(value = "sys_operation_log")
@Data
public class SysOperationLog {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 日志ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 操作人ID
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 操作人账号
     */
    @TableField(value = "user_account")
    private String userAccount;

    /**
     * 操作人姓名
     */
    @TableField(value = "user_name")
    private String userName;

    // endregion

    // region 操作信息字段
    // ===================================
    // 操作信息字段
    // ===================================

    /**
     * 业务模块，如 客户管理
     */
    @TableField(value = "module")
    private String module;

    /**
     * 操作类型：CREATE/UPDATE/DELETE/APPROVE/EXPORT/IMPORT/UPLOAD/DOWNLOAD/OTHER
     */
    @TableField(value = "operation_type")
    private String operationType;

    /**
     * 操作描述，如 新增客户
     */
    @TableField(value = "operation_desc")
    private String operationDesc;

    /**
     * 业务对象类型，如 CUSTOMER
     */
    @TableField(value = "business_type")
    private String businessType;

    /**
     * 业务对象ID，便于按单据追溯
     */
    @TableField(value = "business_id")
    private String businessId;

    // endregion

    // region 请求与结果字段
    // ===================================
    // 请求与结果字段
    // ===================================

    /**
     * HTTP方法
     */
    @TableField(value = "request_method")
    private String requestMethod;

    /**
     * 请求地址（含查询串，已截断）
     */
    @TableField(value = "request_url")
    private String requestUrl;

    /**
     * 执行的方法全名
     */
    @TableField(value = "class_method")
    private String classMethod;

    /**
     * 请求参数摘要（脱敏 + 截断）
     */
    @TableField(value = "request_params")
    private String requestParams;

    /**
     * 业务响应码
     */
    @TableField(value = "response_code")
    private Integer responseCode;

    /**
     * 结果：1成功/0失败
     */
    @TableField(value = "result_status")
    private Integer resultStatus;

    /**
     * 失败时的异常信息
     */
    @TableField(value = "error_msg")
    private String errorMsg;

    /**
     * 耗时（毫秒）
     */
    @TableField(value = "cost_time")
    private Integer costTime;

    // endregion

    // region 客户端信息字段
    // ===================================
    // 客户端信息字段
    // ===================================

    /**
     * 客户端IP（IPv4/IPv6原样记录）
     */
    @TableField(value = "client_ip")
    private String clientIp;

    /**
     * IP归属地
     */
    @TableField(value = "ip_location")
    private String ipLocation;

    /**
     * 客户端User-Agent
     */
    @TableField(value = "user_agent")
    private String userAgent;

    /**
     * 操作时间（请求进入时间）
     */
    @TableField(value = "created_time")
    private LocalDateTime createdTime;

    // endregion
}
