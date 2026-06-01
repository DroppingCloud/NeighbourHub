package com.community.platform.common;

import lombok.Getter;

/**
 * Unified response codes.
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "success"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "权限不足"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "系统内部错误"),

    PASSWORD_ERROR(1001, "用户名或密码错误"),
    ACCOUNT_NOT_EXISTS(1002, "账号不存在"),
    ACCOUNT_DISABLED(1003, "账号已被禁用"),
    TOKEN_EXPIRED(1004, "Token 已过期"),
    TOKEN_INVALID(1005, "Token 无效"),
    ACCOUNT_EXISTS(1006, "账号已存在"),

    SERVICE_ITEM_NOT_EXISTS(2001, "事项不存在"),
    SERVICE_ITEM_OFFLINE(2002, "事项已下线"),
    CONDITION_NOT_MET(2003, "不满足申请条件"),

    APPLICATION_NOT_EXISTS(2101, "申请单不存在"),
    APPLICATION_STATUS_ERROR(2102, "申请状态不允许该操作"),
    APPLICATION_NO_PERMISSION(2103, "无权操作该申请"),

    WORK_ORDER_NOT_EXISTS(2201, "工单不存在"),
    WORK_ORDER_NO_PERMISSION(2202, "无权处理该工单"),
    WORK_ORDER_STATUS_ERROR(2203, "工单状态不允许该操作"),

    SERVICE_NOT_AVAILABLE(2301, "服务暂不可用"),
    TIME_CONFLICT(2302, "预约时间冲突"),
    BOOKING_NOT_EXISTS(2303, "预约记录不存在"),
    BOOKING_STATUS_ERROR(2304, "预约状态不允许该操作"),
    NO_PERMISSION(403, "无权限访问");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
