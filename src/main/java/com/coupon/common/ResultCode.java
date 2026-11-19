package com.coupon.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通用业务状态码枚举。
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    PARAM_ERROR(400, "参数错误"),
    NOT_FOUND(404, "资源不存在"),
    SERVER_ERROR(500, "服务器内部错误");

    private final Integer code;
    private final String message;
}
