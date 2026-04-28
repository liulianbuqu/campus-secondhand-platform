package com.campus.exception;

/**
 * 自定义业务异常类
 * 技术亮点：统一业务异常处理，便于全局捕获和响应
 */
public class BusinessException extends RuntimeException {
    private Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}

