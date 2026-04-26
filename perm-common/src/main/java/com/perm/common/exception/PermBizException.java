package com.perm.common.exception;

import lombok.Getter;

/**
 * 权限管理业务异常
 */
@Getter
public class PermBizException extends RuntimeException {

    private final int code;

    public PermBizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public PermBizException(String message) {
        this(400, message);
    }
}
